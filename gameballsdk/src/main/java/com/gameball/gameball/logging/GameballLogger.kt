package com.gameball.gameball.logging

import android.content.Context
import android.os.Build
import com.gameball.gameball.BuildConfig
import com.gameball.gameball.local.SharedPreferencesUtils
import com.gameball.gameball.network.api.GameBallApi
import io.reactivex.schedulers.Schedulers

/**
 * Fail-silent SDK telemetry logger. Fires one diagnostic entry per call directly to the Gameball
 * backend (api/v4.0/integrations/mobile/logs, forwarded to Datadog). The payload is sent as-is,
 * immediately and fire-and-forget; this layer must never throw into, or block, the host app.
 */
class GameballLogger(private val context: Context) {

    /** The active API client. Set by GameballApp on init (and re-set if a custom apiPrefix is used). */
    @Volatile
    var api: GameBallApi? = null

    // Built once; device/SDK context is identical for every entry from this install.
    private val deviceContext: Map<String, Any?> by lazy { buildContext() }

    /** Fire one SDK event immediately. Safe from any thread; never throws. params is sent as-is. */
    @JvmOverloads
    fun log(event: String, params: Any? = null) {
        try {
            val currentApi = api ?: return

            val entry = HashMap<String, Any?>()
            entry["event"] = event
            entry["timestamp"] = System.currentTimeMillis()
            params?.let { entry["params"] = it }

            val body = HashMap<String, Any?>()
            body["context"] = deviceContext
            body["logs"] = listOf(entry)

            currentApi.sendMobileLogs(body)
                .subscribeOn(Schedulers.io())
                .subscribe({ /* no-op */ }, { /* swallow: fail-silent */ })
        } catch (ignored: Throwable) {
            // Telemetry must never affect the host app.
        }
    }

    private fun buildContext(): Map<String, Any?> {
        val ctx = HashMap<String, Any?>()
        ctx["sdkType"] = "android"
        ctx["sdkVersion"] = BuildConfig.SDK_VERSION
        ctx["devicePlatform"] = "Android"
        ctx["deviceOsVersion"] = Build.VERSION.RELEASE
        ctx["deviceModel"] = "${Build.MANUFACTURER} ${Build.MODEL}"
        ctx["appBundleId"] = context.packageName
        ctx["installId"] = try {
            SharedPreferencesUtils.getInstance().getOrCreateInstallId()
        } catch (e: Throwable) {
            null
        }
        return ctx
    }
}
