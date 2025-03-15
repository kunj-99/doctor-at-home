package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cashfree.pg.api.CFPaymentGatewayService;
import com.cashfree.pg.core.api.CFSession;
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback;
import com.cashfree.pg.core.api.exception.CFException;
import com.cashfree.pg.core.api.utils.CFErrorResponse;
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment;
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutTheme;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class pending_bill extends AppCompatActivity implements CFCheckoutResponseCallback {

    private static final String TAG = "PendingBill";

    private CFPaymentGatewayService gatewayService;
    private String orderID;
    private String paymentSessionID;
    private String api = "TEST1049127073b42a1e211a7cabe17207219401";
    private String secret_api = "cfsk_ma_test_a1ff9b5ff8e6e3f11107cb84b0037b7f_88f81c16";
    private CFSession.Environment cfEnvironment = CFSession.Environment.SANDBOX;

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod;

    // Flag to track if a request is in progress
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Button payButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Get booking details from Intent
        Intent intent = getIntent();
        patientName = intent.getStringExtra("patient_name");
        age = String.valueOf(intent.getIntExtra("age", 0));
        gender = intent.getStringExtra("gender");
        problem = intent.getStringExtra("problem");
        address = intent.getStringExtra("address");
        doctorId = intent.getStringExtra("doctor_id");
        doctorName = intent.getStringExtra("doctorName");
        Status = intent.getStringExtra("appointment_status");

        if (Status != null) {
            if (Status.equals("Request for visit")) {
                Status = "Requested";
            } else if (Status.equals("Book Appointment")) {
                Status = "Confirmed";
            }
        }

        // Initialize Cashfree SDK
        try {
            gatewayService = CFPaymentGatewayService.getInstance();
            gatewayService.setCheckoutCallback(this);
        } catch (CFException e) {
            Log.e(TAG, "Failed to initialize Cashfree SDK: " + e.getMessage());
            Toast.makeText(this, "Failed to initialize Cashfree SDK", Toast.LENGTH_LONG).show();
            return;
        }

        RadioGroup paymentMethodGroup = findViewById(R.id.payment_method_group);
        payButton = findViewById(R.id.pay_button);

        payButton.setOnClickListener(v -> {
            // Check if processing is already happening and prevent multiple clicks
            if (isProcessing.compareAndSet(false, true)) {
                try {
                    // Disable the button to provide visual feedback
                    payButton.setEnabled(false);

                    // Show processing message
                    Toast.makeText(pending_bill.this, "Processing your request...", Toast.LENGTH_SHORT).show();

                    int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
                    selectedPaymentMethod = (selectedId == R.id.payment_online) ? "Online" : "Offline";

                    if (selectedId == R.id.payment_online) {
                        generateSessionToken();  // Online Payment
                    } else if (selectedId == R.id.payment_offline) {
                        saveBookingData();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing payment: " + e.getMessage());
                    resetProcessingState();
                    Toast.makeText(pending_bill.this, "Error processing your request. Please try again.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(pending_bill.this, "Your request is being processed. Please wait...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to reset the processing state
    private void resetProcessingState() {
        isProcessing.set(false);
        runOnUiThread(() -> payButton.setEnabled(true));
    }

    private void generateSessionToken() {
        orderID = "ORDER_" + System.currentTimeMillis(); // Unique order ID
        String url = "https://sandbox.cashfree.com/pg/orders"; // Sandbox API

        // Create request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("order_id", orderID);
            requestBody.put("order_amount", "100.00");
            requestBody.put("order_currency", "INR");

            // Customer details inside "customer_details"
            JSONObject customerDetails = new JSONObject();
            customerDetails.put("customer_id", doctorId);
            customerDetails.put("customer_email", "test@example.com");
            customerDetails.put("customer_phone", "9999999999");

            requestBody.put("customer_details", customerDetails);
        } catch (JSONException e) {
            Log.e(TAG, "Error preparing payment JSON: " + e.getMessage());
            resetProcessingState();
            Toast.makeText(this, "Error preparing payment data", Toast.LENGTH_LONG).show();
            return;
        }

        // API Request to generate session token
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        paymentSessionID = response.getString("payment_session_id");
                        if (!paymentSessionID.isEmpty()) {
                            startPayment();
                        } else {
                            resetProcessingState();
                            Toast.makeText(this, "Failed to get session ID", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing session response: " + e.getMessage());
                        resetProcessingState();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    resetProcessingState();
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Cashfree API error: " + error.networkResponse.statusCode);
                    } else {
                        Log.e(TAG, "Cashfree API request failed: " + error.getMessage());
                    }
                    Toast.makeText(this, "Failed to get session ID", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-version", "2022-09-01");
                headers.put("x-client-id", api);
                headers.put("x-client-secret", secret_api);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    // Start Payment
    private void startPayment() {
        try {
            CFSession cfSession = new CFSession.CFSessionBuilder()
                    .setEnvironment(cfEnvironment)
                    .setPaymentSessionID(paymentSessionID)
                    .setOrderId(orderID)
                    .build();

            CFWebCheckoutTheme cfTheme = new CFWebCheckoutTheme.CFWebCheckoutThemeBuilder()
                    .setNavigationBarBackgroundColor("#fc2678")
                    .setNavigationBarTextColor("#ffffff")
                    .build();

            CFWebCheckoutPayment cfWebCheckoutPayment = new CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                    .setSession(cfSession)
                    .setCFWebCheckoutUITheme(cfTheme)
                    .build();

            gatewayService.doPayment(this, cfWebCheckoutPayment);
        } catch (CFException e) {
            Log.e(TAG, "Payment initialization error: " + e.getMessage());
            resetProcessingState();
            Toast.makeText(this, "Payment Initialization Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Handle Payment Success and store data in DB
    @Override
    public void onPaymentVerify(String orderID) {
        Log.i(TAG, "Payment verified for order: " + orderID);
        saveBookingData();
        runOnUiThread(() -> {
            Toast.makeText(this, "Payment Verified! Order ID: " + orderID, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onPaymentFailure(CFErrorResponse cfErrorResponse, String orderID) {
        Log.e(TAG, "Payment failed for order: " + orderID + ", Error: " + cfErrorResponse.getMessage());
        resetProcessingState();
        runOnUiThread(() -> Toast.makeText(this, "Payment Failed! Order ID: " + orderID, Toast.LENGTH_LONG).show());
    }

    private void saveBookingData() {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.i(TAG, "Appointment saved successfully");
                    resetProcessingState();
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                    // Move to success screen or finish activity
                    finish();
                },
                error -> {
                    resetProcessingState();
                    Log.e(TAG, "Failed to save appointment: " +
                            (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                    Toast.makeText(this, "Error saving appointment. Contact support.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", "1");
                params.put("patient_name", patientName);
                params.put("age", age);
                params.put("gender", gender);
                params.put("address", address);
                params.put("doctor_id", doctorId);
                params.put("reason_for_visit", problem);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    params.put("appointment_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                }
                params.put("time_slot", "10:00 AM");
                params.put("pincode", "112345");
                params.put("appointment_mode", "Offline");
                params.put("payment_method", selectedPaymentMethod);
                params.put("status", Status);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}