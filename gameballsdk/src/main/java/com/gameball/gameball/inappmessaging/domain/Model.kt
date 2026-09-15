package com.gameball.gameball.inappmessaging.domain

/** The backend's default when the sync response omits cooldownSeconds. */
internal const val DEFAULT_COOLDOWN_SECONDS = 30

internal enum class MessageType(val wire: Int) {
    SLIDEUP(1),
    MODAL(2),
    FULLSCREEN(3),

    /**
     * Covers 4 (htmlFullscreen) and 5 (emailCapture), which are specified but not implemented,
     * and any future type this SDK version predates. The campaign is kept and filtered at
     * selection so a usable lower-priority campaign can win the occurrence.
     */
    UNSUPPORTED(-1);

    val isSupported: Boolean get() = this != UNSUPPORTED

    companion object {
        fun from(wire: Int): MessageType = when (wire) {
            1 -> SLIDEUP
            2 -> MODAL
            3 -> FULLSCREEN
            else -> UNSUPPORTED
        }
    }
}

internal enum class SlidePosition { TOP, BOTTOM }

internal enum class MessageOrientation { ANY, PORTRAIT, LANDSCAPE }

/** A rendering hint, never a contract. An unrecognised value falls back to DEFAULT. */
internal enum class MessageLayout { DEFAULT, IMAGE_ONLY }

internal enum class TextAlign {
    START, END, CENTER, LEFT, RIGHT;

    companion object {
        fun from(raw: String?): TextAlign? = when (raw?.trim()?.lowercase()) {
            "start" -> START
            "end" -> END
            "center", "centre" -> CENTER
            "left" -> LEFT
            "right" -> RIGHT
            else -> null
        }
    }
}

internal sealed class MessageAction {
    object Dismiss : MessageAction()

    data class OpenUrl(val url: String, val external: Boolean) : MessageAction()

    /** [route] is a bare name with no leading slash. */
    data class Navigate(val route: String, val arguments: Map<String, Any?>?) : MessageAction()

    /** log_event, log_attribute, request_push_permission and anything unknown. */
    data class Unsupported(val type: String) : MessageAction()
}

/**
 * Every colour is optional; absent means fall back to the host's theme, never to a literal.
 * [border] is parsed and carried but never painted — no message-level surface draws one.
 */
internal data class MessageColors(
    val background: Int? = null,
    val text: Int? = null,
    val header: Int? = null,
    val closeButton: Int? = null,
    val border: Int? = null,
    val frame: Int? = null
) {
    companion object { val EMPTY = MessageColors() }
}

internal data class ButtonColors(
    val background: Int? = null,
    val text: Int? = null,
    /** No default outline: a border is drawn only when this is set. */
    val border: Int? = null
)

/** Paired across content and locale by [id]; unmatched ids are dropped at parse. */
internal data class MessageButton(
    val id: String,
    val text: String,
    val action: MessageAction,
    val colors: ButtonColors? = null
)

internal data class MessageContent(
    val header: String?,
    val body: String?,
    /** Already resolved per type: fullscreen prefers media.url, others prefer imageUrl. */
    val imageUrl: String?,
    /** Slideup only. */
    val iconUrl: String?,
    val layout: MessageLayout,
    val colors: MessageColors,
    val buttons: List<MessageButton>,
    /** Null means the surface is inert. Never defaulted to dismiss. */
    val clickAction: MessageAction?,
    val showCloseButton: Boolean,
    val dismissOnScrimTap: Boolean,
    val slidePosition: SlidePosition,
    val orientation: MessageOrientation,
    /** Null means no timer. A slideup receives the 8 s default at parse. */
    val autoDismissMillis: Long?,
    val headerAlign: TextAlign?,
    val bodyAlign: TextAlign?,
    val extras: Map<String, String>
) {
    val hasArtwork: Boolean get() = !imageUrl.isNullOrBlank()
    val hasIcon: Boolean get() = !iconUrl.isNullOrBlank()
    val hasText: Boolean get() = !header.isNullOrBlank() || !body.isNullOrBlank()
}

internal data class Campaign(
    val campaignId: Int,
    val variationId: Int?,
    val dispatchId: String?,
    /** Logs and debug UI only. Never used in logic. */
    val name: String?,
    val priority: Int,
    val messageType: MessageType,
    /** The wire value, retained for logs when [messageType] is UNSUPPORTED. */
    val rawMessageType: Int,
    val expiresAtMillis: Long?,
    /** Displays normally; reports nothing at all. */
    val isTest: Boolean,
    val trigger: Trigger,
    val content: MessageContent,
    /**
     * Position in the response's messages array. The marketer's dashboard ordering, and the
     * tie-break for equal priorities — meaningful, not merely deterministic.
     */
    val responseIndex: Int
)

internal data class SyncResult(
    val campaigns: List<Campaign>,
    val cooldownSeconds: Int,
    val quietHours: QuietHours?
) {
    companion object {
        val EMPTY = SyncResult(emptyList(), DEFAULT_COOLDOWN_SECONDS, null)
    }
}
