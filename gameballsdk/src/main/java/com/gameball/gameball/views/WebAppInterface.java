package com.gameball.gameball.views;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

public class  WebAppInterface {
    private Context mcontext;

    public WebAppInterface(Context context) {
        this.mcontext = context;
    }

    @JavascriptInterface
    public void nativeShare(String title, String text, String url) {
        String bodyText = title + "\n\n" + url;

        Intent intent = new Intent(Intent.ACTION_SEND);
        //intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_TEXT, bodyText);
        intent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(intent, bodyText);

        mcontext.startActivity(shareIntent);
    }
}
