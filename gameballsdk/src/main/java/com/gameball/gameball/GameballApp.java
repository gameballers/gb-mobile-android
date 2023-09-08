package com.gameball.gameball;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.request.Event;
import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.NotificationBody;
import com.gameball.gameball.model.response.PlayerAttributes;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Callback;
import com.gameball.gameball.network.Network;
import com.gameball.gameball.network.api.GameBallApi;
import com.gameball.gameball.utils.Constants;
import com.gameball.gameball.views.GameballWidgetActivity;
import com.gameball.gameball.views.laregNotificationView.LargeNotificationActivity;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

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
    private int mNotificationIcon;
    private String mDeviceToken;
    private GameBallApi gameBallApi;
    private String shop = null;
    private String platform = null;
    private String SDKVersion = BuildConfig.SDK_VERSION;
    private String OS = String.format("android-sdk-%s", Build.VERSION.SDK_INT);
    private String referralCode;

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

        if(referralCode != null)
            registerDeviceRequest.setReferrerCode(referralCode);

        if (mDeviceToken != null)
        {
            registerDeviceRequest.setDeviceToken(mDeviceToken);
            SharedPreferencesUtils.getInstance().putDeviceToken(mDeviceToken);
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

    public void init(@NonNull String apiKey, @DrawableRes int notificationIcon,
                     String lang, String platform, String shop)
    {
        this.platform = platform;
        this.shop = shop;
        this.mApiKey = apiKey;
        this.mNotificationIcon = notificationIcon;

        SharedPreferencesUtils.getInstance().putPlatformPreference(this.platform);

        SharedPreferencesUtils.getInstance().putShopPreference(this.shop);

        SharedPreferencesUtils.getInstance().putOSPreference(this.OS);

        SharedPreferencesUtils.getInstance().putSDKPreference(this.SDKVersion);

        SharedPreferencesUtils.getInstance().putApiKey(this.mApiKey);

        SharedPreferencesUtils.getInstance().putLanguagePreference(lang);

        SharedPreferencesUtils.getInstance().putApiKey(apiKey);

        SharedPreferencesUtils.getInstance().putLanguagePreference(lang);

        getBotSettings();
    }


    public void changeLanguage(String language) {
        if (language != null && language.length() != 2) {
            SharedPreferencesUtils.getInstance().putLanguagePreference(language);
        }
    }

    public void initializeFirebase()
    {

        try{
            if(isGmsAvailable(this.mContext)){
                Task<String> fbTask = FirebaseMessaging.getInstance().getToken();
                Tasks.whenAll(fbTask);

                String result = fbTask.getResult();

                if(result != null && !result.trim().isEmpty()){
                    mDeviceToken = result;
                    Log.d(TAG, "Device token retrieved successfully");
                }
                else{
                    mDeviceToken = null;
                    Log.d(TAG, "Failed to retrieve device token.");
                }
            }
        }
        catch(Throwable t){
            mDeviceToken = null;
            Log.d(TAG, t.getMessage(), t);
        }
    }

    public void initializeFirebase(FirebaseMessaging firebaseMessagingInstance)
    {

        try{
            if(isGmsAvailable(this.mContext)){
                Task<String> fbTask = firebaseMessagingInstance.getToken();
                Tasks.whenAll(fbTask);

                String result = fbTask.getResult();

                if(result != null && !result.trim().isEmpty()){
                    mDeviceToken = result;
                    Log.d(TAG, "Device token retrieved successfully");
                }
                else{
                    mDeviceToken = null;
                    Log.d(TAG, "Failed to retrieve device token.");
                }
            }
        }
        catch (Throwable t){
            mDeviceToken = null;
            Log.d(TAG, t.getMessage(), t);
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
            checkReferral(activity, intent, new Callback<String>() {
                @Override
                public void onSuccess(String s) {
                    referralCode = s;
                    registerDevice(playerAttributes, responseCallback);
                }
                @Override
                public void onError(Throwable e) {
                    referralCode = null;
                    registerDevice(playerAttributes, responseCallback);
                }
            });
        }
        catch (Throwable t){
            Log.d(TAG, t.getMessage(), t);
            referralCode = null;
            registerDevice(playerAttributes, responseCallback);
        }
    }

    private void sendNotification(final NotificationBody messageBody)
    {
        Intent intent = new Intent(MAIN_ACTIVITY_ACTION);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = mContext.getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext, channelId)
                        .setContentTitle(messageBody.getTitle())
                        .setContentText(messageBody.getTitle())
                        .setAutoCancel(true)
                        .setSmallIcon(mNotificationIcon)
                        .setSound(defaultSoundUri);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run()
            {

                Intent notificationIntent = new Intent(mContext, LargeNotificationActivity.class);
                notificationIntent.putExtra(Constants.NOTIFICATION_OBJ, messageBody);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(notificationIntent);
            }
        }, 100);

        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(channelId, "Game Ball Demo",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(999999999 /* ID of notification */, notificationBuilder.build());
    }

    public boolean isGameBallNotification(RemoteMessage remoteMessage)
    {
        if (remoteMessage != null && Boolean.valueOf(remoteMessage.getData().get("isGB"))
                && remoteMessage.getNotification() != null)
        {

            Map<String, String> notificationData = remoteMessage.getData();
            NotificationBody notificationBody = new NotificationBody();
            notificationBody.setTitle(notificationData.get("title"));
            notificationBody.setBody(notificationData.get("body"));
            notificationBody.setIcon(notificationData.get("icon"));

            sendNotification(notificationBody);
            return true;
        }
        return false;
    }

    public void showProfile(final AppCompatActivity activity, @Nullable final String playerUniqueId, @Nullable String openDetail, @Nullable Boolean hideNavigation)
    {
        SharedPreferencesUtils.getInstance().putOpenDetailPreference(openDetail);
        SharedPreferencesUtils.getInstance().putHideNavigationPreference(hideNavigation);

        GameballWidgetActivity.start(activity, playerUniqueId);
    }

    public void showNotification()
    {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_toast_layout, null);

        Toast toast = new Toast(mContext);

        //use both property in single function
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
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
                                String uriString = intent.getDataString();
                                if(uriString != null){
                                    Uri uri = Uri.parse(uriString);
                                    String referralCode = uri.getQueryParameter("GBReferral");

                                    callback.onSuccess(referralCode);
                                }

                                callback.onSuccess(null);
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
}
