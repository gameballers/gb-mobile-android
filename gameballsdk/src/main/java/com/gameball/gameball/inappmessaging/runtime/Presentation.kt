package com.gameball.gameball.inappmessaging.runtime

import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.model.DisplayDecision

/**
 * The drawing seam.
 *
 * It lives here rather than with the views because the service is its only caller, and a fake
 * implementation is what lets the whole sequencing layer be tested without a screen.
 */
internal interface MessagePresenter {
    /**
     * Returns false when it cannot draw right now — no Activity, one already showing, or a
     * fullscreen whose orientation does not match — so the service defers. Never throws.
     */
    fun present(
        campaign: Campaign,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ): Boolean

    fun dismissCurrent()

    val isShowing: Boolean
}

internal interface PresentationCallbacks {
    /** Fired on the first pre-draw pass, not at attachment. */
    fun onShown()

    /** [button] is null when the message surface itself was tapped. */
    fun onTapped(button: MessageButton?)

    fun onDismissed()
}

/** A campaign's copy after personalisation and the blanking pass. */
internal data class ResolvedMessage(
    val header: String?,
    val body: String?,
    /** Labels already substituted and blanked. */
    val buttons: List<MessageButton>
)

/**
 * One matched message being held or shown.
 *
 * The impression flag lives here, on the presentation, rather than on the view: rotation
 * destroys the view and re-presents, and a flag carried on the view would log a second
 * impression for a single customer view. Neither Flutter nor iOS faced this — an overlay
 * entry and a UIWindow both survive rotation — so it is a decision only Android has to make.
 */
internal data class PendingPresentation(
    val campaign: Campaign,
    var impressionReported: Boolean = false,
    var impressionAtMillis: Long? = null,
    var engaged: Boolean = false
)

/**
 * The host's four hooks, already unwrapped from their public interfaces and each already
 * wrapped in its own try/catch by the facade — so nothing in the service has to defend
 * against a throwing host, and a buggy host loses its override rather than its messages.
 */
internal data class HostHooks(
    val beforeDisplay: (Campaign) -> DisplayDecision = { DisplayDecision.SHOW },
    val onAction: (Campaign, MessageButton?, MessageAction) -> Boolean = { _, _, _ -> false },
    val onNavigate: ((String, Map<String, Any?>?) -> Unit)? = null,
    val observer: ((Campaign) -> Unit)? = null
)
