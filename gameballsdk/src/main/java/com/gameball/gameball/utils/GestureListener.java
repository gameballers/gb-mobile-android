package com.gameball.gameball.utils;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;

import com.gameball.gameball.network.Callback;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {

    private static Callback<Boolean> onSwipeDown;
    private WebView webView; // Add WebView reference
    private static final int SWIPE_THRESHOLD = 500;  // Minimum swipe distance
    private static final int SWIPE_VELOCITY_THRESHOLD = 500;  // Minimum swipe velocity

    public GestureListener(Callback<Boolean> onSwipeDown, WebView webView) {
        this.onSwipeDown = onSwipeDown;
        this.webView = webView;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float diffY = e2.getY() - e1.getY();
        float diffX = e2.getX() - e1.getX();

        // Check if the WebView is at the top and allow swipe down to close
        if (!webView.canScrollVertically(-1)) {
            if (Math.abs(diffY) > Math.abs(diffX)) {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0 && webView.getScrollY() == 0) {
                        onSwipeDown.onSuccess(true);  // Detected swipe down
                    }
                }
                return true;
            }
        }
        return false;  // Otherwise, let the WebView handle the scroll
    }
}
