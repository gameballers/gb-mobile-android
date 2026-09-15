package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * The module's four storage slots, over the SDK's existing SharedPreferences helper so that
 * logout's clearData() clears in-app messaging state too.
 *
 * Three slots are customer-scoped and discarded on a mismatch at read: showing one person's
 * campaigns — or their name — to another is the single failure this scoping exists to
 * prevent.
 *
 * The outbox is deliberately NOT scoped. Each queued event carries the customerId it was
 * produced under and flushes group by it, so discarding the queue on a customer change would
 * lose analytics that are already correctly attributed. Unlike the other three, the outbox is
 * never shown to anybody.
 *
 * No read timeout: SharedPreferences is in-process, so after the first load reads are memory
 * access. Flutter needs a bound because its store is a platform channel, and porting that
 * bound is how a once-ever campaign displays twice.
 */
internal class IamStore(private val prefs: SharedPreferencesUtils) {

    enum class Slot { CAMPAIGN_CACHE, DISPLAY_HISTORY, OUTBOX, VARIABLES }

    fun readRaw(slot: Slot): String? = when (slot) {
        Slot.CAMPAIGN_CACHE -> prefs.getIamCampaignCache()
        Slot.DISPLAY_HISTORY -> prefs.getIamDisplayHistory()
        Slot.OUTBOX -> prefs.getIamOutbox()
        Slot.VARIABLES -> prefs.getIamVariables()
    }

    /** apply(), never commit(): a display must never wait on storage. */
    fun writeRaw(slot: Slot, payload: String?) {
        when (slot) {
            Slot.CAMPAIGN_CACHE -> prefs.putIamCampaignCache(payload)
            Slot.DISPLAY_HISTORY -> prefs.putIamDisplayHistory(payload)
            Slot.OUTBOX -> prefs.putIamOutbox(payload)
            Slot.VARIABLES -> prefs.putIamVariables(payload)
        }
    }

    /** Returns null when the slot is empty, unreadable, or belongs to another customer. */
    fun readScoped(slot: Slot, customerId: String): String? {
        val raw = readRaw(slot) ?: return null
        val envelope = try {
            @Suppress("DEPRECATION")
            JsonParser().parse(raw).takeIf { it.isJsonObject }?.asJsonObject
        } catch (t: Throwable) {
            IamLog.w("$slot is unreadable; discarding it")
            clear(slot)
            return null
        }
        if (envelope == null) {
            IamLog.w("$slot is not an object; discarding it")
            clear(slot)
            return null
        }
        val owner = envelope.str("customerId")
        if (owner == null || owner != customerId) {
            IamLog.d("$slot belongs to a different customer; discarding it")
            clear(slot)
            return null
        }
        return envelope.child("data")?.toString()
    }

    fun writeScoped(slot: Slot, customerId: String, payload: String) {
        val envelope = JsonObject()
        envelope.addProperty("customerId", customerId)
        try {
            @Suppress("DEPRECATION")
            envelope.add("data", JsonParser().parse(payload))
        } catch (t: Throwable) {
            IamLog.e("refusing to store unparseable payload in $slot", t)
            return
        }
        writeRaw(slot, envelope.toString())
    }

    fun clear(slot: Slot) = writeRaw(slot, null)

    fun clearAll() = Slot.values().forEach { clear(it) }
}
