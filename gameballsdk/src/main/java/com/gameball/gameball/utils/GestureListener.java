package com.gameball.gameball.utils;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.gameball.gameball.network.Callback;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {

    private static Callback<Boolean> onSwipeDown;
    private static final int SWIPE_THRESHOLD = 100;  // Minimum swipe distance
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;  // Minimum swipe velocity

    public GestureListener(Callback<Boolean> onSwipeDown){
        this.onSwipeDown = onSwipeDown;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float diffY = e2.getY() - e1.getY();
        float diffX = e2.getX() - e1.getX();
        if (Math.abs(diffY) > Math.abs(diffX)) {
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeDown.onSuccess(true);  // Detected swipe down
                }
            }
            return true;
        }
        return false;
    }
}
