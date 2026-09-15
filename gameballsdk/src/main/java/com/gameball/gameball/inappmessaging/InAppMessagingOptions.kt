package com.gameball.gameball.inappmessaging

import com.gameball.gameball.inappmessaging.model.DisplayDecision
import com.gameball.gameball.inappmessaging.model.GameballMessageAction
import com.gameball.gameball.inappmessaging.model.InAppMessage
import com.gameball.gameball.inappmessaging.model.InAppMessageButton

/**
 * Called before a message is drawn.
 *
 * Synchronous. If it throws, the SDK shows the message: a buggy host loses its override, not
 * its messages.
 */
fun interface BeforeDisplayHook {
    fun beforeDisplay(message: InAppMessage): DisplayDecision
}

/**
 * Called when a message or one of its buttons is tapped.
 *
 * Return true when the host has handled the action and the SDK should do nothing further.
 * [button] is null when the message surface itself was tapped.
 *
 * The click is reported to Gameball either way: a host that intercepts every tap must not
 * thereby erase its own click analytics.
 */
fun interface MessageActionHook {
    fun onAction(
        message: InAppMessage,
        button: InAppMessageButton?,
        action: GameballMessageAction
    ): Boolean
}

/** For hosts whose routing the SDK cannot drive. An unknown route should log and continue. */
fun interface MessageNavigationHook {
    fun onNavigate(route: String, arguments: Map<String, Any?>?)
}

/**
 * Receives every message the SDK selects, whatever happens to it next - including one a hook
 * then defers or discards. An observer, not a display notification.
 */
fun interface MessageObserver {
    fun onMessageSelected(message: InAppMessage)
}

/** Optional configuration for [com.gameball.gameball.GameballApp.startInAppMessaging]. */
class InAppMessagingOptions private constructor(
    @JvmField val sessionTimeoutSeconds: Int,
    @JvmField val beforeDisplay: BeforeDisplayHook?,
    @JvmField val onAction: MessageActionHook?,
    @JvmField val onNavigate: MessageNavigationHook?,
    @JvmField val observer: MessageObserver?,
    @JvmField val appVersion: String?
) {

    class Builder {
        private var sessionTimeoutSeconds: Int = 30
        private var beforeDisplay: BeforeDisplayHook? = null
        private var onAction: MessageActionHook? = null
        private var onNavigate: MessageNavigationHook? = null
        private var observer: MessageObserver? = null
        private var appVersion: String? = null

        /**
         * How long in the background counts as a new session. Defaults to 30 seconds, which
         * deliberately matches the display cooldown default - lowering it reintroduces a gap
         * in which the cooldown can suppress a warm session-start message.
         */
        fun sessionTimeoutSeconds(seconds: Int) = apply { this.sessionTimeoutSeconds = seconds }

        fun beforeDisplay(hook: BeforeDisplayHook?) = apply { this.beforeDisplay = hook }

        fun onAction(hook: MessageActionHook?) = apply { this.onAction = hook }

        fun onNavigate(hook: MessageNavigationHook?) = apply { this.onNavigate = hook }

        fun observer(observer: MessageObserver?) = apply { this.observer = observer }

        /** Reported with each sync so campaigns can target app versions. */
        fun appVersion(version: String?) = apply { this.appVersion = version }

        fun build() = InAppMessagingOptions(
            sessionTimeoutSeconds, beforeDisplay, onAction, onNavigate, observer, appVersion
        )
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
