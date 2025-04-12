package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class policy extends AppCompatActivity {

    // 🔁 Replace this URL with your actual hosted HTML file
    private static final String PRIVACY_POLICY_URL = "http://sxm.a58.mytemp.website/policy.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Open the Privacy Policy URL in browser
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
        startActivity(intent);

        // Optional: Close this activity so user can’t come back to a blank screen
        finish();
    }
}
