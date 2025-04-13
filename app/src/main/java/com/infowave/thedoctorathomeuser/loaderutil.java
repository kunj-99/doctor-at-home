package com.infowave.thedoctorathomeuser;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class loaderutil {
    private static Dialog loaderDialog;

    public static void showLoader(Context context) {
        if (loaderDialog != null && loaderDialog.isShowing()) {
            return;
        }

        // Inflate the custom loader layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View loaderView = inflater.inflate(R.layout.item_loader, null);

        // Create a dialog that uses the custom loader view
        loaderDialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        loaderDialog.setContentView(loaderView);
        loaderDialog.setCancelable(false);
        loaderDialog.show();
    }

    public static void hideLoader() {
        if (loaderDialog != null && loaderDialog.isShowing()) {
            loaderDialog.dismiss();
        }
    }
}
