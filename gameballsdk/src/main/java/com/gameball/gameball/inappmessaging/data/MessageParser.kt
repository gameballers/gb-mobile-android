package com.gameball.gameball.inappmessaging.data

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.domain.Campaign
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
import com.gameball.gameball.inappmessaging.domain.TriggerType
import com.gameball.gameball.inappmessaging.runtime.IamLog
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
        val content = parseContent(obj, campaignId, messageType) ?: return null

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
            responseIndex = index
        )
    }

    // Task 5 replaces this with the full implementation.
    private fun parseContent(
        campaignObject: JsonObject,
        campaignId: Int,
        messageType: MessageType
    ): MessageContent? {
        val content = campaignObject.obj("content") ?: JsonObject()
        val locale = campaignObject.obj("locale") ?: JsonObject()
        val header = locale.str("header")
        val body = locale.str("message") ?: locale.str("body")
        val imageUrl = content.str("imageUrl")
        val iconUrl = content.str("iconUrl")

        if (header == null && body == null && imageUrl == null) {
            IamLog.w("campaign $campaignId has no header, body or image; dropped")
            return null
        }
        if (messageType == MessageType.SLIDEUP && header == null && body == null) {
            IamLog.w("campaign $campaignId is a slideup with no text; dropped")
            return null
        }

        return MessageContent(
            header = header,
            body = body,
            imageUrl = imageUrl,
            iconUrl = iconUrl,
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

    // Task 6 replaces this with the full implementation.
    private fun parseTrigger(triggerObject: JsonObject?, campaignId: Int): Trigger? =
        when (triggerObject?.str("type")?.lowercase()) {
            "session_start" -> Trigger(TriggerType.SESSION_START)
            "event" -> Trigger(TriggerType.EVENT, eventName = triggerObject.str("name"))
            else -> null
        }
}
