package com.gameball.gameball.inappmessaging.runtime

/**
 * Wall-clock time, injectable so selection and session logic are testable without waiting.
 *
 * Wall clock rather than a monotonic one is deliberate: repeat rules and the cooldown floor
 * must survive process death, and a monotonic clock does not. A customer who moves the device
 * clock backwards can suppress messages. Accepted and unfixable client-side.
 */
internal fun interface Clock {
    fun nowMillis(): Long
}

internal val SystemClock = Clock { System.currentTimeMillis() }
