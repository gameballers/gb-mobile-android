package com.gameball.gameball.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gameball.gameball.BuildConfig;
import com.gameball.gameball.R;
import com.gameball.gameball.local.SharedPreferencesUtils;

import java.util.Locale;

public class GameballWidgetActivity extends AppCompatActivity {
    private WebView widgetView;
    private String playerUniqueId;


    private static String PLAYER_UNIQUE_ID_KEY = "PLAYER_UNIQUE_ID_KEY";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameball_wedgit);
        findViewById(R.id.widget_parent).setNestedScrollingEnabled(true);
        widgetView = (WebView) findViewById(R.id.gb_profile_webview);
        extractDataFromBundle();
        setupWidget();
        loadWidget();

        if (android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void extractDataFromBundle() {
        playerUniqueId = getIntent().getStringExtra(PLAYER_UNIQUE_ID_KEY);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWidget() {
        WebSettings settings = widgetView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAppCacheEnabled(false);
        settings.setDomStorageEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setUseWideViewPort(true);
        settings.setLoadsImagesAutomatically(true);

        widgetView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        widgetView.setWebChromeClient(new WebChromeClient());

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                GameballWidgetActivity.this
                        .overridePendingTransition(R.anim.translate_bottom_to_top, R.anim.translate_top_to_bottom);
            }
        });
    }

    private void loadWidget() {
        SharedPreferencesUtils sharedPreferences = SharedPreferencesUtils.getInstance();

        String apiKey = sharedPreferences.getClientId();
        String language = Locale.getDefault().getLanguage();


        Uri.Builder uri = new Uri.Builder();

        uri.scheme("https")
                .encodedAuthority(BuildConfig.Widget_URL)
                .appendEncodedPath("m/")
                .appendQueryParameter("main", sharedPreferences.getClientBotSettings().getBotMainColor().replace("#", ""))
                .appendQueryParameter("lang", language)
                .appendQueryParameter("apiKey", apiKey);

        if (playerUniqueId != null)
            uri.appendQueryParameter("playerid", playerUniqueId);
        else if (sharedPreferences.getPlayerUniqueId() != null)
            uri.appendQueryParameter("playerid", sharedPreferences.getPlayerUniqueId());

        widgetView.loadUrl(uri.toString());
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

    public static void start(Activity context, String playerUniqueId) {
        Intent instance = new Intent(context, GameballWidgetActivity.class);
        instance.putExtra(PLAYER_UNIQUE_ID_KEY, playerUniqueId);
        context.startActivity(instance);
        context.overridePendingTransition(R.anim.translate_bottom_to_top, R.anim.translate_top_to_bottom);
    }
}