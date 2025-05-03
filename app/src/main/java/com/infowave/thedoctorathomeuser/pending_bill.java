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

    // ─── STATIC CHARGES (TEMP) ────────────────────────────────────────────────────
    private static  double APPOINTMENT_CHARGE ;  // ₹100 flat
    private static double DEPOSIT           ;   // ₹50 deposit
    private static  double PER_KM_CHARGE     ;    // ₹7 per extra km
    private static  double GST_PERCENT      ;    // 0% GST
    private static  double FREE_DISTANCE_KM  ;    // first 3 km free

    // Calculated fees

    private boolean platformChargeAdded = false;
    private double consultingFee;
    private double distanceKm       = 0.0;
    private double distanceCharge   = 0.0;
    private double gstAmount        = 0.0;
    private double finalCost        = 0.0;
    private double docLat           = 0.0;
    private double docLng           = 0.0;

    // Dynamic UPI/config (if you want to reintegrate)
    private String merchantUpiId, merchantName, transactionNote, currency;
    private double baseDistance, extraCostPerKm;

    // Booking Data
    private String patientName, age, gender, problem, address,
            doctorId, doctorName, Status, selectedPaymentMethod;
    private String patientId, pincode;

    // UI References
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;
    private TextView tvAppointmentCharge, tvDeposit, tvConsultingFeeValue,
            tvDistanceKmValue, tvDistanceChargeValue,
            tvGstValue, tvTotalPaidValue, tvWalletBalance;

    // Location & wallet
    private double userLat, userLng;
    private String googleMapsLink = "";
    private double walletBalance = 0.0;

    // Handler for wallet refresh
    private final Handler walletHandler = new Handler();
    private Runnable walletRunnable;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // SharedPreferences for patient_id
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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





        // Bind UI
        tvBillPatientName     = findViewById(R.id.tv_bill_patient_name);
        tvBillDoctorName      = findViewById(R.id.tv_bill_doctor_name);
        tvBillDate            = findViewById(R.id.tv_bill_date);
        tvBillTime            = findViewById(R.id.tv_bill_time);
        tvWalletBalance       = findViewById(R.id.tv_wallet_balance);

        tvAppointmentCharge   = findViewById(R.id.tv_appointment_charge);
        tvDeposit             = findViewById(R.id.tv_deposit);
        tvConsultingFeeValue  = findViewById(R.id.tv_consultation_fee);
        tvDistanceKmValue     = findViewById(R.id.tv_distance_km_value);
        tvDistanceChargeValue = findViewById(R.id.tv_distance_charge_value);
        tvGstValue            = findViewById(R.id.tv_gst_value);
        tvTotalPaidValue      = findViewById(R.id.tv_total_paid_value);

        btnOnlinePayment      = findViewById(R.id.btn_online_payment);
        btnOfflinePayment     = findViewById(R.id.btn_offline_payment);
        btnRechargeWallet     = findViewById(R.id.btn_recharge_wallet);
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

