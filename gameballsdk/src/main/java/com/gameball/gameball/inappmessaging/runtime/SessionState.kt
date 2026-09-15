package com.gameball.gameball.inappmessaging.runtime

/**
 * Decides when an absence was long enough to count as a new session.
 *
 * Default timeout is 30s, deliberately equal to the display cooldown default: a message can
 * only display in the foreground, so time-since-last-display is always at least
 * time-spent-in-background, and aligning them guarantees the cooldown cannot suppress a warm
 * session-start message.
 *
 * Do not raise this dynamically when the server raises the cooldown: a later return with a
 * 35-second gap would then stop counting as a new session at all.
 */
internal class SessionState(
    private val clock: Clock,
    private val sessionTimeoutMillis: Long = DEFAULT_SESSION_TIMEOUT_MS
) {

    companion object {
        const val DEFAULT_SESSION_TIMEOUT_MS = 30_000L
    }

    private var lastPausedAtMillis: Long? = null

    /**
     * First pause wins.
     *
     * This is the fix that does not depend on getting the callback taxonomy exactly right: if
     * a future refactor adds another source of "backgrounded", the earliest stamp still
     * measures the real absence. Last-wins would measure every absence as zero, so
     * session_start would only ever fire on a cold launch and every "welcome back" campaign
     * would be dead for the life of the install.
     */
    fun onBackgrounded() {
        if (lastPausedAtMillis == null) lastPausedAtMillis = clock.nowMillis()
    }

    /** True when the absence exceeded the timeout and a new session should start. */
    fun onForegrounded(): Boolean {
        val since = lastPausedAtMillis ?: return false
        lastPausedAtMillis = null
        return clock.nowMillis() - since > sessionTimeoutMillis
    }

    fun reset() {
        lastPausedAtMillis = null
    }
}
