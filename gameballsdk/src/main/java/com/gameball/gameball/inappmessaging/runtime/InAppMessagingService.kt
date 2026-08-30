package com.gameball.gameball.inappmessaging.runtime

import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.artwork.ArtworkState
import com.gameball.gameball.inappmessaging.artwork.stateOf
import com.gameball.gameball.inappmessaging.data.CampaignCache
import com.gameball.gameball.inappmessaging.data.DisplayHistory
import com.gameball.gameball.inappmessaging.data.IamEvent
import com.gameball.gameball.inappmessaging.data.IamEventType
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.SyncOutcome
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.DisplayHistorySnapshot
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.MessageSelector
import com.gameball.gameball.inappmessaging.domain.Personalization
import com.gameball.gameball.inappmessaging.domain.QuietHours
import com.gameball.gameball.inappmessaging.domain.SyncResult
import com.gameball.gameball.inappmessaging.domain.TriggerOccurrence
import com.gameball.gameball.inappmessaging.model.DisplayDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sequences everything: sync, evaluation, deferral and the single pending slot.
 *
 * It owns no display policy. Selection is entirely a pure function in MessageSelector; this
 * class decides only when to ask and what to do with the answer. Keeping that boundary is
 * what lets every selection rule be tested with plain data and no mocking.
 */
