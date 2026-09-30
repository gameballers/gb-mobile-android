package com.gameball.gameball.inappmessaging.data

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.domain.ButtonColors
import com.gameball.gameball.inappmessaging.domain.FilterOperator
import com.gameball.gameball.inappmessaging.domain.MetadataFilter
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.TextAlign
import com.gameball.gameball.inappmessaging.domain.DEFAULT_COOLDOWN_SECONDS
import com.gameball.gameball.inappmessaging.domain.MessageColors
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.domain.MessageOrientation
import com.gameball.gameball.inappmessaging.domain.MessageType
import com.gameball.gameball.inappmessaging.domain.QuietHours
import com.gameball.gameball.inappmessaging.domain.SlidePosition
import com.gameball.gameball.inappmessaging.domain.SyncResult
import com.gameball.gameball.inappmessaging.domain.Trigger
import com.gameball.gameball.inappmessaging.domain.WidgetContent
import com.gameball.gameball.inappmessaging.domain.TriggerType
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.gameball.gameball.inappmessaging.ui.MessageMetrics
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Turns a sync payload into domain objects, applying every leniency rule.
 *
 * The rules are deliberately asymmetric: a contract problem drops the campaign, a content
 * problem degrades it. This class must never throw — a malformed payload returns
 * [SyncResult.EMPTY] and a log line, because a parser that throws takes messaging down for
 * a payload the backend can fix in a minute.
 */
internal object MessageParser {

    private const val PRERENDERED = "prerendered"
    private const val MODAL_MAX_BUTTONS = 2

    /** An outbound message: content in the payload, drawn by this SDK. */
    private const val OUTBOUND_CAMPAIGN_TYPE = 1

    /** A game campaign: a pointer to a reward campaign this SDK opens in the Gameball widget. */
    private const val GAME_CAMPAIGN_TYPE = 2

    /**
     * The games this SDK version can open, as the widget's open-details text spells them.
     *
     * An unrecognised value drops the campaign rather than composing a details string the widget
     * will not understand — which would open to "this campaign is not available", indistinguishable
     * from a campaign that has ended.
     */
    private val KNOWN_GAME_TYPES = setOf(
        "wheel", "slot", "quiz", "scratch", "match", "catcher",
        "tictactoe", "shooter", "puzzle", "tap_target", "highway_drive"
    )

    /** The contract's ceiling on how long a game may be held back after its trigger fires. */
    private const val MAX_GAME_DELAY_SECONDS = 60
    private val KNOWN_CLOSE_BEHAVIOURS = setOf("both", "button", "swipe")

    fun parse(rawJson: String?): SyncResult {
        if (rawJson.isNullOrBlank()) {
            IamLog.w("sync payload was null or empty")
            return SyncResult.EMPTY
        }
        // JsonParser.parseString is Gson 2.8.6+; this SDK resolves Gson 2.8.5 transitively
        // through retrofit's converter-gson, and bumping it would change behaviour for every
        // consumer. The instance method works on both.
        @Suppress("DEPRECATION")
        val root = try {
            JsonParser().parse(rawJson)
        } catch (t: Throwable) {
            IamLog.e("sync payload is not valid JSON; ignoring it", t)
            return SyncResult.EMPTY
        }
        if (root == null || !root.isJsonObject) {
            IamLog.w("sync payload root is not an object; ignoring it")
            return SyncResult.EMPTY
        }
        return parse(root.asJsonObject)
    }

    @VisibleForTesting
    fun parse(root: JsonObject): SyncResult {
      return try {
        val cooldown = root.int("cooldownSeconds")?.takeIf { it >= 0 } ?: DEFAULT_COOLDOWN_SECONDS

        val quietHoursObject = root.obj("quietHours")
        val quietHours = quietHoursObject?.let {
            QuietHours.from(it.bool("enabled"), it.str("start"), it.str("end"))
        }

        val messages = root.arr("messages")
        if (messages == null) {
            IamLog.w("sync payload has no usable messages array")
            return SyncResult(emptyList(), cooldown, quietHours)
        }

        val campaigns = ArrayList<Campaign>(messages.size())
        messages.forEachIndexed { index, element ->
            if (!element.isJsonObject) {
                IamLog.w("messages[$index] is not an object; dropped")
                return@forEachIndexed
            }
            // responseIndex is the position in the array, not the surviving index: it is the
            // marketer's dashboard ordering and must not shift when a sibling is dropped.
            parseCampaign(element.asJsonObject, index)?.let(campaigns::add)
        }

        IamLog.d("parsed ${campaigns.size}/${messages.size()} campaigns, cooldown ${cooldown}s")
        SyncResult(campaigns, cooldown, quietHours)
      } catch (t: Throwable) {
        // Defensive: no path above is expected to throw, and if one ever does, messaging must
        // degrade to "no campaigns" rather than take the host's sync call down with it.
        IamLog.e("unexpected failure parsing the sync payload; ignoring it", t)
        SyncResult.EMPTY
      }
    }

