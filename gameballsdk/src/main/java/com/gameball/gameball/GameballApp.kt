package com.gameball.gameball

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Nullable
import com.gameball.gameball.local.SharedPreferencesUtils
import com.gameball.gameball.model.enums.PushProvider
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
    private var mCustomerId: String? = null
    private var mCustomerEmail: String? = null
    private var mCustomerMobile: String? = null
    private var mDeviceToken: String? = null
    private var mPushProvider: PushProvider? = null
    private var mIsGuest: Boolean? = null
    private var shop: String? = null
    private var platform: String? = null
    private val SDKVersion = BuildConfig.SDK_VERSION
    private val OS = String.format("android-sdk-%s", Build.VERSION.SDK_INT)
    private var mReferralCode: String? = null

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

    private fun registerDevice(@Nullable customerAttributes: CustomerAttributes?, callback: Callback<InitializeCustomerResponse>) {
        if (mCustomerId == null || mApiKey == null) {
            return
        }

        SharedPreferencesUtils.getInstance().putApiKey(mApiKey!!)
        SharedPreferencesUtils.getInstance().putCustomerId(mCustomerId!!)

        val registerDeviceRequest = InitializeCustomerRequest.builder()
            .customerId(mCustomerId!!)
            .apply { mReferralCode?.let { referralCode(it) } }
            .apply { mCustomerMobile?.let { mobile(it) } }
            .apply { mCustomerEmail?.let { email(it) } }
            .apply { customerAttributes?.let { customerAttributes(it) } }
            .isGuest(mIsGuest ?: false)
            .build()

        GameballCoroutineService.registerDevice(TAG, registerDeviceRequest, mPushProvider, mDeviceToken, callback, gameBallApi)
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
        if (config.apiPrefix != null) {
            gameBallApi = Network.getInstance().getGameBallApi(config.apiPrefix)
        }
        
        this.platform = config.platform
        this.shop = config.shop
        this.mApiKey = config.apiKey

        if (config.lang != null && config.lang.length == 2) {
            SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(config.lang)
        }

        config.platform?.let { SharedPreferencesUtils.getInstance().putPlatformPreference(it) }
        config.shop?.let { SharedPreferencesUtils.getInstance().putShopPreference(it) }
        SharedPreferencesUtils.getInstance().putOSPreference(this.OS)
        SharedPreferencesUtils.getInstance().putSDKPreference(this.SDKVersion)
        SharedPreferencesUtils.getInstance().putApiKey(this.mApiKey)

        getBotSettings()
    }


    /** Change the preferred language */
    fun changeLanguage(lang: String) {
        if (lang.length == 2) {
            SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(lang)
        }
    }

    /** Register a customer using builder pattern request */
    fun initializeCustomer(
        customerRequest: InitializeCustomerRequest,
        callback: Callback<InitializeCustomerResponse>
    ) {
        try {
            if (customerRequest.customerId?.isNotEmpty() == true) {
                this.mCustomerId = customerRequest.customerId
            } else {
                Log.e(TAG, "Customer registration: customerId cannot be empty")
                return
            }

            customerRequest.email?.let { this.mCustomerEmail = it }
            customerRequest.mobile?.let { this.mCustomerMobile = it }
            
            val attributes = customerRequest.customerAttributes
            attributes?.let { setCustomerPreferredLanguage(it.preferredLanguage) }
            
            this.mReferralCode = customerRequest.referralCode
            this.mIsGuest = customerRequest.isGuest

            registerDevice(attributes, callback)
        } catch (t: Throwable) {
            Log.d(TAG, t.message, t)
            this.mReferralCode = null
            registerDevice(null, callback)
        }
    }

    /** Send an event using builder pattern request */
    fun sendEvent(event: Event, callback: Callback<Boolean>) {
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
    }

    /** Show profile widget */
    fun showProfile(activity: Activity, profileRequest: ShowProfileRequest) {
        SharedPreferencesUtils.getInstance().putOpenDetailPreference(profileRequest.openDetail)
        SharedPreferencesUtils.getInstance().putHideNavigationPreference(profileRequest.hideNavigation)

        GameballWidgetActivity.start(
            activity,
            profileRequest.customerId,
            profileRequest.showCloseButton,
            profileRequest.closeButtonColor,
            profileRequest.widgetUrlPrefix,
            profileRequest.capturedLinkCallback
        )
    }
}