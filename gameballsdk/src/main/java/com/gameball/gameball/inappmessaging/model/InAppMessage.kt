package com.gameball.gameball.inappmessaging.model

/**
 * A message the SDK has selected, as handed to a host hook.
 *
 * A deliberate mirror of the internal model rather than the model itself: everything inside
 * the module is `internal`, and Kotlin forbids an internal type in a public signature. Keeping
 * this small also means the internal model can change without breaking hosts.
 */
data class InAppMessage(
    val campaignId: Int,
    val variationId: Int?,
    /** The campaign's dashboard name. For logs and debug UI; never key behaviour on it. */
    val name: String?,
    /** 1 slideup, 2 modal, 3 fullscreen. */
    val messageType: Int,
    val header: String?,
    val body: String?,
    val buttons: List<InAppMessageButton>,
    /** A test send. It displays normally and reports no analytics. */
    val isTest: Boolean
)

data class InAppMessageButton(
    val id: String,
    val text: String
)

/** What tapping a message or one of its buttons is meant to do. */
sealed class GameballMessageAction {
    object Dismiss : GameballMessageAction()

    data class OpenUrl(val url: String, val external: Boolean) : GameballMessageAction()

    /** [route] is a bare name with no leading slash. */
    data class Navigate(
        val route: String,
        val arguments: Map<String, Any?>?
    ) : GameballMessageAction()
}
