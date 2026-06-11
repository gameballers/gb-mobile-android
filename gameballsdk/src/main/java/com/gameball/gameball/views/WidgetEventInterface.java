package com.gameball.gameball.views;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

import androidx.annotation.Nullable;

import com.gameball.gameball.network.Callback;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

/**
 * JavaScript bridge dedicated to widget → SDK events, registered under the JS name
 * "WidgetEvent". Kept separate from {@link WebAppInterface} (native share) so each
 * bridge carries only the dependency it needs — this one holds just the listener.
 */
public class WidgetEventInterface {
    private static final Gson GSON = new Gson();

    @Nullable private final Callback<Map<String, Object>> widgetEventCallback;

    public WidgetEventInterface(@Nullable Callback<Map<String, Object>> widgetEventCallback) {
        this.widgetEventCallback = widgetEventCallback;
    }

    /**
     * Entry point invoked from the widget webview with a raw event string. Parsed into a
     * Map of key-value pairs ({type, metadata}) and delivered to the registered listener on
     * the main thread (this method runs on the WebView's JS thread). Malformed JSON is routed
     * to onError. No-op when no listener was registered.
     */
    @JavascriptInterface
    public void postEvent(final String raw) {
        if (widgetEventCallback == null) return;
        new  Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> event = GSON.fromJson(raw, new TypeToken<Map<String, Object>>() {}.getType());
                    widgetEventCallback.onSuccess(event);
                } catch (Exception e) {
                    widgetEventCallback.onError(e);
                }
            }
        });
    }
}
