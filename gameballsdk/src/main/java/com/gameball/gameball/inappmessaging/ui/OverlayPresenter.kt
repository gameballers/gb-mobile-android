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
 * That is what Braze Android does and it needs no permission. SYSTEM_ALERT_WINDOW /
 * TYPE_APPLICATION_OVERLAY is deliberately not used: drawing over other apps requires a
 * permission we must not ask an integrator's users for.
 */
internal class OverlayPresenter(
    private val tracker: ActivityTracker,
    private val clock: Clock
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

    /**
     * The window-attached check catches the case that isShowing cannot: after rotation the
     * view reference is still non-null (dismissCurrent has not run), but its window is gone.
     * That is what tells the service the presenter is holding an orphan and needs to
     * re-present against the new Activity.
     */
    override val isOrphaned: Boolean
        get() {
            val view = currentView ?: return false
            return view.windowToken == null || currentActivity !== tracker.currentActivity
        }

    override val currentCampaign: Campaign?
        get() = presentation?.campaign

    override fun present(
        campaign: Campaign,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ): Boolean = try {
        presentInternal(campaign, resolved, callbacks)
    } catch (t: Throwable) {
        // A presenter that throws would take the host's frame down with it. Defer instead.
        IamLog.e("could not present campaign ${campaign.campaignId}", t)
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
            IamLog.d("no Activity available; campaign ${campaign.campaignId} deferred")
            return false
        }
        if (isShowing) return false

        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return false

        val slot = presentation?.takeIf { it.campaign.campaignId == campaign.campaignId }
            ?: PendingPresentation(campaign)
        presentation = slot

        val view = when (campaign.messageType) {
            MessageType.SLIDEUP -> SlideupMessageView(activity)
                .also { it.bind(campaign.content, resolved, wrap(slot, callbacks)) }
            MessageType.MODAL -> ModalMessageView(activity)
                .also { it.bind(campaign.content, resolved, wrap(slot, callbacks)) }
            MessageType.FULLSCREEN -> {
                val fullscreen = FullscreenMessageView(activity)
                if (!fullscreen.orientationMatches(campaign.content)) {
                    IamLog.d(
                        "campaign ${campaign.campaignId} wants " +
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
        // The slideup owns a direction the modal does not, so it translates rather than fades
        // and the view knows its own edge.
        if (campaign.messageType == MessageType.SLIDEUP && view is SlideupMessageView) {
            view.animateEnter(duration)
            return
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
     * single view becomes two and impressions = clicks + dismissals stops holding. The internal
     * presentation slot's impressionReported flag survives the config change and suppresses the
     * second onShown; the service passes fresh callbacks bound to a slot that also treats the
     * impression as booked, so a subsequent dismiss reports without a phantom re-impression.
     */
    override fun rePresent(
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
