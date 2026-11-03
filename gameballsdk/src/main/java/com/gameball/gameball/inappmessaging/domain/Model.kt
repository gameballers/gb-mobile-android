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

    companion object {
        /**
         * The content of a campaign whose content is a game.
         *
         * Every field is empty because the campaign *is* the content: the widget draws itself and
         * offers its own close affordance. This exists because [Campaign.content] is non-null —
         * it is not a message with missing fields, and nothing reads it for a game.
         */
        fun forGame(): MessageContent = MessageContent(
            header = null,
            body = null,
            imageUrl = null,
            iconUrl = null,
            layout = MessageLayout.DEFAULT,
            colors = MessageColors.EMPTY,
            buttons = emptyList(),
            clickAction = null,
            showCloseButton = true,
            dismissOnScrimTap = true,
            slidePosition = SlidePosition.BOTTOM,
            orientation = MessageOrientation.ANY,
            autoDismissMillis = null,
            headerAlign = null,
            bodyAlign = null,
            extras = emptyMap()
        )
    }
}

/**
 * A reward campaign rendered as the message's own content.
 *
 * The campaign **is** the content — no header, body, image or button, because this is not a
 * teaser that links to a game, it is the game.
 *
 * Deliberately not a URL on the wire: one would carry the api key and the player id, making the
 * payload both credential-bearing and customer-specific, and stale the moment either changed.
 * The SDK composes the URL from state it already holds and the two fields here.
 *
 * There is no delay and no dismissal limit: a game opens when its trigger fires and is offered
 * by exactly the rules a message is — repeatable, minIntervalSeconds, the cooldown and quiet hours.
 */
internal data class WidgetContent(
    /** The challenge to open. All bookkeeping keys off this, never off the entry's campaignId. */
    val gameCampaignId: Int,
    /** Which game it is, as the open-details text: `wheel`, `quiz`, `scratch`... */
    val gameCampaignType: String
) {
    /** What the host widget is asked to open. */
    val openDetail: String get() = "details_${gameCampaignType}_$gameCampaignId"
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
    val responseIndex: Int,
    /**
     * A reward campaign to open instead of drawing artwork and copy. Null for every message.
     *
     * A campaign carrying this fetches its content over the network at display time rather than
     * receiving it in the payload, which is why it is exempt from the parser's
     * "needs something to render" rule.
     */
    val widget: WidgetContent? = null
) {
    val isWidget: Boolean get() = widget != null

    /**
     * How this campaign is identified in display history.
     *
     * A game's campaignId is always 0, so its identity is the id of the game it opens.
     */
    val key: CampaignKey
        get() = widget?.let { CampaignKey.game(it.gameCampaignId) } ?: CampaignKey.message(campaignId)

    /**
     * How this campaign appears in a log line.
     *
     * A game's campaignId is always 0, so labelling it by that number would make every log line
     * about every game read "campaign 0".
     */
    val label: String get() = if (isWidget) key.storageKey else campaignId.toString()
}

internal data class SyncResult(
    val campaigns: List<Campaign>,
    val cooldownSeconds: Int,
    val quietHours: QuietHours?
) {
    companion object {
        val EMPTY = SyncResult(emptyList(), DEFAULT_COOLDOWN_SECONDS, null)
    }
}
