package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class pending_bill extends AppCompatActivity {

    private static final int UPI_PAYMENT_REQUEST_CODE = 123;

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod;
    private String patientId;  // Retrieved from SharedPreferences
    private Button payButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Retrieve data from intent
        Intent intent = getIntent();
        patientName = intent.getStringExtra("patient_name");
        age = String.valueOf(intent.getIntExtra("age", 0));
        gender = intent.getStringExtra("gender");
        problem = intent.getStringExtra("problem");
        address = intent.getStringExtra("address");
        doctorId = intent.getStringExtra("doctor_id");
        doctorName = intent.getStringExtra("doctorName");
        Status = intent.getStringExtra("appointment_status");
        if ("Request for visit".equals(Status)) {
            Status = "Requested";
        } else if ("Book Appointment".equals(Status)) {
            Status = "Confirmed";
        }

        // Retrieve patient_id from SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RadioGroup paymentMethodGroup = findViewById(R.id.payment_method_group);
        payButton = findViewById(R.id.pay_button);

        payButton.setOnClickListener(v -> {
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
            selectedPaymentMethod = (selectedId == R.id.payment_online) ? "Online" : "Offline";
            if (selectedId == R.id.payment_online) {
                // Launch UPI payment intent instead of using a payment gateway
                startUpiPayment();
            } else if (selectedId == R.id.payment_offline) {
                // For offline payment, simply save the booking data
                saveBookingData();
            }
        });
    }

    private void startUpiPayment() {
        // Build UPI URI with your merchant details:
        // UPI ID: "abhitadhani98244-2@okicici"
        // Merchant Name: "name the doctor at home"
        String upiUri = "upi://pay?pa=abhitadhani98244-2@okicici&pn=name%20the%20doctor%20at%20home&tn=Payment%20for%20appointment&am=10.00&cu=INR";
        Intent upiIntent = new Intent(Intent.ACTION_VIEW);
        upiIntent.setData(Uri.parse(upiUri));
        if (upiIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No UPI app found. Please install one to proceed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = (data != null) ? data.getStringExtra("response") : null;
            processUpiPaymentResponse(response);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processUpiPaymentResponse(String response) {
        // Example response format: "Status=SUCCESS&txnRef=123456789&ApprovalRefNo=123456789"
        if (response == null) {
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        String status = "";
        String[] responseArray = response.split("&");
        for (String resp : responseArray) {
            String[] keyValue = resp.split("=");
            if (keyValue.length >= 2 && keyValue[0].toLowerCase().equals("status")) {
                status = keyValue[1].toLowerCase();
            }
        }
        if (status.equals("success")) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            // Payment was successful; now save the booking data.
            saveBookingData();
        } else {
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBookingData() {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Error saving appointment. Contact support.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                // Prepare the parameters to be sent to your API
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
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
                // You can adjust the appointment mode if needed
                params.put("appointment_mode", "Online");
                params.put("payment_method", selectedPaymentMethod);
                params.put("status", Status);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                // If needed, specify headers here
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
