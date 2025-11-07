package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class FcmTokenHelper {

    private static final String TAG = "FCM_HELPER";

    private FcmTokenHelper() { /* no instances */ }

    /** Ensure the latest FCM token is fetched and uploaded with per-device identity */
    public static void ensureTokenSynced(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "Fresh token received (len=" + (token == null ? 0 : token.length()) + ")");
                    resolveDeviceIdAndUpload(ctx.getApplicationContext(), token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "getToken failed", e));
    }

    /** Backward-compatible entry: will also attach device_id before uploading */
    public static void upload(Context ctx, String token) {
        resolveDeviceIdAndUpload(ctx.getApplicationContext(), token);
    }

    // -------------------- internal helpers --------------------

    private static void resolveDeviceIdAndUpload(Context ctx, String token) {
        try {
            FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(fid -> {
                        String deviceId = (fid != null && !fid.isEmpty())
                                ? fid : getAndroidIdFallback(ctx);
                        doUpload(ctx, token, deviceId);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "FID fetch failed, using ANDROID_ID fallback", e);
                        doUpload(ctx, token, getAndroidIdFallback(ctx));
                    });
        } catch (Exception e) {
            Log.w(TAG, "FID fetch exception, using ANDROID_ID fallback", e);
            doUpload(ctx, token, getAndroidIdFallback(ctx));
        }
    }

    private static String getAndroidIdFallback(Context ctx) {
        try {
            @SuppressLint("HardwareIds") String id = Settings.Secure.getString(
                    ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (id != null && !id.isEmpty()) ? id : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** Final POST including device_id + minimal metadata */
    private static void doUpload(Context ctx, String token, String deviceId) {
        SharedPreferences sp = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");

        int patientId;
        try { patientId = Integer.parseInt(patientIdStr); } catch (Exception e) { patientId = 0; }

        if (patientId <= 0) {
            Log.e(TAG, "Invalid/missing patient_id; not uploading token");
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "FCM token is empty; skipping upload");
            return;
        }

        String url = ApiConfig.endpoint("save_patient_token.php");
        RequestQueue queue = Volley.newRequestQueue(ctx);

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
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App version lookup failed", e);
        }
        final String finalAppVersion = appVersion;
        // -----------------------------------------------------

        // Pre-request payload log (do not dump full token; log length)
        Log.d(TAG, "POST " + url
                + " | patient_id=" + patientId
                + " | device_id=" + deviceId
                + " | token_len=" + token.length()
                + " | model=" + model
                + " | app_version=" + finalAppVersion);

        int finalPatientId = patientId;
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "Token saved OK: " + resp),
                err  -> {
                    int code = (err.networkResponse != null) ? err.networkResponse.statusCode : -1;
                    String body = "";
                    try {
                        if (err.networkResponse != null && err.networkResponse.data != null) {
                            body = new String(err.networkResponse.data, StandardCharsets.UTF_8);
                        }
                    } catch (Exception ignored) { /* no-op */ }

                    Log.e(TAG, "Token save FAILED | http=" + code + " | body=" + body, err);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("patient_id", String.valueOf(finalPatientId));
                m.put("fcm_token", token);
                m.put("device_id", deviceId);     // separates emulator vs phone
                m.put("platform", "android");
                m.put("model", model);
                m.put("app_version", finalAppVersion);
                return m;
            }
        };

        queue.add(req);
    }
}
