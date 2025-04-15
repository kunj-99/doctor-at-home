package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class pending_bill extends AppCompatActivity {

    private static final String TAG = "PendingBill";

    // UPI Payment Constants
    private static final int UPI_PAYMENT_REQUEST_CODE = 123;
    private static final String MERCHANT_UPI_ID  = "mohitvatiya3333@oksbi";
    private static final String MERCHANT_NAME    = "the doctor at home";
    private static final String TRANSACTION_NOTE = "Payment for appointment";
    private static final String CURRENCY         = "INR";

    // Payment Base and Fees
    private final double baseCost = 1.0;
    private final double consultingFee = 0.0;
    private final double gst = 0.0;
    private double distanceCharge = 0.0;
    private double distanceKm = 0.0;
    private double finalCost = 0.0;

    // Distance Charge Calculation
    private static final double BASE_DISTANCE = 3.0;
    private static final double EXTRA_COST_PER_KM = 7.0;

    // Platform Charge
    private static final double PLATFORM_CHARGE = 50.0;

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod = "";
    private String patientId;
    private String pincode;

    // UI References
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;
    // Payment Details UI References
    private TextView tvPaymentAmountValue, tvConsultingFeeValue, tvDistanceKmValue, tvDistanceChargeValue, tvGstValue, tvTotalPaidValue;
    // New UI elements for wallet
    private TextView tvWalletBalance;
    private TextView tvPlatformCharge; // To display platform charge debited or added

    // Coordinates for Route Calculations
    private double userLat;
    private double userLng;
    private double docLat;
    private double docLng;

    private String googleMapsLink = "";

    // Wallet Balance (fetched dynamically)
    private double walletBalance = 0.0;

    // To ensure that for online payment the platform charge is added only once
    private boolean platformChargeAdded = false;

    // Handler for continuous wallet refresh (e.g., every 30 seconds)
    private final Handler walletHandler = new Handler();
    private Runnable walletRunnable;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Retrieve data passed via Intent
        Intent intent = getIntent();
        patientName = intent.getStringExtra("patient_name");
        age = String.valueOf(intent.getIntExtra("age", 0));
        gender = intent.getStringExtra("gender");
        problem = intent.getStringExtra("problem");
        address = intent.getStringExtra("address");
        doctorId = intent.getStringExtra("doctor_id");
        doctorName = intent.getStringExtra("doctorName");
        Status = intent.getStringExtra("appointment_status");
        pincode = intent.getStringExtra("pincode");

        if ("Request for visit".equals(Status)) {
            Status = "Requested";
        } else if ("Book Appointment".equals(Status)) {
            Status = "Confirmed";
        }
        Log.d(TAG, "Booking details => patientName=" + patientName + ", age=" + age +
                ", gender=" + gender + ", problem=" + problem +
                ", address=" + address + ", doctorId=" + doctorId +
                ", Status=" + Status);

        // User Coordinates
        userLat = intent.getDoubleExtra("latitude", 0.0);
        userLng = intent.getDoubleExtra("longitude", 0.0);
        if (userLat != 0.0 && userLng != 0.0) {
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=" + userLat + "," + userLng;
        }
        Log.d(TAG, "User lat,lng => " + userLat + "," + userLng);

        // Retrieve Patient ID from SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link XML UI elements
        tvBillPatientName = findViewById(R.id.tv_bill_patient_name);
        tvBillDoctorName = findViewById(R.id.tv_bill_doctor_name);
        tvBillDate = findViewById(R.id.tv_bill_date);
        tvBillTime = findViewById(R.id.tv_bill_time);
        tvWalletBalance = findViewById(R.id.tv_wallet_balance);
        tvPlatformCharge = findViewById(R.id.tv_platform_charge);

        // New button for wallet recharge (visible when offline payment is not possible)
        btnRechargeWallet = findViewById(R.id.btn_recharge_wallet);
        btnRechargeWallet.setVisibility(View.GONE); // Initially hidden

        if (patientName != null) {
            tvBillPatientName.setText(patientName);
        }
        if (doctorName != null) {
            tvBillDoctorName.setText(doctorName);
        }

        // Set current date/time
        String curDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(new Date());
        tvBillDate.setText(curDate);
        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvBillTime.setText(curTime);

        // Link Payment Details UI elements
        tvPaymentAmountValue = findViewById(R.id.tv_payment_amount_value);
        tvConsultingFeeValue = findViewById(R.id.tv_consulting_fee_value);
        tvDistanceKmValue = findViewById(R.id.tv_distance_km_value);
        tvDistanceChargeValue = findViewById(R.id.tv_distance_charge_value);
        tvGstValue = findViewById(R.id.tv_gst_value);
        tvTotalPaidValue = findViewById(R.id.tv_total_paid_value);

        // Link Payment Method Buttons
        btnOnlinePayment = findViewById(R.id.btn_online_payment);
        btnOfflinePayment = findViewById(R.id.btn_offline_payment);

        // Set initial button background colors using custom colors
        btnOnlinePayment.setBackgroundColor(getResources().getColor(R.color.custom_gray));
        btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_gray));

        // Initialize final cost calculation
        finalCost = baseCost + consultingFee + gst + distanceCharge;
        updatePaymentUI();

        // Link the Proceed to Payment button and set initial background color.
        payButton = findViewById(R.id.pay_button);
        payButton.setBackgroundColor(getResources().getColor(R.color.custom_gray));

        if (!areRequiredParametersPresent()) {
            payButton.setEnabled(false);
            payButton.setAlpha(0.5f);
            Toast.makeText(this, "Some booking details are missing.", Toast.LENGTH_LONG).show();
        }

        // Fetch dynamic wallet balance immediately
        fetchWalletBalance();

        // Start continuous wallet refresh (every 30 seconds)
        walletRunnable = new Runnable() {
            @Override
            public void run() {
                fetchWalletBalance();
                walletHandler.postDelayed(this, 3000);
            }
        };
        walletHandler.post(walletRunnable);

        // Button listeners

        btnOfflinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Offline";
            Toast.makeText(this, "Offline Payment selected", Toast.LENGTH_SHORT).show();
            btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_green));
            btnOnlinePayment.setBackgroundColor(getResources().getColor(R.color.custom_gray));

            // Remove platform charge if it was added for online payment
            if (platformChargeAdded) {
                finalCost -= PLATFORM_CHARGE;
                platformChargeAdded = false;  // Reset the flag
                tvPlatformCharge.setText(""); // Clear the platform charge text
            }

            // Check offline payment: if wallet balance is sufficient (>= PLATFORM_CHARGE)
            if (walletBalance >= PLATFORM_CHARGE) {
                payButton.setEnabled(true);
                payButton.setAlpha(1.0f);
                payButton.setBackgroundColor(getResources().getColor(R.color.custom_green));
                btnRechargeWallet.setVisibility(View.GONE);
            } else {
                payButton.setEnabled(false);
                payButton.setAlpha(0.5f);
                payButton.setBackgroundColor(getResources().getColor(R.color.custom_gray));
                Toast.makeText(this, "Insufficient wallet balance for offline payment.", Toast.LENGTH_SHORT).show();
                // Show the recharge wallet button instead of immediately redirecting
                btnRechargeWallet.setVisibility(View.VISIBLE);
            }

            // Update the payment UI for offline payment (with platform charge removed)
            updatePaymentUI();
        });

        btnOnlinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Online";
            Toast.makeText(this, "Online Payment selected", Toast.LENGTH_SHORT).show();
            btnOnlinePayment.setBackgroundColor(getResources().getColor(R.color.custom_green));
            btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_gray));

            // If platform charge hasn't been added yet, add it now
            if (!platformChargeAdded) {
                finalCost += PLATFORM_CHARGE;
                platformChargeAdded = true;
                tvPlatformCharge.setText("Platform Charge Added: ₹" + PLATFORM_CHARGE);
            }
            // Always allow payButton click for online payment
            payButton.setEnabled(true);
            payButton.setAlpha(1.0f);
            payButton.setBackgroundColor(getResources().getColor(R.color.custom_green));
        });

        // Recharge Wallet Button listener (for offline if insufficient balance)
        btnRechargeWallet.setOnClickListener(v -> {
            // Redirect to the payments (recharge) activity/page.
            Intent rechargeIntent = new Intent(pending_bill.this, payments.class);
            startActivity(rechargeIntent);
        });

        // Fetch doctor's location and calculate driving distance
        fetchDoctorLocation(doctorId);

        // Proceed button listener with wallet logic based on payment method
        payButton.setOnClickListener(v -> {
            if (selectedPaymentMethod == null || selectedPaymentMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            } else if ("Offline".equals(selectedPaymentMethod)) {
                // For offline payment, check again
                if (walletBalance >= PLATFORM_CHARGE) {
                    deductWalletCharge(PLATFORM_CHARGE, "Platform charge for offline appointment booking");
                    // Show in platform charge text view
                    tvPlatformCharge.setText("Platform Charge Debited: ₹" + PLATFORM_CHARGE);
                    saveBookingData(googleMapsLink);
                } else {
                    Toast.makeText(this, "Insufficient balance for offline payment.", Toast.LENGTH_SHORT).show();
                    btnRechargeWallet.setVisibility(View.VISIBLE);
                }
            } else if ("Online".equals(selectedPaymentMethod)) {
                if (walletBalance >= PLATFORM_CHARGE) {
                    deductWalletCharge(PLATFORM_CHARGE, "Platform charge for online appointment (wallet debit)");
                    tvPlatformCharge.setText("Platform Charge Debited: ₹" + PLATFORM_CHARGE);
                    startUpiPayment();
                } else {
                    // If insufficient wallet balance, add platform charge only once to final cost
                    if (!platformChargeAdded) {
                        finalCost = finalCost + PLATFORM_CHARGE;
                        platformChargeAdded = true;
                        tvPlatformCharge.setText("Platform Charge Added: ₹" + PLATFORM_CHARGE);
                    }
                    updatePaymentUI();
                    startUpiPayment();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        walletHandler.removeCallbacks(walletRunnable);
    }

    // Check that all required parameters are present
    private boolean areRequiredParametersPresent() {
        return (patientName != null && !patientName.trim().isEmpty() &&
                age != null && !age.trim().isEmpty() &&
                gender != null && !gender.trim().isEmpty() &&
                problem != null && !problem.trim().isEmpty() &&
                address != null && !address.trim().isEmpty() &&
                doctorId != null && !doctorId.trim().isEmpty() &&
                doctorName != null && !doctorName.trim().isEmpty() &&
                Status != null && !Status.trim().isEmpty());
    }

    // Fetch wallet balance using API
    private void fetchWalletBalance() {
        String url = "http://sxm.a58.mytemp.website/get_wallet_balance.php";

        @SuppressLint("SetTextI18n") StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
                            walletBalance = Double.parseDouble(obj.getString("wallet_balance"));
                            Log.d(TAG, "Wallet balance fetched: ₹" + walletBalance);
                            tvWalletBalance.setText("Wallet Balance: ₹" + String.format(Locale.getDefault(), "%.2f", walletBalance));
                        } else {
                            Toast.makeText(this, "Unable to fetch wallet balance", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Wallet fetch error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Network error while fetching wallet", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Fetch doctor's location using API
    private void fetchDoctorLocation(String docId) {
        String fetchUrl = "http://sxm.a58.mytemp.website/get_doctor_location.php?doctor_id=" + docId;
        Log.d(TAG, "Fetching doctor location => " + fetchUrl);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                fetchUrl,
                null,
                response -> {
                    Log.d(TAG, "Doctor location response => " + response);
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            String docLocationUrl = response.getString("location");
                            parseDoctorLatLng(docLocationUrl);
                        } else {
                            Log.e(TAG, "Doctor location not successful => " + response);
                            updatePaymentUI();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error => " + e.getMessage());
                        updatePaymentUI();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching doctor location => " + error.getMessage());
                    updatePaymentUI();
                }
        );
        queue.add(request);
    }

    // Parse doctor's latitude and longitude from URL
    private void parseDoctorLatLng(String docUrl) {
        Log.d(TAG, "Parsing doctor lat,lng => " + docUrl);
        try {
            Uri uri = Uri.parse(docUrl);
            String qParam = uri.getQueryParameter("query");
            if (qParam != null && qParam.contains(",")) {
                String[] parts = qParam.split(",");
                docLat = Double.parseDouble(parts[0]);
                docLng = Double.parseDouble(parts[1]);
                Log.d(TAG, "Doctor lat,lng => " + docLat + "," + docLng);
                fetchDrivingDistance(userLat, userLng, docLat, docLng);
            } else {
                Log.e(TAG, "No valid lat,lng found => " + qParam);
                updatePaymentUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing doctor lat,lng => " + e.getMessage());
            updatePaymentUI();
        }
    }

    // Fetch driving distance using Google Routes API
    private void fetchDrivingDistance(double lat1, double lng1, double lat2, double lng2) {
        // Construct the URL with your API key (replace with your own key)
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + lat1 + "," + lng1 + "&destination=" + lat2 + "," + lng2 + "&key=" + getString(R.string.google_maps_key);

        // Make the network request using Volley
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest routeReq = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Routes API response => " + response);
                    parseRoutesResponse(response);  // Handle the response after the request
                },
                error -> {
                    String errorMessage = error.getMessage();
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMessage = new String(error.networkResponse.data);
                    }
                    Log.e(TAG, "Routes API error => " + errorMessage);
                    updatePaymentUI();  // Handle errors
                }
        );
        queue.add(routeReq);
    }

    // Parse Routes API response to extract driving distance and calculate charge
    // Parse Routes API response to extract driving distance and calculate charge
    private void parseRoutesResponse(JSONObject response) {
        try {
            // Extract the "routes" array from the response
            JSONArray routes = response.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject firstRoute = routes.getJSONObject(0);

                // Extract the "legs" array from the first route
                JSONArray legs = firstRoute.getJSONArray("legs");

                if (legs.length() > 0) {
                    JSONObject firstLeg = legs.getJSONObject(0);

                    // Get the distance in meters from the first leg
                    JSONObject distance = firstLeg.getJSONObject("distance");
                    long distanceMeters = distance.getLong("value");

                    // Convert the distance to kilometers
                    distanceKm = distanceMeters / 1000.0;

                    Log.d(TAG, "Driving distance (Routes API) => " + distanceKm + " km");

                    // Calculate the charge based on the distance
                    if (distanceKm > BASE_DISTANCE) {
                        double extraDist = distanceKm - BASE_DISTANCE;
                        distanceCharge = extraDist * EXTRA_COST_PER_KM;
                    } else {
                        distanceCharge = 0.0;
                    }

                    // Calculate the final cost
                    finalCost = baseCost + consultingFee + gst + distanceCharge;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Routes API response => " + e.getMessage());
        }

        // Update the UI with the parsed data
        updatePaymentUI();
    }


    // Update Payment UI fields
    @SuppressLint("SetTextI18n")
    private void updatePaymentUI() {
        Log.d(TAG, "Updating UI => distance: " + distanceKm + " km, charge: " + distanceCharge + ", final cost: " + finalCost);
        tvPaymentAmountValue.setText("₹ " + String.format(Locale.getDefault(), "%.0f", baseCost));
        tvConsultingFeeValue.setText("₹ " + String.format(Locale.getDefault(), "%.0f", consultingFee));
        tvDistanceKmValue.setText(String.format(Locale.getDefault(), "%.1f km", distanceKm));
        tvDistanceChargeValue.setText("₹ " + String.format(Locale.getDefault(), "%.0f", distanceCharge));
        tvGstValue.setText("₹ " + String.format(Locale.getDefault(), "%.0f", gst));
        tvTotalPaidValue.setText("₹ " + String.format(Locale.getDefault(), "%.0f", finalCost));
    }

    // Deduct the platform charge from wallet and record transaction
    private void deductWalletCharge(double charge, String reason) {
        walletBalance -= charge;
        updateUserWallet(patientId, walletBalance);
        addWalletTransaction(Integer.parseInt(patientId), charge, "debit", reason);
    }

    // Initiate UPI Payment flow
    private void startUpiPayment() {
        String amtStr = String.format(Locale.getDefault(), "%.2f", finalCost);
        String upiUri = "upi://pay?pa=" + MERCHANT_UPI_ID +
                "&pn=" + Uri.encode(MERCHANT_NAME) +
                "&tn=" + Uri.encode(TRANSACTION_NOTE) +
                "&am=" + amtStr +
                "&cu=" + CURRENCY;
        Log.d(TAG, "UPI Payment URI => " + upiUri);
        Intent upiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(upiUri));
        Intent chooser = Intent.createChooser(upiIntent, "Pay via UPI");
        if (upiIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No UPI app found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, @Nullable Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = (data != null) ? data.getStringExtra("response") : null;
            processUpiPaymentResponse(response);
        }
    }

    // Process the response from UPI Payment intent
    private void processUpiPaymentResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "UPI Payment Response => " + response);
        String status = "";
        String approvalRefNo = "";
        String[] arr = response.split("&");
        for (String part : arr) {
            String[] kv = part.split("=");
            if (kv.length >= 2) {
                String key = kv[0].toLowerCase();
                String val = kv[1].toLowerCase();
                if ("status".equals(key)) {
                    status = val;
                } else if ("txnref".equals(key) || "txnrefno".equals(key) || "approvalrefno".equals(key)) {
                    approvalRefNo = val;
                }
            }
        }
        Log.d(TAG, "UPI Payment status => " + status + ", reference => " + approvalRefNo);
        if ("success".equals(status)) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            saveBookingData(googleMapsLink);
        } else {
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    // Save appointment data to the server
    private void saveBookingData(String googleMapsLink) {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php";
        Log.d(TAG, "Saving booking data => " + url);
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    Log.d(TAG, "Booking saved: " + resp);
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                    onBookingSuccess();
                },
                err -> {
                    Log.e(TAG, "Error saving booking: " + err.getMessage());
                    Toast.makeText(this, "Error saving appointment!", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("patient_name", patientName);
                p.put("age", age);
                p.put("gender", gender);
                p.put("address", address);
                p.put("doctor_id", doctorId);
                p.put("reason_for_visit", problem);
                String apptDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                p.put("appointment_date", apptDate);
                p.put("time_slot", "10:00 AM");
                p.put("pincode", pincode);
                p.put("appointment_mode", "Online");
                p.put("payment_method", selectedPaymentMethod);
                p.put("status", Status);
                p.put("location", googleMapsLink);
                String costStr = String.format(Locale.getDefault(), "%.2f", finalCost);
                p.put("final_cost", costStr);
                return p;
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/x-www-form-urlencoded");
                return h;
            }
        };
        RequestQueue q = Volley.newRequestQueue(this);
        q.add(req);
    }

    // Update the wallet balance on the server
    private void updateUserWallet(String userId, double newBalance) {
        String url = "http://sxm.a58.mytemp.website/update_wallet.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "User wallet updated: " + response);
                },
                error -> {
                    Log.e(TAG, "Error updating wallet: " + error.getMessage());
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                params.put("wallet_balance", String.format(Locale.getDefault(), "%.2f", newBalance));
                return params;
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        RequestQueue q = Volley.newRequestQueue(this);
        q.add(req);
    }

    // Add a wallet transaction entry
    private void addWalletTransaction(int patientId, double amount, String type, String reason) {
        String url = "http://sxm.a58.mytemp.website/add_wallet_transaction.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Wallet transaction added: " + response);
                    // Handle the response from the server, e.g., show a success message
                },
                error -> {
                    Log.e(TAG, "Error adding wallet transaction: " + error.getMessage());
                    // Handle the error, e.g., show an error message
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", String.valueOf(patientId));  // Use patientId here
                params.put("amount", String.format(Locale.getDefault(), "%.2f", amount));
                params.put("type", type);
                params.put("reason", reason);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        RequestQueue q = Volley.newRequestQueue(this);
        q.add(req);
    }

    // On successful booking, return to MainActivity
    private void onBookingSuccess() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_fragment", 2);
        startActivity(i);
        finish();
    }
}
