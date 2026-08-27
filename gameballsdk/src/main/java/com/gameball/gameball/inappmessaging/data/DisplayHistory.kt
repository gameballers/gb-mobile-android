package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.DisplayHistorySnapshot
import com.gameball.gameball.inappmessaging.domain.DisplayRecord
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * When each campaign last displayed, and when anything last displayed.
 *
 * Recorded at impression, never at selection: a message selected then deferred or suppressed
 * must not burn its slot, which is why a suppressed campaign is eligible again next session
 * with no manual reset.
 *
 * The history grows without pruning, deliberately. The backend stops returning a
 * non-repeatable campaign once its impression lands, so forgetting it locally could show a
 * once-ever message twice. If it must ever be bounded, cap the entry count and drop oldest —
 * never prune by "no longer in the current sync".
 */
internal class DisplayHistory(private val store: IamStore) {

    fun load(customerId: String): DisplayHistorySnapshot {
        val raw = store.readScoped(IamStore.Slot.DISPLAY_HISTORY, customerId)
            ?: return DisplayHistorySnapshot()
        return try {
            @Suppress("DEPRECATION")
            val root = JsonParser().parse(raw).takeIf { it.isJsonObject }?.asJsonObject
                ?: return DisplayHistorySnapshot()
            val records = HashMap<Int, DisplayRecord>()
            root.obj("records")?.entrySet()?.forEach { (key, value) ->
                val id = key.toIntOrNull() ?: return@forEach
                val entry = value.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                val at = entry.long("at") ?: return@forEach
                records[id] = DisplayRecord(at, entry.int("count") ?: 1)
            }
            DisplayHistorySnapshot(records, root.long("last"))
        } catch (t: Throwable) {
            // A corrupt store must not stop messaging from starting.
            IamLog.w("display history is unreadable; starting from empty")
            DisplayHistorySnapshot()
        }
    }

    fun recordImpression(customerId: String, campaignId: Int, atMillis: Long) {
        val current = load(customerId)
        val existing = current.perCampaign[campaignId]
        val updated = current.perCampaign.toMutableMap().apply {
            put(campaignId, DisplayRecord(atMillis, (existing?.count ?: 0) + 1))
        }
        val records = JsonObject()
        updated.forEach { (id, record) ->
            records.add(id.toString(), JsonObject().apply {
                addProperty("at", record.lastDisplayAtMillis)
                addProperty("count", record.count)
            })
        }
        val root = JsonObject().apply {
            addProperty("last", atMillis)
            add("records", records)
        }
        store.writeScoped(IamStore.Slot.DISPLAY_HISTORY, customerId, root.toString())
    }

    fun clear() = store.clear(IamStore.Slot.DISPLAY_HISTORY)
}
