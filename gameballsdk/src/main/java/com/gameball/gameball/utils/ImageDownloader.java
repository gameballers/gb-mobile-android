package com.gameball.gameball.utils;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.gameball.gameball.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ImageDownloader
{
    public static void downloadImage(final Context context, final ImageView imageView, String imageUrl)
    {
        Picasso.get()
                .load(imageUrl)
                .into(imageView, new Callback()
                {
                    @Override
                    public void onSuccess()
                    {
                        Animation zoomInAnim = AnimationUtils.loadAnimation(context, R.anim.zoom_in);
                        zoomInAnim.setDuration(500);
                        imageView.startAnimation(zoomInAnim);
                    }

                    @Override
                    public void onError(Exception e)
                    {

                    }
                });
    }
}