internal class InAppMessagingService(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val source: MessageSource,
    private val cache: CampaignCache,
    private val history: DisplayHistory,
    private val artwork: ArtworkPrefetcher,
    private val analytics: MessageAnalytics,
    private val variables: VariableSource,
    private val presenter: MessagePresenter,
    private val sessionState: SessionState,
    private var hooks: HostHooks = HostHooks()
) {

    private companion object {
        /** A dead network must not delay a tap the user is waiting on. */
        const val OUTWARD_FLUSH_BUDGET_MS = 800L

        /**
         * Grace given to the top-priority winner when its artwork is still loading. Falls
         * back to the next priority only after this budget elapses, so a slow CDN cannot
         * silently promote a lower-priority campaign over the marketer's stated preference.
         */
        const val ARTWORK_GRACE_MS = 1_000L
    }

    private var held: SyncResult = SyncResult.EMPTY
    private var neededTokens: Set<String> = emptySet()
    private var pending: PendingPresentation? = null
    private var currentCustomerId: String? = null

    var isStarted: Boolean = false
        private set

    // --- lifecycle ---

    fun start(customerId: String) {
        if (isStarted && currentCustomerId == customerId) {
            IamLog.d("in-app messaging is already running for '$customerId'")
            return
        }
        // A different customer refetches, resets caps and discards the previous customer's
        // cache and stored values, so the host need not stop first.
        if (isStarted && currentCustomerId != customerId) {
            IamLog.d("customer changed to '$customerId'; resetting")
            resetForCustomerChange()
        }
        isStarted = true
        currentCustomerId = customerId
        sessionState.reset()
        analytics.start()
        scope.launch { startSession(customerId) }
    }

    fun stop() {
        if (!isStarted) return
        IamLog.d("stopping in-app messaging")
        presenter.dismissCurrent()
        held = SyncResult.EMPTY
        neededTokens = emptySet()
        pending = null
        artwork.reset()
        sessionState.reset()
        variables.clear()
        // Flush BEFORE dispose, and let the in-flight request finish while guaranteeing
        // nothing new is scheduled.
        scope.launch {
            withTimeoutOrNull(OUTWARD_FLUSH_BUDGET_MS) { analytics.flush() }
            analytics.dispose()
        }
        isStarted = false
        currentCustomerId = null
    }

    private fun resetForCustomerChange() {
        presenter.dismissCurrent()
        held = SyncResult.EMPTY
        neededTokens = emptySet()
        pending = null
        artwork.reset()
        cache.clear()
        history.clear()
        variables.clear()
    }

    // --- session ---

    private suspend fun startSession(customerId: String) {
        // Concurrently: the stored history gates the decision, not the request.
        val syncDeferred = scope.async { source.fetch(customerId) }
        val historyDeferred = scope.async { history.load(customerId) }
        val outcome = syncDeferred.await()
        val snapshot = historyDeferred.await()

        held = when (outcome) {
            is SyncOutcome.Success -> {
                // A successful response replaces the entire cache; it is never merged, and an
                // empty one is still a replacement.
                cache.put(customerId, outcome.rawPayload)
                outcome.result
            }
            // Only a failure falls back. Applying the cache unconditionally reintroduces the
            // race where a slow cache read lands after a fast sync and clobbers fresher data.
            is SyncOutcome.Failure -> {
                IamLog.w("sync failed (${outcome.reason}); falling back to the cache")
                cache.get(customerId) ?: SyncResult.EMPTY
            }
        }

        // Order matters: warm artwork, then declare personalisation needs, then evaluate.
        artwork.warm(
            held.campaigns.flatMapTo(HashSet()) {
                listOfNotNull(it.content.imageUrl, it.content.iconUrl)
            }
        )
        neededTokens = held.campaigns.flatMapTo(HashSet()) { campaign ->
            Personalization.tokenNames(
                campaign.content.header,
                campaign.content.body,
                *campaign.content.buttons.map { it.text }.toTypedArray()
            )
        }

        evaluate(TriggerOccurrence.SessionStart, snapshot)
    }

    fun onAppForegrounded() {
        if (!isStarted) return
        val customerId = currentCustomerId ?: return
        if (sessionState.onForegrounded()) {
            IamLog.d("returned after the session timeout; starting a new session")
            scope.launch { startSession(customerId) }
        }
    }

    fun onAppBackgrounded() {
        if (!isStarted) return
        sessionState.onBackgrounded()
        // The last point the OS reliably gives us: an app killed from the background never
        // resumes, so anything buffered would wait for the next launch.
        scope.launch { analytics.flush() }
    }

    // --- triggers ---

    fun onEvent(name: String, metadata: Map<String, Any?>) {
        if (!isStarted) return
        val customerId = currentCustomerId ?: return
        // Dropped on every event and purchase, before evaluating: the campaign this exists
        // for is "you just earned 200 points, you now have X", and a value cached before the
        // event quotes the number from before the change. Not dropped on session start.
        variables.invalidate()
        scope.launch {
            evaluate(TriggerOccurrence.Event(name, metadata), history.load(customerId))
        }
    }

    fun onSurfaceAvailable() {
        if (!isStarted) return
        // Rotation is the canonical case: the presenter is still carrying a view whose Activity
        // was destroyed under it, and retryPending would treat isShowing as authoritative and
        // leave the customer staring at a screen that used to have a message on it.
        if (rePresentIfOrphaned()) return
        retryPending()
    }

    private fun rePresentIfOrphaned(): Boolean {
        if (!presenter.isOrphaned) return false
        val campaign = presenter.currentCampaign ?: return false
        // The presenter's own presentation slot has impressionReported=true from the pre-rotation
        // paint, so the second onShown is suppressed internally. The service-side slot must
        // mirror that or a dismiss after rotation would race the "shown and ignored" check with
        // an impression that never fired on this slot.
        val slot = pending?.takeIf { it.campaign.campaignId == campaign.campaignId }
            ?: PendingPresentation(campaign, impressionReported = true)
        scope.launch {
            val resolved = resolve(campaign)
            presenter.rePresent(resolved, callbacksFor(slot))
        }
        return true
    }

    fun setHooks(hooks: HostHooks) {
        this.hooks = hooks
    }

    // --- evaluation ---

    private fun evaluate(
        occurrence: TriggerOccurrence,
        snapshot: DisplayHistorySnapshot,
        exclude: Set<Int> = emptySet()
    ) {
        // Before the early return: a session where everything failed is exactly the one that
        // has to recover.
        artwork.retryFailedIfDue(clock.nowMillis())

        if (held.campaigns.isEmpty()) return

        val pool = if (exclude.isEmpty()) held.campaigns
            else held.campaigns.filterNot { it.campaignId in exclude }

        val winner = MessageSelector.select(
            occurrence = occurrence,
            campaigns = pool,
            history = snapshot,
            nowMillis = clock.nowMillis(),
            cooldownSeconds = held.cooldownSeconds,
            quietHours = held.quietHours,
            // LOADING is eligible; the grace below gives the winner a chance to land before
            // the service falls through to a lower priority. FAILED alone drops a campaign.
            isArtworkReady = { artwork.stateOf(it.content) != ArtworkState.FAILED }
        ) ?: return

        // Every message selected is observed, whatever happens to it next — including one a
        // hook then defers or discards. It is an observer, not a display notification.
        hooks.observer?.invoke(winner)

        when (hooks.beforeDisplay(winner)) {
            DisplayDecision.LATER -> {
                defer(winner, "the host asked to defer it")
                return
            }
            DisplayDecision.DISCARD -> {
                IamLog.d("campaign ${winner.campaignId} discarded by the host")
                return
            }
            DisplayDecision.SHOW -> Unit
        }

        scope.launch {
            if (artwork.stateOf(winner.content) == ArtworkState.LOADING) {
                if (!waitForArtwork(winner)) {
                    IamLog.d(
                        "campaign ${winner.campaignId} artwork did not land in " +
                            "${ARTWORK_GRACE_MS}ms; re-selecting without it"
                    )
                    evaluate(occurrence, snapshot, exclude + winner.campaignId)
                    return@launch
                }
            }
            presentOrDefer(PendingPresentation(winner))
        }
    }

    /**
     * Sequential awaits share the budget - a URL that lands in 200ms leaves the icon 800ms.
     * The verdict at the end is the aggregate state, not either await's return: an icon that
     * completes after the budget still updates the ready set, and the check reflects it.
     */
    private suspend fun waitForArtwork(campaign: Campaign): Boolean {
        val start = clock.nowMillis()
        campaign.content.imageUrl?.takeIf { it.isNotBlank() }?.let {
            artwork.awaitReady(it, ARTWORK_GRACE_MS)
        }
        val elapsed = clock.nowMillis() - start
        val remaining = (ARTWORK_GRACE_MS - elapsed).coerceAtLeast(0L)
        campaign.content.iconUrl?.takeIf { it.isNotBlank() }?.let {
            artwork.awaitReady(it, remaining)
        }
        return artwork.stateOf(campaign.content) == ArtworkState.READY
    }

    private suspend fun presentOrDefer(slot: PendingPresentation) {
        if (presenter.isShowing) {
            defer(slot.campaign, "another message is already showing")
            return
        }
        val resolved = resolve(slot.campaign)
        if (!presenter.present(slot.campaign, resolved, callbacksFor(slot))) {
            defer(slot.campaign, "no surface was available")
            return
        }
        pending = slot
    }

    /**
     * One pending slot, not a queue. A newer deferral displaces an older one, with a log
     * naming both. The slot is in-memory only and dies with the process, deliberately.
     */
    private fun defer(campaign: Campaign, reason: String) {
        val existing = pending
        if (existing != null && existing.campaign.campaignId != campaign.campaignId) {
            IamLog.d(
                "campaign ${campaign.campaignId} displaces campaign " +
                    "${existing.campaign.campaignId} in the pending slot ($reason)"
            )
        } else {
            IamLog.d("campaign ${campaign.campaignId} deferred: $reason")
        }
        pending = PendingPresentation(campaign)
    }

    private fun retryPending() {
        val slot = pending ?: return
        if (presenter.isShowing) return
        val customerId = currentCustomerId ?: return

        // "May this display now", not "has it ever displayed". The cruder question threw away
        // every repeatable campaign that happened to be waiting.
        val eligible = MessageSelector.mayDisplayNow(
            campaign = slot.campaign,
            history = history.load(customerId),
            nowMillis = clock.nowMillis(),
            cooldownSeconds = held.cooldownSeconds,
            quietHours = held.quietHours,
            isArtworkReady = { artwork.stateOf(it.content) != ArtworkState.FAILED }
        )
        if (!eligible) {
            IamLog.d("pending campaign ${slot.campaign.campaignId} is no longer eligible; dropped")
            pending = null
            return
        }
        pending = null
        scope.launch {
            if (artwork.stateOf(slot.campaign.content) == ArtworkState.LOADING) {
                if (!waitForArtwork(slot.campaign)) {
                    IamLog.d(
                        "pending campaign ${slot.campaign.campaignId} artwork did not land " +
                            "in ${ARTWORK_GRACE_MS}ms; dropped"
                    )
                    return@launch
                }
            }
            presentOrDefer(slot)
        }
    }

    // --- personalisation ---

    private suspend fun resolve(campaign: Campaign): ResolvedMessage {
        val content = campaign.content
        val customerId = currentCustomerId
        val carriesToken = Personalization.hasToken(content.header) ||
            Personalization.hasToken(content.body) ||
            content.buttons.any { Personalization.hasToken(it.text) }

        // A message with no token costs one character comparison and never calls the endpoint.
        val values = if (carriesToken && customerId != null) {
            variables.values(customerId, neededTokens)
        } else {
            emptyMap()
        }

        // The blanking pass runs on every path, including the one where substitution never
        // ran at all, which is the only way to guarantee a raw template never reaches a screen.
        return ResolvedMessage(
            header = Personalization.blankUnresolved(
                Personalization.substitute(content.header, values)
            ),
            body = Personalization.blankUnresolved(
                Personalization.substitute(content.body, values)
            ),
            buttons = content.buttons.map { button ->
                button.copy(
                    text = Personalization.blankUnresolved(
                        Personalization.substitute(button.text, values)
                    ).orEmpty()
                )
            }
        )
    }

    // --- reporting ---

    private fun callbacksFor(slot: PendingPresentation) = object : PresentationCallbacks {

        override fun onShown() {
            // Recorded here, at the first painted frame, so a message dismissed before it
            // paints does not burn its slot, and so the auto-dismiss timer measures time
            // visible rather than time since insertion.
            if (slot.impressionReported) return
            slot.impressionReported = true
            val at = clock.nowMillis()
            slot.impressionAtMillis = at
            currentCustomerId?.let { history.recordImpression(it, slot.campaign.campaignId, at) }
            report(slot, IamEventType.IMPRESSION)
        }

        override fun onTapped(button: MessageButton?) {
            slot.engaged = true
            val action = button?.action ?: slot.campaign.content.clickAction ?: return

            // The click is reported regardless of what the host's hook returns: a host that
            // intercepts every tap must not thereby erase its own click analytics.
            report(
                slot, IamEventType.CLICK,
                buttonId = button?.id,
                url = (action as? MessageAction.OpenUrl)?.url
            )

            val handled = hooks.onAction(slot.campaign, button, action)

            // Dismiss before performing: a navigate starts a transition, and leaving the
            // message up during it briefly covers the screen the user just asked for.
            presenter.dismissCurrent()
            pending = null

            if (!handled) scope.launch { perform(action) }
        }

        override fun onDismissed() {
            // A dismissal without an impression is nonsense, and the dismissal that follows a
            // tap must not also count as "shown and ignored".
            if (slot.impressionReported && !slot.engaged) report(slot, IamEventType.DISMISS)
            pending = null
            retryPending()
        }
    }

    private fun report(
        slot: PendingPresentation,
        type: IamEventType,
        buttonId: String? = null,
        url: String? = null
    ) {
        // An isTest campaign displays normally so a marketer can see their work; its
        // telemetry must never reach campaign statistics.
        if (slot.campaign.isTest) return
        val customerId = currentCustomerId ?: return
        analytics.record(
            IamEvent(
                eventUid = IamEvent.newUid(),
                customerId = customerId,
                dispatchId = slot.campaign.dispatchId,
                campaignId = slot.campaign.campaignId,
                variationId = slot.campaign.variationId,
                type = type,
                occurredAtMillis = clock.nowMillis(),
                buttonId = buttonId,
                url = url
            )
        )
    }

    private suspend fun perform(action: MessageAction) {
        // Bounded: the events are on disk either way, so the worst case is next launch, and a
        // dead network must not delay a tap the user is waiting on.
        if (action is MessageAction.OpenUrl || action is MessageAction.Navigate) {
            withTimeoutOrNull(OUTWARD_FLUSH_BUDGET_MS) { analytics.flush() }
        }
        when (action) {
            is MessageAction.Navigate -> {
                val navigate = hooks.onNavigate
                if (navigate == null) {
                    IamLog.w("campaign wants route '${action.route}' but no navigator is set")
                } else {
                    navigate(action.route, action.arguments)
                }
            }
            is MessageAction.OpenUrl -> IamLog.d("open_url is handled by the presenter layer")
            is MessageAction.Dismiss -> Unit
            is MessageAction.Unsupported ->
                IamLog.w("action type '${action.type}' is not implemented")
        }
    }
}
