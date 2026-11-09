package com.gameball.gameball.engagement

/** A Gameball push that reached the phone and hasn't been tapped. [key] identifies the push. */
internal data class ReceivedPush(
    val key: String,
    val clickToken: String?,
    val sentAt: Long,
    val receivedAt: Long
)

/** Where received pushes are kept between app launches. */
internal interface ReceivedPushStore {
    fun load(): List<ReceivedPush>
    fun save(pushes: List<ReceivedPush>)
}

/**
 * Influenced opens: a push arrived, the user didn't tap it, and opened the app within
 * [windowMillis] of receiving it.
 *
 * - received → saved
 * - tapped → removed; it's a normal tap and never counts as influenced
 * - app opened by a tap → a normal click; nothing is influenced, even by an older untapped push
 * - app opened otherwise → if the most recent saved push arrived within the window, that push
 *   influenced the open
 *
 * Every open clears the saved list.
 */
internal class InfluencedOpens(
    private val store: ReceivedPushStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val windowMillis: Long = WINDOW_MS
) {

    @Synchronized
    fun onReceived(key: String, clickToken: String?, sentAt: Long) {
        val pushes = store.load().filter { it.key != key } + ReceivedPush(key, clickToken, sentAt, now())
        store.save(pushes.takeLast(MAX_SAVED))
    }

    @Synchronized
    fun onTap(key: String) {
        val pushes = store.load()
        if (pushes.any { it.key == key }) store.save(pushes.filter { it.key != key })
    }

    /** The push that influenced [open], or null for an ordinary open. */
    @Synchronized
    fun influencedBy(open: AppOpen): ReceivedPush? {
        val pushes = store.load()
        if (pushes.isEmpty()) return null
        store.save(emptyList())
        if (open.fromTap) return null
        val latest = pushes.maxByOrNull { it.receivedAt } ?: return null
        return latest.takeIf { open.openedAt - it.receivedAt in 0..windowMillis }
    }

    /** Another customer's pushes must never credit this customer's opens. */
    @Synchronized
    fun clear() = store.save(emptyList())

    companion object {
        const val WINDOW_MS = 30 * 60 * 1000L
        const val MAX_SAVED = 20
    }
}
