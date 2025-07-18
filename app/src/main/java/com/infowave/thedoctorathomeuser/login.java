package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {

    private EditText etMobile;
    private Button sendotp;
    // Store the original button text to restore it later
    private String originalButtonText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etMobile = findViewById(R.id.etMobileNumber);
        sendotp = findViewById(R.id.btnSendOtp);
        originalButtonText = sendotp.getText().toString();

        // Register "Create Account" Intent
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        sendotp.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String mobile = etMobile.getText().toString().trim();

                if (TextUtils.isEmpty(mobile) || mobile.length() != 10) {
                    etMobile.setError("Please enter a valid 10-digit mobile number.");
                    return;
                }

                // Disable the button and show loader text
                sendotp.setEnabled(false);
                sendotp.setText("Loading...");
                checkMobileNumber(mobile, originalButtonText);
            }
        });
    }

    private void checkMobileNumber(String mobile, final String originalText) {
        String URL = "https://thedoctorathome.in/login.php"; // Your server URL

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        if (success) {
                            Toast.makeText(login.this, "OTP sent successfully. Please check your SMS.", Toast.LENGTH_SHORT).show();

                            // Pass mobile number to OTP Verification Activity
                            Intent intent = new Intent(login.this, otp_verification.class);
                            intent.putExtra("mobile", mobile);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(login.this, "Mobile number not found. Please check and try again.", Toast.LENGTH_SHORT).show();
                            sendotp.setEnabled(true);
                            sendotp.setText(originalText);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(login.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        sendotp.setEnabled(true);
                        sendotp.setText(originalText);
                    }
                },
                error -> {
                    Toast.makeText(login.this, "Unable to connect to the server. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                    sendotp.setEnabled(true);
                    sendotp.setText(originalText);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // Override onBackPressed to close the app
    @Override
    public void onBackPressed() {
        // Closes all activities and exits the app
        super.onBackPressed();
        finishAffinity();
    }
}
