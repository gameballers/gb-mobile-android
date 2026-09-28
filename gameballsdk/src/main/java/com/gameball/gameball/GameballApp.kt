package com.gameball.gameball

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import com.gameball.gameball.local.SharedPreferencesUtils
import com.gameball.gameball.logging.GameballLogger
import com.gameball.gameball.model.request.CustomerAttributes
import com.gameball.gameball.model.request.Event
import com.gameball.gameball.model.request.GameballConfig
import com.gameball.gameball.model.request.InitializeCustomerRequest
import com.gameball.gameball.model.request.ShowProfileRequest
import com.gameball.gameball.model.response.BaseResponse
import com.gameball.gameball.model.response.ClientBotSettings
import com.gameball.gameball.model.response.InitializeCustomerResponse
import com.gameball.gameball.network.Callback
import com.gameball.gameball.network.Network
import com.gameball.gameball.network.api.GameBallApi
import com.gameball.gameball.services.GameballCoroutineService
import com.gameball.gameball.utils.Constants
import com.gameball.gameball.utils.Constants.TAG
import com.gameball.gameball.views.GameballWidgetActivity
import com.google.gson.Gson
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Primary Gameball SDK entry point providing comprehensive functionality
 * with modern Kotlin API and builder pattern request models.
 */
class GameballApp private constructor(context: Context) {

    private val mContext: Context = context
    private var gameBallApi: GameBallApi = Network.getInstance().getGameBallApi()
    private var mApiKey: String? = null
    private val SDKVersion = BuildConfig.SDK_VERSION
    private val OS = String.format("android-sdk-%s", Build.VERSION.SDK_INT)
    private val logger = GameballLogger(mContext)

    /**
     * The customer from this process's last [initializeCustomer], kept in memory for [setLanguage]'s
     * profile sync. Deliberately not the persisted customer ID: that survives app restarts and may
     * belong to a customer from an earlier session.
     */
    private var customerId: String? = null

    /**
     * Carried over from the last [initializeCustomer] so [setLanguage] can re-send it as-is, rather
     * than falling back to the builder's default of `false` and flipping a guest customer to registered.
     */
    private var customerIsGuest: Boolean? = null

    init {
        SharedPreferencesUtils.init(mContext, Gson())
        SharedPreferencesUtils.getInstance().putClientBotSettings(null)
    }

    companion object {
        @Volatile
        private var instance: GameballApp? = null

        /**
         * Initialize and obtain the singleton instance. Must be called before
         * [getInstance].
         */
        @JvmStatic
        private fun initGameball(context: Context): GameballApp =
            instance ?: synchronized(this) {
                instance ?: GameballApp(context.applicationContext).also { instance = it }
            }

        /**
         * Retrieve the singleton instance.
         */
        @JvmStatic
        fun getInstance(context: Context): GameballApp = initGameball(context)
    }


