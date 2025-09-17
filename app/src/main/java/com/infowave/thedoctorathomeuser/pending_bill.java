package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.phonepe.intent.sdk.api.PhonePeKt; // Kotlin top-level API for Checkout

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

    // Dynamic charges (from API)
    private static double APPOINTMENT_CHARGE;
    private static double DEPOSIT;
    private static double PER_KM_CHARGE;
    private static double GST_PERCENT;
    private static double FREE_DISTANCE_KM;

    // Loader gating
    private boolean cfgLoaded = false;
    private boolean chargeLoaded = false;
    private boolean distanceReady = false;

    // Calculated
    private double consultingFee;
    private double distanceKm = 0.0;
    private double distanceCharge = 0.0;
    private double gstAmount = 0.0;
    private double finalCost = 0.0; // what UI shows as “Total Pay”

    // Coords
    private double docLat = 0.0, docLng = 0.0, userLat = 0.0, userLng = 0.0;

    // UPI (legacy)
    private String merchantUpiId, merchantName, transactionNote, currency;

    // Booking data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod = "";
    private String patientId, pincode, googleMapsLink = "";

    // UI
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;
    private TextView tvAppointmentCharge, tvDeposit, tvConsultingFeeValue, tvDistanceKmValue, tvDistanceChargeValue, tvGstValue, tvTotalPaidValue, tvWalletBalance;

    // Row control to remove blank space
    private View depositRow;         // container of value
    private View depositLabelView;   // optional label view (if present in layout)

    // PhonePe (reuse the same backend endpoints you already have)
    private final String ppCreateOrderUrl = ApiConfig.endpoint("create_order.php");
    private final String ppStatusUrl      = ApiConfig.endpoint("check_status.php");

    // Track the PG order for this appointment payment
    private String ppMerchantOrderId;

    // Result launcher for PhonePe UI
    private ActivityResultLauncher<Intent> ppCheckoutLauncher;

    // Wallet
    private double walletBalance = 0.0;
    private final Handler walletHandler = new Handler();
    private Runnable walletRunnable;

    // Deposit decision snapshot at confirmation time
    private enum DepositMode { NONE, WALLET, BILL }
    private DepositMode lastConfirmedDepositMode = DepositMode.NONE;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        ppCheckoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (ppMerchantOrderId != null) {
                        // Verify final state with server; no wallet credit for appointment
                        checkPhonePeStatus(ppMerchantOrderId);
                    } else {
                        Log.w(TAG, "Returned from PhonePe but merchantOrderId is null");
                    }
                }
        );

        // Identify user
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Sorry, we could not identify your profile. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Loader while inputs load
        loaderutil.showLoader(this);

        // Intent extras
        Intent intent = getIntent();
        patientName = intent.getStringExtra("patient_name");
        age         = String.valueOf(intent.getIntExtra("age", 0));
        gender      = intent.getStringExtra("gender");
        problem     = intent.getStringExtra("problem");
        address     = intent.getStringExtra("address");
        doctorId    = intent.getStringExtra("doctor_id");
        if (doctorId == null || doctorId.isEmpty()) {
            Toast.makeText(this, "Sorry, we could not find the doctor information. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        doctorName  = intent.getStringExtra("doctorName");
        Status      = intent.getStringExtra("appointment_status");
        pincode     = intent.getStringExtra("pincode");

        if ("Request for visit".equals(Status))      Status = "Requested";
        else if ("Book Appointment".equals(Status))  Status = "Confirmed";

        // Location
        userLat = intent.getDoubleExtra("latitude", 0.0);
        userLng = intent.getDoubleExtra("longitude", 0.0);
        if (userLat != 0.0 && userLng != 0.0) {
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=" + userLat + "," + userLng;
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

        // Locate a real "row" container to hide without leaving a gap
        depositRow = tvDeposit != null ? (View) tvDeposit.getParent() : null;
        // try to find a label view (optional) so we can hide it too
        int labelId = getResources().getIdentifier("tv_deposit_label", "id", getPackageName());
        if (labelId != 0) {
            depositLabelView = findViewById(labelId);
        } else {
            depositLabelView = null;
        }
        hideDepositRow(); // start hidden

        // Initial UI
        btnRechargeWallet.setVisibility(View.GONE);
        if (patientName != null) tvBillPatientName.setText(patientName);
        if (doctorName  != null) tvBillDoctorName.setText(doctorName);

        String curDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(new Date());
        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvBillDate.setText(curDate);
        tvBillTime.setText(curTime);

        setButtonNeutralState();
        disablePayButton();

        // Start data flow
        fetchUpiConfig(doctorId);
        fetchDoctorLocation(doctorId);

        // Wallet auto-refresh
        fetchWalletBalance();
        walletRunnable = () -> {
            fetchWalletBalance();
            walletHandler.postDelayed(walletRunnable, 30000);
        };
        walletHandler.postDelayed(walletRunnable, 30000);

        // Selectors
        btnOfflinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Offline";
            stylePaymentButtons();
            recomputeTotalsAndUI();
        });

        btnOnlinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Online";
            stylePaymentButtons();
            recomputeTotalsAndUI();
        });

        // Recharge
        btnRechargeWallet.setOnClickListener(v ->
                startActivity(new Intent(pending_bill.this, payments.class))
        );

        // Pay
        payButton.setOnClickListener(v -> new AlertDialog.Builder(pending_bill.this)
                .setTitle("Confirm Appointment")
                .setMessage("Are you sure?\n\nBooking appointment charge will be ₹" + String.format(Locale.getDefault(), "%.0f", DEPOSIT) + " if you cancel.")
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    loaderutil.showLoader(pending_bill.this);

                    if (selectedPaymentMethod.isEmpty()) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Please choose a payment option to continue.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Snapshot of the deposit decision at the moment of confirmation
                    lastConfirmedDepositMode = (walletBalance >= DEPOSIT) ? DepositMode.WALLET : DepositMode.BILL;

                    if ("Offline".equals(selectedPaymentMethod)) {
                        // Unified logic for Offline
                        if (lastConfirmedDepositMode == DepositMode.WALLET) {
                            deductWalletCharge(DEPOSIT, "Platform charge for offline appointment booking");
                            setDepositLine("Platform Charge debited from wallet: ₹" + (int) DEPOSIT, true);
                        } else {
                            setDepositLine("Platform Charge added to bill: ₹" + (int) DEPOSIT, true);
                        }
                        // Direct booking without PG
                        saveBookingData(googleMapsLink);
                    } else {
                        // Online flow
                        if (lastConfirmedDepositMode == DepositMode.WALLET) {
                            setDepositLine("Wallet will be debited: ₹" + (int) DEPOSIT, true);
                        } else {
                            setDepositLine("Platform Charge added to bill: ₹" + (int) DEPOSIT, true);
                        }
                        startPhonePeCheckout();
                    }

                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show()
        );
    }

    /** Create PhonePe SDK order (server) and open Standard Checkout UI (client). */
    private void startPhonePeCheckout() {
        loaderutil.showLoader(this);

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ppCreateOrderUrl,
                resp -> {
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            loaderutil.hideLoader();
                            Toast.makeText(this, "Failed to create order.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "create_order failed: " + obj);
                            return;
                        }

                        ppMerchantOrderId = obj.optString("merchantOrderId", null);
                        String token   = obj.optString("token", "");
                        String orderId = obj.optString("orderId", "");

                        if (ppMerchantOrderId == null || token.isEmpty() || orderId.isEmpty()) {
                            loaderutil.hideLoader();
                            Toast.makeText(this, "Invalid order response.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Missing token/orderId/merchantOrderId: " + obj);
                            return;
                        }

                        try {
                            // ✅ No Intent returned; pass your launcher
                            PhonePeKt.startCheckoutPage(this, token, orderId, ppCheckoutLauncher);
                            Log.d(TAG, "Launched PhonePe checkout: orderId=" + orderId);
                        } catch (Throwable t) {
                            loaderutil.hideLoader();
                            Log.e(TAG, "startCheckoutPage failed", t);
                            Toast.makeText(this, "Unable to open PhonePe UI.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        Log.e(TAG, "create_order parse error", e);
                        Toast.makeText(this, "Order parse error.", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    loaderutil.hideLoader();
                    Log.e(TAG, "create_order network error", err);
                    Toast.makeText(this, "Network error creating order.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("amount", String.format(Locale.getDefault(), "%.2f", finalCost));
                p.put("purpose", "APPOINTMENT");
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    /** Verify on server; on COMPLETED → handle wallet (if needed) then save booking & payment history. */
    private void checkPhonePeStatus(String merchantOrderId) {
        // For appointment flow we DO NOT credit wallet; PHP will respect skipWallet soon
        String url = ppStatusUrl + "?merchantOrderId=" + merchantOrderId + "&skipWallet=1";

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            loaderutil.hideLoader();
                            Toast.makeText(this, "Status check failed.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "status failed: " + obj);
                            return;
                        }

                        String state = obj.optString("state", "PENDING");

                        if ("COMPLETED".equalsIgnoreCase(state)) {
                            // If the confirmed plan was wallet → debit now
                            if ("Online".equals(selectedPaymentMethod) && lastConfirmedDepositMode == DepositMode.WALLET) {
                                deductWalletCharge(DEPOSIT, "Platform charge for online appointment (wallet debit)");
                                setDepositLine("Platform Charge debited from wallet: ₹" + (int) DEPOSIT, true);
                            }
                            Toast.makeText(this, "Payment successful.", Toast.LENGTH_SHORT).show();
                            saveBookingData(googleMapsLink);
                        } else if ("FAILED".equalsIgnoreCase(state)) {
                            loaderutil.hideLoader();
                            Toast.makeText(this, "Payment failed.", Toast.LENGTH_SHORT).show();
                        } else {
                            loaderutil.hideLoader();
                            Toast.makeText(this, "Payment pending. Please check later.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        Log.e(TAG, "status parse error", e);
                        Toast.makeText(this, "Status parse error.", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    loaderutil.hideLoader();
                    Log.e(TAG, "status network error", err);
                    Toast.makeText(this, "Network error in status check.", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(req);
    }

    // ------------------------- DATA FETCHERS -------------------------

    private void fetchUpiConfig(String doctorId) {
        String url = ApiConfig.endpoint("get_upi_config.php");

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
                        GST_PERCENT      = resp.optDouble("gst_percent", 10.0);
                    } else {
                        Log.e(TAG, "Config fetch failed: " + resp.optString("message"));
                        // fail-safe defaults (will keep loader logic moving)
                        FREE_DISTANCE_KM = 3.0;
                        PER_KM_CHARGE    = 7.0;
                        DEPOSIT          = 50.0;
                        GST_PERCENT      = 10.0;
                    }
                    cfgLoaded = true;
                    fetchAppointmentCharge(doctorId);
                },
                err -> {
                    Log.e(TAG, "Network error fetching config", err);
                    // fail-safe defaults
                    FREE_DISTANCE_KM = 3.0;
                    PER_KM_CHARGE    = 7.0;
                    DEPOSIT          = 50.0;
                    GST_PERCENT      = 10.0;
                    cfgLoaded = true;
                    fetchAppointmentCharge(doctorId);
                }
        );

        Volley.newRequestQueue(this).add(req);
    }

    private void fetchAppointmentCharge(String doctorId) {
        String url = ApiConfig.endpoint("get_appointment_charge.php", "doctor_id", doctorId);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.optBoolean("success")) {
                        APPOINTMENT_CHARGE = response.optDouble("appointment_charge", 250.0);
                    } else {
                        APPOINTMENT_CHARGE = 250.0;
                    }
                    gstAmount     = APPOINTMENT_CHARGE * (GST_PERCENT / 100.0);
                    consultingFee = APPOINTMENT_CHARGE - DEPOSIT;  // business rule
                    chargeLoaded  = true;
                    recomputeTotalsAndUI();
                },
                error -> {
                    Log.e(TAG, "Volley error fetching doctor charge", error);
                    APPOINTMENT_CHARGE = 250.0;
                    gstAmount     = APPOINTMENT_CHARGE * (GST_PERCENT / 100.0);
                    consultingFee = APPOINTMENT_CHARGE - DEPOSIT;
                    chargeLoaded  = true;
                    recomputeTotalsAndUI();
                }
        );

        Volley.newRequestQueue(this).add(req);
    }

    private void fetchDoctorLocation(String docId) {
        String url = ApiConfig.endpoint("get_doctor_location.php", "doctor_id", docId);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    try {
                        if (resp.getBoolean("success")) {
                            parseDoctorLatLng(resp.getString("location"));
                        } else {
                            distanceReady = true;
                            recomputeTotalsAndUI();
                        }
                    } catch (JSONException e) {
                        distanceReady = true;
                        recomputeTotalsAndUI();
                    }
                },
                err -> {
                    distanceReady = true;
                    recomputeTotalsAndUI();
                }
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
                distanceReady = true;
                recomputeTotalsAndUI();
            }
        } catch (Exception e) {
            distanceReady = true;
            recomputeTotalsAndUI();
        }
    }

    private void fetchDrivingDistance(double lat1, double lng1, double lat2, double lng2) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + lat1 + "," + lng1
                + "&destination=" + lat2 + "," + lng2
                + "&key=" + getString(R.string.google_maps_key);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                this::parseRoutesResponse,
                err -> {
                    distanceReady = true;
                    recomputeTotalsAndUI();
                }
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void parseRoutesResponse(JSONObject response) {
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0);
                long meters = leg.getJSONObject("distance").getLong("value");
                distanceKm  = meters / 1000.0;

                if (distanceKm <= FREE_DISTANCE_KM) {
                    distanceCharge = 0.0;
                    tvDistanceChargeValue.setText("Free under " + String.format(Locale.getDefault(),"%.0f", FREE_DISTANCE_KM) + " km");
                } else {
                    distanceCharge = distanceKm * PER_KM_CHARGE;
                    tvDistanceChargeValue.setText(String.format(Locale.getDefault(), "₹ %.0f", distanceCharge));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Routes parse error", e);
            // keep defaults
        }

        distanceReady = true;
        recomputeTotalsAndUI();
    }

    // ------------------------- CORE MATH & UI -------------------------

    /**
     * Unified rule for both Online and Offline:
     * - If wallet ≥ DEPOSIT → do NOT add deposit to bill; deposit will be taken from wallet.
     * - If wallet < DEPOSIT → add deposit into bill.
     * Deposit row shows the plan for both modes.
     */
    @SuppressLint("SetTextI18n")
    private void recomputeTotalsAndUI() {
        boolean ready = cfgLoaded && chargeLoaded && distanceReady;

        // Base bill (without deposit)
        double baseTotal = consultingFee + gstAmount + distanceCharge;

        boolean depositCoveredByWallet = walletBalance >= DEPOSIT;
        boolean addDepositToBill = false;
        String depositLine = "";

        if ("Online".equals(selectedPaymentMethod) || "Offline".equals(selectedPaymentMethod)) {
            addDepositToBill = !depositCoveredByWallet; // add only when wallet insufficient
            if (depositCoveredByWallet) {
                depositLine = "Wallet will be debited: ₹" + (int) DEPOSIT;
            } else {
                depositLine = "Platform Charge added to bill: ₹" + (int) DEPOSIT;
            }
        } else {
            addDepositToBill = false;
            depositLine = "";
        }

        finalCost = baseTotal + (addDepositToBill ? DEPOSIT : 0);

        // --- UI atoms ---
        tvAppointmentCharge.setText("₹ " + (int) APPOINTMENT_CHARGE);
        tvConsultingFeeValue.setText("₹ " + (int) consultingFee);
        tvDistanceKmValue.setText(String.format(Locale.getDefault(),"%.1f km", distanceKm));
        tvGstValue.setText("₹ " + (int) gstAmount);
        tvTotalPaidValue.setText("₹ " + (int) Math.round(finalCost));
        tvWalletBalance.setText("₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));

        if (distanceCharge > 0) {
            tvDistanceChargeValue.setText("₹ " + (int) distanceCharge);
        } else {
            tvDistanceChargeValue.setText("Free under " + (int) FREE_DISTANCE_KM + " km");
        }

        // Deposit row visibility for both modes
        if (depositLine.isEmpty()) {
            hideDepositRow();
        } else {
            showDepositRow(depositLine);
        }

        // Loader + enable/disable logic
        if (!ready) {
            disablePayButton();
            loaderutil.showLoader(this);
        } else {
            loaderutil.hideLoader();

            if ("Offline".equals(selectedPaymentMethod)) {
                // Always allow proceed; rule is applied at booking
                btnRechargeWallet.setVisibility(View.GONE);
                enablePayButton();
            } else if ("Online".equals(selectedPaymentMethod)) {
                btnRechargeWallet.setVisibility(View.GONE);
                enablePayButton();
            } else {
                disablePayButton();
            }
        }

        Log.d(TAG, "recomputeTotals → mode=" + selectedPaymentMethod
                + " wallet=" + walletBalance
                + " final=" + finalCost
                + " addDepositToBill=" + addDepositToBill);
    }

    // Hide/show helpers for the deposit row (avoid blank space)
    private void hideDepositRow() {
        if (depositRow != null) depositRow.setVisibility(View.GONE);
        if (tvDeposit != null) tvDeposit.setText("");
    }

    private void showDepositRow(String text) {
        if (depositRow != null) depositRow.setVisibility(View.VISIBLE);
        if (tvDeposit != null) tvDeposit.setText(text);
    }

    private void setButtonNeutralState() {
        int gray = getResources().getColor(R.color.custom_gray);
        btnOnlinePayment.setBackgroundColor(gray);
        btnOfflinePayment.setBackgroundColor(gray);
        payButton.setBackgroundColor(gray);
        payButton.setAlpha(0.5f);
    }

    private void stylePaymentButtons() {
        int cyan = getResources().getColor(R.color.dark_cyan);
        int gray = getResources().getColor(R.color.custom_gray);
        if ("Online".equals(selectedPaymentMethod)) {
            btnOnlinePayment.setBackgroundColor(cyan);
            btnOfflinePayment.setBackgroundColor(gray);
        } else if ("Offline".equals(selectedPaymentMethod)) {
            btnOfflinePayment.setBackgroundColor(cyan);
            btnOnlinePayment.setBackgroundColor(gray);
        } else {
            setButtonNeutralState();
        }
    }

    // ------------------------- WALLET & BOOKING -------------------------

    private void fetchWalletBalance() {
        String url = ApiConfig.endpoint("get_wallet_balance.php");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if ("success".equals(obj.optString("status"))) {
                            walletBalance = obj.optDouble("wallet_balance", 0.0);
                            tvWalletBalance.setText("₹" + String.format(Locale.getDefault(), "%.2f", walletBalance));
                            recomputeTotalsAndUI();
                        }
                    } catch (JSONException ignored) { }
                },
                err -> Log.e(TAG, "Wallet fetch error", err)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void deductWalletCharge(double charge, String reason) {
        walletBalance -= charge;
        if (walletBalance < 0) walletBalance = 0;
        updateUserWallet(patientId, walletBalance);
        addWalletTransaction(Integer.parseInt(patientId), charge, "debit", reason);
        tvWalletBalance.setText("₹" + String.format(Locale.getDefault(), "%.2f", walletBalance));
    }

    private void saveBookingData(String googleMapsLink) {
        String url = ApiConfig.endpoint("save_appointment.php");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    try {
                        JSONObject r = new JSONObject(resp);
                        String appointmentId = r.optString("appointment_id", "0");
                        insertPaymentHistory(appointmentId);
                    } catch (JSONException e) {
                        loaderutil.hideLoader();
                    }
                    Toast.makeText(this, "Your appointment has been booked successfully!", Toast.LENGTH_SHORT).show();
                },
                err -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Could not book your appointment. Please check your connection and try again.", Toast.LENGTH_LONG).show();
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

                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                p.put("appointment_date", dateFormat.format(now));
                p.put("time_slot", timeFormat.format(now));

                p.put("pincode", pincode);
                p.put("appointment_mode", "Online");
                p.put("payment_method", selectedPaymentMethod);
                p.put("status", Status);
                p.put("location", googleMapsLink);
                p.put("final_cost", String.format(Locale.getDefault(), "%.2f", finalCost));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void updateUserWallet(String userId, double newBalance) {
        String url = ApiConfig.endpoint("update_wallet.php");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "Wallet updated: " + resp),
                err -> Log.e(TAG, "Wallet update error", err)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", userId);
                p.put("wallet_balance", String.format(Locale.getDefault(), "%.2f", newBalance));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void addWalletTransaction(int patientId, double amount, String type, String reason) {
        String url = ApiConfig.endpoint("add_wallet_transaction.php");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> Log.d(TAG, "Wallet txn added: " + resp),
                err -> Log.e(TAG, "Wallet txn error", err)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", String.valueOf(patientId));
                p.put("amount", String.format(Locale.getDefault(), "%.2f", amount));
                p.put("type", type);
                p.put("reason", reason);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void insertPaymentHistory(String appointmentId) {
        String statusEnum = "Pending";
        String url = ApiConfig.endpoint("payment_history.php");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Your payment details have been saved.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Payment inserted => " + resp);
                    onBookingSuccess();
                },
                err -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Could not save payment details. Please try again.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("appointment_id", appointmentId);
                p.put("doctor_id", doctorId);
                p.put("patient_name", patientName);

                p.put("amount", String.format(Locale.getDefault(), "%.2f", finalCost));
                p.put("consultation_fee", String.format(Locale.getDefault(), "%.2f", consultingFee));
                p.put("deposit", String.format(Locale.getDefault(), "%.2f", DEPOSIT));

                // Unified status for both modes based on confirmation-time decision
                if (lastConfirmedDepositMode == DepositMode.WALLET) {
                    p.put("deposit_status", "Wallet Debited");
                } else if (lastConfirmedDepositMode == DepositMode.BILL) {
                    p.put("deposit_status", "Added in Bill");
                } else {
                    p.put("deposit_status", "None");
                }

                p.put("payment_method", selectedPaymentMethod);
                p.put("distance", String.format(Locale.getDefault(), "%.2f", distanceKm));
                p.put("distance_charge", String.format(Locale.getDefault(), "%.2f", distanceCharge));
                p.put("gst", String.format(Locale.getDefault(), "%.2f", gstAmount));

                p.put("total_payment", String.format(Locale.getDefault(), "%.2f", APPOINTMENT_CHARGE));
                p.put("admin_commission", "0.00");
                p.put("doctor_earning", "0.00");

                p.put("payment_status", statusEnum);
                p.put("refund_status", "None");
                p.put("notes", "None");
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
        payButton.setBackgroundColor(getResources().getColor(R.color.navy_blue));
    }
    private void disablePayButton() {
        payButton.setEnabled(false);
        payButton.setAlpha(0.5f);
        payButton.setBackgroundColor(getResources().getColor(R.color.custom_gray));
    }

    // ------------------------- LEGACY UPI -------------------------

    @Override
    protected void onActivityResult(int reqCode, int resCode, @Nullable Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = (data != null) ? data.getStringExtra("response") : null;
            processUpiPaymentResponse(response);
        }
    }

    @SuppressLint("SetTextI18n")
    private void processUpiPaymentResponse(String response) {
        loaderutil.hideLoader();
        if (response == null || response.trim().isEmpty()) {
            Toast.makeText(this, "Your payment was cancelled or did not go through.", Toast.LENGTH_SHORT).show();
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
            if ("Online".equals(selectedPaymentMethod) && lastConfirmedDepositMode == DepositMode.WALLET) {
                deductWalletCharge(DEPOSIT, "Platform charge for online appointment (wallet debit)");
                setDepositLine("Platform Charge debited from wallet: ₹" + (int) DEPOSIT, true);
            }
            Toast.makeText(this, "Payment completed successfully!", Toast.LENGTH_SHORT).show();
            saveBookingData(googleMapsLink);
        } else {
            Toast.makeText(this, "Payment was not successful. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // Convenience for setting the deposit line and ensuring the row is visible/hidden correctly
    private void setDepositLine(String text, boolean show) {
        if (show) showDepositRow(text); else hideDepositRow();
    }
}
