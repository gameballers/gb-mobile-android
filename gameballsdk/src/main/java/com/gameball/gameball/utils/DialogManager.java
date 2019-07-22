package com.gameball.gameball.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gameball.gameball.R;
import com.gameball.gameball.model.response.NotificationBody;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


/**
 * Created by abdelhafiz on 10/07/18.
 */
public class DialogManager
{
    public static void showDialog(Context context, int msgStrId)
    {
        if (context != null)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.app_name);
            dialog.setMessage(msgStrId);
            dialog.setPositiveButton(R.string.ok, null);
            dialog.show();
        }
    }

    public static void showDialog(Context context, String msg)
    {
        if (context != null)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.app_name);
            dialog.setMessage(msg);
            dialog.setPositiveButton(R.string.ok, null);
            dialog.show();
        }
    }

    public static void showCustomNotification(final Context context, NotificationBody notificationBody)
    {
        if (context != null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View toastLayout = inflater.inflate(R.layout.custom_toast_layout, null);
            toastLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            final Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 10);
            toast.setView(toastLayout);

            TextView text = toastLayout.findViewById(R.id.textView);
            TextView title = toastLayout.findViewById(R.id.notification_title);
            ImageButton closeBtn = toastLayout.findViewById(R.id.close_btn);
            ImageView icon = toastLayout.findViewById(R.id.imageView);

            text.setText(notificationBody.getBody());
            title.setText(notificationBody.getTitle());


            closeBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    toast.cancel();
                }
            });

            toastLayout.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.i("toast_clicked", "1");
                }
            });

            Picasso.get()
                    .load(notificationBody.getIcon())
                    .into(icon, new Callback()
                    {
                        @Override
                        public void onSuccess()
                        {
                            toast.show();
                        }

                        @Override
                        public void onError(Exception e)
                        {

                        }
                    });

        }
    }

    public static void showDialog(Context context, String msg, DialogInterface.OnClickListener listener)
    {
        if (context != null)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.app_name);
            dialog.setMessage(msg);
            dialog.setCancelable(false);
            dialog.setPositiveButton(R.string.ok, listener);
            dialog.show();
        }
    }

    public static void showSnackError(View v, int msgStrId)
    {
        Snackbar.make(v, msgStrId, Snackbar.LENGTH_LONG).show();
    }

    public static void showDialog(Context context, String msg, String positiveBtn, String negativeBtn,
                                  boolean cancelable,
                                  String title,
                                  DialogInterface.OnClickListener positiveListener,
                                  DialogInterface.OnClickListener negativeListener)
    {
        if (context != null)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(title);
            dialog.setMessage(msg);
            dialog.setPositiveButton(positiveBtn, positiveListener);
            dialog.setNegativeButton(negativeBtn, negativeListener);
            dialog.setCancelable(cancelable);
            dialog.show();
        }
    }

    public static void showToast(Context context, String message)
    {
        if (context != null)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
