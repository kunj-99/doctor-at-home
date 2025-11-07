package com.infowave.thedoctorathomeuser;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
// (No BuildConfig import here)
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Single-file solution:
 * - Uploads token per device (emulator vs phone separated by device_id)
 * - No extra classes created
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";
    private static final String CHANNEL_ID = "patient_channel";

    // ======================= TOKEN HANDLING =========================

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token: " + token);
        // Get a stable per-install device_id (FID with ANDROID_ID fallback), then upload
        fetchDeviceIdAndUpload(getApplicationContext(), token);
    }

    /** Call this from MainActivity or right after patient logs in */
    public static void refreshTokenIfNeeded(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "fresh token: " + token);
                    // Call static helper directly (no 'new Service()')
                    fetchDeviceIdAndUpload(ctx.getApplicationContext(), token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "getToken failed", e));
    }

    /** Get device_id (FID -> fallback ANDROID_ID) and then call upload */
    private static void fetchDeviceIdAndUpload(Context ctx, String token) {
        try {
            FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(fid -> {
                        String deviceId = (fid != null && !fid.isEmpty())
                                ? fid
                                : getAndroidIdFallback(ctx);
                        uploadTokenToServer(ctx, token, deviceId);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "FID fetch failed, using ANDROID_ID fallback", e);
                        uploadTokenToServer(ctx, token, getAndroidIdFallback(ctx));
                    });
        } catch (Exception e) {
            Log.w(TAG, "FID fetch exception, using ANDROID_ID fallback", e);
            uploadTokenToServer(ctx, token, getAndroidIdFallback(ctx));
        }
    }

    private static String getAndroidIdFallback(Context ctx) {
        try {
            String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (id != null && !id.isEmpty()) ? id : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** Final upload with device_id + minimal metadata */
    private static void uploadTokenToServer(Context ctx, String token, String deviceId) {
        SharedPreferences sp = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");
        Log.d(TAG, "Uploading token. patient_id: " + patientIdStr + " | token: " + token + " | device_id: " + deviceId);

        final int patientId;
        try { patientId = Integer.parseInt(patientIdStr); } catch (Exception ignored) { return; }
        if (patientId <= 0) {
            Log.e(TAG, "Invalid/missing patient_id; not uploading token");
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "FCM token is empty; skipping upload");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(ctx);
        String url = ApiConfig.endpoint("save_patient_token.php");

        final String model = Build.MANUFACTURER + " " + Build.MODEL;

        // ---- Safe appVersion lookup (API 33+ compatible) ----
        String appVersion = "unknown";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appVersion = ctx.getPackageManager()
                        .getPackageInfo(ctx.getPackageName(), PackageManager.PackageInfoFlags.of(0))
                        .versionName;
            } else {
                appVersion = ctx.getPackageManager()
                        .getPackageInfo(ctx.getPackageName(), 0)
                        .versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) { }
        final String finalAppVersion = appVersion;
        // -----------------------------------------------------

        // Pre-request payload log (helps confirm what we send)
        Log.d(TAG, "POST " + url
                + " | patient_id=" + patientId
                + " | device_id=" + deviceId
                + " | token_len=" + token.length()
                + " | model=" + model
                + " | app_version=" + finalAppVersion);

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "token saved: " + resp),
                err  -> {
                    int code = (err.networkResponse != null) ? err.networkResponse.statusCode : -1;
                    String body = "";
                    try {
                        if (err.networkResponse != null && err.networkResponse.data != null) {
                            body = new String(err.networkResponse.data, StandardCharsets.UTF_8);
                        }
                    } catch (Exception ignored2) { /* ignore */ }

                    Log.e(TAG, "token save error | http=" + code
                            + " | body=" + body, err);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("patient_id", String.valueOf(patientId));
                m.put("fcm_token", token);
                m.put("device_id", deviceId);   // separates emulator vs phone
                m.put("platform", "android");
                m.put("model", model);
                m.put("app_version", finalAppVersion);
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

        if (rm.getData().size() > 0) {
            Map<String, String> data = rm.getData();
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body"))  body  = data.get("body");
        } else if (rm.getNotification() != null) {
            if (rm.getNotification().getTitle() != null) title = rm.getNotification().getTitle();
            if (rm.getNotification().getBody() != null)  body  = rm.getNotification().getBody();
        }

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_notification", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted; notification will not be shown
            return;
        }

        NotificationManagerCompat.from(this).notify(1001, builder.build());
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) return;

            // If R.raw.sound doesn't exist, remove the setSound() lines or wrap in try/catch
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sound);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Patient App Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Doctor At Home notifications for patients");
            ch.setSound(soundUri, attrs);
            nm.createNotificationChannel(ch);
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