    @VisibleForTesting
    fun parseCampaign(obj: JsonObject, index: Int): Campaign? {
        val campaignId = obj.int("campaignId") ?: run {
            IamLog.w("messages[$index] has no campaignId; dropped")
            return null
        }

        // The fork. An entry with no campaignType is a message: that is what every payload
        // written before this field existed contains, and defaulting to a game would try to open
        // a campaign that is not there.
        val campaignType = obj.int("campaignType") ?: OUTBOUND_CAMPAIGN_TYPE
        if (campaignType != OUTBOUND_CAMPAIGN_TYPE && campaignType != GAME_CAMPAIGN_TYPE) {
            IamLog.w(
                "campaign $campaignId has campaignType $campaignType, which this SDK version " +
                    "neither renders nor opens; dropped"
            )
            return null
        }

        val contentMode = obj.str("contentMode") ?: PRERENDERED
        if (!contentMode.equals(PRERENDERED, ignoreCase = true)) {
            IamLog.w("campaign $campaignId has contentMode '$contentMode'; dropped")
            return null
        }

        val rawMessageType = obj.int("messageType") ?: run {
            IamLog.w("campaign $campaignId has no messageType; dropped")
            return null
        }
        val messageType = MessageType.from(rawMessageType)
        if (!messageType.isSupported) {
            IamLog.w(
                "campaign $campaignId has messageType $rawMessageType, which this SDK version " +
                    "cannot draw; kept and marked unsupported"
            )
        }

        val trigger = parseTrigger(obj.obj("trigger"), campaignId) ?: return null

        val widget = if (campaignType == GAME_CAMPAIGN_TYPE) {
            parseGameContent(obj, campaignId) ?: return null
        } else {
            null
        }

        // A game carries no content of its own — the campaign is the content — so the
        // "nothing to render" guard is skipped. Keyed off the kind rather than off which fields
        // are absent, so a *message* missing everything still fails for that reason.
        val content = if (widget != null) {
            MessageContent.forGame()
        } else {
            parseContent(obj, campaignId, messageType) ?: return null
        }

        return Campaign(
            campaignId = campaignId,
            variationId = obj.int("variationId"),
            dispatchId = obj.str("dispatchId"),
            name = obj.str("name"),
            priority = obj.int("priority") ?: 0,
            messageType = messageType,
            rawMessageType = rawMessageType,
            expiresAtMillis = IamTime.parseIso8601(obj.str("expiresAt")),
            isTest = obj.bool("isTest") ?: false,
            trigger = trigger,
            content = content,
            responseIndex = index,
            widget = widget
        )
    }

    /**
     * Reads the game fields from a `campaignType: 2` entry.
     *
     * Null when the entry cannot be opened, which drops it. Both fields are required: without the
     * id there is nothing to open, and without a recognised type there is no renderer to ask for.
     */
    private fun parseGameContent(obj: JsonObject, campaignId: Int): WidgetContent? {
        val gameCampaignId = obj.int("gameCampaignId")
        if (gameCampaignId == null || gameCampaignId == 0) {
            IamLog.w(
                "campaign $campaignId is a game with no gameCampaignId; dropped " +
                    "(its campaignId is always 0 and identifies nothing)"
            )
            return null
        }

        val gameCampaignType = obj.str("gameCampaignType")
        if (gameCampaignType == null || gameCampaignType !in KNOWN_GAME_TYPES) {
            IamLog.w(
                "campaign $campaignId has gameCampaignType '$gameCampaignType', which this SDK " +
                    "version has no renderer for; dropped"
            )
            return null
        }

        return WidgetContent(
            gameCampaignId = gameCampaignId,
            gameCampaignType = gameCampaignType,
            delayMillis = parseGameDelay(obj, campaignId),
            dismissLimit = obj.int("dismissLimit")
        )
    }

