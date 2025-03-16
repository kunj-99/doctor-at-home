package com.example.thedoctorathomeuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "OTPVerification";
    private static final String VERIFY_OTP_URL = "http://sxm.a58.mytemp.website/verify_otp.php"; // API URL

    private Button continu;
    private EditText etOtp;
    private String mobileNumber;
    private SharedPreferences sharedPreferences; // SharedPreferences for storing user data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Log.d(TAG, "SharedPreferences 'UserPrefs' initialized.");

        // Initialize views
        continu = findViewById(R.id.continu);
        etOtp = findViewById(R.id.etLoginInput);

        // Get mobile number from intent
        mobileNumber = getIntent().getStringExtra("mobile");

        if (mobileNumber == null || mobileNumber.isEmpty()) {
            Log.e(TAG, "Error: Mobile number is missing in intent!");
            finish();
            return;
        } else {
            Log.d(TAG, "Mobile number received: " + mobileNumber);
        }

        continu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Continue button clicked.");
                verifyOtp();
            }
        });
    }

    private void verifyOtp() {
        final String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Log.w(TAG, "OTP field is empty.");
            return;
        }

        Log.d(TAG, "Entered OTP: " + enteredOtp + " | Sending to API for verification");

        StringRequest request = new StringRequest(Request.Method.POST, VERIFY_OTP_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");

                            if (success) {
                                Log.d(TAG, "OTP Verified Successfully!");
                                Toast.makeText(otp_verification.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                                // Fetch user details from API response
                                String userId = jsonObject.getString("user_id");
                                String username = jsonObject.getString("username");
                                String email = jsonObject.getString("email");
                                // Retrieve patient_id from API response
                                String patientId = jsonObject.getString("patient_id");

                                // Store user data in SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_id", userId);
                                editor.putString("username", username);
                                editor.putString("email", email);
                                editor.putString("patient_id", patientId);
                                editor.apply();

                                Log.d(TAG, "User data saved in SharedPreferences: user_id=" + userId +
                                        ", username=" + username + ", email=" + email + ", patient_id=" + patientId);

                                // Redirect to MainActivity
                                Intent intent = new Intent(otp_verification.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonObject.getString("message");
                                Log.w(TAG, "OTP Verification Failed: " + message);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMsg = "Volley Error: " + error.toString();
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            errorMsg += ", Status Code: " + networkResponse.statusCode;
                            Log.e(TAG, "Network Response Data: " + new String(networkResponse.data));
                        }
                        Log.e(TAG, errorMsg);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);
                params.put("otp", enteredOtp); // Send entered OTP
                Log.d(TAG, "Sending request params: " + params.toString());
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Log.d(TAG, "Adding request to Volley queue");
        requestQueue.add(request);
    }
}
