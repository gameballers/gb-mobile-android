package com.gameball.gameball.engagement

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class AppOpenObserverTest {

    private var clock = 1_000_000L
    private val opens = mutableListOf<AppOpen>()
    private val observer = AppOpenObserver(now = { clock }) { opens += it }

    // The observer never looks at the owner
    private val owner = object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = throw UnsupportedOperationException()
    }

    private fun foreground() {
        observer.onStart(owner)
        observer.onResume(owner)
    }

    private fun background() {
        observer.onPause(owner)
        observer.onStop(owner)
    }

    @Test
    fun firstForegroundIsCold() {
        foreground()

        assertEquals(listOf(AppOpen("cold", 1_000_000L)), opens)
    }

    @Test
    fun returnAfterTheGapIsWarm() {
        foreground()
        background()
        clock += 31_000
        foreground()

        assertEquals(listOf("cold", "warm"), opens.map { it.type })
    }

    @Test
    fun returnWithinTheGapIsNotAnOpen() {
        foreground()
        background()
        clock += 10_000
        foreground()

        assertEquals(1, opens.size)
    }

    @Test
    fun openIsReportedAtStart() {
        observer.onStart(owner)

        assertEquals(1, opens.size)
    }

    @Test
    fun aColdOpenFromATapIsMarked() {
        observer.onTap()
        foreground()

        assertEquals(listOf(AppOpen("cold", 1_000_000L, fromTap = true)), opens)
    }

    @Test
    fun aWarmOpenFromATapIsMarked() {
        foreground()
        background()
        clock += 31_000
        observer.onTap()
        foreground()

        assertEquals(listOf(false, true), opens.map { it.fromTap })
    }

    @Test
    fun aTapWhileOnScreenMarksNothing() {
        foreground()
        observer.onTap()
        background()
        clock += 31_000
        foreground()

        assertEquals(listOf(false, false), opens.map { it.fromTap })
    }

    @Test
    fun aReturnWithinTheGapUsesUpTheTap() {
        foreground()
        background()
        clock += 10_000
        observer.onTap()
        foreground() // back within the gap: not an open, but the tap is spent
        background()
        clock += 31_000
        foreground()

        assertEquals(listOf(false, false), opens.map { it.fromTap })
    }

    @Test
    fun aTapLongBeforeTheOpenDoesNotMarkIt() {
        foreground()
        background()
        observer.onTap()
        clock += 2 * 60_000
        foreground()

        assertEquals(listOf(false, false), opens.map { it.fromTap })
    }
}