    /** Seconds on the wire, capped: a campaign held back longer than a minute has missed its moment. */
    private fun parseGameDelay(obj: JsonObject, campaignId: Int): Long? {
        val seconds = obj.int("delay") ?: return null
        if (seconds <= 0) return null
        if (seconds > MAX_GAME_DELAY_SECONDS) {
            IamLog.w("campaign $campaignId asked for a ${seconds}s delay; capped at ${MAX_GAME_DELAY_SECONDS}s")
            return MAX_GAME_DELAY_SECONDS * 1000L
        }
        return seconds * 1000L
    }

    private fun parseContent(
        campaignObject: JsonObject,
        campaignId: Int,
        messageType: MessageType
    ): MessageContent? {
        val content = campaignObject.obj("content") ?: JsonObject()
        val locale = campaignObject.obj("locale") ?: JsonObject()

        val header = locale.str("header")
        // A slideup shows one line and falls back to the header, so a campaign that filled
        // the wrong field still says something.
        val body = if (messageType == MessageType.SLIDEUP) {
            locale.str("message") ?: header
        } else {
            locale.str("message") ?: locale.str("body")
        }

        val imageUrl = resolveArtwork(content, messageType, campaignId)
        val iconUrl = content.str("iconUrl")

        // Drawing an empty box is worse than showing nothing.
        if (header == null && body == null && imageUrl == null) {
            IamLog.w("campaign $campaignId has no header, body or image; dropped")
            return null
        }
        // A 40dp icon with no words is not a message.
        if (messageType == MessageType.SLIDEUP && body.isNullOrBlank()) {
            IamLog.w("campaign $campaignId is a slideup with no text; dropped")
            return null
        }

        val buttons = parseButtons(content, locale, messageType, campaignId)
        val (showClose, dismissOnScrim) = parseCloseBehaviour(
            content.str("closeBehaviour"), messageType, campaignId
        )

        return MessageContent(
            header = header,
            body = body,
            imageUrl = imageUrl,
            iconUrl = iconUrl,
            layout = parseLayout(content.str("layout"), imageUrl, campaignId),
            colors = parseColors(content.obj("colors")),
            buttons = buttons,
            clickAction = parseAction(content.obj("action")),
            showCloseButton = showClose,
            dismissOnScrimTap = dismissOnScrim,
            slidePosition = if (content.str("slideFrom")?.lowercase() == "top") {
                SlidePosition.TOP
            } else {
                SlidePosition.BOTTOM
            },
            orientation = when (content.str("orientation")?.lowercase()) {
                "portrait" -> MessageOrientation.PORTRAIT
                "landscape" -> MessageOrientation.LANDSCAPE
                else -> MessageOrientation.ANY
            },
            autoDismissMillis = parseAutoDismiss(content, messageType),
            headerAlign = TextAlign.from(content.obj("textAlignment")?.str("header")),
            bodyAlign = TextAlign.from(content.obj("textAlignment")?.str("body")),
            extras = parseExtras(content.obj("extras"))
        )
    }

    /**
     * Fullscreen prefers media.url; every other type prefers imageUrl. Each falls back to the
     * other. A parser reading only imageUrl finds nothing on the live QA campaign, renders
     * nothing, and looks like a backend problem.
     */
    private fun resolveArtwork(
        content: JsonObject,
        messageType: MessageType,
        campaignId: Int
    ): String? {
        val direct = content.str("imageUrl")
        val media = content.obj("media")
        val mediaType = media?.str("type")?.lowercase()
        val fromMedia = when {
            media == null -> null
            mediaType == null || mediaType == "image" -> media.str("url")
            else -> {
                // Handing a video URL to an ImageView draws a broken frame.
                IamLog.w("campaign $campaignId has media.type '$mediaType'; ignored")
                null
            }
        }
        return if (messageType == MessageType.FULLSCREEN) fromMedia ?: direct
        else direct ?: fromMedia
    }

