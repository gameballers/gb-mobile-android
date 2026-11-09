package com.gameball.gameball.engagement

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * One app-level open. [type] is cold for the first in this process, warm for a later one.
 * [fromTap] means a tap on a Gameball push brought the app up: a normal click, never influenced.
 */
internal data class AppOpen(val type: String, val openedAt: Long, val fromTap: Boolean = false)

/**
 * Watches the app as a whole through ProcessLifecycleOwner, so moving between screens and
 * rotating never count as opens. A return within [sessionGapMillis] continues the last open,
 * the same rule as an in-app messaging session.
 */
internal class AppOpenObserver(
    private val now: () -> Long = System::currentTimeMillis,
    private val sessionGapMillis: Long = SESSION_GAP_MS,
    private val report: (AppOpen) -> Unit
) : DefaultLifecycleObserver {

    private var started = false
    private var inForeground = false
    private var backgroundedAt: Long? = null
    private var tappedAt: Long? = null

    /**
     * A tap on a Gameball push. Android delivers it before the open it causes, cold and warm.
     * One made while the app is on screen opens nothing, so it marks nothing.
     */
    @Synchronized
    fun onTap() {
        if (!inForeground) tappedAt = now()
    }

    override fun onStart(owner: LifecycleOwner) {
        val open = synchronized(this) { foregrounded(now()) } ?: return
        report(open)
    }

    @Synchronized
    override fun onStop(owner: LifecycleOwner) {
        inForeground = false
        backgroundedAt = now()
    }

    // Every return uses up a pending tap, even one that isn't an open, so it can't mark a later one
    private fun foregrounded(at: Long): AppOpen? {
        val fromTap = tappedAt?.let { at - it in 0..TAP_TO_OPEN_MS } == true
        tappedAt = null
        inForeground = true
        val away = backgroundedAt?.let { at - it }
        backgroundedAt = null
        if (started && away != null && away <= sessionGapMillis) return null
        val open = AppOpen(if (started) TYPE_WARM else TYPE_COLD, at, fromTap)
        started = true
        return open
    }

    companion object {
        const val TYPE_COLD = "cold"
        const val TYPE_WARM = "warm"

        /** In-app messaging's default session timeout, so both agree on what an open is. */
        const val SESSION_GAP_MS = 30_000L

        /** A tap only explains an open that follows within this time, not one hours later. */
        const val TAP_TO_OPEN_MS = 60_000L
    }
}
