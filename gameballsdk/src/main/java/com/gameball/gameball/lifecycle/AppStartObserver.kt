package com.gameball.gameball.lifecycle

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.gameball.gameball.utils.Constants.TAG

/** Labels each app foreground: the first one in this process is a cold start, later ones are warm. */
internal object AppStartObserver : DefaultLifecycleObserver {

    private var started = false

    override fun onStart(owner: LifecycleOwner) {
        val type = if (started) "warm" else "cold"
        started = true
        Log.d(TAG, "app start: $type")
    }
}
