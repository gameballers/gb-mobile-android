package com.gameball.gameball

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.OnNewIntentProvider
import androidx.lifecycle.ProcessLifecycleOwner
import com.gameball.gameball.engagement.AppOpen
import com.gameball.gameball.engagement.AppOpenObserver
import com.gameball.gameball.engagement.InfluencedOpens
import com.gameball.gameball.engagement.PushEngagementType
import com.gameball.gameball.engagement.ReceivedPushes
import com.gameball.gameball.engagement.pushEngagementBody
import com.gameball.gameball.inappmessaging.GameballInAppMessaging
import com.gameball.gameball.inappmessaging.InAppMessagingOptions
import com.gameball.gameball.inappmessaging.runtime.IamLog
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
import com.gameball.gameball.push.GameballPush
import com.gameball.gameball.push.PushNotificationDisplay
import com.gameball.gameball.push.detectPush
import com.gameball.gameball.services.GameballCoroutineService
import com.gameball.gameball.utils.Constants
import com.gameball.gameball.utils.Constants.TAG
import com.gameball.gameball.utils.LanguageUtils
import com.gameball.gameball.views.GameballWidgetActivity
import com.google.firebase.messaging.RemoteMessage
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
    private var apiPrefix: String? = null

    /**
     * Lazy on purpose: constructing it does nothing, and until startInAppMessaging is called
     * the module makes no requests, arms no timers, writes no storage, draws nothing and
     * registers no lifecycle callbacks.
     */
    private val inAppMessaging: GameballInAppMessaging by lazy {
        GameballInAppMessaging(mContext.applicationContext)
    }

    // --- engagement signals: app opens, push taps, swipe-aways and push arrivals ---

    private val appOpens = AppOpenObserver { open -> onAppOpen(open) }

    // Opens that follow an untapped push, from the pushes saved as they arrive
    private val influencedOpens by lazy { InfluencedOpens(ReceivedPushes(mContext)) }

    // Taps already handled in this process, so one tap that reaches the SDK twice counts once
    private val handledTaps = mutableSetOf<String>()

    private var pushLifecycleRegistered = false

    init {
        SharedPreferencesUtils.init(mContext, Gson())
        SharedPreferencesUtils.getInstance().putClientBotSettings(null)

        // Settles the in-app messaging logging default up front, so inAppMessagingLogging reads
        // correctly before messaging starts. It only reads the app's debuggable flag.
        try {
            IamLog.resolveDefaultFrom(mContext)
        } catch (t: Throwable) {
            Log.e(TAG, "could not read the app's build type for in-app messaging logging", t)
        }
    }

    companion object {
        /** Purchases reach campaigns as an ordinary event, so filters work on their fields. */
        private const val PURCHASE_EVENT = "purchase"

        /** The action FCM delivers incoming messages with, to FirebaseMessagingService.handleIntent. */
        private const val RECEIVE_ACTION = "com.google.android.c2dm.intent.RECEIVE"

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

        /**
         * Whether in-app messaging writes its diagnostics to logcat, under the tag GameballIAM.
         *
         * On in a debuggable build and off in a release build, where these lines reach every end
         * user's device log. Set it to true in a release build only to reproduce a reported
         * problem. Takes effect immediately, before or after startInAppMessaging.
         */
        @JvmStatic
        var inAppMessagingLogging: Boolean
            get() = IamLog.enabled
            set(value) {
                IamLog.enabled = value
            }
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
        if (!customerPreferredLanguage.isNullOrEmpty()) {
            SharedPreferencesUtils.getInstance().putCustomerPreferredLanguage(LanguageUtils.normalizeLanguage(customerPreferredLanguage))
        }
    }

    private fun applyGlobalLanguage(lang: String): String {
        val language = LanguageUtils.normalizeLanguage(lang)
        SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(language)
        return language
    }

    /** Initialize the Gameball SDK */
    fun init(config: GameballConfig) {
        config.apiPrefix?.let {
            gameBallApi = Network.getInstance().getGameBallApi(it)
            apiPrefix = it
        }

        this.mApiKey = config.apiKey

        applyGlobalLanguage(config.lang)

        config.platform?.let { SharedPreferencesUtils.getInstance().putPlatformPreference(it) }
        config.shop?.let { SharedPreferencesUtils.getInstance().putShopPreference(it) }

        config.sessionToken?.let { SharedPreferencesUtils.getInstance().putSessionTokenPreference(it) } ?: SharedPreferencesUtils.getInstance().removeSessionTokenPreference()

        SharedPreferencesUtils.getInstance().putOSPreference(this.OS)
        SharedPreferencesUtils.getInstance().putSDKPreference(this.SDKVersion)
        SharedPreferencesUtils.getInstance().putApiKey(this.mApiKey)

        logger.api = gameBallApi

        getBotSettings()

        startEngagementSignals()
    }

    /**
     * Changes the SDK's global language on demand, without re-calling [init]. Affects everything
     * that isn't overridden per-call: future [showProfile] presentations that don't pass their own
     * `lang`, and any other SDK call that resolves language via [LanguageUtils.handleLanguage].
     *
     * A `showProfile` call with an explicit `lang` still takes precedence over this for that one
     * presentation — this only changes the fallback used when no per-call override is given.
     *
     * Also becomes the customer's preferred language and is synced to the last initialized customer's profile.
     *
     * @param lang A supported language code (e.g. "en", "ar"). Any other code falls back to "en".
     */
    fun setLanguage(lang: String) {
        val language = applyGlobalLanguage(lang)

        // Customer language is read before the global one, so set it too
        SharedPreferencesUtils.getInstance().putCustomerPreferredLanguage(language)

        val customerId = SharedPreferencesUtils.getInstance().getCustomerId()
        if (mApiKey.isNullOrBlank() || customerId.isNullOrEmpty()) return

        val request = InitializeCustomerRequest.builder()
            .customerId(customerId)
            .customerAttributes(CustomerAttributes.builder().preferredLanguage(language).build())
            .build()

        // Pass the current session token: initializeCustomer replaces the stored one
        initializeCustomer(request, object : Callback<InitializeCustomerResponse> {
            override fun onSuccess(response: InitializeCustomerResponse) {}

            override fun onError(error: Throwable) {}
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
        // Another customer's pushes must never credit this customer's opens
        if (SharedPreferencesUtils.getInstance().getCustomerId() != customerRequest.customerId) {
            influencedOpens.clear()
        }
        SharedPreferencesUtils.getInstance().putCustomerId(customerRequest.customerId)

        GameballCoroutineService.initializeCustomerService(TAG, customerRequest, callback, gameBallApi)

        // Its own guard, outside the Rx chain above: a throw inside an addition to someone
        // else's chain escapes into their code path.
        try {
            if (inAppMessaging.isStarted) {
                // Keeps the options the host started with; restarting with defaults would
                // quietly drop their hooks.
                inAppMessaging.onCustomerChanged(
                    customerRequest.customerId, apiPrefix, SDKVersion
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "in-app messaging could not follow the customer change", t)
        }
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

        // Pass the name AND the metadata map. Passing only the name would make every
        // metadata-filtered campaign unmatchable, while every unit test still passed.
        try {
            event.events.forEach { (name, metadata) -> inAppMessaging.onEvent(name, metadata) }
        } catch (t: Throwable) {
            Log.e(TAG, "in-app messaging could not process the event", t)
        }
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
        ) {
            // The widget closed, however it was closed: a message held back while it was open
            // may show now.
            try {
                inAppMessaging.onWidgetClosed()
            } catch (t: Throwable) {
                Log.e(TAG, "in-app messaging could not record the widget closing", t)
            }
        }
    }

    /** Hides the currently shown profile widget. No-op when nothing is shown. Counterpart to [showProfile]. */
    fun hideProfile() {
        // In-app messaging hears about the close from the widget itself, as for every other way
        // the widget can close.
        GameballWidgetActivity.closeCurrentWidget()
    }

    /**
     * Detects a Gameball push in [intent] and reports it: a tap as a click, a swipe-away as a
     * dismissal. The SDK calls this itself for every activity launch and for its dismiss
     * receiver, so most apps never need to. Call it only for an intent those can't see, such as
     * one a plain Activity (not a ComponentActivity) receives in onNewIntent.
     */
    fun handlePushClick(intent: Intent?) {
        detectPush(intent, onClick = { onClick(it) }, onDismiss = { onDismiss(it) })
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
     * Kept for apps that followed the 4.0.0 docs. Taps are now detected automatically, so a tap
     * reported both ways still counts once.
     *
     * @param sessionToken Optional session token for this request.
     *                     If provided, overrides the global sessionToken.
     *                     If null, clears the global sessionToken.
     */
    @Deprecated("Taps on Gameball pushes are now detected automatically. For an intent the SDK can't see, call handlePushClick(intent).")
    @JvmOverloads
    fun handlePushClick(payload: Map<String, String>, callback: Callback<Boolean>? = null, sessionToken: String? = null): Boolean {
        if (!payload[Constants.IS_GB_KEY].equals("true", ignoreCase = true)) {
            return false
        }

        // The open a Gameball tap causes is a normal click, even when the tap can't be reported
        // (no token, or no API key yet). A second mark before the same open changes nothing.
        appOpens.onTap()

        val token = payload[Constants.PUSH_CLICK_TOKEN_KEY]

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

        // Automatic detection may already have reported this tap
        if (!synchronized(handledTaps) { handledTaps.add(token) }) {
            callback?.onSuccess(true)
            return true
        }

        // A tapped push is a normal tap and never counts toward an influenced open
        influencedOpens.onTap(token)
        reportPushEngagement(token, PushEngagementType.CLICKED, callback)
        return true
    }

    /**
     * Lets the SDK see Gameball pushes as they arrive: it saves each one for influenced opens
     * and shows it itself, so swipe-aways are tracked. Call it first in your
     * FirebaseMessagingService.handleIntent, and skip super.handleIntent when it returns true.
     *
     * Returns false for other pushes, while the app is on screen (FCM then calls
     * onMessageReceived as usual), and whenever the push can't be shown this way, so FCM shows it.
     */
    fun handlePushIntent(intent: Intent): Boolean = try {
        val extras = intent.extras
        val push = if (intent.action == RECEIVE_ACTION) GameballPush.from(extras) else null
        if (push == null || extras == null) {
            false
        } else {
            // Every arrival passes here once the app opts in, whatever state the app is in.
            // RemoteMessage reads FCM's google.sent_time whether it arrives as a Long or a String.
            val sentAt = RemoteMessage(extras).sentTime
            logSignal(
                "push_received",
                "clickToken" to push.clickToken,
                "sentAt" to sentAt,
                "receivedAt" to System.currentTimeMillis()
            )
            // Saved before the push is shown; a failure here never stops it from showing
            push.key?.let {
                try {
                    influencedOpens.onReceived(it, push.clickToken, sentAt)
                } catch (t: Throwable) {
                    Log.e(TAG, "could not save a received Gameball push", t)
                }
            }
            PushNotificationDisplay.handle(mContext, extras)
        }
    } catch (t: Throwable) {
        // Never in the way of the push itself: FCM shows it as usual
        Log.e(TAG, "could not handle an incoming Gameball push", t)
        false
    }

    // Registers what notices taps and app opens. Safe on every init.
    private fun startEngagementSignals() {
        (mContext as? Application)?.let { registerPushLifecycle(it) }
        // addObserver only works on the main thread, and init can run on any thread.
        // Adding the same observer again is ignored, so a second init is safe.
        Handler(Looper.getMainLooper()).post { ProcessLifecycleOwner.get().lifecycle.addObserver(appOpens) }
    }

    @Synchronized
    private fun registerPushLifecycle(app: Application) {
        // init can run more than once, and Android would call a duplicate registration twice
        if (pushLifecycleRegistered) return
        pushLifecycleRegistered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Saved state means a rotation or process restore re-delivering a tap already handled
                if (savedInstanceState == null) handlePushClick(activity.intent)
                // Warm taps on singleTop and singleTask activities arrive in onNewIntent instead
                (activity as? OnNewIntentProvider)?.addOnNewIntentListener { handlePushClick(it) }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    // A tap on a Gameball push: counted once and reported. App opens are a separate signal.
    private fun onClick(push: GameballPush) {
        val key = push.key
        if (key != null && !synchronized(handledTaps) { handledTaps.add(key) }) return
        logSignal("push_click", "clickToken" to push.clickToken)
        // A tapped push is a normal tap and never counts toward an influenced open
        key?.let { influencedOpens.onTap(it) }
        // Nor does the open the tap causes, whatever else is saved
        appOpens.onTap()
        push.clickToken?.let { reportPushEngagement(it, PushEngagementType.CLICKED) }
    }

    // A swipe-away of a push the SDK showed; a delete intent fires once, so there's nothing to drop
    private fun onDismiss(push: GameballPush) {
        logSignal("push_dismiss", "clickToken" to push.clickToken)
        push.clickToken?.let { reportPushEngagement(it, PushEngagementType.DISMISSED) }
    }

    private fun onAppOpen(open: AppOpen) {
        logSignal("app_open", "type" to open.type, "openedAt" to open.openedAt, "fromTap" to open.fromTap)
        // Not from a tap, and the most recent untapped push arrived within the window: it influenced this open
        influencedOpens.influencedBy(open)?.let { push ->
            logSignal(
                "influenced_open",
                "clickToken" to push.clickToken,
                "sentAt" to push.sentAt,
                "receivedAt" to push.receivedAt,
                "type" to open.type,
                "openedAt" to open.openedAt
            )
            // Timed at the open, which is what the push influenced
            push.clickToken?.let { reportPushEngagement(it, PushEngagementType.INFLUENCED_OPEN, occurredAt = open.openedAt) }
        }
    }

    // Every signal is logged. App opens and arrivals have no endpoint, so for them that's all.
    private fun logSignal(name: String, vararg fields: Pair<String, Any?>) {
        Log.d(TAG, "engagement $name " + fields.joinToString(" ") { (key, value) -> "$key=$value" })
    }

    // Sent once, with no retry. A push without a click token can't be reported.
    private fun reportPushEngagement(
        token: String,
        type: PushEngagementType,
        callback: Callback<Boolean>? = null,
        occurredAt: Long = System.currentTimeMillis()
    ) {
        gameBallApi.reportPushEngagement(pushEngagementBody(token, type, occurredAt))
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
    }

    // --- in-app messaging -------------------------------------------------------------
    //
    // Entirely opt-in. A host that never calls startInAppMessaging is unaffected by this
    // module: nothing is constructed, nothing is requested, nothing is registered.

    /**
     * Starts in-app messaging for [customerId].
     *
     * Idempotent for the same customer - a second call logs and returns. A different customer
     * refetches, resets frequency caps and discards the previous customer's cached campaigns
     * and stored values, so there is no need to stop first.
     *
     * Call it after [init]. Campaigns are configured in the Gameball dashboard.
     */
    @JvmOverloads
    fun startInAppMessaging(customerId: String, options: InAppMessagingOptions? = null) {
        try {
            IamLog.resolveDefaultFrom(mContext)
            inAppMessaging.start(
                customerId = customerId,
                options = options ?: InAppMessagingOptions.builder().build(),
                apiPrefix = apiPrefix,
                sdkVersion = SDKVersion
            )
        } catch (t: Throwable) {
            // Messaging must never take the host app down.
            Log.e(TAG, "in-app messaging failed to start", t)
        }
    }

    /**
     * Stops in-app messaging: dismisses anything showing, flushes pending telemetry, clears
     * per-customer state and unregisters the module's lifecycle callbacks. Safe to call when it
     * was never started.
     */
    fun stopInAppMessaging() {
        try {
            inAppMessaging.stop()
        } catch (t: Throwable) {
            Log.e(TAG, "in-app messaging failed to stop cleanly", t)
        }
    }

    /** Whether in-app messaging is currently running. */
    fun isInAppMessagingStarted(): Boolean = try {
        inAppMessaging.isStarted
    } catch (t: Throwable) {
        false
    }

    /**
     * Logs a purchase: sends Gameball an event named `purchase` with [productId], [price],
     * [currency] and [quantity] as its metadata, whether or not in-app messaging is running.
     * While it is, the same event reaches purchase-triggered campaigns, which can filter on
     * those fields and on [properties].
     *
     * Routed through [sendEvent], which is the single place that feeds the trigger engine, so
     * one call produces exactly one trigger occurrence. Notifying the module here as well
     * would fire it twice, and the duplicate would take the pending slot from whatever was
     * legitimately waiting.
     *
     * A blank [customerId] is reported to [callback] and nothing is sent. [callback] and
     * [sessionToken] behave as they do on [sendEvent].
     */
    @JvmOverloads
    fun logPurchase(
        customerId: String,
        productId: String,
        price: Double,
        currency: String,
        quantity: Int = 1,
        properties: Map<String, Any>? = null,
        callback: Callback<Boolean>? = null,
        sessionToken: String? = null
    ) {
        if (customerId.isBlank()) {
            val error = IllegalArgumentException("logPurchase needs a customer ID")
            Log.e(TAG, error.message, error)
            callback?.onError(error)
            return
        }
        val builder = Event.builder()
            .customerId(customerId)
            .eventName(PURCHASE_EVENT)
            .eventMetaData("productId", productId)
            .eventMetaData("price", price)
            .eventMetaData("currency", currency)
            .eventMetaData("quantity", quantity)
        properties?.forEach { (key, value) -> builder.eventMetaData(key, value) }

        sendEvent(
            builder.build(),
            callback ?: object : Callback<Boolean> {
                override fun onSuccess(t: Boolean?) = Unit
                override fun onError(e: Throwable?) = Unit
            },
            sessionToken
        )
    }
}
