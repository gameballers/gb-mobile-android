package com.gameball.gameball;

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
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.model.request.Action;
import com.gameball.gameball.model.request.PlayerRegisterRequest;
import com.gameball.gameball.model.request.RewardPointsBody;
import com.gameball.gameball.model.response.BaseResponse;
import com.gameball.gameball.model.response.ClientBotSettings;
import com.gameball.gameball.model.response.PlayerRegisterResponse;
import com.gameball.gameball.network.Callback;
import com.gameball.gameball.network.Network;
import com.gameball.gameball.network.api.GameBallApi;
import com.gameball.gameball.network.transactionRemote.TransactionDataSourceContract;
import com.gameball.gameball.network.transactionRemote.TransactionRemoteDataSource;
import com.gameball.gameball.views.mainContainer.MainContainerFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class GameBallApp {
    private static final String TAG = GameBallApp.class.getSimpleName();
    private static final String APPLICATION_ID = "1:252563989296:android:cf5a4f42fc122b54";
    private static final String API_KEY = "AIzaSyCk3X3ZleIQjnaV-QBij9M57iBatAewMGg";
    private static final String SENDER_ID = "252563989296";
    private static final String MAIN_ACTIVITY_ACTION = "GAME_BALL_MAIN_ACTIVITY";
    private static final String TAG_GAMEBALL_PROFILE_DIALOG = "gameball_profile_dialog";

    private static GameBallApp ourInstance;
    private Context mContext;
    private FirebaseApp GameBallFirebaseApp;
    private String mClientID;
    private String mPlayerID;
    private Integer mPlayerCategoryID;
    private int mNotificationIcon;
    private String mDeviceToken;
    private GameBallApi gameBallApi;
    private TransactionRemoteDataSource transactionRemoteDataSource;

    private GameBallApp(Context context) {
        if (this.mContext == null) {
            this.mContext = context;
            gameBallApi = Network.getInstance().getGameBallApi();
            SharedPreferencesUtils.init(mContext, new Gson());
            transactionRemoteDataSource = TransactionRemoteDataSource.getInstance();
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
                String playerId = SharedPreferencesUtils.getInstance().getPlayerId();
                String clientId = SharedPreferencesUtils.getInstance().getClientId();
                int playerCategoryId = SharedPreferencesUtils.getInstance().getPlayerCategoryId();

                if (deviceToken != null && mDeviceToken != null && mDeviceToken.equals(deviceToken)
                        && clientId.equals(mClientID)
                        && playerId != null && mPlayerID != null
                        && playerCategoryId == mPlayerCategoryID
                        && mPlayerID.equals(playerId)) {
                    Log.d(TAG, "Device already registered");
                    return deviceToken;
                } else {
                    SharedPreferencesUtils.getInstance().clearData();
                    SharedPreferencesUtils.getInstance().putClientId(mClientID);
                    SharedPreferencesUtils.getInstance().putPlayerId(mPlayerID);
                    SharedPreferencesUtils.getInstance().putPlayerCategoryId(mPlayerCategoryID);
                }


                PlayerRegisterRequest registerDeviceRequest = new PlayerRegisterRequest();
                registerDeviceRequest.setPlayerUniqueID(mPlayerID);
                registerDeviceRequest.setPlayerCategoryID(mPlayerCategoryID);
                registerDeviceRequest.setDeviceToken(mDeviceToken);

                Log.i("register_body",new Gson().toJson(registerDeviceRequest));

                BaseResponse<PlayerRegisterResponse> response = gameBallApi
                        .registrationPlayer(registerDeviceRequest)
                        .blockingGet();

                if (response.isSuccess()) {
                    SharedPreferencesUtils.getInstance().putDeviceToken(mDeviceToken);
                }
                else
                {
                    response.getErrorMsg();
                }

                return mDeviceToken;
            }
        }).subscribeOn(Schedulers.io());
    }

    private void getBotSettings() {
        gameBallApi.getBotSettings()
                .subscribeOn(Schedulers.io())
                .retry()
                .subscribe(new SingleObserver<BaseResponse<ClientBotSettings>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(BaseResponse<ClientBotSettings> clientBotSettingsBaseResponse) {
                        SharedPreferencesUtils.getInstance().putClientBotSettings(
                                clientBotSettingsBaseResponse.getResponse());

                        Log.i("bot_settings", new Gson().toJson(
                                SharedPreferencesUtils.getInstance().getClientBotSettings()));

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("bot_settings_error", e.getMessage());
                    }
                });
    }

    public void init(@NonNull String clientID, String playerID, int playerCategoryId,
                     @DrawableRes int notificationIcon) {
        // TODO: 8/23/2018
        this.mClientID = clientID;
        this.mPlayerID = playerID;
        this.mPlayerCategoryID = playerCategoryId;
        mNotificationIcon = notificationIcon;

        SharedPreferencesUtils.getInstance().putClientId(clientID);

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(APPLICATION_ID) // Required for Analytics.
                .setApiKey(API_KEY) // Required for Auth.
                .build();

        // Initialize with secondary app.
        FirebaseApp.initializeApp(mContext, options, TAG);

        // Retrieve secondary app.
        GameBallFirebaseApp = FirebaseApp.getInstance(TAG);
        if(playerID != null && !playerID.trim().isEmpty())
        {
            registerDevice().subscribe(new io.reactivex.functions.Action()
            {
                @Override
                public void run()
                {
                    // pass
                }
            }, new Consumer<Throwable>()
            {
                @Override
                public void accept(Throwable throwable)
                {
                    // pass
                }
            });
        }
        getBotSettings();
    }

    public void init(String clientID,String playerId, @DrawableRes int notificationIcon)
    {
        init(clientID, playerId,-1, notificationIcon);
    }

    public void init(String clientID, @DrawableRes int notificationIcon)
    {
        init(clientID, null,-1, notificationIcon);
    }



    public void registerPlayer(@NonNull String playerID)
    {
        registerPlayer(playerID,-1);
    }

    public void registerPlayer(@NonNull String playerID, int playerCategoryId)
    {
        if(!playerID.trim().isEmpty())
        {
            mPlayerID = playerID;
            mPlayerCategoryID = playerCategoryId;

            registerDevice().subscribe(new io.reactivex.functions.Action()
            {
                @Override
                public void run()
                {
                    // pass
                }
            }, new Consumer<Throwable>()
            {
                @Override
                public void accept(Throwable throwable)
                {
                    // pass
                }
            });
        }
        else
        {
            Log.e(TAG, "Player registration: playerID cannot be empty");
        }
    }

    private void sendNotification(final String messageBody) {
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

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(mContext, messageBody, Toast.LENGTH_SHORT).show();
            }
        },100);

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

    /**
     * Use Show profile method where ever you like.
     * It just shows the profile of the user that contains the user details, user achievements and the leader board.
     * basically call this method when ever you want to allow the user to see his profile.
     * @param activity if you are using an activity send an instance of the activity to be able to show the profile
     */
    public void showProfile(AppCompatActivity activity) throws  Exception{
        if(SharedPreferencesUtils.getInstance().getPlayerId() == null)
        {
            throw new RuntimeException(TAG + ": User is not logged in yet!");
        }

        showProfile(activity.getSupportFragmentManager());
    }

    /**
     * It just shows the profile of the user that contains the user details, user achievements and the leader board.
     * basically call this method when ever you want to allow the user to see his profile.
     * @param fragment if you are using a fragment send an instance of the fragment to be able to show the profile
     */
    public void showProfile(Fragment fragment) throws Exception{
        if(SharedPreferencesUtils.getInstance().getPlayerId() == null)
        {
            throw new RuntimeException(TAG + ": User is not logged in yet!");
        }

        showProfile(fragment.getChildFragmentManager());
    }

    private void showProfile(final FragmentManager fragmentManager) {
        Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                ClientBotSettings clientBotSettings = SharedPreferencesUtils.getInstance().getClientBotSettings();
                return clientBotSettings != null;
            }
        }).flatMap(new Function<Boolean, ObservableSource<ClientBotSettings>>() {
            @Override
            public ObservableSource<ClientBotSettings> apply(Boolean aBoolean) throws Exception {
                if (aBoolean) {
                    return Observable.just(SharedPreferencesUtils.getInstance().getClientBotSettings());
                }
                return gameBallApi.getBotSettings().flatMapObservable(new Function<BaseResponse<ClientBotSettings>, ObservableSource<? extends ClientBotSettings>>() {
                    @Override
                    public ObservableSource<? extends ClientBotSettings> apply(BaseResponse<ClientBotSettings> clientBotSettingsBaseResponse) throws Exception {
                        return Observable.just(clientBotSettingsBaseResponse.getResponse());
                    }
                });
            }
        }).doOnNext(new Consumer<ClientBotSettings>() {
            @Override
            public void accept(ClientBotSettings clientBotSettings) throws Exception {
                SharedPreferencesUtils.getInstance().putClientBotSettings(clientBotSettings);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ClientBotSettings>() {
                    @Override
                    public void accept(ClientBotSettings clientBotSettings) throws Exception {
                        FragmentTransaction ft = fragmentManager.beginTransaction();

                        Fragment prev = fragmentManager
                                .findFragmentByTag(TAG_GAMEBALL_PROFILE_DIALOG);
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);
                        DialogFragment dialogFragment = new MainContainerFragment();
                        dialogFragment.show(ft, TAG_GAMEBALL_PROFILE_DIALOG);
                    }
                });
    }

    /**
     * use AddAction when ever you want to trigger that an action is done by the user.
     * @param action the method requires and action object that requires the challengeApiId
     *               another two paramaeters are optional.
     *               amount: is needed if the challenge is amount based
     *               playerCategoryID: us needed if you have multi users categories(default value is 0)
     */
    public void addAction(Action action) {
        gameBallApi.addNewAtion(action).
                subscribeOn(Schedulers.io())
                .retry()
                .subscribe();
    }

    public void rewardPoints(RewardPointsBody body, final Callback callback)
    {
        transactionRemoteDataSource.RewardPoints(body)
                .subscribe(new CompletableObserver()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onComplete()
                    {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        callback.onError(e);
                    }
                });
    }


    public void showNotification() {
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
}
