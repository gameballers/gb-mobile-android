package com.gameball.gameball;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Network;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class GameBallApp {
    private static final String TAG = GameBallApp.class.getSimpleName();
    private static final String APPLICATION_ID = "1:252563989296:android:cf5a4f42fc122b54";
    private static final String API_KEY = "AIzaSyCk3X3ZleIQjnaV-QBij9M57iBatAewMGg";
    private static final String SENDER_ID = "252563989296";
    private static final String MAIN_ACTIVITY_ACTION = "GAME_BALL_SDK";

    private static GameBallApp ourInstance;
    private Context mContext;
    private FirebaseApp GameBallFirebaseApp;
    private int mClientID;
    private String mExternalId;
    private int mNotificationIcon;
    private String mDeviceToken;

    private GameBallApp(Context context) {
        if (this.mContext == null) {
            this.mContext = context;
        }
    }

    public static GameBallApp getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new GameBallApp(context);
        }
        return ourInstance;
    }

    private Completable registerDevice() {
        return Completable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                mDeviceToken = FirebaseInstanceId.getInstance(GameBallFirebaseApp)
                        .getToken(SENDER_ID, "FCM");

                Log.d(TAG, "Game ball sdk token = " + mDeviceToken);

                String deviceToken = SharedPreferencesUtils.getInstance().getDeviceToken();
                String externalId = SharedPreferencesUtils.getInstance().getExternalId();
                int clientId = SharedPreferencesUtils.getInstance().getClientId();

                if (deviceToken != null && mDeviceToken != null && mDeviceToken.equals(deviceToken)
                        && clientId == mClientID
                        && externalId != null && mExternalId != null
                        && mExternalId.equals(externalId)) {
                    Log.d(TAG, "Device already registered");
                    return deviceToken;
                } else {
                    SharedPreferencesUtils.getInstance().clearData();
                    SharedPreferencesUtils.getInstance().putClientId(mClientID);
                    SharedPreferencesUtils.getInstance().putExternalId(mExternalId);
                }


                PlayerRegisterRequest registerDeviceRequest = new PlayerRegisterRequest();
                registerDeviceRequest.setClientID(mClientID);
                registerDeviceRequest.setExternalID(mExternalId);
                registerDeviceRequest.setDeviceToken(mDeviceToken);

                BaseResponse<PlayerRegisterResponse> response = Network.getInstance().getGameBallApi()
                        .registrationPlayer(registerDeviceRequest)
                        .blockingGet();

                if (response.isSuccess()) {
                    SharedPreferencesUtils.getInstance().putDeviceToken(mDeviceToken);
                }

                return mDeviceToken;
            }
        }).subscribeOn(Schedulers.io());
    }

    public void init(int clientID, String externalId, @DrawableRes int notificationIcon) {
        // TODO: 8/23/2018
        this.mClientID = clientID;
        this.mExternalId = externalId;
        mNotificationIcon = notificationIcon;

        SharedPreferencesUtils.init(mContext, new Gson());

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(APPLICATION_ID) // Required for Analytics.
                .setApiKey(API_KEY) // Required for Auth.
                .build();

        // Initialize with secondary app.
        FirebaseApp.initializeApp(mContext, options, TAG);

        // Retrieve secondary app.
        GameBallFirebaseApp = FirebaseApp.getInstance(TAG);

        registerDevice().subscribe(new Action() {
            @Override
            public void run() {
                // pass
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                // pass
            }
        });

    }

    private void sendNotification(String messageBody) {
        Intent intent = new Intent(MAIN_ACTIVITY_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = mContext.getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext, channelId)
                        .setContentTitle("Game Ball Demo")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSmallIcon(mNotificationIcon)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Game Ball Demo",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(999999999 /* ID of notification */, notificationBuilder.build());
    }

    public boolean isGameBallNotification(RemoteMessage remoteMessage) {
        if (remoteMessage != null && SENDER_ID.equals(remoteMessage.getFrom())
                && remoteMessage.getNotification() != null) {
            sendNotification(remoteMessage.getNotification().getBody());
            return true;
        }
        return false;
    }
}