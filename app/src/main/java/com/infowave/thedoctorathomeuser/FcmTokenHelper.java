package com.infowave.thedoctorathomeuser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class FcmTokenHelper {

    private static final String TAG = "FCM_HELPER";

    private FcmTokenHelper() {
        // private constructor to prevent instantiation
    }

    /** Ensure the latest FCM token is fetched and uploaded */
    public static void ensureTokenSynced(Context ctx) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "Fresh token: " + token);
                    upload(ctx.getApplicationContext(), token);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "getToken failed", e)
                );
    }

    /** Upload token to your backend */
    public static void upload(Context ctx, String token) {
        SharedPreferences sp = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");
        int patientId;
        try { patientId = Integer.parseInt(patientIdStr); } catch (Exception e) { return; }
        if (patientId <= 0) return;

        RequestQueue queue = Volley.newRequestQueue(ctx);
        String url = ApiConfig.endpoint("save_patient_token.php"); // keep your endpoint format

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "Token saved: " + resp),
                err  -> Log.e(TAG, "Token save error", err)) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> m = new HashMap<>();
                m.put("patient_id", String.valueOf(patientId));
                m.put("fcm_token", token);
                return m;
            }
        };
        queue.add(req);
    }
}
