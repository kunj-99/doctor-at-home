package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
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
    private static final String TAG = "PendingBill";

    // Merchant UPI details (ideally, store these securely)
    private static final String MERCHANT_UPI_ID = "abhitadhani98244-2@okicici";
    private static final String MERCHANT_NAME = " the doctor at home";
    private static final String TRANSACTION_NOTE = "Payment for appointment";
    private static final String AMOUNT = "500.00";
    private static final String CURRENCY = "INR";

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod;
    private String patientId;  // Retrieved from SharedPreferences
    private Button payButton;


    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName, tvAppointmentId;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

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
        Log.d(TAG, "Booking details: patientName=" + patientName + ", age=" + age +
                ", gender=" + gender + ", problem=" + problem + ", address=" + address +
                ", doctorId=" + doctorId + ", Status=" + Status);

        // Retrieve patient_id from SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Patient ID not available in SharedPreferences");
            finish();
            return;
        }
        Log.d(TAG, "Patient ID: " + patientId);

        // Retrieve dynamic bill info TextViews from the layout
        // (Make sure these IDs exist in your layout file)
        tvBillPatientName = findViewById(R.id.tv_bill_patient_name);
        tvBillDoctorName = findViewById(R.id.tv_bill_doctor_name);
        tvBillDate = findViewById(R.id.tv_bill_date);
        tvBillTime = findViewById(R.id.tv_bill_time);

        // Set dynamic values:
        if (patientName != null) {
            tvBillPatientName.setText(patientName);
        }
        if (doctorName != null) {
            tvBillDoctorName.setText(doctorName);
        }
        // Set current date as the bill date in the format "dd MMMM, yyyy"
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        String currentDate = sdfDate.format(new Date());
        tvBillDate.setText(currentDate);

        // Set current time in the format "hh:mm a"
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = sdfTime.format(new Date());
        tvBillTime.setText(currentTime);

        // Optionally, set appointment id if available from intent (or generate one)
        String dynamicAppointmentId = intent.getStringExtra("appointment_id");
        if (dynamicAppointmentId != null && !dynamicAppointmentId.isEmpty()) {
            tvAppointmentId.setText("#" + dynamicAppointmentId);
        }

        // Initialize remaining views
        RadioGroup paymentMethodGroup = findViewById(R.id.payment_method_group);
        payButton = findViewById(R.id.pay_button);

        payButton.setOnClickListener(v -> {
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.payment_online) {
                selectedPaymentMethod = "Online";
                Log.d(TAG, "Selected payment method: Online");
                startUpiPayment();
            } else if (selectedId == R.id.payment_offline) {
                selectedPaymentMethod = "Offline";
                Log.d(TAG, "Selected payment method: Offline");
                saveBookingData();
            } else {
                Log.e(TAG, "No payment method selected.");
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startUpiPayment() {
        // Build secure UPI URI using merchant details
        String upiUri = "upi://pay?pa=" + MERCHANT_UPI_ID +
                "&pn=" + Uri.encode(MERCHANT_NAME) +
                "&tn=" + Uri.encode(TRANSACTION_NOTE) +
                "&am=" + AMOUNT +
                "&cu=" + CURRENCY;
        Log.d(TAG, "Initiating UPI Payment with URI: " + upiUri);
        Intent upiIntent = new Intent(Intent.ACTION_VIEW);
        upiIntent.setData(Uri.parse(upiUri));

        // Use an intent chooser to allow the user to select their UPI app securely
        Intent chooser = Intent.createChooser(upiIntent, "Pay via UPI");

        if (upiIntent.resolveActivity(getPackageManager()) != null) {
            try {
                startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
            } catch (Exception e) {
                Log.e(TAG, "Exception while starting UPI payment: ", e);
                Toast.makeText(this, "Error starting payment. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No UPI app found for handling payment.");
            Toast.makeText(this, "No UPI app found. Please install one to proceed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = (data != null) ? data.getStringExtra("response") : null;
            Log.d(TAG, "UPI Payment Response: " + response);
            processUpiPaymentResponse(response);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processUpiPaymentResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.e(TAG, "UPI Payment cancelled or failed: response is null or empty");
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Raw UPI response: " + response);
        String status = "";
        String approvalRefNo = "";
        String[] responseArray = response.split("&");
        for (String resp : responseArray) {
            String[] keyValue = resp.split("=");
            if (keyValue.length >= 2) {
                String key = keyValue[0].toLowerCase();
                String value = keyValue[1].toLowerCase();
                if ("status".equals(key)) {
                    status = value;
                } else if ("txnref".equals(key) || "txnrefno".equals(key) || "approvalrefno".equals(key)) {
                    approvalRefNo = value;
                }
            } else {
                Log.e(TAG, "Invalid key-value pair in UPI response: " + resp);
            }
        }
        Log.d(TAG, "Processed UPI Payment Status: " + status + ", ApprovalRefNo: " + approvalRefNo);
        if ("success".equals(status)) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            saveBookingData();
        } else {
            Log.e(TAG, "Payment failed or cancelled. Status: " + status);
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    // Redirects to MainActivity with ongoing appointment fragment
    public void onBookingSuccess() {
        Intent intent = new Intent(pending_bill.this, MainActivity.class);
        // Set the extra to open the ongoing appointments fragment (index 2)
        intent.putExtra("open_fragment", 2);
        startActivity(intent);
        finish();  // Clear this activity from the back stack
    }

    private void saveBookingData() {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php"; // Use HTTPS in production
        Log.d(TAG, "Saving booking data to: " + url);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Booking data saved successfully. Server response: " + response);
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                    // Redirect to ongoing appointments after saving booking data
                    onBookingSuccess();
                },
                error -> {
                    Log.e(TAG, "Error saving booking data: " + error.getMessage());
                    Toast.makeText(this, "Error saving appointment. Contact support.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                params.put("patient_name", patientName);
                params.put("age", age);
                params.put("gender", gender);
                params.put("address", address);
                params.put("doctor_id", doctorId);
                params.put("reason_for_visit", problem);
                String appointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                params.put("appointment_date", appointmentDate);
                params.put("time_slot", "10:00 AM");
                params.put("pincode", "112345");
                params.put("appointment_mode", "Online");
                params.put("payment_method", selectedPaymentMethod);
                params.put("status", Status);
                Log.d(TAG, "Booking data params: " + params.toString());
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
