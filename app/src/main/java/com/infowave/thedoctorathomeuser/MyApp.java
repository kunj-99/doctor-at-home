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
            // ✅ PRODUCTION init (for real payments)
            boolean ok = PhonePeKt.init(
                    this,                                   // Context
                    "SU2509171931032509641494",                       // Client ID (PRODUCTION - replace with your real LIVE-XXXXXX)
                    "DAH-" + System.currentTimeMillis(),    // Flow ID (unique per session)
                    PhonePeEnvironment.RELEASE,             // ✅ PRODUCTION environment
                    false,                                  // logging OFF in production
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
