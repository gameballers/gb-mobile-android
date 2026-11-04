package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnPreDraw
import com.gameball.gameball.inappmessaging.data.WidgetEvents
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.MessageType
import com.gameball.gameball.inappmessaging.runtime.ActivityTracker
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.gameball.gameball.inappmessaging.runtime.MessagePresenter
import com.gameball.gameball.inappmessaging.runtime.PendingPresentation
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage

/**
 * Draws a message into the current Activity's content root.
 *
 * The content root needs no permission. SYSTEM_ALERT_WINDOW / TYPE_APPLICATION_OVERLAY is
 * deliberately not used: drawing over other apps requires a permission we must not ask an
 * integrator's users for.
 */
internal class OverlayPresenter(
    private val tracker: ActivityTracker,
    private val clock: Clock,
    /**
     * Composes the widget URL a game campaign opens, from the open-details text.
     *
     * Null means this build cannot open games — every game campaign is then passed over with a
     * log rather than drawn as an empty card.
     */
    private val widgetUrlBuilder: ((String) -> String)? = null
) : MessagePresenter {

    private var currentView: View? = null
    private var currentActivity: Activity? = null
    private var backCallback: OnBackPressedCallback? = null
    private var autoDismissRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Carried across a re-present so rotation does not log a second impression. It lives here
     * rather than on the view because rotation destroys the view.
     */
    private var presentation: PendingPresentation? = null

    override val isShowing: Boolean
        get() = currentView != null

    override fun present(
        campaign: Campaign,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ): Boolean = try {
        presentInternal(campaign, resolved, callbacks)
    } catch (t: Throwable) {
        // A presenter that throws would take the host's frame down with it. Defer instead.
        IamLog.e("could not present campaign ${campaign.label}", t)
        false
    }

    private fun presentInternal(
        campaign: Campaign,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ): Boolean {
        // Resolved at presentation time and never cached: a handle bound once points at a
        // dead surface after the first rotation, and messages then silently never appear.
        val activity = tracker.currentActivity
        if (activity == null || activity.isFinishing) {
            IamLog.d("no Activity available; campaign ${campaign.label} deferred")
            return false
        }
        if (isShowing) return false

        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return false

        // By kind and id: every game's campaignId is 0, so matching on it handed one game the
        // presentation of another, already marked shown.
        val slot = presentation?.takeIf { it.campaign.key == campaign.key }
            ?: PendingPresentation(campaign)
        presentation = slot

        if (campaign.isWidget) {
            return presentWidget(campaign, activity, root, slot, callbacks)
        }

        val view = when (campaign.messageType) {
            MessageType.SLIDEUP -> SlideupMessageView(activity)
                .also { it.bind(campaign.content, resolved, wrap(slot, callbacks)) }
            MessageType.MODAL -> ModalMessageView(activity)
                .also { it.bind(campaign.content, resolved, wrap(slot, callbacks)) }
            MessageType.FULLSCREEN -> {
                val fullscreen = FullscreenMessageView(activity)
                if (!fullscreen.orientationMatches(campaign.content)) {
                    IamLog.d(
                        "campaign ${campaign.label} wants " +
                            "${campaign.content.orientation}; deferred until rotation"
                    )
                    return false
                }
                fullscreen.also { it.bind(campaign.content, resolved, wrap(slot, callbacks)) }
            }
            MessageType.UNSUPPORTED -> return false
        }

        root.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        currentView = view
        currentActivity = activity

        registerBack(activity, campaign, slot, callbacks)
        animateIn(view, campaign)

        // Insertion only schedules a frame; nothing is on screen until it paints. Counting at
        // insert time books a view of something the customer may never see - and if the app
        // is backgrounded in that instant, frames stop and this correctly never runs.
        view.doOnPreDraw {
            if (!slot.impressionReported) {
                // The presenter keeps its own flag: the service holds a separate
                // PendingPresentation for its own bookkeeping, so relying on that one would
                // never mark this slot and every rotation would book a second impression.
                slot.impressionReported = true
                slot.impressionAtMillis = clock.nowMillis()
                callbacks.onShown()
            }
            // Started on every presentation, but measured from the ORIGINAL impression, so a
            // rotation continues the countdown rather than restarting it.
            startAutoDismiss(campaign, slot, callbacks)
        }
        return true
    }

    /**
     * Mounts a game campaign, hidden, and reveals it only once the widget reports itself drawn.
     *
     * The impression is counted at readiness rather than at the first pre-draw pass, which is the
     * whole point: the surface is attached and painting for seconds before there is anything on
     * it, so counting at pre-draw books a view of a loading skeleton.
     *
     * A readiness timeout takes the message away **without** counting an impression, so no
     * telemetry is logged and the cap is untouched, and tells the service it failed so the game
     * is let go rather than loaded again on every resume; it is offered again at its next trigger.
     */
    private fun presentWidget(
        campaign: Campaign,
        activity: Activity,
        root: ViewGroup,
        slot: PendingPresentation,
        callbacks: PresentationCallbacks
    ): Boolean {
        val widget = campaign.widget ?: return false
        val builder = widgetUrlBuilder ?: run {
            IamLog.w("campaign ${campaign.label} needs the Gameball widget, which this build cannot open; passed over")
            return false
        }

        // Every way out takes the game off the screen first and then tells the service, exactly
        // as back does. Telling the service alone left the game on screen with nothing to close.
        val close = {
            dismissCurrent()
            callbacks.onDismissed()
        }

        val view = WidgetMessageView(activity)
        view.bind(
            campaign,
            onScrimTap = { if (campaign.content.dismissOnScrimTap) close() },
            onClose = close
        )

        root.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        currentView = view
        currentActivity = activity
        // Back is not taken here. Until the game is drawn nothing is on screen, so back belongs
        // to the app; it is registered with the reveal below.

        view.widget.load(
            url = builder(widget.openDetail),
            onReady = {
                if (!view.isAttachedToWindow) {
                    // Its screen went away while it loaded — back, say, which is the app's until
                    // the game is drawn. Nobody can see it, so it goes like a game that never
                    // loaded rather than booking an impression for an empty window.
                    IamLog.w("campaign ${campaign.label} became ready after its screen went away; removed unshown")
                    dismissCurrent()
                    callbacks.onFailed()
                } else {
                    view.reveal()
                    registerBack(activity, campaign, slot, callbacks)
                    if (!slot.impressionReported) {
                        slot.impressionReported = true
                        slot.impressionAtMillis = clock.nowMillis()
                        callbacks.onShown()
                    }
                }
            },
            // Taken away unshown: nothing was ever visible, so nothing is reported and the cap is
            // left alone. Not onDismissed, which is the customer closing something they saw.
            onFailed = {
                IamLog.w("campaign ${campaign.label} never became ready; removed unshown")
                dismissCurrent()
                callbacks.onFailed()
            },
            onCloseRequested = close,
            // A play does not close the game: the customer is still looking at the result.
            onEvent = { raw -> if (WidgetEvents.isGameCompleted(raw)) callbacks.onGameCompleted() }
        )
        return true
    }

    /** Marks engagement on the shared slot so a dismissal after a tap is not double-counted. */
    private fun wrap(slot: PendingPresentation, callbacks: PresentationCallbacks) =
        object : PresentationCallbacks {
            override fun onShown() = callbacks.onShown()
            override fun onTapped(button: MessageButton?) {
                slot.engaged = true
                callbacks.onTapped(button)
            }
            override fun onDismissed() {
                dismissCurrent()
                callbacks.onDismissed()
            }
        }

    /**
     * Modal and fullscreen consume back: it dismisses the message and must not pop the host's
     * route. A slideup deliberately does not - a non-blocking banner has no claim on the
     * gesture, and intercepting it would break navigation for a message the customer is
     * entitled to ignore.
     */
    private fun registerBack(
        activity: Activity,
        campaign: Campaign,
        slot: PendingPresentation,
        callbacks: PresentationCallbacks
    ) {
        if (campaign.messageType == MessageType.SLIDEUP) return

        if (activity is ComponentActivity) {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismissCurrent()
                    callbacks.onDismissed()
                }
            }
            activity.onBackPressedDispatcher.addCallback(callback)
            backCallback = callback
        } else {
            // Fallback for a host Activity that is not a ComponentActivity. Note this stops
            // firing when the host opts into predictive back, which is why the dispatcher
            // path above is preferred.
            currentView?.apply {
                isFocusableInTouchMode = true
                requestFocus()
                if (this is WidgetMessageView) {
                    // A key listener hears back only while this view itself has focus, and a
                    // game's WebView takes focus at the first touch. The game catches back on its
                    // way to whatever inside it has focus.
                    onBack = {
                        dismissCurrent()
                        callbacks.onDismissed()
                    }
                } else {
                    setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK &&
                            event.action == KeyEvent.ACTION_UP
                        ) {
                            dismissCurrent()
                            callbacks.onDismissed()
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }
    }

    /**
     * The timer measures time visible, and it continues from the original impression across a
     * rotation rather than restarting.
     */
    private fun startAutoDismiss(
        campaign: Campaign,
        slot: PendingPresentation,
        callbacks: PresentationCallbacks
    ) {
        val duration = campaign.content.autoDismissMillis ?: return
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        val elapsed = slot.impressionAtMillis?.let { clock.nowMillis() - it } ?: 0L
        val remaining = (duration - elapsed).coerceAtLeast(0L)
        val runnable = Runnable {
            dismissCurrent()
            callbacks.onDismissed()
        }
        autoDismissRunnable = runnable
        handler.postDelayed(runnable, remaining)
    }

    /**
     * Reduce motion drops the duration to zero rather than shortening it: a customer who
     * asked the OS for less movement gets the message immediately.
     */
    private fun animateIn(view: View, campaign: Campaign) {
        val duration = if (animationsDisabled(view)) 0L else when (campaign.messageType) {
            MessageType.MODAL -> MessageMetrics.Motion.MODAL_DURATION_MS
            MessageType.FULLSCREEN -> MessageMetrics.Motion.FULLSCREEN_DURATION_MS
            MessageType.SLIDEUP -> MessageMetrics.Motion.SLIDEUP_DURATION_MS
            MessageType.UNSUPPORTED -> 0L
        }
        if (duration == 0L) {
            view.alpha = 1f
            return
        }
        view.alpha = 0f
        val animator = view.animate().alpha(1f).setDuration(duration)
        if (campaign.messageType == MessageType.MODAL) {
            // A card arriving from an edge would imply a direction the message does not have;
            // 4% is enough to read as arriving.
            view.scaleX = MessageMetrics.Motion.MODAL_SCALE_FROM
            view.scaleY = MessageMetrics.Motion.MODAL_SCALE_FROM
            animator.scaleX(1f).scaleY(1f)
        }
        animator.start()
    }

    @VisibleForTesting
    internal fun animationsDisabled(view: View): Boolean = try {
        Settings.Global.getFloat(
            view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    } catch (t: Throwable) {
        false
    }

    override fun dismissCurrent() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        autoDismissRunnable = null
        backCallback?.remove()
        backCallback = null
        currentView?.let { view ->
            // A WebView left attached keeps a renderer process and this Activity alive.
            (view as? WidgetMessageView)?.destroy()
            // Dismissal removes the overlay on the spot: there is no exit animation on any
            // type, the one exception being a swiped slideup, which the gesture drives.
            (view.parent as? ViewGroup)?.removeView(view)
        }
        currentView = null
        currentActivity = null
        presentation = null
    }

    /**
     * Rotation destroys the Activity and takes the view with it. The message was not dismissed
     * by the user, so it comes back - but re-presenting must not log a second impression, or a
     * single view becomes two and impressions = clicks + dismissals stops holding.
     */
    fun rePresentAfterConfigurationChange(
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ): Boolean {
        val slot = presentation ?: return false
        currentView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentView = null
        backCallback?.remove()
        backCallback = null
        return present(slot.campaign, resolved, callbacks)
    }

    @VisibleForTesting
    internal fun currentPresentation(): PendingPresentation? = presentation
}
