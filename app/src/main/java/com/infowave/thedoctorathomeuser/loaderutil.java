package com.infowave.thedoctorathomeuser;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class loaderutil {
    private static Dialog loaderDialog;

    public static void showLoader(Context context) {
        if (loaderDialog != null && loaderDialog.isShowing()) {
            return;
        }

        // Inflate the custom loader layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View loaderView = inflater.inflate(R.layout.item_loader, null);

        // Find the GifImageView and start its animation via GifDrawable
        GifImageView gifView = loaderView.findViewById(R.id.gif_loader);
        Drawable drawable = gifView.getDrawable();
        if (drawable instanceof GifDrawable) {
            ((GifDrawable) drawable).start();
        }

        // Create the translucent, no‑title dialog
        loaderDialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        if (loaderDialog.getWindow() != null) {
            loaderDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        loaderDialog.setContentView(loaderView);
        loaderDialog.setCancelable(false);
        loaderDialog.show();
    }

    public static void hideLoader() {
        if (loaderDialog != null && loaderDialog.isShowing()) {
            // Stop the GIF before dismissing
            GifImageView gifView = loaderDialog.findViewById(R.id.gif_loader);
            if (gifView != null) {
                Drawable drawable = gifView.getDrawable();
                if (drawable instanceof GifDrawable) {
                    ((GifDrawable) drawable).stop();
                }
            }
            loaderDialog.dismiss();
        }
    }
}
