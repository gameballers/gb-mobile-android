package com.gameball.gameball.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.network.Callback;
import com.gameball.gameball.utils.GestureListener;
import com.gameball.gameball.utils.LanguageUtils;
import com.google.gson.Gson;

public class GameballWidgetActivity extends AppCompatActivity {
    private WebView widgetView;
    private String customerId;
    private String language;
    private ImageView closeButton;
    private ImageView primaryCloseButton;
    private ImageView secondaryCloseButton;
    private String widgetUrlPrefix = BuildConfig.Widget_Url;
    private static Boolean showCloseButton = true;
    private static Callback<String> capturedLinkCallback;
    private GestureDetector gestureDetector;
    final private static String WIDGET_URL_KEY = "WIDGET_URL_KEY";
    final private static String CUSTOMER_ID_KEY = "PLAYER_UNIQUE_ID_KEY";
    final private static String LANGUAGE_QUERY_KEY = "lang";
    final private static String API_KEY_QUERY_KEY = "apiKey";
    final private static String MAIN_COLOR_QUERY_KEY = "main";
    final private static String CUSTOMER_QUERY_KEY = "playerid";
    final private static String SHOP_QUERY_KEY = "shop";
    final private static String PLATFORM_QUERY_KEY = "platform";
    final private static String OS_VERSION_QUERY_KEY = "os";
    final private static String GB_SDK_VERSION_QUERY_KEY = "sdk";
    final private static String OPEN_DETAIL_QUERY_KEY = "openDetail";
    final private static String HIDE_NAVIGATION_QUERY_KEY = "hideNavigation";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameball_widget);

        // Ensure SharedPreferences is initialized before any usage
        if (!SharedPreferencesUtils.isInitialized()) {
            SharedPreferencesUtils.init(this, new Gson());
        }

        findViewById(R.id.widget_parent).setNestedScrollingEnabled(true);
        widgetView = (WebView) findViewById(R.id.gb_profile_webview);
        primaryCloseButton = (ImageView) findViewById(R.id.btn_close_right);
        secondaryCloseButton = (ImageView) findViewById(R.id.btn_close_left);

        widgetView.setBackgroundColor(Color.WHITE);

        // Initialize GestureDetector with a simple gesture listener
        gestureDetector = new GestureDetector(this, new GestureListener(new Callback<Boolean>(){
            @Override
            public void onSuccess(Boolean aBoolean) {
                if(aBoolean){
                    closeWidget();
                }
            }

            @Override
            public void onError(Throwable e) {

            }
        }, widgetView));

        language = LanguageUtils.handleLanguage();

        closeButton = primaryCloseButton;

        extractDataFromBundle();
        setupWidget();
        loadWidget();

        if (android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void extractDataFromBundle() {
        customerId = getIntent().getStringExtra(CUSTOMER_ID_KEY);
        String widgetUrlPrefixTmp = getIntent().getStringExtra(WIDGET_URL_KEY);
        widgetUrlPrefix = widgetUrlPrefixTmp == null ||
                widgetUrlPrefixTmp.isEmpty() ? BuildConfig.Widget_Url : widgetUrlPrefixTmp;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWidget() {
        WebSettings settings = widgetView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDomStorageEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setUseWideViewPort(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        WebAppInterface webAppInterface = new WebAppInterface(this);
        widgetView.addJavascriptInterface(webAppInterface, "Android");

        // Set an onTouchListener on the WebView to pass touch events to the GestureDetector
        widgetView.setOnTouchListener((view, event) -> {
            gestureDetector.onTouchEvent(event);  // Pass touch events to GestureDetector
            return widgetView.onTouchEvent(event);  // Also allow WebView to handle the events
        });

        widgetView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(request == null) {
                    return false;
                }

                return handleCapturedLink(request.getUrl().toString());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleCapturedLink(url);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                findViewById(R.id.no_internet_layout).setVisibility(View.VISIBLE);
            }
        });

        if(!showCloseButton){
            primaryCloseButton.setVisibility(View.GONE);
            secondaryCloseButton.setVisibility(View.GONE);
        }
        else
        {
            if(LanguageUtils.shouldHandleCloseButtonDirection(this.language)){

                primaryCloseButton.setVisibility(View.GONE);
                secondaryCloseButton.setVisibility(View.VISIBLE);

                closeButton = secondaryCloseButton;
            }

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeWidget();
                }
            });
        }
    }

    private void closeWidget(){
        finish();
        GameballWidgetActivity.this.overridePendingTransition(R.anim.translate_bottom_to_top, R.anim.translate_top_to_bottom);
    }

    private void loadWidget() {
        SharedPreferencesUtils sharedPreferences = SharedPreferencesUtils.getInstance();

        String apiKey = sharedPreferences.getApiKey();

        Uri.Builder uri = new Uri.Builder();

        uri.scheme("https")
                .encodedAuthority(widgetUrlPrefix)
                .appendEncodedPath("")
                .appendQueryParameter(LANGUAGE_QUERY_KEY, language)
                .appendQueryParameter(API_KEY_QUERY_KEY, apiKey);

        if (sharedPreferences.getClientBotSettings() != null)
            uri.appendQueryParameter(MAIN_COLOR_QUERY_KEY, sharedPreferences.getClientBotSettings().getBotMainColor().replace("#", ""));

        if (customerId != null)
            uri.appendQueryParameter(CUSTOMER_QUERY_KEY, customerId);
        else if (sharedPreferences.getCustomerId() != null)
            uri.appendQueryParameter(CUSTOMER_QUERY_KEY, sharedPreferences.getCustomerId());

        String platform = sharedPreferences.getPlatformPreference();
        String shop = sharedPreferences.getShopPreference();
        String osVersion = sharedPreferences.getOSPreference();
        String sdkVersion = sharedPreferences.getSDKPreference();
        String openDetail = sharedPreferences.getOpenDetailPreference();
        String hideNavigation = sharedPreferences.getHideNavigationPreference();

        sharedPreferences.removeOpenDetailPreference();
        sharedPreferences.removeHideNavigationPreference();

        if(platform != null)
            uri.appendQueryParameter(PLATFORM_QUERY_KEY, platform);
        if(shop != null)
            uri.appendQueryParameter(SHOP_QUERY_KEY, shop);
        if(sdkVersion != null)
            uri.appendQueryParameter(GB_SDK_VERSION_QUERY_KEY, sdkVersion);
        if(osVersion != null)
            uri.appendQueryParameter(OS_VERSION_QUERY_KEY, osVersion);
        if(openDetail != null)
            uri.appendQueryParameter(OPEN_DETAIL_QUERY_KEY, openDetail);
        if(hideNavigation != null)
            uri.appendQueryParameter(HIDE_NAVIGATION_QUERY_KEY, hideNavigation);

        Log.d("GameballApp", uri.toString());

        widgetView.loadUrl(uri.toString());
    }

    private Boolean handleCapturedLink(String url){

        if(capturedLinkCallback != null){
            try{
                capturedLinkCallback.onSuccess(url);
                closeWidget();
                return true;
            }
            catch(Exception e){
                capturedLinkCallback.onError(e);
                closeWidget();
                return true;
            }
        }

        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        startActivity(intent);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (widgetView.canGoBack())
            widgetView.goBack();
        else {
            finish();
            this.overridePendingTransition(R.anim.translate_bottom_to_top, R.anim.translate_top_to_bottom);
        }
    }

    // Override the onTouchEvent to pass touch events to the GestureDetector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public static void start(Activity context, String customerId, @Nullable Boolean showCloseButton, @Nullable String widgetUrlPrefix, @Nullable Callback<String> capturedUrlCallback) {
        // Ensure SharedPreferences is initialized
        if (!SharedPreferencesUtils.isInitialized()) {
            SharedPreferencesUtils.init(context, new Gson());
        }

        GameballWidgetActivity.capturedLinkCallback = capturedUrlCallback;
        if(showCloseButton != null){
            GameballWidgetActivity.showCloseButton = showCloseButton;
        }
        Intent instance = new Intent(context, GameballWidgetActivity.class);
        instance.putExtra(CUSTOMER_ID_KEY, customerId);
        instance.putExtra(WIDGET_URL_KEY, widgetUrlPrefix);
        context.startActivity(instance);
        context.overridePendingTransition(R.anim.translate_bottom_to_top, R.anim.translate_top_to_bottom);
    }
}