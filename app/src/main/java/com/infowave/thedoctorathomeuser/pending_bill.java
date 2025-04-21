package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.Dialog;
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
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
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
    private static final int UPI_PAYMENT_REQUEST_CODE = 123;

    // Dynamically loaded from get_upi_config.php
    private String merchantUpiId;
    private String merchantName;
    private String transactionNote;
    private String currency;
    private double baseDistance;
    private double extraCostPerKm;
    private double platformCharge;

    // Payment Base and Fees
    private final double baseCost     = 1.0;
    private final double consultingFee = 0.0;
    private final double gst          = 0.0;
    private double distanceCharge     = 0.0;
    private double distanceKm         = 0.0;
    private double finalCost          = 0.0;

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod = "";
    private String patientId, pincode;

    // UI References
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;
    private TextView tvPaymentAmountValue, tvConsultingFeeValue, tvDistanceKmValue,
            tvDistanceChargeValue, tvGstValue, tvTotalPaidValue;
    private TextView tvWalletBalance, tvPlatformCharge;

    // Coordinates
    private double userLat, userLng, docLat, docLng;
    private String googleMapsLink = "";

    // Wallet Balance
    private double walletBalance = 0.0;
    private boolean platformChargeAdded = false;

    // Handler for wallet refresh
    private final Handler walletHandler = new Handler();
    private Runnable walletRunnable;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // show loader for 3s on startup while we fetch config + other APIs
        loaderutil.showLoader(this);
        new Handler().postDelayed(loaderutil::hideLoader, 3000);

        // 1) load everything (UPI + distances + platform) from DB
        fetchUpiConfig();

        // Retrieve Intent data
        Intent intent = getIntent();
        patientName = intent.getStringExtra("patient_name");
        age         = String.valueOf(intent.getIntExtra("age", 0));
        gender      = intent.getStringExtra("gender");
        problem     = intent.getStringExtra("problem");
        address     = intent.getStringExtra("address");
        doctorId    = intent.getStringExtra("doctor_id");
        doctorName  = intent.getStringExtra("doctorName");
        Status      = intent.getStringExtra("appointment_status");
        pincode     = intent.getStringExtra("pincode");

        if ("Request for visit".equals(Status)) {
            Status = "Requested";
        } else if ("Book Appointment".equals(Status)) {
            Status = "Confirmed";
        }

        // Coordinates
        userLat = intent.getDoubleExtra("latitude", 0.0);
        userLng = intent.getDoubleExtra("longitude", 0.0);
        if (userLat != 0.0 && userLng != 0.0) {
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query="
                    + userLat + "," + userLng;
        }

        // SharedPreferences for patient_id
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind UI
        tvBillPatientName     = findViewById(R.id.tv_bill_patient_name);
        tvBillDoctorName      = findViewById(R.id.tv_bill_doctor_name);
        tvBillDate            = findViewById(R.id.tv_bill_date);
        tvBillTime            = findViewById(R.id.tv_bill_time);
        tvWalletBalance       = findViewById(R.id.tv_wallet_balance);
        tvPlatformCharge      = findViewById(R.id.tv_platform_charge);
        btnRechargeWallet     = findViewById(R.id.btn_recharge_wallet);
        tvPaymentAmountValue  = findViewById(R.id.tv_payment_amount_value);
        tvConsultingFeeValue  = findViewById(R.id.tv_consulting_fee_value);
        tvDistanceKmValue     = findViewById(R.id.tv_distance_km_value);
        tvDistanceChargeValue = findViewById(R.id.tv_distance_charge_value);
        tvGstValue            = findViewById(R.id.tv_gst_value);
        tvTotalPaidValue      = findViewById(R.id.tv_total_paid_value);
        btnOnlinePayment      = findViewById(R.id.btn_online_payment);
        btnOfflinePayment     = findViewById(R.id.btn_offline_payment);
        payButton             = findViewById(R.id.pay_button);

        // Initial UI setup
        btnRechargeWallet.setVisibility(View.GONE);
        if (patientName != null) tvBillPatientName.setText(patientName);
        if (doctorName  != null) tvBillDoctorName.setText(doctorName);

        String curDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                .format(new Date());
        tvBillDate.setText(curDate);
        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date());
        tvBillTime.setText(curTime);

        btnOnlinePayment .setBackgroundColor(getResources().getColor(R.color.custom_gray));
        btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_gray));
        payButton        .setBackgroundColor(getResources().getColor(R.color.custom_gray));
        if (!areRequiredParametersPresent()) {
            disablePayButton();
            Toast.makeText(this, "Some booking details are missing.", Toast.LENGTH_LONG).show();
        }

        finalCost = baseCost + consultingFee + gst + distanceCharge;
        updatePaymentUI();

        // Fetch wallet & start auto-refresh every 30s
        fetchWalletBalance();
        walletRunnable = () -> {
            fetchWalletBalance();
            walletHandler.postDelayed(walletRunnable, 30000);
        };
        walletHandler.post(walletRunnable);

        // Offline button
        btnOfflinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Offline";
            Toast.makeText(this, "Offline Payment selected", Toast.LENGTH_SHORT).show();
            btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_green));
            btnOnlinePayment .setBackgroundColor(getResources().getColor(R.color.custom_gray));
            if (platformChargeAdded) {
                finalCost -= platformCharge;
                platformChargeAdded = false;
                tvPlatformCharge.setText("");
            }
            if (walletBalance >= platformCharge) {
                enablePayButton();
            } else {
                disablePayButton();
                btnRechargeWallet.setVisibility(View.VISIBLE);
            }
            updatePaymentUI();
        });

        // Online button
        btnOnlinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Online";
            Toast.makeText(this, "Online Payment selected", Toast.LENGTH_SHORT).show();
            btnOnlinePayment .setBackgroundColor(getResources().getColor(R.color.custom_green));
            btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_gray));
            if (!platformChargeAdded) {
                finalCost += platformCharge;
                platformChargeAdded = true;
                tvPlatformCharge.setText("Platform Charge Added: ₹" + platformCharge);
            }
            enablePayButton();
        });

        btnRechargeWallet.setOnClickListener(v ->
                startActivity(new Intent(pending_bill.this, payments.class))
        );

        fetchDoctorLocation(doctorId);

        // Proceed – show loader, then save / pay
        payButton.setOnClickListener(v -> new AlertDialog.Builder(pending_bill.this)
                .setTitle("Confirm Appointment")
                .setMessage("Are you sure?\n\nBooking appointment charge will be ₹"
                        + platformCharge + " if you cancel.")
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    // show loader until everything finishes
                    loaderutil.showLoader(pending_bill.this);

                    if (selectedPaymentMethod.isEmpty()) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Please select a payment method",
                                Toast.LENGTH_SHORT).show();

                    } else if ("Offline".equals(selectedPaymentMethod)) {
                        if (walletBalance >= platformCharge) {
                            deductWalletCharge(platformCharge,
                                    "Platform charge for offline appointment booking");
                            tvPlatformCharge.setText("Platform Charge Debited: ₹" + platformCharge);
                            saveBookingData(googleMapsLink);
                        } else {
                            loaderutil.hideLoader();
                            Toast.makeText(this,
                                    "Insufficient balance for offline payment.",
                                    Toast.LENGTH_SHORT).show();
                            btnRechargeWallet.setVisibility(View.VISIBLE);
                        }

                    } else { // Online
                        if (walletBalance >= platformCharge) {
                            deductWalletCharge(platformCharge,
                                    "Platform charge for online appointment (wallet debit)");
                            tvPlatformCharge.setText("Platform Charge Debited: ₹" + platformCharge);
                            startUpiPayment();
                        } else {
                            if (!platformChargeAdded) {
                                finalCost += platformCharge;
                                platformChargeAdded = true;
                                tvPlatformCharge.setText(
                                        "Platform Charge Added: ₹" + platformCharge);
                            }
                            updatePaymentUI();
                            startUpiPayment();
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show()
        );
    }

    // Load UPI + distance + platform config from your PHP
    private void fetchUpiConfig() {
        String url = "http://sxm.a58.mytemp.website/get_upi_config.php";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    if (resp.optBoolean("success")) {
                        merchantUpiId   = resp.optString("upi_id");
                        merchantName    = resp.optString("merchant_name");
                        transactionNote = resp.optString("transaction_note");
                        currency        = resp.optString("currency", "INR");
                        baseDistance    = resp.optDouble("base_distance", 3.0);
                        extraCostPerKm  = resp.optDouble("extra_cost_per_km", 7.0);
                        platformCharge  = resp.optDouble("platform_charge", 50.0);
                        Log.d(TAG, "Config loaded: baseDist=" + baseDistance +
                                " extraKm=" + extraCostPerKm +
                                " platChg=" + platformCharge);
                    } else {
                        Log.e(TAG, "Config not found: " + resp.optString("message"));
                    }
                },
                err -> Log.e(TAG, "Network error fetching config", err)
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void startUpiPayment() {
        if (merchantUpiId == null) {
            Toast.makeText(this, "Loading payment settings…", Toast.LENGTH_SHORT).show();
            return;
        }
        String amtStr = String.format(Locale.getDefault(), "%.2f", finalCost);
        Uri upiUri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", merchantUpiId)
                .appendQueryParameter("pn", merchantName)
                .appendQueryParameter("tn", transactionNote)
                .appendQueryParameter("am", amtStr)
                .appendQueryParameter("cu", currency)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, upiUri);
        startActivityForResult(Intent.createChooser(intent, "Pay via UPI"),
                UPI_PAYMENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, @Nullable Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = data != null ? data.getStringExtra("response") : null;
            processUpiPaymentResponse(response);
        }
    }

    private void processUpiPaymentResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            loaderutil.hideLoader();
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        String status = "";
        String[] arr = response.split("&");
        for (String part : arr) {
            String[] kv = part.split("=");
            if (kv.length >= 2 && "status".equals(kv[0].toLowerCase())) {
                status = kv[1].toLowerCase();
            }
        }
        if ("success".equals(status)) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            saveBookingData(googleMapsLink);
        } else {
            loaderutil.hideLoader();
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updatePaymentUI() {
        tvPaymentAmountValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", baseCost));
        tvConsultingFeeValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", consultingFee));
        tvDistanceKmValue.setText(String.format(Locale.getDefault(),"%.1f km", distanceKm));
        tvDistanceChargeValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", distanceCharge));
        tvGstValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", gst));
        tvTotalPaidValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", finalCost));
    }

    private boolean areRequiredParametersPresent() {
        return patientName  != null && !patientName.trim().isEmpty()
                && age          != null && !age.trim().isEmpty()
                && gender       != null && !gender.trim().isEmpty()
                && problem      != null && !problem.trim().isEmpty()
                && address      != null && !address.trim().isEmpty()
                && doctorId     != null && !doctorId.trim().isEmpty()
                && doctorName   != null && !doctorName.trim().isEmpty()
                && Status       != null && !Status.trim().isEmpty();
    }

    private void fetchWalletBalance() {
        String url = "http://sxm.a58.mytemp.website/get_wallet_balance.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if ("success".equals(obj.getString("status"))) {
                            walletBalance = obj.getDouble("wallet_balance");
                            tvWalletBalance.setText(
                                    "Wallet Balance: ₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));
                        }
                    } catch (JSONException e) { /*…*/ }
                },
                err -> Log.e(TAG, "Wallet fetch error", err)
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id", patientId);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void fetchDoctorLocation(String docId) {
        String url = "http://sxm.a58.mytemp.website/get_doctor_location.php?doctor_id=" + docId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url,null,
                resp -> {
                    try {
                        if (resp.getBoolean("success")) {
                            parseDoctorLatLng(resp.getString("location"));
                        } else updatePaymentUI();
                    } catch (JSONException e) { updatePaymentUI(); }
                },
                err -> updatePaymentUI()
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void parseDoctorLatLng(String docUrl) {
        try {
            Uri uri = Uri.parse(docUrl);
            String q = uri.getQueryParameter("query");
            if (q != null && q.contains(",")) {
                String[] p = q.split(",");
                docLat = Double.parseDouble(p[0]);
                docLng = Double.parseDouble(p[1]);
                fetchDrivingDistance(userLat, userLng, docLat, docLng);
            } else {
                updatePaymentUI();
            }
        } catch (Exception e) {
            updatePaymentUI();
        }
    }

    private void fetchDrivingDistance(double lat1, double lng1,
                                      double lat2, double lng2) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + lat1 + "," + lng1
                + "&destination=" + lat2 + "," + lng2
                + "&key=" + getString(R.string.google_maps_key);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url,null,
                resp -> parseRoutesResponse(resp),
                err -> updatePaymentUI()
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void parseRoutesResponse(JSONObject response) {
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length()>0) {
                JSONObject leg = routes.getJSONObject(0)
                        .getJSONArray("legs")
                        .getJSONObject(0);
                long meters = leg.getJSONObject("distance").getLong("value");
                distanceKm   = meters/1000.0;
                if (distanceKm > baseDistance) {
                    distanceCharge = (distanceKm - baseDistance)*extraCostPerKm;
                } else distanceCharge = 0.0;
                finalCost = baseCost + consultingFee + gst + distanceCharge;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Routes parse error", e);
        }
        updatePaymentUI();
    }

    private void deductWalletCharge(double charge, String reason) {
        walletBalance -= charge;
        updateUserWallet(patientId, walletBalance);
        addWalletTransaction(Integer.parseInt(patientId), charge, "debit", reason);
    }

    private void saveBookingData(String googleMapsLink) {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    try {
                        JSONObject r = new JSONObject(resp);
                        String appointmentId = r.optString("appointment_id","0");
                        insertPaymentHistory(appointmentId);
                    } catch (JSONException e) {
                        loaderutil.hideLoader();
                    }
                    Toast.makeText(this,"Appointment saved successfully!",Toast.LENGTH_SHORT).show();
                },
                err -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this,"Error saving appointment!",Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id",     patientId);
                p.put("patient_name",   patientName);
                p.put("age",            age);
                p.put("gender",         gender);
                p.put("address",        address);
                p.put("doctor_id",      doctorId);
                p.put("reason_for_visit", problem);
                p.put("appointment_date",
                        new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault())
                                .format(new Date()));
                p.put("time_slot",      "10:00 AM");
                p.put("pincode",        pincode);
                p.put("appointment_mode","Online");
                p.put("payment_method", selectedPaymentMethod);
                p.put("status",         Status);
                p.put("location",       googleMapsLink);
                p.put("final_cost",
                        String.format(Locale.getDefault(),"%.2f", finalCost));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void updateUserWallet(String userId, double newBalance) {
        String url = "http://sxm.a58.mytemp.website/update_wallet.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG,"Wallet updated: "+resp),
                err -> Log.e(TAG,"Wallet update error",err)
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("user_id", userId);
                p.put("wallet_balance",
                        String.format(Locale.getDefault(),"%.2f",newBalance));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void addWalletTransaction(int patientId, double amount,
                                      String type, String reason) {
        String url = "http://sxm.a58.mytemp.website/add_wallet_transaction.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG,"Wallet txn added: "+resp),
                err -> Log.e(TAG,"Wallet txn error",err)
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id", String.valueOf(patientId));
                p.put("amount",     String.format(Locale.getDefault(),"%.2f",amount));
                p.put("type",       type);
                p.put("reason",     reason);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void insertPaymentHistory(String appointmentId) {
        String statusEnum = "Pending";
        String url = "http://sxm.a58.mytemp.website/payment_history.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this,"Payment recorded.",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Payment inserted => "+resp);
                    onBookingSuccess();
                },
                err -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this,"Failed to record payment.",Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id",     patientId);
                p.put("appointment_id", appointmentId);
                p.put("doctor_id",      doctorId);
                p.put("amount",
                        String.format(Locale.getDefault(),"%.2f", finalCost));
                p.put("payment_method", selectedPaymentMethod);
                p.put("distance",
                        String.format(Locale.getDefault(),"%.2f", distanceKm));
                p.put("distance_charge",
                        String.format(Locale.getDefault(),"%.2f", distanceCharge));
                p.put("gst",
                        String.format(Locale.getDefault(),"%.2f", gst));
                p.put("payment_status", statusEnum);
                p.put("refund_status",  "None");
                p.put("notes",          "None");
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void onBookingSuccess() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_fragment", 2);
        startActivity(i);
        finish();
    }

    private void enablePayButton() {
        payButton.setEnabled(true);
        payButton.setAlpha(1f);
        payButton.setBackgroundColor(getResources().getColor(R.color.custom_green));
    }
    private void disablePayButton() {
        payButton.setEnabled(false);
        payButton.setAlpha(0.5f);
        payButton.setBackgroundColor(getResources().getColor(R.color.custom_gray));
    }
}
