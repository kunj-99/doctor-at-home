package com.infowave.thedoctorathomeuser;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;

// ✅ Standard Checkout top-level API
import com.phonepe.intent.sdk.api.PhonePeKt;
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Firebase (optional)
        FirebaseApp.initializeApp(this);

        try {
            // ✅ DEMO / SANDBOX init
            boolean ok = PhonePeKt.init(
                    this,                                   // Context
                    "TEST-M234ZHDNNC58R_25091",             // Client ID (TEST)
                    "DAH-" + System.currentTimeMillis(),    // Flow ID (unique per session)
                    PhonePeEnvironment.SANDBOX,             // ✅ SANDBOX (use RELEASE for prod)
                    true,                                   // logging ON (set false in prod)
                    null                                    // appId (optional)
            );

            Log.d("PHONEPE", "PhonePe SDK init result: " + ok);

            // ❌ PRODUCTION (commented; flip when going live)
            // PhonePeKt.init(this, "LIVE-CLIENT-ID", "DAH-"+System.currentTimeMillis(),
            //         PhonePeEnvironment.RELEASE, false, null);

        } catch (Throwable t) {
            Log.e("PHONEPE", "PhonePe init failed", t);
        }
    }
}
