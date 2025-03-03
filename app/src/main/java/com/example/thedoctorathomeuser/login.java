package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class login extends AppCompatActivity {

    Button sendotp ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        sendotp = findViewById(R.id.btnSendOtp);

        // Find the "Create new account" TextView
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);

        // Set click listener
        tvCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start RegisterActivity
                Intent intent = new Intent(login.this, Register.class);
                startActivity(intent);
            }
        });

        sendotp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(login.this,otp_verification.class);
                startActivity(intent);
            }
        });
    }
}
