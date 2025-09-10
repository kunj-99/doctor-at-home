package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class tarmsandcondition extends AppCompatActivity {

    // Replace this with your actual URL
    private static final String PRIVACY_URL = ApiConfig.endpoint("term.html");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immediately launch browser with the privacy policy URL
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL));
        startActivity(browserIntent);

        // Optional: Close this activity so the user doesn't come back to a blank screen
        finish();
    }
}