    /**
     * Buttons arrive split across the styled half and the translated half and are paired by
     * string id. One without the other has no action or no label; either way it is dropped.
     */
    private fun parseButtons(
        content: JsonObject,
        locale: JsonObject,
        messageType: MessageType,
        campaignId: Int
    ): List<MessageButton> {
        // JsonArray is Gson's Iterable, not a Kotlin collection, so no orEmpty() here.
        val styled = LinkedHashMap<String, JsonObject>()
        content.arr("buttons")?.forEach { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            obj.str("id")?.let { styled[it] = obj }
        }

        val translated = LinkedHashMap<String, JsonObject>()
        locale.arr("buttons")?.forEach { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            obj.str("id")?.let { translated[it] = obj }
        }

        // Order follows the styled half, which is the payload order the dashboard arranged.
        var buttons = styled.mapNotNull { (id, styledObject) ->
            val text = translated[id]?.str("text") ?: return@mapNotNull null
            MessageButton(
                id = id,
                text = text,
                // A dead button is worse than a closing one.
                action = parseAction(styledObject.obj("action")) ?: MessageAction.Dismiss,
                colors = styledObject.obj("colors")?.let { colors ->
                    ButtonColors(
                        background = ColorParser.parse(colors.scalar("background")),
                        text = ColorParser.parse(colors.scalar("text")),
                        border = ColorParser.parse(colors.scalar("border"))
                    )
                }
            )
        }

        val dropped = styled.size - buttons.size
        if (dropped > 0) IamLog.w("campaign $campaignId dropped $dropped unpaired button(s)")

        when (messageType) {
            MessageType.SLIDEUP -> if (buttons.isNotEmpty()) {
                IamLog.w(
                    "campaign $campaignId is a slideup carrying ${buttons.size} button(s); " +
                        "dropped - a slideup's whole surface is the tap target"
                )
                buttons = emptyList()
            }
            MessageType.MODAL -> if (buttons.size > MODAL_MAX_BUTTONS) {
                IamLog.w(
                    "campaign $campaignId is a modal carrying ${buttons.size} buttons; " +
                        "keeping the first $MODAL_MAX_BUTTONS"
                )
                buttons = buttons.take(MODAL_MAX_BUTTONS)
            }
            else -> Unit // fullscreen has no cap; buttons stack full-width
        }
        return buttons
    }

    /** Null means "no action" - the caller decides whether that is inert or a dismiss. */
    private fun parseAction(action: JsonObject?): MessageAction? {
        val type = action?.str("type")?.lowercase() ?: return null
        return when (type) {
            "dismiss" -> MessageAction.Dismiss
            "open_url" -> action.str("url")
                ?.let { MessageAction.OpenUrl(it, action.bool("external") ?: false) }
            "navigate" -> action.str("route")
                ?.let { MessageAction.Navigate(it, parseArguments(action.obj("arguments"))) }
            else -> MessageAction.Unsupported(type)
        }
    }

    private fun parseArguments(arguments: JsonObject?): Map<String, Any?>? =
        arguments?.entrySet()?.associate { (key, value) -> key to value.scalar() }

    private fun parseLayout(raw: String?, imageUrl: String?, campaignId: Int): MessageLayout {
        val layout = when (raw?.lowercase()) {
            null -> MessageLayout.DEFAULT
            "text_with_image", "image_and_text" -> MessageLayout.DEFAULT
            "image_only" -> MessageLayout.IMAGE_ONLY
            else -> {
                IamLog.w("campaign $campaignId has layout '$raw'; using the type's default")
                MessageLayout.DEFAULT
            }
        }
        // The full-bleed branch never references header or body, so without artwork it would
        // render a bare background, count an impression and report nothing wrong.
        if (layout == MessageLayout.IMAGE_ONLY && imageUrl.isNullOrBlank()) {
            IamLog.w(
                "campaign $campaignId declares image_only with no artwork; " +
                    "using the stacked layout"
            )
            return MessageLayout.DEFAULT
        }
        return layout
    }

    private fun parseColors(colors: JsonObject?): MessageColors {
        if (colors == null) return MessageColors.EMPTY
        return MessageColors(
            background = ColorParser.parse(colors.scalar("background")),
            text = ColorParser.parse(colors.scalar("text")),
            header = ColorParser.parse(colors.scalar("header")),
            closeButton = ColorParser.parse(colors.scalar("closeButton")),
            border = ColorParser.parse(colors.scalar("border")),
            frame = ColorParser.parse(colors.scalar("frame"))
        )
    }

    /** Returns (showCloseButton, dismissOnScrimTap). */
    private fun parseCloseBehaviour(
        raw: String?,
        messageType: MessageType,
        campaignId: Int
    ): Pair<Boolean, Boolean> {
        val behaviour = raw?.trim()?.lowercase()
        if (behaviour != null && behaviour !in KNOWN_CLOSE_BEHAVIOURS) {
            IamLog.w("campaign $campaignId has closeBehaviour '$raw'; treating it as 'both'")
            return true to true
        }
        // A fullscreen has no scrim and no swipe gesture of its own, so "swipe" would leave
        // only the system back gesture - and that does not exist on iOS. Promote and log.
        if (behaviour == "swipe" && messageType == MessageType.FULLSCREEN) {
            IamLog.w(
                "campaign $campaignId is a fullscreen with closeBehaviour 'swipe', which would " +
                    "leave no way out; promoted to 'both'"
            )
            return true to true
        }
        return when (behaviour) {
            null, "both" -> true to true
            "button" -> true to false
            "swipe" -> false to true
            else -> true to true
        }
    }

