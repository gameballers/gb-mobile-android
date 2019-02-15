package com.gameball.gameball.utils;

import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ImageDownloader
{
    public static void downloadImage(ImageView imageView, String imageUrl)
    {
        Picasso.get()
                .load(imageUrl)
                .into(imageView);
    }
}
