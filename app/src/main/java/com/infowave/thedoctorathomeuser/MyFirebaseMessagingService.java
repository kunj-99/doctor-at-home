package com.infowave.thedoctorathomeuser;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;


import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG        = "FCM";
    private static final String CHANNEL_ID = "patient_channel";

    // ======================= TOKEN HANDLING =========================

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token: " + token);
        uploadTokenToServer(token);
    }

    /** Call this from your MainActivity or after patient logs in! */
    public static void refreshTokenIfNeeded(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "fresh token: " + token);
                    new MyFirebaseMessagingService().uploadTokenToServer(ctx, token);
                });
    }

    private void uploadTokenToServer(String token) {
        uploadTokenToServer(getApplicationContext(), token);
    }

    private void uploadTokenToServer(Context ctx, String token) {
        SharedPreferences sp = ctx.getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");
        Log.d(TAG, "Uploading token. patient_id: " + patientIdStr + " | token: " + token);

        final int patientId;
        try { patientId = Integer.parseInt(patientIdStr); } catch (Exception ignored) { return; }
        if (patientId <= 0) {
            Log.e(TAG, "Invalid or missing patient_id, not uploading token!");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(ctx);
        String url = "http://sxm.a58.mytemp.website/save_patient_token.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "token saved: " + resp),
                err  -> Log.e(TAG, "token save error", err))
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("patient_id", String.valueOf(patientId));
                m.put("fcm_token", token);
                return m;
            }
        };
        queue.add(req);
    }


    // ======================= NOTIFICATION HANDLING =========================

    @Override
    public void onMessageReceived(RemoteMessage rm) {
        super.onMessageReceived(rm);

        createChannelIfNeeded();

        String title = "Doctor At Home";
        String body  = "You have a new update!";

        // Prefer data payload for flexibility
        if (rm.getData().size() > 0) {
            Map<String, String> data = rm.getData();
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body"))  body  = data.get("body");
        }
        // Fallback to notification payload
        else if (rm.getNotification() != null) {
            if (rm.getNotification().getTitle() != null) title = rm.getNotification().getTitle();
            if (rm.getNotification().getBody() != null)  body  = rm.getNotification().getBody();
        }

        Intent i = new Intent(this, MainActivity.class); // Change if your main activity is different
        i.putExtra("open_notification", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_logo)   // use your app's notification icon
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // permission not granted, skip notification
            return;
        }

        NotificationManagerCompat.from(this).notify(1001, nb.build());
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID,
                        "Patient App Notifications",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Doctor At Home notifications for patients");
                nm.createNotificationChannel(ch);
            }
        }
    }

    // ======================= ANDROID 13+ PERMISSION REQUEST =========================
    public static void requestNotificationPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }
}
