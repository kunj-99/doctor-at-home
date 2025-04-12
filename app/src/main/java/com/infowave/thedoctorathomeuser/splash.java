package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Apply fade-in animation to the logo image
        ImageView logo = findViewById(R.id.logoImage);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        // Use the same SharedPreferences ("UserPrefs") as in otp_verification
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        // Check if the "user_id" is stored to determine if the user is logged in
        String userId = prefs.getString("user_id", "");

        // Delay for the splash screen (5000ms) and then decide the next activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                if (userId.isEmpty()) {
                    // User is not logged in; redirect to the login screen
                    intent = new Intent(splash.this, login.class);
                } else {
                    // User is already logged in; redirect to MainActivity
                    intent = new Intent(splash.this, MainActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }, 5000);
    }
}