    /**
     * Absent and zero are different values. A slideup draws no glyph and has no scrim, so
     * without a timer its only exit is a gesture nobody told the customer about - hence the
     * default. An explicit 0 is an author turning the timer off and is honoured.
     */
    private fun parseAutoDismiss(content: JsonObject, messageType: MessageType): Long? {
        val seconds = content.double("autoDismissSeconds")
        if (seconds == null) {
            return if (messageType == MessageType.SLIDEUP) MessageMetrics.Slideup.DEFAULT_AUTO_DISMISS_MS else null
        }
        if (seconds <= 0.0) return null
        return Math.round(seconds * 1000.0)
    }

    /**
     * Coerced rather than dropped: silently discarding non-string extras would lose campaign
     * data with no diagnostic. A null value is dropped.
     */
    private fun parseExtras(extras: JsonObject?): Map<String, String> {
        if (extras == null) return emptyMap()
        val result = LinkedHashMap<String, String>()
        extras.entrySet().forEach { (key, value) ->
            value.scalar()?.let { result[key] = it.toString() }
        }
        return result
    }

    private fun parseTrigger(triggerObject: JsonObject?, campaignId: Int): Trigger? {
        if (triggerObject == null) {
            IamLog.w("campaign $campaignId has no trigger; dropped")
            return null
        }
        return when (triggerObject.str("type")?.lowercase()) {
            "session_start" -> Trigger(TriggerType.SESSION_START)

            // Deliberate, not unimplemented. A screen trigger addresses a registered screen name,
            // which this SDK has no API to report; the same targeting is expressed as a custom
            // event. Named separately from the catch-all below so the log says why, not just what.
            "screen" -> {
                IamLog.w(
                    "campaign $campaignId has a screen trigger ('${triggerObject.str("name")}'), " +
                        "which this SDK does not support; dropped"
                )
                null
            }

            "event" -> {
                // Match on name, never on eventId - the numeric id is internal to the backend.
                val name = triggerObject.str("name")
                if (name == null) {
                    IamLog.w("campaign $campaignId has an event trigger with no name; dropped")
                    return null
                }
                val logicalOperator = triggerObject.str("metadataLogicalOperator")
                if (logicalOperator != null && !logicalOperator.equals("And", ignoreCase = true)) {
                    IamLog.w(
                        "campaign $campaignId uses metadataLogicalOperator " +
                            "'$logicalOperator', which this SDK cannot evaluate; dropped"
                    )
                    return null
                }
                val filters = parseFilters(triggerObject, campaignId) ?: return null
                Trigger(
                    type = TriggerType.EVENT,
                    eventName = name,
                    filters = filters,
                    repeatable = triggerObject.bool("repeatable") ?: false,
                    minIntervalSeconds = triggerObject.int("minIntervalSeconds")?.takeIf { it > 0 }
                )
            }

            else -> {
                IamLog.w(
                    "campaign $campaignId has trigger type " +
                        "'${triggerObject.str("type")}'; dropped"
                )
                null
            }
        }
    }

    /** Null means "drop the campaign"; an empty list means "no filters". */
    private fun parseFilters(trigger: JsonObject, campaignId: Int): List<MetadataFilter>? {
        val raw = trigger.arr("metadataFilters") ?: return emptyList()
        val filters = ArrayList<MetadataFilter>(raw.size())
        for (element in raw) {
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue

            // A filter that cannot be named cannot be evaluated, and treating it as always
            // true silently widens the campaign - a "spent over $100" message shown to
            // everyone. That is worse than showing nothing, so the campaign goes.
            val name = obj.str("name")
            if (name == null) {
                IamLog.w("campaign $campaignId has a metadata filter with no name; dropped")
                return null
            }

            // One bad field widens rather than narrows, which is the right response here.
            val operator = FilterOperator.from(obj.str("operator"))
            if (operator == null) {
                IamLog.w(
                    "campaign $campaignId filter '$name' has operator " +
                        "'${obj.str("operator")}'; that filter dropped"
                )
                continue
            }
            val value = obj.scalar("value")
            if (value == null) {
                IamLog.w(
                    "campaign $campaignId filter '$name' has a null value; that filter dropped"
                )
                continue
            }
            filters.add(MetadataFilter(name, operator, value))
        }
        return filters
    }
}
