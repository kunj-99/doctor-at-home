package com.example.thedoctorathomeuser;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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

import java.util.HashMap;
import java.util.Map;

public class pending_bill extends AppCompatActivity implements CFCheckoutResponseCallback {

    private CFPaymentGatewayService gatewayService;
    private String orderID;
    private String paymentSessionID;
    private String api = "TEST1049127073b42a1e211a7cabe17207219401";
    private String secret_api = "cfsk_ma_test_a1ff9b5ff8e6e3f11107cb84b0037b7f_88f81c16";
    private CFSession.Environment cfEnvironment = CFSession.Environment.SANDBOX; // Use sandbox for testing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Initialize Cashfree SDK
        try {
            gatewayService = CFPaymentGatewayService.getInstance();
            gatewayService.setCheckoutCallback(this);
        } catch (CFException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to initialize Cashfree SDK", Toast.LENGTH_LONG).show();
            return;
        }

        Button payButton = findViewById(R.id.pay_button);
        payButton.setOnClickListener(v -> generateSessionToken());
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

            // ✅ FIX: Wrap customer details inside "customer_details"
            JSONObject customerDetails = new JSONObject();
            customerDetails.put("customer_id", "CUST_12345");
            customerDetails.put("customer_email", "test@example.com");
            customerDetails.put("customer_phone", "9999999999");

            requestBody.put("customer_details", customerDetails);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Make API request to generate session token
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
                headers.put("x-client-id", api); // Your Test Client ID
                headers.put("x-client-secret", secret_api); // Your Test Client Secret
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }


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

    @Override
    public void onPaymentVerify(String orderID) {
        Log.e("onPaymentVerify", "verifyPayment triggered for order: " + orderID);
        runOnUiThread(() -> Toast.makeText(this, "Payment Verified! Order ID: " + orderID, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onPaymentFailure(CFErrorResponse cfErrorResponse, String orderID) {
        Log.e("onPaymentFailure", "Payment failed for order: " + orderID + ", Error: " + cfErrorResponse.getMessage());
        runOnUiThread(() -> Toast.makeText(this, "Payment Failed! Order ID: " + orderID, Toast.LENGTH_LONG).show());
    }
}

