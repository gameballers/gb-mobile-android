package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.SyncResult

/**
 * The last good sync payload, so a failed sync falls back to real campaigns rather than none.
 *
 * Stores the RAW payload, not serialised objects. Two reasons: there is no serialiser to keep
 * in step with the model, and the parser stays the only thing that reads a sync. Re-parsing on
 * read also means a field-by-field rebuild cannot silently drop something — Flutter's cache
 * rebuilt the result field by field and lost the quiet-hours window, which made going offline
 * a way to message somebody at 3am.
 */
internal class CampaignCache(private val store: IamStore) {

    fun put(customerId: String, rawPayload: String) {
        store.writeScoped(IamStore.Slot.CAMPAIGN_CACHE, customerId, rawPayload)
    }

    /** Null when there is nothing cached for this customer. */
    fun get(customerId: String): SyncResult? {
        val raw = store.readScoped(IamStore.Slot.CAMPAIGN_CACHE, customerId) ?: return null
        return MessageParser.parse(raw)
    }

    fun clear() = store.clear(IamStore.Slot.CAMPAIGN_CACHE)
}
