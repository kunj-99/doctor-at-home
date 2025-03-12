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
import com.android.volley.Response;
import com.android.volley.VolleyError;
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

public class pending_bill extends AppCompatActivity implements CFCheckoutResponseCallback {

    private CFPaymentGatewayService gatewayService;
    private String orderID;
    private String paymentSessionID;
    private String api = "TEST1049127073b42a1e211a7cabe17207219401";
    private String secret_api = "cfsk_ma_test_a1ff9b5ff8e6e3f11107cb84b0037b7f_88f81c16";
    private CFSession.Environment cfEnvironment = CFSession.Environment.SANDBOX; // Use sandbox for testing

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName,Status,selectedPaymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // ✅ Get booking details from Intent
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


        // ✅ Initialize Cashfree SDK
        try {
            gatewayService = CFPaymentGatewayService.getInstance();
            gatewayService.setCheckoutCallback(this);
        } catch (CFException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to initialize Cashfree SDK", Toast.LENGTH_LONG).show();
            return;
        }

        RadioGroup paymentMethodGroup = findViewById(R.id.payment_method_group);
        Button payButton = findViewById(R.id.pay_button);

        payButton.setOnClickListener(v -> {
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
             selectedPaymentMethod = (selectedId == R.id.payment_online) ? "Online" : "Offline";
            if (selectedId == R.id.payment_online) {
                generateSessionToken();  // Online Payment
            } else if (selectedId == R.id.payment_offline) {
                saveBookingData();
            }
        });


    }

    private void generateSessionToken() {
        orderID = "ORDER_" + System.currentTimeMillis(); // Unique order ID

        String url = "https://sandbox.cashfree.com/pg/orders"; // Sandbox API

        // ✅ Create request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("order_id", orderID);
            requestBody.put("order_amount", "100.00"); // Change this if needed
            requestBody.put("order_currency", "INR");

            // ✅ Customer details inside "customer_details"
            JSONObject customerDetails = new JSONObject();
            customerDetails.put("customer_id", doctorId);
            customerDetails.put("customer_email", "test@example.com");
            customerDetails.put("customer_phone", "9999999999");

            requestBody.put("customer_details", customerDetails);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // ✅ API Request to generate session token
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        paymentSessionID = response.getString("payment_session_id");
                        Log.d("Cashfree", "Session ID: " + paymentSessionID);
                        if (paymentSessionID != null && !paymentSessionID.isEmpty()) {
                            startPayment();
                        } else {
                            Toast.makeText(this, "Failed to get session ID", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        String responseBody = new String(error.networkResponse.data);
                        Log.e("Cashfree", "Error Code: " + error.networkResponse.statusCode);
                        Log.e("Cashfree", "Error Response: " + responseBody);
                    } else {
                        Log.e("Cashfree", "API Request Failed: " + error.getMessage());
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

    // ✅ Start Payment (Fully retained)
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
            e.printStackTrace();
            Toast.makeText(this, "Payment Initialization Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ✅ Handle Payment Success and store data in DB
    @Override
    public void onPaymentVerify(String orderID) {
        Log.e("onPaymentVerify", "Payment verified for order: " + orderID);

        // ✅ Check if this method is being called
        Log.e("DB_SAVE", "Calling saveBookingData() method...");

        // ✅ Store booking details in database
        saveBookingData();

        runOnUiThread(() -> Toast.makeText(this, "Payment Verified! Order ID: " + orderID, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onPaymentFailure(CFErrorResponse cfErrorResponse, String orderID) {
        Log.e("onPaymentFailure", "Payment failed for order: " + orderID + ", Error: " + cfErrorResponse.getMessage());
        runOnUiThread(() -> Toast.makeText(this, "Payment Failed! Order ID: " + orderID, Toast.LENGTH_LONG).show());
    }
    private void saveBookingData() {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php";

        Log.d("DB_SAVE", "Sending request to: " + url); // ✅ Log request URL

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("DB_SAVE", "Server Response: " + response); // ✅ Log API Response
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    if (error.networkResponse != null) {
                        String responseBody = new String(error.networkResponse.data);
                        Log.e("DB_SAVE", "Failed to save booking | Status Code: " + error.networkResponse.statusCode);
                        Log.e("DB_SAVE", "Error Response: " + responseBody);
                    } else {
                        Log.e("DB_SAVE", "API Request Failed: " + error.getMessage());
                    }
                    Toast.makeText(this, "Error saving appointment. Contact support.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", "1"); // 🔥 Static Patient ID
                params.put("patient_name", patientName);
                params.put("age", age);
                params.put("gender", gender);
                params.put("address", address);
                params.put("doctor_id", doctorId);
                params.put("reason_for_visit", problem);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    params.put("appointment_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                }
                params.put("time_slot", "10:00 AM"); // ✅ Ensure non-null value
                params.put("pincode", "112345"); // ✅ Check if this reaches the API
                params.put("appointment_mode", "Offline");
                params.put("payment_method",selectedPaymentMethod);
                params.put("status", Status);

                Log.d("DB_SAVE", "Params: " + params.toString()); // ✅ Debugging - Log request parameters

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