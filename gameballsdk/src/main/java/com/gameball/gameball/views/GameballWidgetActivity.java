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
    @Nullable private String customerId;
    private String language;
    private ImageView closeButton;
    private ImageView primaryCloseButton;
    private ImageView secondaryCloseButton;
    private String widgetUrlPrefix = BuildConfig.Widget_Url;
    private static Boolean showCloseButton = true;
    private static String closeButtonColor = null;
    private static Callback<String> externalLinkCallback;
    private GestureDetector gestureDetector;
    final private static String WIDGET_URL_KEY = "WIDGET_URL_KEY";
    final private static String CUSTOMER_ID_KEY = "CUSTOMER_ID_KEY";
    final private static String LANGUAGE_QUERY_KEY = "lang";
    final private static String API_KEY_QUERY_KEY = "apiKey";
    final private static String MAIN_COLOR_QUERY_KEY = "main";
    final private static String CUSTOMER_QUERY_KEY = "customerId";
    final private static String SHOP_QUERY_KEY = "shop";
    final private static String PLATFORM_QUERY_KEY = "platform";
    final private static String OS_VERSION_QUERY_KEY = "os";
    final private static String GB_SDK_VERSION_QUERY_KEY = "sdk";
    final private static String OPEN_DETAIL_QUERY_KEY = "openDetail";
    final private static String HIDE_NAVIGATION_QUERY_KEY = "hideNavigation";
    final private static String MOBILE_QUERY_KEY = "mobile";
    final private static String EMAIL_QUERY_KEY = "email";
    final private static String SESSION_TOKEN_QUERY_KEY = "sessionToken";

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
                return overrideIfExternal(request.getUrl().toString());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return overrideIfExternal(url);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                findViewById(R.id.no_internet_layout).setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String javascript =
                        "javascript:(function() {" +
                                "document.body.style.webkitUserSelect = 'none';" +
                                "document.body.style.webkitTouchCallout = 'none';" +
                                "document.body.style.userSelect = 'none';" +
                                "document.addEventListener('contextmenu', function(e) { e.preventDefault(); });" +
                                "})()";
                view.evaluateJavascript(javascript, null);
            }
        });

        widgetView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
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
            
            // Apply close button color
            String colorToUse = closeButtonColor != null ? closeButtonColor : "#CECECE";
            closeButton.setColorFilter(Color.parseColor(colorToUse));
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

        uri.appendQueryParameter(CUSTOMER_QUERY_KEY, customerId != null ? customerId : "");

        String platform = sharedPreferences.getPlatformPreference();
        String shop = sharedPreferences.getShopPreference();
        String osVersion = sharedPreferences.getOSPreference();
        String sdkVersion = sharedPreferences.getSDKPreference();
        String openDetail = sharedPreferences.getOpenDetailPreference();
        String hideNavigation = sharedPreferences.getHideNavigationPreference();
        String mobile = sharedPreferences.getMobilePreference();
        String email = sharedPreferences.getEmailPreference();
        String sessionToken = sharedPreferences.getSessionTokenPreference();

        sharedPreferences.removeOpenDetailPreference();
        sharedPreferences.removeHideNavigationPreference();
        sharedPreferences.removeMobilePreference();
        sharedPreferences.removeEmailPreference();

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
        if(mobile != null)
            uri.appendQueryParameter(MOBILE_QUERY_KEY, mobile);
        if(email != null)
            uri.appendQueryParameter(EMAIL_QUERY_KEY, email);
        if(sessionToken != null)
            uri.appendQueryParameter(SESSION_TOKEN_QUERY_KEY, sessionToken);

        Log.d("GameballApp", uri.toString());

        widgetView.loadUrl(uri.toString());
    }

    // Host that identifies Gameball widget content. Any subdomain of this (m., www.,
    // app., alpha., …) loads in-webview; everything else is treated as a cross-host link.
    private static final String GAMEBALL_HOST = "gameball.app";
    private static final String GAMEBALL_HOST_SUFFIX = ".gameball.app";
    // Query marker the widget appends to a link it wants opened in the device browser.
    private static final String EXTERNAL_BROWSER_FLAG = "gbExternalBrowser=true";

    // Returns true (overriding the WebView's own load) when the URL is a cross-host link
    // that has been handled externally; Gameball-host URLs return false to load in-webview.
    private boolean overrideIfExternal(String url){
        if(isGameballHost(url)){
            return false;
        }
        handleExternalBrowserLink(url);
        return true;
    }

    // True when the URL belongs to Gameball — host is "gameball.app" or ends with
    // ".gameball.app" (so m./www./app./alpha. etc. all match). The suffix check with the
    // leading dot rejects look-alikes such as "evilgameball.app". A URL with no host
    // (about:blank, data:) is also treated as in-widget so the widget renders normally.
    private boolean isGameballHost(String url){
        if(url == null){
            return true;
        }
        String host = Uri.parse(url).getHost();
        if(host == null){
            return true;
        }
        host = host.toLowerCase();
        return host.equals(GAMEBALL_HOST) || host.endsWith(GAMEBALL_HOST_SUFFIX);
    }

    // Handle a cross-host link (never loads in-widget):
    //   1) gbExternalBrowser=true → device browser (flag outranks the callback)
    //   2) else if externalLinkCallback set → delegate to it
    //   3) else → device browser (safety net)
    private void handleExternalBrowserLink(String url){
        if(url.contains(EXTERNAL_BROWSER_FLAG)){
            openInDeviceBrowser(url);
        }
        else if(externalLinkCallback != null){
            try{
                externalLinkCallback.onSuccess(url);
            }
            catch(Exception e){
                externalLinkCallback.onError(e);
            }
        }
        else{
            openInDeviceBrowser(url);
        }
    }

    private void openInDeviceBrowser(String url){
        try{
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        catch(Exception e){
            // No app available to handle the link; ignore.
        }
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

    public static void start(Activity context, @Nullable String customerId, @Nullable Boolean showCloseButton, @Nullable String closeButtonColor, @Nullable String widgetUrlPrefix, @Nullable Callback<String> externalLinkCallback) {
        // Ensure SharedPreferences is initialized
        if (!SharedPreferencesUtils.isInitialized()) {
            SharedPreferencesUtils.init(context, new Gson());
        }

        GameballWidgetActivity.externalLinkCallback = externalLinkCallback;
        if(showCloseButton != null){
            GameballWidgetActivity.showCloseButton = showCloseButton;
        }
        if(closeButtonColor != null && !closeButtonColor.isEmpty()){
            GameballWidgetActivity.closeButtonColor = closeButtonColor;
        }
        Intent instance = new Intent(context, GameballWidgetActivity.class);
        instance.putExtra(CUSTOMER_ID_KEY, customerId);
        instance.putExtra(WIDGET_URL_KEY, widgetUrlPrefix);
        context.startActivity(instance);
        context.overridePendingTransition(R.anim.translate_bottom_to_top, R.anim.translate_top_to_bottom);
    }
}