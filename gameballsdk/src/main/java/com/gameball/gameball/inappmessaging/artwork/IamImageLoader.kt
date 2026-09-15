package com.gameball.gameball.inappmessaging.artwork

import android.content.Context
import android.widget.ImageView
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

/**
 * The module's image loader.
 *
 * Picasso.get() relies on an auto-initialising ContentProvider and throws
 * IllegalStateException when that has not run - in a unit test, in an app that strips
 * providers, or under some multi-process setups. An SDK cannot let that take a message down,
 * so the instance is built from the application context on first use and every call site is
 * guarded: a load that cannot even start is reported as a failure, exactly like a 404.
 */
internal object IamImageLoader {

    @Volatile
    private var instance: Picasso? = null

    fun init(context: Context) {
        if (instance != null) return
        synchronized(this) {
            if (instance != null) return
            instance = try {
                Picasso.Builder(context.applicationContext).build()
            } catch (t: Throwable) {
                IamLog.e("could not build the image loader; artwork will be skipped", t)
                null
            }
        }
    }

    private fun picasso(): Picasso? = instance ?: runCatching { Picasso.get() }.getOrNull()

    /** Warms the cache with no target view - the prefetcher primitive. */
    fun fetch(url: String, callback: Callback) {
        val picasso = picasso()
        if (picasso == null) {
            callback.onError(IllegalStateException("no image loader"))
            return
        }
        try {
            picasso.load(url).fetch(callback)
        } catch (t: Throwable) {
            callback.onError(Exception(t))
        }
    }

    /** Loads into a view, hitting the cache the prefetcher warmed. */
    fun load(url: String, into: ImageView, callback: Callback) {
        val picasso = picasso()
        if (picasso == null) {
            callback.onError(IllegalStateException("no image loader"))
            return
        }
        try {
            picasso.load(url).into(into, callback)
        } catch (t: Throwable) {
            callback.onError(Exception(t))
        }
    }
}
