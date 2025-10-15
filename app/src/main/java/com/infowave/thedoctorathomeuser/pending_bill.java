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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.phonepe.intent.sdk.api.PhonePeKt;

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
    private double finalCost = 0.0;      // raw total (double)
    private long   finalPayRupees = 0L;  // rounded ceil for UI

    // Coords
    private double docLat = 0.0, docLng = 0.0, userLat = 0.0, userLng = 0.0;

    // Booking data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status;
    private String selectedPaymentMethod = "Online"; // Default ONLINE selected
    private String patientId, pincode, googleMapsLink = "";

    // UI
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;
    private TextView tvAppointmentCharge, tvDeposit, tvConsultingFeeValue, tvDistanceKmValue, tvDistanceChargeValue, tvGstValue, tvTotalPaidValue, tvWalletBalance;

    // Deposit row container
    private View depositRow;
    private View depositLabelView; // optional label

    // PhonePe endpoints
    private final String ppCreateOrderUrl = ApiConfig.endpoint("create_order.php");
    private final String ppStatusUrl      = ApiConfig.endpoint("check_status.php");

    // Track PG order
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

        // PhonePe result launcher
        ppCheckoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (ppMerchantOrderId != null) {
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

        // Deposit row refs
        depositRow = tvDeposit != null ? (View) tvDeposit.getParent() : null;
        int labelId = getResources().getIdentifier("tv_deposit_label", "id", getPackageName());
        depositLabelView = (labelId != 0) ? findViewById(labelId) : null;
        hideDepositRow();

        // Header details
        if (patientName != null) tvBillPatientName.setText(patientName);
        if (doctorName  != null) tvBillDoctorName.setText(doctorName);
        String curDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(new Date());
        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvBillDate.setText(curDate);
        tvBillTime.setText(curTime);

        // Default selection = Online
        setButtonNeutralState();
        selectedPaymentMethod = "Online";
        stylePaymentButtons();
        disablePayButton();

        // Start data flow
        fetchAppConfig();
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
            if (walletBalance < DEPOSIT) {
                Toast.makeText(this,
                        "You need at least ₹" + (int) DEPOSIT + " in your wallet to book Offline.",
                        Toast.LENGTH_SHORT).show();

            }
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
                .setMessage("Are you sure?\n\nBooking appointment charge will be ₹" +
                        String.format(Locale.getDefault(), "%.0f", DEPOSIT) + " if you cancel.")
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    loaderutil.showLoader(pending_bill.this);

                    if (selectedPaymentMethod.isEmpty()) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Please choose a payment option to continue.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Safety: Offline must have deposit in wallet
                    if ("Offline".equals(selectedPaymentMethod) && walletBalance < DEPOSIT) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Wallet में ₹" + (int) DEPOSIT + " होने पर ही Offline booking होगी.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Snapshot deposit plan
                    lastConfirmedDepositMode = (walletBalance >= DEPOSIT) ? DepositMode.WALLET : DepositMode.BILL;

                    if ("Offline".equals(selectedPaymentMethod)) {
                        // Immediate booking; wallet debit if applicable
                        if (lastConfirmedDepositMode == DepositMode.WALLET) {
                            deductWalletCharge(DEPOSIT, "Platform charge for offline appointment booking");
                            setDepositLine("Platform Charge debited from wallet: ₹" + (int) DEPOSIT, true);
                        } else {
                            setDepositLine("Platform Charge added to bill: ₹" + (int) DEPOSIT, true);
                        }
                        saveBookingData(googleMapsLink);
                    } else {
                        // Online with PhonePe
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



    // ------------------------- PhonePe -------------------------

    private void startPhonePeCheckout() {
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

    private void checkPhonePeStatus(String merchantOrderId) {
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

    // ------------------------- CONFIG & DATA -------------------------

    /** Fetch pricing config (app_settings → gst_percent, base_distance, extra_cost_per_km, platform_charge) */
    private void fetchAppConfig() {
        String url = ApiConfig.endpoint("get_app_config.php"); // Your PHP that reads app_settings

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    if (!resp.optBoolean("success")) {
                        String err = resp.optString("error", "CONFIG_FAILED");
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Config error: " + err, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    FREE_DISTANCE_KM = resp.optDouble("base_distance");
                    PER_KM_CHARGE    = resp.optDouble("extra_cost_per_km");
                    DEPOSIT          = resp.optDouble("platform_charge");
                    GST_PERCENT      = resp.optDouble("gst_percent");

                    cfgLoaded = true;
                    fetchAppointmentCharge(doctorId);
                },
                err -> {
                    loaderutil.hideLoader();
                    Log.e(TAG, "Network error fetching config", err);
                    Toast.makeText(this, "Config network error", Toast.LENGTH_LONG).show();
                    finish();
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
        }

        distanceReady = true;
        recomputeTotalsAndUI();
    }

    // ------------------------- CORE MATH & UI -------------------------

    @SuppressLint("SetTextI18n")
    private void recomputeTotalsAndUI() {
        boolean ready = cfgLoaded && chargeLoaded && distanceReady;

        // Recompute pieces
        double distanceChargeRaw = (distanceKm <= FREE_DISTANCE_KM) ? 0.0 : (distanceKm * PER_KM_CHARGE);
        double baseTotal         = consultingFee + gstAmount + distanceChargeRaw;

        boolean depositCoveredByWallet = walletBalance >= DEPOSIT;
        boolean addDepositToBill = false;
        String depositLine = "";

        if ("Online".equals(selectedPaymentMethod) || "Offline".equals(selectedPaymentMethod)) {
            addDepositToBill = !depositCoveredByWallet;

            if (depositCoveredByWallet) {
                depositLine = "Wallet will be debited: ₹" + (int) DEPOSIT;
            } else {
                if ("Offline".equals(selectedPaymentMethod)) {
                    depositLine = "   Offline booking requires ₹" + (int) DEPOSIT + " in your wallet.";

                } else {
                    depositLine = "Platform Charge added to bill: ₹" + (int) DEPOSIT;
                }
            }
        }

        double finalCostRaw = baseTotal + (addDepositToBill ? DEPOSIT : 0.0);
        finalPayRupees = (long) Math.ceil(finalCostRaw);  // round UP to rupees
        finalCost = finalCostRaw;

        // Update UI labels
        tvAppointmentCharge.setText("₹ " + (int) APPOINTMENT_CHARGE);
        tvConsultingFeeValue.setText("₹ " + (int) consultingFee);
        tvDistanceKmValue.setText(String.format(Locale.getDefault(),"%.1f km", distanceKm));
        tvGstValue.setText("₹ " + (int) gstAmount);
        tvDistanceChargeValue.setText(
                distanceChargeRaw > 0 ? "₹ " + (int) Math.round(distanceChargeRaw)
                        : "Free under " + (int) FREE_DISTANCE_KM + " km"
        );
        tvTotalPaidValue.setText("₹ " + finalPayRupees);
        tvWalletBalance.setText("₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));

        if (depositLine.isEmpty()) hideDepositRow(); else showDepositRow(depositLine);

        // Recharge + Pay state
        if ("Offline".equals(selectedPaymentMethod) && walletBalance < DEPOSIT) {
            btnRechargeWallet.setVisibility(View.VISIBLE);
            disablePayButton();
        } else {
            btnRechargeWallet.setVisibility(View.GONE);
            if (!ready) {
                disablePayButton();
                loaderutil.showLoader(this);
            } else {
                loaderutil.hideLoader();
                enablePayButton();
            }
        }

        // Visual hint for offline when not eligible
        boolean canUseOffline = walletBalance >= DEPOSIT;
        btnOfflinePayment.setAlpha(canUseOffline ? 1f : 0.7f);

        Log.d(TAG, "recomputeTotals → mode=" + selectedPaymentMethod
                + " wallet=" + walletBalance
                + " finalRaw=" + finalCostRaw
                + " ceil=" + finalPayRupees
                + " addDepositToBill=" + addDepositToBill);
    }

    // Deposit row helpers
    private void hideDepositRow() {
        if (depositRow != null) depositRow.setVisibility(View.GONE);
        if (depositLabelView != null) depositLabelView.setVisibility(View.GONE); // hide the red label too
        if (tvDeposit != null) {
            tvDeposit.setText("");
            tvDeposit.setVisibility(View.GONE);
        }
    }

    private void showDepositRow(String text) {
        if (depositLabelView != null) depositLabelView.setVisibility(View.GONE); // always hide label for clean look
        if (depositRow != null) depositRow.setVisibility(View.VISIBLE);
        if (tvDeposit != null) {
            tvDeposit.setVisibility(View.VISIBLE);
            tvDeposit.setSingleLine(false);
            tvDeposit.setMaxLines(3);
            tvDeposit.setEllipsize(null);
            tvDeposit.setText(text); // e.g. "Offline booking requires ₹50 in your wallet."
        }
    }


    private void setDepositLine(String text, boolean show) {
        if (show) showDepositRow(text); else hideDepositRow();
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
}
