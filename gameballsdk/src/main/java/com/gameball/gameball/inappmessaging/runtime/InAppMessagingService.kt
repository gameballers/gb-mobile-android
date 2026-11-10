package com.gameball.gameball.inappmessaging.runtime

import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.artwork.isReady
import com.gameball.gameball.inappmessaging.data.CampaignCache
import com.gameball.gameball.inappmessaging.data.DisplayHistory
import com.gameball.gameball.inappmessaging.data.IamEvent
import com.gameball.gameball.inappmessaging.data.IamEventType
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.SyncOutcome
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.CampaignKey
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
import kotlinx.coroutines.delay
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
    private var hooks: HostHooks = HostHooks(),
    /** Asked whenever a message is about to show: nothing is drawn over the host's widget. */
    private val isWidgetOpen: () -> Boolean = { false },
    /**
     * Runs [block] after a delay and returns what cancels it. The waiting slot uses it to try
     * again the moment the display floor lapses; tests pass one they run by hand.
     */
    private val schedule: (delayMillis: Long, block: () -> Unit) -> (() -> Unit) =
        { delayMillis, block ->
            val job = scope.launch {
                delay(delayMillis)
                block()
            }
            val cancel: () -> Unit = { job.cancel() }
            cancel
        }
) {

    private companion object {
        /** A dead network must not delay a tap the user is waiting on. */
        const val OUTWARD_FLUSH_BUDGET_MS = 800L
    }

    private var held: SyncResult = SyncResult.EMPTY
    private var neededTokens: Set<String> = emptySet()

    /**
     * The campaign waiting for the screen, never the one on it. Holding the one on screen here
     * too meant closing it emptied the slot, and whatever waited behind it was lost.
     */
    private var pending: PendingPresentation? = null

    /** Cancels the retry booked for when the display floor lapses. */
    private var floorRetry: (() -> Unit)? = null

    private var currentCustomerId: String? = null

    /**
     * Games played to their end since the last successful sync, which are not offered again
     * until the next one: the backend decides whether a customer may play again, and it says so
     * only in a sync.
     */
    private val playedSinceSync = HashSet<CampaignKey>()

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
        cancelFloorRetry()
        playedSinceSync.clear()
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
        cancelFloorRetry()
        playedSinceSync.clear()
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
                // The backend has now had its say on every game played since the last sync.
                playedSinceSync.clear()
                outcome.result
            }
            // Only a failure falls back. Applying the cache unconditionally reintroduces the
            // race where a slow cache read lands after a fast sync and clobbers fresher data.
            is SyncOutcome.Failure -> {
                IamLog.w("sync failed (${outcome.reason}); falling back to the cache")
                dropCachedGames(cache.get(customerId) ?: SyncResult.EMPTY)
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
        retryPending()
    }

    /** The host's widget closed, however it was closed: whatever waited for it may show now. */
    fun onWidgetClosed() {
        retryPending()
    }

    fun setHooks(hooks: HostHooks) {
        this.hooks = hooks
    }

    // --- evaluation ---

    private fun evaluate(occurrence: TriggerOccurrence, snapshot: DisplayHistorySnapshot) {
        // Before the early return: a session where everything failed is exactly the one that
        // has to recover.
        artwork.retryFailedIfDue(clock.nowMillis())

        if (held.campaigns.isEmpty()) return

        val candidates = offerable()
        val winner = MessageSelector.select(
            occurrence = occurrence,
            campaigns = candidates,
            history = snapshot,
            nowMillis = clock.nowMillis(),
            cooldownSeconds = held.cooldownSeconds,
            quietHours = held.quietHours,
            isArtworkReady = { artwork.isReady(it.content) }
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
                IamLog.d("campaign ${winner.label} discarded by the host")
                return
            }
            DisplayDecision.SHOW -> Unit
        }

        scope.launch {
            if (presentOrDefer(PendingPresentation(winner))) {
                deferRunnerUp(occurrence, candidates, snapshot, winner)
            }
        }
    }

    /**
     * Keeps the next-best campaign for this occurrence waiting behind the one that took the screen.
     *
     * Without it a trigger that matched two campaigns showed one and forgot the other: the game
     * behind a higher-priority message, or the second of two games at the same priority, never
     * opened. Judged on the history from before the winner displayed, so the winner's own
     * impression cannot rule it out; the floor that impression starts is waited out at retry.
     */
    private fun deferRunnerUp(
        occurrence: TriggerOccurrence,
        candidates: List<Campaign>,
        snapshot: DisplayHistorySnapshot,
        winner: Campaign
    ) {
        if (!presenter.isShowing || pending != null) return
        val runnerUp = MessageSelector.select(
            occurrence = occurrence,
            // By kind and id: every game's campaignId is 0, so the number alone would rule out
            // every other game along with the winner.
            campaigns = candidates.filter { it.key != winner.key },
            history = snapshot,
            nowMillis = clock.nowMillis(),
            cooldownSeconds = held.cooldownSeconds,
            quietHours = held.quietHours,
            isArtworkReady = { artwork.isReady(it.content) }
        ) ?: return
        defer(runnerUp, "campaign ${winner.label} took this trigger first")
    }

    /** The latest sync's campaigns, less any game played since that sync arrived. */
    private fun offerable(): List<Campaign> =
        if (playedSinceSync.isEmpty()) held.campaigns
        else held.campaigns.filterNot { it.key in playedSinceSync }

    /** True when the campaign took the screen; false when it was deferred instead. */
    private suspend fun presentOrDefer(slot: PendingPresentation): Boolean {
        if (isWidgetOpen()) {
            defer(slot.campaign, "the host's widget is open")
            return false
        }
        if (presenter.isShowing) {
            defer(slot.campaign, "another message is already showing")
            return false
        }
        val resolved = resolve(slot.campaign)
        if (!presenter.present(slot.campaign, resolved, callbacksFor(slot))) {
            defer(slot.campaign, "no surface was available")
            return false
        }
        return true
    }

    /**
     * One pending slot, not a queue. A newer deferral displaces an older one, with a log
     * naming both. The slot is in-memory only and dies with the process, deliberately.
     */
    private fun defer(campaign: Campaign, reason: String) {
        val existing = pending
        // By kind and id: every game's campaignId is 0, so the number alone would make every
        // game the same campaign.
        if (existing != null && existing.campaign.key != campaign.key) {
            IamLog.d(
                "campaign ${campaign.label} displaces campaign " +
                    "${existing.campaign.label} in the pending slot ($reason)"
            )
        } else {
            IamLog.d("campaign ${campaign.label} deferred: $reason")
        }
        pending = PendingPresentation(campaign)
    }

    private fun retryPending() {
        val waiting = pending ?: return
        if (presenter.isShowing || isWidgetOpen()) return
        val customerId = currentCustomerId ?: return

        // It may have waited across a sync, so what it holds is an older response's promise. Ask
        // the latest one, by kind and id: a campaign it no longer offers — ended, paused, or a
        // game played since — is dropped, and one it still offers is judged on its latest terms,
        // its expiry included.
        val campaign = offerable().firstOrNull { it.key == waiting.campaign.key }
        if (campaign == null) {
            IamLog.d("pending campaign ${waiting.campaign.label} is not in the latest sync; dropped")
            pending = null
            return
        }

        // "May this display now", not "has it ever displayed". The cruder question threw away
        // every repeatable campaign that happened to be waiting. The floor is left out here and
        // waited out below: it is usually the one the campaign ahead of this one just started.
        val snapshot = history.load(customerId)
        val now = clock.nowMillis()
        val eligible = MessageSelector.mayDisplayNow(
            campaign = campaign,
            history = snapshot,
            nowMillis = now,
            cooldownSeconds = 0,
            quietHours = held.quietHours,
            isArtworkReady = { artwork.isReady(it.content) }
        )
        if (!eligible) {
            IamLog.d("pending campaign ${campaign.label} is no longer eligible; dropped")
            pending = null
            return
        }
        val floorLeft = MessageSelector.floorRemainingMillis(snapshot, now, held.cooldownSeconds)
        if (floorLeft > 0) {
            IamLog.d("pending campaign ${campaign.label} waits ${floorLeft}ms for the display floor")
            cancelFloorRetry()
            floorRetry = schedule(floorLeft) {
                floorRetry = null
                retryPending()
            }
            return
        }
        pending = null
        scope.launch { presentOrDefer(PendingPresentation(campaign)) }
    }

    private fun cancelFloorRetry() {
        floorRetry?.invoke()
        floorRetry = null
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
            currentCustomerId?.let { history.recordImpression(it, slot.campaign.key, at) }
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

            if (!handled) scope.launch { perform(action) }
            // Whatever waited behind this message keeps its place. After a button that leaves
            // the screen it waits for the next one, whose resume retries it, rather than being
            // drawn on the screen being left.
            if (action !is MessageAction.Navigate && action !is MessageAction.OpenUrl) retryPending()
        }

        override fun onDismissed() {
            // A dismissal without an impression is nonsense, and the dismissal that follows a
            // tap must not also count as "shown and ignored".
            if (slot.impressionReported && !slot.engaged) report(slot, IamEventType.DISMISS)
            retryPending()
        }

        override fun onFailed() {
            // Never seen: nothing to report, nothing recorded. Let go rather than kept waiting,
            // or every Activity resume would load it again; its next trigger may offer it again.
            // The waiting slot never holds the presentation on screen, so a campaign deferred
            // behind this one keeps its place and gets its turn now.
            IamLog.d("campaign ${slot.campaign.label} could not be shown; waiting for its next trigger")
            retryPending()
        }

        override fun onGameCompleted() {
            // Matched by the open presentation, never by the event's own ids: the widget names
            // the game in its own terms, and this is the only game that is open.
            IamLog.d("game ${slot.campaign.label} was played; not offered again until the next sync")
            playedSinceSync.add(slot.campaign.key)
        }
    }

    /**
     * Drops game campaigns from a cached set.
     *
     * A message carries its own content, so a cached one is still correct to draw. A game carries
     * only a pointer and fetches its content live: the campaign may have ended, been paused, or
     * run out of rewards since it was cached, and the customer would open it to an error. This
     * narrows the exposure to one foreground session rather than eliminating it.
     */
    private fun dropCachedGames(cached: SyncResult): SyncResult {
        val openable = cached.campaigns.filterNot { it.isWidget }
        val dropped = cached.campaigns.size - openable.size
        if (dropped == 0) return cached
        IamLog.w(
            "$dropped cached game campaign(s) skipped: a game's content is fetched live and a " +
                "cached entry may no longer be open"
        )
        return cached.copy(campaigns = openable)
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
        // A game has no message analytics to report against: its campaignId is always 0, so the
        // default path would file every impression, click and dismiss under campaign zero. The
        // game reports its own engagement from inside the widget. This does NOT suppress the
        // display history — that still records, or a once-ever game would reappear every session.
        if (slot.campaign.isWidget) return
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
