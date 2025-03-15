package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private static final String VERIFY_OTP_URL = "http://sxm.a58.mytemp.website/verify_otp.php"; // ✅ API URL

    private Button continu;
    private EditText etOtp;
    private String mobileNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // ✅ Initialize views
        continu = findViewById(R.id.continu);
        etOtp = findViewById(R.id.etLoginInput);

        // ✅ Get mobile number from intent
        mobileNumber = getIntent().getStringExtra("mobile");

        if (mobileNumber == null || mobileNumber.isEmpty()) {
            Log.e(TAG, "Error: Mobile number is missing in intent!");
            Toast.makeText(this, "Error: Mobile number missing!", Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            Log.d(TAG, "Mobile number received: " + mobileNumber);
        }

        continu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtp();
            }
        });
    }

    private void verifyOtp() {
        String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(otp_verification.this, "Enter OTP", Toast.LENGTH_SHORT).show();
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

                                // ✅ Redirect to MainActivity
                                Intent intent = new Intent(otp_verification.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonObject.getString("message");
                                Log.w(TAG, "OTP Verification Failed: " + message);
                                Toast.makeText(otp_verification.this, "Invalid OTP! Try again.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                            Toast.makeText(otp_verification.this, "Error processing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley Error: " + error.toString());
                        Toast.makeText(otp_verification.this, "Network Error! Try again.", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);
                params.put("otp", enteredOtp); // ✅ Send entered OTP
                Log.d(TAG, "Sending request params: " + params);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }
}