//        if (!areRequiredParametersPresent()) {
//            disablePayButton();
//            Toast.makeText(this, "Some booking details are missing.", Toast.LENGTH_LONG).show();
//        }


        gstAmount     = APPOINTMENT_CHARGE * (GST_PERCENT / 100.0);

        consultingFee = APPOINTMENT_CHARGE - DEPOSIT;


        finalCost = consultingFee + gstAmount+ distanceCharge;


        updatePaymentUI();

        // Fetch wallet & start auto-refresh every 30s
        fetchWalletBalance();
        walletRunnable = () -> {
            fetchWalletBalance();
            walletHandler.postDelayed(walletRunnable, 3000);
        };
        walletHandler.post(walletRunnable);


        // Offline button
        btnOfflinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Offline";
            Toast.makeText(this, "Offline Payment selected", Toast.LENGTH_SHORT).show();
            btnOfflinePayment.setBackgroundColor(getResources().getColor(R.color.custom_green));
            btnOnlinePayment .setBackgroundColor(getResources().getColor(R.color.custom_gray));
            if (platformChargeAdded) {
                finalCost -= DEPOSIT;
                platformChargeAdded = false;
                tvDeposit   .setText("-"+DEPOSIT);
            }
            if (walletBalance >= DEPOSIT) {
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
            if (walletBalance >=DEPOSIT) {
                tvDeposit   .setText(" debite in wallet: ₹ -" + DEPOSIT);

            } else {
                if (!platformChargeAdded) {
                    finalCost +=DEPOSIT;
                    platformChargeAdded = true;
                    tvDeposit   .setText(
                            " Charge Added in bill : ₹ +" + DEPOSIT);
                }
            }
            enablePayButton();
            updatePaymentUI();
        });

        btnRechargeWallet.setOnClickListener(v ->
                startActivity(new Intent(pending_bill.this, payments.class))

        );

        fetchDoctorLocation(doctorId);

        // Proceed – show loader, then save / pay
        payButton.setOnClickListener(v -> new AlertDialog.Builder(pending_bill.this)
                .setTitle("Confirm Appointment")
                .setMessage("Are you sure?\n\nBooking appointment charge will be ₹"
                        +DEPOSIT + " if you cancel.")
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    // show loader until everything finishes
                    loaderutil.showLoader(pending_bill.this);

                    if (selectedPaymentMethod.isEmpty()) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Please select a payment method",
                                Toast.LENGTH_SHORT).show();

                    } else if ("Offline".equals(selectedPaymentMethod)) {
                        if (walletBalance >= DEPOSIT) {
                            deductWalletCharge(DEPOSIT,
                                    "Platform charge for offline appointment booking");
                            tvDeposit   .setText("Platform Charge Debited: ₹" + DEPOSIT);
                            saveBookingData(googleMapsLink);
                        } else {
                            loaderutil.hideLoader();
                            Toast.makeText(this,
                                    "Insufficient balance for offline payment.",
                                    Toast.LENGTH_SHORT).show();
                            btnRechargeWallet.setVisibility(View.VISIBLE);
                        }

                    } else { // Online
                        if (walletBalance >=DEPOSIT) {

                            tvDeposit.setText("Platform Charge Debited: ₹" + DEPOSIT);
                            startUpiPayment();
                        } else {
                            if (!platformChargeAdded) {
                                finalCost +=DEPOSIT;
                                platformChargeAdded = true;
                                tvDeposit   .setText(
                                        "Platform Charge Added: ₹" + DEPOSIT);
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

                        FREE_DISTANCE_KM = resp.optDouble("base_distance", 3.0);
                        PER_KM_CHARGE    = resp.optDouble("extra_cost_per_km", 7.0);
                        DEPOSIT          = resp.optDouble("platform_charge", 50.0);
                        APPOINTMENT_CHARGE = resp.optDouble("appointment_charge", 100.0);
                        GST_PERCENT      = resp.optDouble("gst_percent", 10.0);

                        Log.d(TAG, "UPI Config loaded: " +
                                "Distance base=" + FREE_DISTANCE_KM +
                                ", Extra/Km=" + PER_KM_CHARGE +
                                ", Deposit=" + DEPOSIT +
                                ", AppCharge=" + APPOINTMENT_CHARGE +
                                ", GST=" + GST_PERCENT);
                    } else {
                        Log.e(TAG, "Config fetch failed: " + resp.optString("message"));
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

    @SuppressLint("SetTextI18n")
    private void processUpiPaymentResponse(String response) {
        loaderutil.hideLoader();
        if (response == null || response.trim().isEmpty()) {
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        String status = "";
        for (String part : response.split("&")) {
            String[] kv = part.split("=");
            if (kv.length >= 2 && "status".equalsIgnoreCase(kv[0])) {
                status = kv[1].toLowerCase();
                break;
            }
        }

        if ("success".equals(status)) {
            // *1) Deduct deposit from wallet*
            if (walletBalance >= DEPOSIT) {
                deductWalletCharge(DEPOSIT,
                        "Platform charge for online appointment (wallet debit)");
            } else {
                // (unlikely if you enabled the button correctly, but just in case)
                Toast.makeText(this, "Insufficient wallet balance for deposit!", Toast.LENGTH_LONG).show();
                return;
            }

            // *2) Refresh UI so new wallet balance and deposit-line appear*
            tvDeposit.setText("Deposit Debited: ₹" + String.format(Locale.getDefault(),"%.0f", DEPOSIT));
            updatePaymentUI();

            // *3) Then save the booking & record payment history*
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            saveBookingData(googleMapsLink);

        } else {
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchWalletBalance() {
        String url = "http://sxm.a58.mytemp.website/get_wallet_balance.php";
        @SuppressLint("SetTextI18n") StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if ("success".equals(obj.getString("status"))) {
                            walletBalance = obj.getDouble("wallet_balance");
                            tvWalletBalance.setText(
                                    "₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));
                        }
                    } catch (JSONException e) { /*...*/ }
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

    @SuppressLint("SetTextI18n")
    private void parseRoutesResponse(JSONObject response) {
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject leg = routes
                        .getJSONObject(0)
                        .getJSONArray("legs")
                        .getJSONObject(0);

                long meters    = leg.getJSONObject("distance").getLong("value");
                distanceKm     = meters / 1000.0;

                // Free under threshold, else full-distance billing
                if (distanceKm <= FREE_DISTANCE_KM) {
                    tvDistanceChargeValue.setText("Free under " + FREE_DISTANCE_KM + " km");
                    distanceCharge = 0.0;
                } else {
                    distanceCharge = distanceKm * PER_KM_CHARGE;
                    tvDistanceChargeValue.setText(
                            String.format(Locale.getDefault(), "₹ %.0f", distanceCharge));
                }

                updatePaymentUI();

                // Recalculate total:
                finalCost = consultingFee + gstAmount + distanceCharge;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Routes parse error", e);
        }

        updatePaymentUI();
    }

    @SuppressLint("SetTextI18n")
    private void updatePaymentUI() {
        tvAppointmentCharge.setText("₹ " + String.format(Locale.getDefault(),"%.0f", APPOINTMENT_CHARGE));
        tvConsultingFeeValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", consultingFee));
        tvDistanceKmValue.setText(String.format(Locale.getDefault(),"%.1f km", distanceKm));

        // Only set the distance‐charge text if you actually have a charge
        if (distanceCharge > 0) {
            tvDistanceChargeValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", distanceCharge));
        }
        // otherwise leave whichever “Free under 3 km” or custom text you last set

        tvGstValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", gstAmount));
        tvTotalPaidValue.setText("₹ " + String.format(Locale.getDefault(),"%.0f", finalCost));

        // Always update wallet text here too
        tvWalletBalance.setText("₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));
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
                    p.put("patient_name",   patientName);

                    p.put("amount", String.format(Locale.getDefault(),"%.2f", finalCost));
                    p.put("consultation_fee", String.format(Locale.getDefault(),"%.2f", consultingFee));
                    p.put("deposit", String.format(Locale.getDefault(),"%.2f", DEPOSIT));

                    if (selectedPaymentMethod.equals("Online")) {
                        if (walletBalance >= DEPOSIT) {
                            p.put("deposit_status", "Wallet Debited");
                        } else {
                            p.put("deposit_status", "Added in Bill");
                        }
                    } else {
                        p.put("deposit_status", "Wallet Debited");
                    }

                    p.put("payment_method", selectedPaymentMethod);

                    p.put("distance", String.format(Locale.getDefault(),"%.2f", distanceKm));
                    p.put("distance_charge", String.format(Locale.getDefault(),"%.2f", distanceCharge));
                    p.put("gst", String.format(Locale.getDefault(),"%.2f", gstAmount));

                    // Total payment
                    p.put("total_payment", String.format(Locale.getDefault(),"%.2f",APPOINTMENT_CHARGE));

                    // Dummy values now for commission, earning (you can calculate if you want)
                    p.put("admin_commission", "0.00");
                    p.put("doctor_earning", "0.00");

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