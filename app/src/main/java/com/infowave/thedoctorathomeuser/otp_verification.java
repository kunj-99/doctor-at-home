package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class otp_verification extends AppCompatActivity {

    private static final String VERIFY_OTP_URL = "http://sxm.a58.mytemp.website/verify_otp.php"; // API URL

    private Button continu, resend_otp;
    private EditText etOtp;
    private String mobileNumber;
    private SharedPreferences sharedPreferences; // For storing user data

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Initialize views
        continu = findViewById(R.id.continu);
        etOtp = findViewById(R.id.etLoginInput);
        resend_otp = findViewById(R.id.resend_otp);

        // Get mobile number from intent
        mobileNumber = getIntent().getStringExtra("mobile");

        if (mobileNumber == null || mobileNumber.isEmpty()) {
            Toast.makeText(this, "Something went wrong. Please try logging in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Disable the resend button initially and start timer
        resend_otp.setEnabled(false);
        startResendOtpTimer();

        // Set click listener for the resend button
        resend_otp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendOtp();
            }
        });

        // Set click listener for the continue button to verify the OTP
        continu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtp();
            }
        });
    }

    /**
     * Starts a 100-second timer that updates the resend button text.
     * When the timer finishes, the resend button is enabled.
     */
    private void startResendOtpTimer() {
        new CountDownTimer(100 * 1000, 1000) { // 100 seconds with 1-second intervals
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                resend_otp.setText("Resend OTP in " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                resend_otp.setText("Resend OTP");
                resend_otp.setEnabled(true);
            }
        }.start();
    }

    /**
     * Resends the OTP via an API call.
     */
    private void resendOtp() {
        // Disable the button immediately and restart timer
        resend_otp.setEnabled(false);
        startResendOtpTimer();

        String URL = "http://sxm.a58.mytemp.website/login.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            String message = jsonObject.getString("message");

                            if (success) {
                                Toast.makeText(otp_verification.this, "A new OTP has been sent. Please check your SMS.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(otp_verification.this, "Unable to send OTP. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(otp_verification.this, "Unexpected error. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(otp_verification.this, "Network error. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(otp_verification.this);
        requestQueue.add(stringRequest);
    }

    private void verifyOtp() {
        final String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(otp_verification.this, "Please enter the OTP sent to your mobile number.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(Request.Method.POST, VERIFY_OTP_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");

                            if (success) {
                                Toast.makeText(otp_verification.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Fetch user details from API response
                                String userId = jsonObject.getString("user_id");
                                String username = jsonObject.getString("username");
                                String email = jsonObject.getString("email");
                                String patientId = jsonObject.getString("patient_id");

                                // Store user data in SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_id", userId);
                                editor.putString("username", username);
                                editor.putString("email", email);
                                editor.putString("patient_id", patientId);
                                editor.apply();

                                // Redirect to MainActivity
                                Intent intent = new Intent(otp_verification.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // OTP verification failed; show toast and clear OTP EditText.
                                String message = jsonObject.getString("message");
                                Toast.makeText(otp_verification.this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
                                etOtp.setText("");
                            }
                        } catch (JSONException e) {
                            Toast.makeText(otp_verification.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(otp_verification.this, "Could not verify OTP. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                        etOtp.setText("");
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);
                params.put("otp", enteredOtp);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }
}
