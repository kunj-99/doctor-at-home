package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

    private static final String TAG = "LoginActivity";  // Log tag
    private EditText etMobile;
    private Button sendotp;
    private String originalButtonText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate: Initializing UI components...");

        etMobile = findViewById(R.id.etMobileNumber);
        sendotp = findViewById(R.id.btnSendOtp);
        originalButtonText = sendotp.getText().toString();

        Log.d(TAG, "onCreate: Original button text = " + originalButtonText);

        // "Create Account" Intent
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            Log.i(TAG, "Create Account clicked. Redirecting to Register activity.");
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        sendotp.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String mobile = etMobile.getText().toString().trim();
                Log.d(TAG, "Send OTP clicked. Entered mobile number: " + mobile);

                if (TextUtils.isEmpty(mobile) || mobile.length() != 10) {
                    Log.w(TAG, "Invalid mobile number entered: " + mobile);
                    etMobile.setError("Please enter a valid 10-digit mobile number.");
                    return;
                }

                // Disable the button and show loader text
                sendotp.setEnabled(false);
                sendotp.setText("Loading...");
                Log.i(TAG, "Valid mobile number. Sending request to server for mobile check...");
                checkMobileNumber(mobile, originalButtonText);
            }
        });
    }

    private void checkMobileNumber(String mobile, final String originalText) {
        String URL = "http://sxm.a58.mytemp.website/login.php";
        Log.d(TAG, "checkMobileNumber: API Endpoint = " + URL);
        Log.d(TAG, "checkMobileNumber: Sending params: mobile=" + mobile);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    Log.d(TAG, "Server Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        Log.d(TAG, "Parsed JSON -> success: " + success + ", message: " + message);

                        if (success) {
                            Toast.makeText(login.this, "OTP sent successfully. Please check your SMS.", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "OTP sent successfully for mobile: " + mobile);

                            // Pass mobile number to OTP Verification Activity
                            Intent intent = new Intent(login.this, otp_verification.class);
                            intent.putExtra("mobile", mobile);
                            startActivity(intent);
                            Log.i(TAG, "Navigating to OTP Verification Activity with mobile: " + mobile);
                            finish();
                        } else {
                            Toast.makeText(login.this, "Mobile number not found. Please check and try again.", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Mobile number not found on server: " + mobile);
                            sendotp.setEnabled(true);
                            sendotp.setText(originalText);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        Toast.makeText(login.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        sendotp.setEnabled(true);
                        sendotp.setText(originalText);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley Error: " + error.toString(), error);
                    Toast.makeText(login.this, "Unable to connect to the server. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                    sendotp.setEnabled(true);
                    sendotp.setText(originalText);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                Log.d(TAG, "Volley Params: " + params.toString());
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
        Log.d(TAG, "Volley request added to queue.");
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "Back button pressed. Closing app with finishAffinity().");
        super.onBackPressed();
        finishAffinity();
    }
}
