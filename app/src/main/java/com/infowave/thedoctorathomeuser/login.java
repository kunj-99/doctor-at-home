package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.ClientError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {

    private static final String TAG = "LOGIN";
    private static final String REQ_TAG = "LOGIN_REQ";

    private EditText etMobile;
    private Button sendotp;
    private String originalButtonText;
    private BottomSheetDialog otpDialog;

    private RequestQueue requestQueue;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init views
        etMobile = findViewById(R.id.etMobileNumber);
        sendotp = findViewById(R.id.btnSendOtp);
        originalButtonText = sendotp.getText().toString();
        requestQueue = Volley.newRequestQueue(this);

        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            Log.d(TAG, "CreateAccount clicked -> Register activity");
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        sendotp.setOnClickListener(v -> {
            String mobile = etMobile.getText().toString().trim();
            Log.d(TAG, "Send OTP clicked | rawMobile=" + mobile);
            if (TextUtils.isEmpty(mobile) || mobile.length() != 10) {
                etMobile.setError("Please enter a valid 10-digit mobile number.");
                Log.w(TAG, "Invalid mobile entered");
                return;
            }
            if (!isOnline()) {
                Log.e(TAG, "No connectivity detected; aborting request");
                Toast.makeText(this, "No internet connection. Please check and try again.", Toast.LENGTH_SHORT).show();
                return;
            }
            setButtonState(true, "Loading...");
            Log.i(TAG, "checkMobileNumber() -> " + maskMobile(mobile));
            checkMobileNumber(mobile, originalButtonText);
        });

        Log.d(TAG, "onCreate() end");
    }

    private void checkMobileNumber(String mobile, final String originalText) {
        final String url = ApiConfig.endpoint("login.php");
        final long startAt = SystemClock.elapsedRealtime();

        Log.d(TAG, "checkMobileNumber() | URL=" + url + " | mobile=" + maskMobile(mobile));

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    long took = SystemClock.elapsedRealtime() - startAt;
                    Log.d(TAG, "checkMobileNumber() SUCCESS | ms=" + took + " | response=" + safeTrim(response));
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.optBoolean("success", false);
                        String srvMsg = jsonObject.optString("message", "");
                        Log.i(TAG, "checkMobileNumber() parsed | success=" + success + " | message=" + srvMsg);
                        if (success) {
                            Toast.makeText(login.this, "OTP sent successfully. Please check your SMS.", Toast.LENGTH_SHORT).show();
                            showOtpBottomSheet(mobile);
                        } else {
                            Log.w(TAG, "checkMobileNumber() server says NOT success");
                            Toast.makeText(login.this, "Mobile number not found. Please check and try again.", Toast.LENGTH_SHORT).show();
                            setButtonState(false, originalText);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "checkMobileNumber() JSON parse error", e);
                        Toast.makeText(login.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        setButtonState(false, originalText);
                    }
                },
                error -> {
                    long took = SystemClock.elapsedRealtime() - startAt;
                    String body = VolleyErrorUtil.body(error);
                    int code = VolleyErrorUtil.status(error);
                    String klass = VolleyErrorUtil.kind(error);
                    Log.e(TAG, "checkMobileNumber() ERROR | ms=" + took + " | http=" + code + " | kind=" + klass + " | body=" + body, error);
                    Toast.makeText(login.this, VolleyErrorUtil.userMsg(this, error), Toast.LENGTH_SHORT).show();
                    setButtonState(false, originalText);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                Log.d(TAG, "checkMobileNumber() params=" + params);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                Log.d(TAG, "checkMobileNumber() headers=" + h);
                return h;
            }
        };
        stringRequest.setTag(REQ_TAG);
        applyRetryPolicyWithLog(stringRequest, "checkMobileNumber");
        requestQueue.add(stringRequest);
        Log.d(TAG, "checkMobileNumber() request enqueued");
    }

    private void showOtpBottomSheet(String mobile) {
        Log.d(TAG, "showOtpBottomSheet() for " + maskMobile(mobile));
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
            String otp = otp1.getText().toString()
                    + otp2.getText().toString()
                    + otp3.getText().toString()
                    + otp4.getText().toString();
            Log.d(TAG, "Continue clicked | otpLen=" + otp.length());
            if (otp.length() == 4) {
                Log.i(TAG, "verifyOtpApi() -> " + maskMobile(mobile) + " | otp=****");
                verifyOtpApi(mobile, otp);
            } else {
                Log.w(TAG, "OTP not 4 digits");
                Toast.makeText(login.this, "Please enter 4-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnResend.setOnClickListener(v -> {
            Log.d(TAG, "Resend OTP clicked");
            btnResend.setEnabled(false);
            resendOtpApi(mobile, btnResend);
        });

        otpDialog.setOnShowListener(d -> Log.d(TAG, "OTP bottom sheet shown"));
        otpDialog.setOnDismissListener(d -> Log.d(TAG, "OTP bottom sheet dismissed"));
        otpDialog.show();
    }

    private void setupOtpInputs(EditText otp1, EditText otp2, EditText otp3, EditText otp4) {
        Log.d(TAG, "setupOtpInputs()");
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
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (s.length() == 1 && next != null) next.requestFocus();
        }
    }

    private void verifyOtpApi(String mobile, String otp) {
        final String url = ApiConfig.endpoint("verify_otp.php");
        final long startAt = SystemClock.elapsedRealtime();

        Log.d(TAG, "verifyOtpApi() | URL=" + url + " | mobile=" + maskMobile(mobile) + " | otp=****");

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    long took = SystemClock.elapsedRealtime() - startAt;
                    Log.d(TAG, "verifyOtpApi() SUCCESS | ms=" + took + " | response=" + safeTrim(response));
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.optBoolean("success", false);
                        String srvMsg = jsonObject.optString("message", "");
                        Log.i(TAG, "verifyOtpApi() parsed | success=" + success + " | message=" + srvMsg);
                        if (success) {
                            String userId   = jsonObject.optString("user_id", "");
                            String username = jsonObject.optString("username", "");
                            String email    = jsonObject.optString("email", "");
                            String patientId= jsonObject.optString("patient_id", "");
                            Log.d(TAG, "Saving to SharedPrefs | user_id=" + userId + " | patient_id=" + patientId);

                            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                            editor.putString("user_id", userId);
                            editor.putString("username", username);
                            editor.putString("email", email);
                            editor.putString("patient_id", patientId);
                            editor.apply();

                            Toast.makeText(login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Launching MainActivity");
                            Intent intent = new Intent(login.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            if (otpDialog != null) otpDialog.dismiss();
                        } else {
                            Log.w(TAG, "verifyOtpApi() server says NOT success");
                            Toast.makeText(login.this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "verifyOtpApi() JSON parse error", e);
                        Toast.makeText(login.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    long took = SystemClock.elapsedRealtime() - startAt;
                    String body = VolleyErrorUtil.body(error);
                    int code = VolleyErrorUtil.status(error);
                    String klass = VolleyErrorUtil.kind(error);
                    Log.e(TAG, "verifyOtpApi() ERROR | ms=" + took + " | http=" + code + " | kind=" + klass + " | body=" + body, error);
                    Toast.makeText(login.this, VolleyErrorUtil.userMsg(this, error), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                params.put("otp", otp);
                Log.d(TAG, "verifyOtpApi() params=" + params);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                Log.d(TAG, "verifyOtpApi() headers=" + h);
                return h;
            }
        };
        request.setTag(REQ_TAG);
        applyRetryPolicyWithLog(request, "verifyOtpApi");
        requestQueue.add(request);
        Log.d(TAG, "verifyOtpApi() request enqueued");
    }

    private void resendOtpApi(String mobile, Button btnResend) {
        final String url = ApiConfig.endpoint("login.php");
        final long startAt = SystemClock.elapsedRealtime();

        Log.d(TAG, "resendOtpApi() | URL=" + url + " | mobile=" + maskMobile(mobile));

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    long took = SystemClock.elapsedRealtime() - startAt;
                    Log.d(TAG, "resendOtpApi() SUCCESS | ms=" + took + " | response=" + safeTrim(response));
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.optBoolean("success", false);
                        String srvMsg = jsonObject.optString("message", "");
                        Log.i(TAG, "resendOtpApi() parsed | success=" + success + " | message=" + srvMsg);
                        if (success) {
                            Toast.makeText(login.this, "A new OTP has been sent. Please check your SMS.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "resendOtpApi() server says NOT success");
                            Toast.makeText(login.this, "Unable to send OTP. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "resendOtpApi() JSON parse error", e);
                        Toast.makeText(login.this, "Unexpected error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    btnResend.setEnabled(true);
                },
                error -> {
                    long took = SystemClock.elapsedRealtime() - startAt;
                    String body = VolleyErrorUtil.body(error);
                    int code = VolleyErrorUtil.status(error);
                    String klass = VolleyErrorUtil.kind(error);
                    Log.e(TAG, "resendOtpApi() ERROR | ms=" + took + " | http=" + code + " | kind=" + klass + " | body=" + body, error);
                    Toast.makeText(login.this, VolleyErrorUtil.userMsg(this, error), Toast.LENGTH_SHORT).show();
                    btnResend.setEnabled(true);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                Log.d(TAG, "resendOtpApi() params=" + params);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                Log.d(TAG, "resendOtpApi() headers=" + h);
                return h;
            }
        };
        stringRequest.setTag(REQ_TAG);
        applyRetryPolicyWithLog(stringRequest, "resendOtpApi");
        requestQueue.add(stringRequest);
        Log.d(TAG, "resendOtpApi() request enqueued");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requestQueue != null) {
            Log.d(TAG, "onStop() -> cancelAll(" + REQ_TAG + ")");
            requestQueue.cancelAll(REQ_TAG);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        super.onBackPressed();
        finishAffinity();
    }

    // --------------- helpers ---------------

    private void setButtonState(boolean loading, String text) {
        sendotp.setEnabled(!loading);
        sendotp.setText(text);
        Log.d(TAG, "Button state -> enabled=" + !loading + " | text=\"" + text + "\"");
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            boolean ok = caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
            Log.d(TAG, "isOnline() -> " + ok);
            return ok;
        } catch (Exception e) {
            Log.w(TAG, "isOnline() check failed", e);
            return true; // don't hard-block if check fails
        }
    }

    private static void applyRetryPolicyWithLog(StringRequest req, String name) {
        int timeoutMs = 15000; // 15s for flaky mobile + TLS
        int maxRetries = 2;
        float backoff = 1.0f;
        Log.d(TAG, name + "() retryPolicy | timeoutMs=" + timeoutMs + " | maxRetries=" + maxRetries + " | backoff=" + backoff);
        req.setRetryPolicy(new DefaultRetryPolicy(timeoutMs, maxRetries, backoff));
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "******" + mobile.substring(mobile.length() - 4);
    }

    private static String safeTrim(String s) {
        if (s == null) return "null";
        String t = s.trim();
        return (t.length() > 1200) ? t.substring(0, 1200) + "...(truncated)" : t;
    }

    /**
     * Centralized helpers for extracting HTTP status/body and classifying VolleyError for better logs.
     */
    public static final class VolleyErrorUtil {
        public static int status(com.android.volley.VolleyError e) {
            try { return (e != null && e.networkResponse != null) ? e.networkResponse.statusCode : -1; }
            catch (Exception ignored) { return -1; }
        }
        public static String body(com.android.volley.VolleyError e) {
            try {
                if (e == null || e.networkResponse == null || e.networkResponse.data == null) return "null";
                String b = new String(e.networkResponse.data);
                return (b.length() > 1200) ? b.substring(0, 1200) + "...(truncated)" : b;
            } catch (Exception ex) {
                return "body-read-failed: " + ex.getMessage();
            }
        }
        public static String kind(com.android.volley.VolleyError e) {
            if (e == null) return "unknown";
            if (e instanceof NoConnectionError) return "NoConnectionError";
            if (e instanceof TimeoutError) return "TimeoutError";
            if (e instanceof AuthFailureError) return "AuthFailureError";
            if (e instanceof ServerError) return "ServerError";
            if (e instanceof ClientError) return "ClientError";
            if (e instanceof NetworkError) return "NetworkError";
            if (e instanceof ParseError) return "ParseError";
            return e.getClass().getSimpleName();
        }
        public static String userMsg(Context ctx, com.android.volley.VolleyError e) {
            if (e instanceof NoConnectionError) return "Unable to connect to the server. Please check your internet.";
            if (e instanceof TimeoutError) return "Request timed out. Please try again.";
            if (e instanceof ServerError) return "Server error. Please try again shortly.";
            if (e instanceof AuthFailureError) return "Authentication failed.";
            if (e instanceof NetworkError) return "Network error. Please try again.";
            if (e instanceof ParseError) return "Response parsing error.";
            return "Something went wrong. Please try again.";
        }
    }
}