    private fun getBotSettings() {
        gameBallApi.getBotSettings()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<BaseResponse<ClientBotSettings>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(clientBotSettingsBaseResponse: BaseResponse<ClientBotSettings>) {
                    SharedPreferencesUtils.getInstance().putClientBotSettings(clientBotSettingsBaseResponse.response)
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "bot_settings_error", e)
                }
            })
    }

    private fun setCustomerPreferredLanguage(customerPreferredLanguage: String?) {
        if (customerPreferredLanguage != null && customerPreferredLanguage.length == 2) {
            SharedPreferencesUtils.getInstance().putCustomerPreferredLanguage(customerPreferredLanguage)
        }
    }

    /** Initialize the Gameball SDK */
    fun init(config: GameballConfig) {
        config.apiPrefix?.let {
            gameBallApi = Network.getInstance().getGameBallApi(it)
        }

        this.mApiKey = config.apiKey

        if (config.lang.length == 2) {
            SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(config.lang)
        }

        config.platform?.let { SharedPreferencesUtils.getInstance().putPlatformPreference(it) }
        config.shop?.let { SharedPreferencesUtils.getInstance().putShopPreference(it) }

        config.sessionToken?.let { SharedPreferencesUtils.getInstance().putSessionTokenPreference(it) } ?: SharedPreferencesUtils.getInstance().removeSessionTokenPreference()

        SharedPreferencesUtils.getInstance().putOSPreference(this.OS)
        SharedPreferencesUtils.getInstance().putSDKPreference(this.SDKVersion)
        SharedPreferencesUtils.getInstance().putApiKey(this.mApiKey)

        // Point the logger at the active API client and record the init call (config logged as-is).
        logger.api = gameBallApi
        logger.log("sdk.init", config)

        getBotSettings()
    }

    /**
     * Changes the SDK's global language on demand, without re-calling [init]. Affects everything
     * that isn't overridden per-call: future [showProfile] presentations that don't pass their own
     * `lang`, and any other SDK call that resolves language via [LanguageUtils.handleLanguage].
     *
     * A `showProfile` call with an explicit `lang` still takes precedence over this for that one
     * presentation — this only changes the fallback used when no per-call override is given.
     *
     * The language also becomes the customer's preferred language, taking precedence over one set
     * earlier through [initializeCustomer]. When the SDK is initialized and a customer has been
     * initialized in this process, it is also mirrored onto that customer's Gameball profile.
     *
     * @param lang A 2-letter language code (e.g. "en", "ar"). Ignored if invalid.
     */
    fun setLanguage(lang: String) {
        if (lang.length != 2) return

        SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(lang)

        // Also claim the customer-level preference, which LanguageUtils.handleLanguage() reads
        // *before* the global one. Without this an explicit setLanguage would be silently outranked
        // by whatever initializeCustomer last persisted. Deliberately done here and not in init(),
        // which writes the global language too: doing it there would overwrite the customer's real
        // preference with the app-wide default on every launch.
        SharedPreferencesUtils.getInstance().putCustomerPreferredLanguage(lang)

        logger.log("sdk.setLanguage", mapOf("lang" to lang))

        // The writes above only steer this device. Mirror the preference onto the customer's
        // Gameball profile so server-driven communications follow it too, sending just
        // preferredLanguage and letting the server merge it into the existing attributes.
        // Skipped until the SDK is initialized and a customer has been initialized in this
        // process. The skipped sync is not replayed: initializeCustomer only sends the
        // preferredLanguage in its own request, so callers pass it there or call setLanguage after.
        val customerId = this.customerId
        if (mApiKey.isNullOrBlank() || customerId.isNullOrEmpty()) return

        val request = InitializeCustomerRequest.builder()
            .customerId(customerId)
            .customerAttributes(CustomerAttributes.builder().preferredLanguage(lang).build())
            .isGuest(customerIsGuest)
            .build()

        // Routed through the public initializeCustomer rather than the network layer so the API-key
        // guard and the customerId/guest bookkeeping stay in one place. The current session token
        // has to be passed explicitly: initializeCustomer overwrites the stored one with its
        // argument, and null clears it.
        initializeCustomer(request, object : Callback<InitializeCustomerResponse> {
            override fun onSuccess(response: InitializeCustomerResponse) {
                logger.log("sdk.setLanguage.profileSync", mapOf("lang" to lang))
            }

            override fun onError(error: Throwable) {
                logger.log("sdk.setLanguage.profileSync", mapOf("lang" to lang, "error" to error.toString()))
            }
        }, SharedPreferencesUtils.getInstance().getSessionTokenPreference())
    }

    /**
     * Register a customer using builder pattern request
     *
     * @param customerRequest The customer initialization request
     * @param callback Callback for handling the response
     * @param sessionToken Optional session token for this request.
     *                     If provided, overrides the global sessionToken.
     *                     If null, clears the global sessionToken.
     */
    @JvmOverloads
    fun initializeCustomer(
        customerRequest: InitializeCustomerRequest,
        callback: Callback<InitializeCustomerResponse>,
        sessionToken: String? = null
    ) {
        if (mApiKey.isNullOrBlank()) {
            val error = IllegalStateException("API key is required for customer initialization")
            Log.e(TAG, error.message, error)
            callback.onError(error)
            return
        }

        // Override or clear sessionToken based on parameter
        sessionToken?.let {
            SharedPreferencesUtils.getInstance().putSessionTokenPreference(it)
        } ?: SharedPreferencesUtils.getInstance().removeSessionTokenPreference()

        val attributes = customerRequest.customerAttributes
        attributes?.let { setCustomerPreferredLanguage(it.preferredLanguage) }

        // Save to SharedPreferences
        SharedPreferencesUtils.getInstance().putApiKey(mApiKey!!)
        SharedPreferencesUtils.getInstance().putCustomerId(customerRequest.customerId)

        // Also tracked in memory, for setLanguage's profile sync
        customerId = customerRequest.customerId
        customerIsGuest = customerRequest.isGuest

        GameballCoroutineService.initializeCustomerService(TAG, customerRequest, callback, gameBallApi)
        // Fire telemetry immediately after dispatching the request.
        logger.log("sdk.initializeCustomer", customerRequest)
    }

    /**
     * Send an event using builder pattern request
     *
     * @param event The event to be sent
     * @param callback Callback for handling the response
     * @param sessionToken Optional session token for this request.
     *                     If provided, overrides the global sessionToken.
     *                     If null, clears the global sessionToken.
     */
    @JvmOverloads
    fun sendEvent(event: Event, callback: Callback<Boolean>, sessionToken: String? = null) {
        if (mApiKey.isNullOrBlank()) {
            val error = IllegalStateException("API key is required for sending events")
            Log.e(TAG, error.message, error)
            callback.onError(error)
            return
        }

        // Override or clear sessionToken based on parameter
        sessionToken?.let {
            SharedPreferencesUtils.getInstance().putSessionTokenPreference(it)
        } ?: SharedPreferencesUtils.getInstance().removeSessionTokenPreference()

        gameBallApi.sendEvent(event)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {}

                override fun onComplete() {
                    callback.onSuccess(true)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
        // Fire telemetry immediately after dispatching the request.
        logger.log("sdk.sendEvent", event)
    }

    /**
     * Show profile widget
     *
     * @param activity The activity context
     * @param profileRequest The profile display request
     * @param sessionToken Optional session token for this request.
     *                     If provided, overrides the global sessionToken.
     *                     If null, clears the global sessionToken.
     */
    @JvmOverloads
    fun showProfile(activity: Activity, profileRequest: ShowProfileRequest, sessionToken: String? = null) {
        // Override or clear sessionToken based on parameter
        sessionToken?.let {
            SharedPreferencesUtils.getInstance().putSessionTokenPreference(it)
        } ?: SharedPreferencesUtils.getInstance().removeSessionTokenPreference()

        SharedPreferencesUtils.getInstance().putOpenDetailPreference(profileRequest.openDetail)
        SharedPreferencesUtils.getInstance().putHideNavigationPreference(profileRequest.hideNavigation)
        SharedPreferencesUtils.getInstance().putMobilePreference(profileRequest.mobile)
        SharedPreferencesUtils.getInstance().putEmailPreference(profileRequest.email)

        // showProfile opens a webview (never hits the backend), so it is invisible server-side — log it here.
        logger.log("sdk.showProfile", profileRequest)

        GameballWidgetActivity.start(
            activity,
            profileRequest.customerId,
            profileRequest.showCloseButton,
            profileRequest.closeButtonColor,
            profileRequest.widgetUrlPrefix,
            profileRequest.lang,
            profileRequest.externalLinkCallback,
            profileRequest.widgetEventCallback
        )
    }

    /** Hides the currently shown profile widget. No-op when nothing is shown. Counterpart to [showProfile]. */
    fun hideProfile() {
        GameballWidgetActivity.closeCurrentWidget()
    }

    /**
     * Handle a tap on a push notification, called from the host app's own
     * notification handler with the notification's FCM data payload
     * (e.g. RemoteMessage.data, or the launcher intent extras when the
     * system tray showed the notification).
     *
     * Returns true when the notification is a Gameball one. When it also
     * carries a click token, the tap is reported to Gameball to count the
     * campaign click; the optional callback receives that report's result.
     *
     * @param sessionToken Optional session token for this request.
     *                     If provided, overrides the global sessionToken.
     *                     If null, clears the global sessionToken.
     */
    @JvmOverloads
    fun handlePushClick(payload: Map<String, String>, callback: Callback<Boolean>? = null, sessionToken: String? = null): Boolean {
        if (!payload[Constants.IS_GB_KEY].equals("true", ignoreCase = true)) {
            return false
        }

        val token = payload[Constants.PUSH_CLICK_TOKEN_KEY]
        // Fire telemetry immediately, regardless of what happens below.
        logger.log("sdk.handlePushClick", mapOf("hasToken" to !token.isNullOrBlank()))

        // Override or clear sessionToken based on parameter
        sessionToken?.let {
            SharedPreferencesUtils.getInstance().putSessionTokenPreference(it)
        } ?: SharedPreferencesUtils.getInstance().removeSessionTokenPreference()

        if (mApiKey.isNullOrBlank()) {
            callback?.onError(IllegalStateException("API key is required for handling push click"))
            return true
        }

        if (token.isNullOrBlank()) {
            return true
        }

        gameBallApi.reportPushClick(mapOf("clickToken" to token))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {}

                override fun onComplete() {
                    callback?.onSuccess(true)
                }

                override fun onError(e: Throwable) {
                    callback?.onError(e)
                }
            })
        return true
    }
}