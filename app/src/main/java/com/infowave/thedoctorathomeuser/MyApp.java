package com.infowave.thedoctorathomeuser;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

// ✅ Use Checkout (top-level) APIs exposed via PhonePeKt
import com.phonepe.intent.sdk.api.PhonePeKt;
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Firebase (optional)
        FirebaseApp.initializeApp(this);

        try {
            // ✅ Standard Checkout init: PhonePeKt.init(...)
            boolean ok = PhonePeKt.init(
                    this,                                   // Context
                    "SU2509171931032509641494",             // Client ID (Test)
                    "DAH-" + System.currentTimeMillis(),    // Flow ID (unique per session)
                    PhonePeEnvironment.RELEASE,             // Environment (use RELEASE for production)
                    true,                                   // Enable logging (false in production)
                    null                                    // appId (optional; keep null if not provided)
            );

            Log.d("PHONEPE", "PhonePe SDK init result: " + ok);
        } catch (Throwable t) {
            Log.e("PHONEPE", "PhonePe init failed", t);
        }
    }
}
