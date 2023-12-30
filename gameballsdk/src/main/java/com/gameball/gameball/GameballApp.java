package com.gameball.gameball;

import static com.google.android.gms.tasks.Tasks.await;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.request.Event;
import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Callback;
import com.gameball.gameball.network.Network;
import com.gameball.gameball.network.api.GameBallApi;
import com.gameball.gameball.views.GameballWidgetActivity;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class GameballApp
{
    private static final String TAG = GameballApp.class.getSimpleName();
    private static final String MAIN_ACTIVITY_ACTION = "GAME_BALL_MAIN_ACTIVITY";
    private static final String TAG_GAMEBALL_PROFILE_DIALOG = "gameball_profile_dialog";
    private static GameballApp ourInstance;
    private String APPLICATION_ID = null;
    private String API_KEY = null;
    private String SENDER_ID = null;
    private Context mContext;
    private FirebaseApp clientFirebaseApp;
    private String mApiKey;
    private String mPlayerUniqueId;
    private String mPlayerEmail;
    private String mPlayerMobile;
    private String mDeviceToken;
    private GameBallApi gameBallApi;
    private String shop = null;
    private String platform = null;
    private String SDKVersion = BuildConfig.SDK_VERSION;
    private String OS = String.format("android-sdk-%s", Build.VERSION.SDK_INT);
    private String mReferralCode;

    private String openDetail;

    private Boolean hideNavigation;

    private GameballApp(Context context)
    {
        if (this.mContext == null)
        {
            this.mContext = context;
            gameBallApi = Network.getInstance().getGameBallApi();

            SharedPreferencesUtils.init(mContext, new Gson());
            SharedPreferencesUtils.getInstance().putClientBotSettings(null);
        }
    }

    public static GameballApp getInstance(Context context)
    {
        if (ourInstance == null)
        {
            ourInstance = new GameballApp(context);
        }
        return ourInstance;
    }

    private void registerDevice(@Nullable PlayerAttributes playerAttributes, final Callback<PlayerRegisterResponse> callback)
    {

        if (mPlayerUniqueId == null || mApiKey == null)
        {
            return;
        }

        SharedPreferencesUtils.getInstance().putApiKey(mApiKey);
        SharedPreferencesUtils.getInstance().putPlayerUniqueId(mPlayerUniqueId);

        PlayerRegisterRequest registerDeviceRequest = new PlayerRegisterRequest();
        registerDeviceRequest.setPlayerUniqueID(mPlayerUniqueId);

        if(mReferralCode != null)
            registerDeviceRequest.setReferrerCode(mReferralCode);

        if (mDeviceToken != null)
        {
            registerDeviceRequest.setDeviceToken(mDeviceToken);
            SharedPreferencesUtils.getInstance().putDeviceToken(mDeviceToken);
        }

        if(mPlayerMobile != null)
        {
            registerDeviceRequest.setMobile(mPlayerMobile);
        }

        if(mPlayerEmail != null){
            registerDeviceRequest.setEmail(mPlayerEmail);
        }

        if (playerAttributes != null)
            registerDeviceRequest.setPlayerAttributes(playerAttributes);

        Log.d(TAG, new Gson().toJson(registerDeviceRequest));

        gameBallApi.registrationPlayer(registerDeviceRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<PlayerRegisterResponse>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(PlayerRegisterResponse playerRegisterResponseBaseResponse)
                    {
                        if (callback != null)
                            callback.onSuccess(playerRegisterResponseBaseResponse);
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (callback != null)
                            callback.onError(e);
                    }
                });
    }

    private void getBotSettings()
    {
        gameBallApi.getBotSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BaseResponse<ClientBotSettings>>()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onSuccess(BaseResponse<ClientBotSettings> clientBotSettingsBaseResponse)
                    {
                        SharedPreferencesUtils.getInstance().
                                putClientBotSettings(clientBotSettingsBaseResponse.getResponse());
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        Log.e(TAG, "bot_settings_error", e);
                    }
                });
    }

    public void init(@NonNull String apiKey, String lang, String platform, String shop, @Nullable String apiPrefix){
        gameBallApi = Network.getInstance().getGameBallApi(apiPrefix);
        init(apiKey, lang, platform, shop);
    }
    
    public void init(@NonNull String apiKey, String lang, String platform, String shop)
    {
        this.platform = platform;
        this.shop = shop;
        this.mApiKey = apiKey;

        if(lang != null && lang.length() == 2) {
            SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(lang);
        }

        SharedPreferencesUtils.getInstance().putPlatformPreference(this.platform);

        SharedPreferencesUtils.getInstance().putShopPreference(this.shop);

        SharedPreferencesUtils.getInstance().putOSPreference(this.OS);

        SharedPreferencesUtils.getInstance().putSDKPreference(this.SDKVersion);

        SharedPreferencesUtils.getInstance().putApiKey(this.mApiKey);

        getBotSettings();
    }
    
    public void changeLanguage(String language) {
        if (language != null && language.length() == 2) {
            SharedPreferencesUtils.getInstance().putGlobalPreferredLanguage(language);
        }
    }

    public void initializeFirebase()
    {
        if(isGmsAvailable(this.mContext)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String deviceToken = await(FirebaseMessaging.getInstance().getToken());
                        Log.d(TAG, deviceToken);

                        if(deviceToken != null && !deviceToken.trim().isEmpty()){
                            mDeviceToken = deviceToken;
                            Log.d(TAG, "Device token retrieved successfully");
                        }
                        else{
                            mDeviceToken = null;
                            Log.d(TAG, "Failed to retrieve device token.");
                        }
                    }
                    catch(Throwable t){
                        mDeviceToken = null;
                        Log.d(TAG, t.getMessage(), t);
                    }
                }
            }).start();
        }
    }

    public void initializeFirebase(final FirebaseMessaging firebaseMessagingInstance)
    {
        if(isGmsAvailable(this.mContext)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String deviceToken = await(firebaseMessagingInstance.getInstance().getToken());

                        if(deviceToken != null && !deviceToken.trim().isEmpty()){
                            mDeviceToken = deviceToken;
                            Log.d(TAG, "Device token retrieved successfully");
                        }
                        else{
                            mDeviceToken = null;
                            Log.d(TAG, "Failed to retrieve device token.");
                        }
                    }
                    catch(Throwable t){
                        mDeviceToken = null;
                        Log.d(TAG, t.getMessage(), t);
                    }
                }
            }).start();
        }
    }

    public void initializeFirebase(String deviceToken){
        if(deviceToken != null && !deviceToken.trim().isEmpty()){
            this.mDeviceToken = deviceToken;
        }
    }

    //Checks for referral automatically
    public void registerPlayer(@NonNull final String playerUniqueId, final PlayerAttributes playerAttributes,
                               @NonNull Activity activity, @NonNull Intent intent,
                               @NonNull final Callback<PlayerRegisterResponse> responseCallback)
    {
        try{
            if (!playerUniqueId.trim().isEmpty()){
                this.mPlayerUniqueId = playerUniqueId;
            }
            else {
                Log.e(TAG, "Player registration: PlayerUniqueId cannot be empty");
            }

            if(playerAttributes != null){
                setPlayerPreferredLanguage(playerAttributes.getPreferredLanguage());
            }

            checkReferral(activity, intent, new Callback<String>() {
                @Override
                public void onSuccess(String s) {
                    mReferralCode = s;
                    registerDevice(playerAttributes, responseCallback);
                }
                @Override
                public void onError(Throwable e) {
                    mReferralCode = null;
                    registerDevice(playerAttributes, responseCallback);
                }
            });
        }
        catch (Throwable t){
            Log.d(TAG, t.getMessage(), t);
            mReferralCode = null;
            registerDevice(playerAttributes, responseCallback);
        }
    }

    public void registerPlayer(@NonNull final String playerUniqueId, @Nullable final String playerEmail, @Nullable final String playerMobile,
                               final PlayerAttributes playerAttributes, @NonNull Activity activity, @NonNull Intent intent,
                               @NonNull final Callback<PlayerRegisterResponse> responseCallback){
        if(playerEmail != null || !playerEmail.trim().isEmpty()){
            this.mPlayerEmail = playerEmail;
        }

        if(playerMobile != null || !playerEmail.trim().isEmpty()){
            this.mPlayerMobile = playerMobile;
        }
        registerPlayer(playerUniqueId, playerAttributes, activity, intent, responseCallback);
    }

    public void registerPlayer(@NonNull final String playerUniqueId, final PlayerAttributes playerAttributes,
                               final String referralCode, @NonNull final Callback<PlayerRegisterResponse> responseCallback)
    {
        try{
            if (!playerUniqueId.trim().isEmpty()){
                this.mPlayerUniqueId = playerUniqueId;
            }
            else {
                Log.e(TAG, "Player registration: PlayerUniqueId cannot be empty");
            }

            if(playerAttributes != null){
                setPlayerPreferredLanguage(playerAttributes.getPreferredLanguage());
            }

            this.mReferralCode = referralCode;
            registerDevice(playerAttributes, responseCallback);
        }
        catch (Throwable t){
            Log.d(TAG, t.getMessage(), t);
            this.mReferralCode = null;
            registerDevice(playerAttributes, responseCallback);
        }
    }

    public void registerPlayer(@NonNull final String playerUniqueId, @Nullable final String playerEmail, @Nullable final String playerMobile,
                               final PlayerAttributes playerAttributes, final String referralCode,
                               @NonNull final Callback<PlayerRegisterResponse> responseCallback){
        if(playerEmail != null || !playerEmail.trim().isEmpty()){
            this.mPlayerEmail = playerEmail;
        }

        if(playerMobile != null || !playerEmail.trim().isEmpty()){
            this.mPlayerMobile = playerMobile;
        }

        registerPlayer(playerUniqueId, playerAttributes, referralCode, responseCallback);
    }

    public void showProfile(final AppCompatActivity activity, @Nullable final String playerUniqueId, @Nullable String openDetail, @Nullable Boolean hideNavigation)
    {
        SharedPreferencesUtils.getInstance().putOpenDetailPreference(openDetail);
        SharedPreferencesUtils.getInstance().putHideNavigationPreference(hideNavigation);

        GameballWidgetActivity.start(activity, playerUniqueId, null);
    }
    
    public void showProfile(final AppCompatActivity activity, @Nullable final String playerUniqueId, @Nullable String openDetail, @Nullable Boolean hideNavigation, @Nullable String widgetUrlPrefix)
    {
        SharedPreferencesUtils.getInstance().putOpenDetailPreference(openDetail);
        SharedPreferencesUtils.getInstance().putHideNavigationPreference(hideNavigation);

        GameballWidgetActivity.start(activity, playerUniqueId, widgetUrlPrefix);
    }

    private void checkReferral(@NonNull Activity activity, @NonNull final Intent intent, @NonNull final Callback callback){
        if(isGmsAvailable(this.mContext)){
            FirebaseDynamicLinks.getInstance()
                    .getDynamicLink(intent)
                    .addOnSuccessListener(activity, new OnSuccessListener<PendingDynamicLinkData>()
                    {
                        @Override
                        public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData)
                        {
                            Uri deepLink = null;
                            if (pendingDynamicLinkData != null)
                            {
                                deepLink = pendingDynamicLinkData.getLink();

                                String referralCode = deepLink.getQueryParameter("GBReferral");

                                callback.onSuccess(referralCode);
                            }
                            else{
                                String referralCode = getReferralCodeManually(intent);

                                callback.onSuccess(referralCode);
                            }
                        }
                    })
                    .addOnFailureListener(activity, new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            Log.e(TAG, "getDynamicLink:onFailure", e);
                            callback.onError(e);
                        }
                    });
        }
    }

    public static String getReferralCodeManually(Intent intent){
        String uriString = intent.getDataString();
        String referralCode = null;
        if(uriString != null) {
            Uri uri = Uri.parse(uriString);
            referralCode = uri.getQueryParameter("GBReferral");
        }
        return referralCode;
    }

    public void sendEvent(Event eventBody, final Callback<Boolean> callback){
        gameBallApi.sendEvent(eventBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver(){

                    @Override
                    public void onSubscribe(Disposable d) {

                    }
                    @Override
                    public void onComplete() {
                        callback.onSuccess(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onError(e);
                    }
                });
    }

    private boolean isGmsAvailable(Context context){
        boolean isGmsAvailable = false;
        if (context != null) {
            int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            isGmsAvailable = (com.google.android.gms.common.ConnectionResult.SUCCESS == result);
        }
        Log.i(TAG, "isGmsAvailable: " + isGmsAvailable);
        return isGmsAvailable;
    }

    private void setPlayerPreferredLanguage(String playerPreferredLanguage){
        if(playerPreferredLanguage != null && playerPreferredLanguage.length() == 2){
            SharedPreferencesUtils.getInstance().putPlayerPreferredLanguage(playerPreferredLanguage);
        }
    }
}
