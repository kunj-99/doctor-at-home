package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {

    private EditText etMobile;
    private Button sendotp;
    private String originalButtonText;
    private BottomSheetDialog otpDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etMobile = findViewById(R.id.etMobileNumber);
        sendotp = findViewById(R.id.btnSendOtp);
        originalButtonText = sendotp.getText().toString();

        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        sendotp.setOnClickListener(v -> {
            String mobile = etMobile.getText().toString().trim();
            if (TextUtils.isEmpty(mobile) || mobile.length() != 10) {
                etMobile.setError("Please enter a valid 10-digit mobile number.");
                return;
            }
            sendotp.setEnabled(false);
            sendotp.setText("Loading...");
            checkMobileNumber(mobile, originalButtonText);
        });
    }

    private void checkMobileNumber(String mobile, final String originalText) {
        String URL = ApiConfig.endpoint("login.php");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            Toast.makeText(login.this, "OTP sent successfully. Please check your SMS.", Toast.LENGTH_SHORT).show();
                            showOtpBottomSheet(mobile);
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

    private void showOtpBottomSheet(String mobile) {
        otpDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottomsheet_otp, null);
        otpDialog.setContentView(sheetView);

        EditText otp1 = sheetView.findViewById(R.id.otp1);
        EditText otp2 = sheetView.findViewById(R.id.otp2);
        EditText otp3 = sheetView.findViewById(R.id.otp3);
        EditText otp4 = sheetView.findViewById(R.id.otp4);
        Button btnContinue = sheetView.findViewById(R.id.btnContinue);
        Button btnResend = sheetView.findViewById(R.id.btnResend);

        setupOtpInputs(otp1, otp2, otp3, otp4);

        btnContinue.setOnClickListener(v -> {
            String otp = otp1.getText().toString() + otp2.getText().toString() + otp3.getText().toString() + otp4.getText().toString();
            if (otp.length() == 4) {
                verifyOtpApi(mobile, otp);
            } else {
                Toast.makeText(login.this, "Please enter 4-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnResend.setOnClickListener(v -> {
            btnResend.setEnabled(false);
            resendOtpApi(mobile, btnResend);
        });

        otpDialog.show();
    }

    private void setupOtpInputs(EditText otp1, EditText otp2, EditText otp3, EditText otp4) {
        otp1.addTextChangedListener(new OtpTextWatcher(otp1, otp2));
        otp2.addTextChangedListener(new OtpTextWatcher(otp2, otp3));
        otp3.addTextChangedListener(new OtpTextWatcher(otp3, otp4));
        otp4.addTextChangedListener(new OtpTextWatcher(otp4, null));
    }

    private static class OtpTextWatcher implements TextWatcher {
        private final EditText current;
        private final EditText next;
        OtpTextWatcher(EditText current, EditText next) {
            this.current = current;
            this.next = next;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && next != null) next.requestFocus();
        }
    }

    // ==== THE MOST IMPORTANT PART FOR PROFILE LOADING ====
    private void verifyOtpApi(String mobile, String otp) {
        String VERIFY_OTP_URL = ApiConfig.endpoint("verify_otp.php");
        StringRequest request = new StringRequest(Request.Method.POST, VERIFY_OTP_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            // Save profile/user info to SharedPreferences
                            String userId = jsonObject.getString("user_id");
                            String username = jsonObject.getString("username");
                            String email = jsonObject.getString("email");
                            String patientId = jsonObject.getString("patient_id");

                            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                            editor.putString("user_id", userId);
                            editor.putString("username", username);
                            editor.putString("email", email);
                            editor.putString("patient_id", patientId);
                            editor.apply();

                            Toast.makeText(login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(login.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            if (otpDialog != null) otpDialog.dismiss();
                        } else {
                            Toast.makeText(login.this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(login.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(login.this, "Could not verify OTP. Please check your internet and try again.", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                params.put("otp", otp);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void resendOtpApi(String mobile, Button btnResend) {
        String URL = ApiConfig.endpoint("login.php");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            Toast.makeText(login.this, "A new OTP has been sent. Please check your SMS.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(login.this, "Unable to send OTP. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(login.this, "Unexpected error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    btnResend.setEnabled(true);
                },
                error -> {
                    Toast.makeText(login.this, "Network error. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
                    btnResend.setEnabled(true);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(login.this);
        requestQueue.add(stringRequest);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
