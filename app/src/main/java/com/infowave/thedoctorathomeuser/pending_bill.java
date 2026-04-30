package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.DefaultRetryPolicy;
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

    // Turn on/off logs from one place
    private static final boolean DBG = true;

    // Pricing/config
    private static double APPOINTMENT_CHARGE;
    private static double DEPOSIT;
    private static double PER_KM_CHARGE;
    private static double GST_PERCENT;
    private static double FREE_DISTANCE_KM;

    // State flags
    private boolean cfgLoaded = false;
    private boolean chargeLoaded = false;
    private boolean distanceReady = false;

    // Computed amounts
    private double consultingFee;
    private double distanceKm = 0.0;
    private double distanceCharge = 0.0;
    private double gstAmount = 0.0;
    private double finalCost = 0.0;          // rupees (double for math)
    private long   finalPayRupees = 0L;      // rupees (rounded up for display)

    // Locations
    private double docLat = 0.0, docLng = 0.0, userLat = 0.0, userLng = 0.0;

    // Appointment/person fields
    private String patientName, patientAge, patientGender, patientProblem, patientAddress, doctorId, doctorName, status;
    private String selectedPaymentMethod = "Online";
    private String patientId, pincode, googleMapsLink = "";

    // Vet case
    private int isVetCase = 0;
    private String animalName, animalGender, animalBreed, animalAge, vaccinationName;
    private String animalCategoryId = "";
    private String vaccinationId   = "";

    // NEW: vaccine price from previous activity
    private double vaccinationPrice = 0.0;

    // UI
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;
    private TextView tvAppointmentCharge, tvDeposit, tvConsultingFeeValue, tvDistanceKmValue, tvDistanceChargeValue, tvGstValue, tvTotalPaidValue, tvWalletBalance;
    private LinearLayout rowAnimalName, rowAnimalAge, rowAnimalGender, rowAnimalBreed, rowAnimalVaccination;
    private TextView tvAnimalName, tvAnimalAge, tvAnimalGender, tvAnimalBreed, tvAnimalVaccination;
    private View depositRow;
    private View depositLabelView;

    // NEW: vaccination price row + value
    private View rowVaccinationPrice;
    private TextView tvVaccinationPriceValue;

    // PhonePe
    private final String ppCreateOrderUrl = ApiConfig.endpoint("phonepe/public/create_order.php");
    private final String ppStatusUrl      = ApiConfig.endpoint("phonepe/public/check_status.php");

    // NEW: UPI verify URL (per two-block rule above)
    private final String upiVerifyUrl     = ApiConfig.endpoint("verify_upi.php");

    private String ppMerchantOrderId;
    private ActivityResultLauncher<Intent> ppCheckoutLauncher;

    // Wallet
    private double walletBalance = 0.0;
    private final Handler walletHandler = new Handler();
    private Runnable walletRunnable;
    private TextView tvPricePerKm;

    private enum DepositMode { NONE, WALLET, BILL }
    private DepositMode lastConfirmedDepositMode = DepositMode.NONE;

    // NEW: hold the verified UPI so we can store it in payment_history later
    private String enteredUpiId = "";

    // Lifecycle guard
    private boolean isDestroyedOrFinishing = false;

    /* -------------------- LOG HELPERS -------------------- */

    private void d(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void w(String msg) {
        if (DBG) Log.w(TAG, msg);
    }

    private void e(String msg, Throwable t) {
        if (DBG) Log.e(TAG, msg, t);
    }

    private static String fmt2(double v) {
        return String.format(Locale.getDefault(), "%.2f", v);
    }

    private static String fmt1(double v) {
        return String.format(Locale.getDefault(), "%.1f", v);
    }

    private static String safe(String s) {
        return (s == null) ? "null" : s;
    }

    // Masks UPI like "ku***@bank"
    private static String maskUpi(String vpa) {
        if (vpa == null) return "null";
        String s = vpa.trim();
        int at = s.indexOf('@');
        if (at <= 0) return "***";
        String left = s.substring(0, at);
        String right = s.substring(at);
        if (left.length() <= 2) return "**" + right;
        return left.substring(0, 2) + "***" + right;
    }

    // Do not print Google Directions API key in logs
    private void logDirectionsRequest(double oLat, double oLng, double dLat, double dLng) {
        d("Directions request (NO KEY LOGGED): origin=" + oLat + "," + oLng + " destination=" + dLat + "," + dLng);
    }

    // Centralized state dump for price/location/distance
    private void logBillingState(String where) {
        d("=== BILLING STATE @ " + where + " ===");
        d("Flags: cfgLoaded=" + cfgLoaded + ", chargeLoaded=" + chargeLoaded + ", distanceReady=" + distanceReady);
        d("Payment: selectedPaymentMethod=" + safe(selectedPaymentMethod) + ", lastConfirmedDepositMode=" + lastConfirmedDepositMode);
        d("User: patientId=" + safe(patientId) + ", doctorId=" + safe(doctorId) + ", pincode=" + safe(pincode));
        d("Vet: isVetCase=" + isVetCase
                + ", animalCategoryId=" + safe(animalCategoryId)
                + ", vaccinationId=" + safe(vaccinationId)
                + ", vaccinationName=" + safe(vaccinationName)
                + ", vaccinationPrice=" + fmt2(vaccinationPrice));
        d("Locations: userLatLng=" + userLat + "," + userLng
                + " docLatLng=" + docLat + "," + docLng
                + " mapLink=" + safe(googleMapsLink));
        d("Config: FREE_DISTANCE_KM=" + fmt2(FREE_DISTANCE_KM)
                + ", PER_KM_CHARGE=" + fmt2(PER_KM_CHARGE)
                + ", DEPOSIT=" + fmt2(DEPOSIT)
                + ", GST_PERCENT=" + fmt2(GST_PERCENT)
                + ", APPOINTMENT_CHARGE=" + fmt2(APPOINTMENT_CHARGE));
        d("Distance: distanceKm=" + fmt2(distanceKm) + ", distanceCharge=" + fmt2(distanceCharge));
        d("Amounts: consultingFee=" + fmt2(consultingFee)
                + ", gstAmount=" + fmt2(gstAmount)
                + ", finalCost=" + fmt2(finalCost)
                + ", finalPayRupees=" + finalPayRupees);
        d("Wallet: walletBalance=" + fmt2(walletBalance));
        d("=== END STATE ===");
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDestroyedOrFinishing = false;
        d("onResume()");
        fetchWalletBalance();
        recomputeTotalsAndUI();
        if (walletRunnable != null) {
            walletHandler.postDelayed(walletRunnable, 30000);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);
        setupSystemBarScrims();

        d("onCreate() started");

        // PhonePe launcher
        ppCheckoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    d("PhonePe activity result received. resultCode=" + result.getResultCode());
                    if (ppMerchantOrderId != null) {
                        d("Triggering PhonePe status check for merchantOrderId=" + ppMerchantOrderId);
                        checkPhonePeStatus(ppMerchantOrderId);
                    } else {
                        w("PhonePe result received but ppMerchantOrderId is null");
                    }
                }
        );

        // Logged-in user
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        d("Loaded patientId from prefs: " + safe(patientId));

        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Sorry, we could not identify your profile. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loader for initial async work
        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this);

        // Intent extras
        Intent intent = getIntent();
        patientName   = intent.getStringExtra("patient_name");
        patientAge    = String.valueOf(intent.getIntExtra("age", 0));
        patientGender = intent.getStringExtra("gender");
        patientProblem= intent.getStringExtra("problem");
        patientAddress= intent.getStringExtra("address");
        doctorId      = intent.getStringExtra("doctor_id");
        doctorName    = intent.getStringExtra("doctorName");
        status        = intent.getStringExtra("appointment_status");
        pincode       = intent.getStringExtra("pincode");

        // Vet extras
        animalName       = intent.getStringExtra("animal_name");
        animalGender     = intent.getStringExtra("animal_gender");
        animalBreed      = intent.getStringExtra("animal_breed");
        animalAge        = String.valueOf(intent.getIntExtra("animal_age", 0));
        vaccinationName  = intent.getStringExtra("vaccination_name");
        animalCategoryId = intent.getStringExtra("animal_category_id");
        vaccinationId    = intent.getStringExtra("vaccination_id");
        isVetCase        = intent.getIntExtra("is_vet_case", 0);

        // NEW: vaccination price
        vaccinationPrice = intent.getDoubleExtra("vaccination_price", 0.0);

        if (animalCategoryId == null) animalCategoryId = "";
        if (vaccinationId == null)    vaccinationId = "";

        d("Intent extras:"
                + " patientName=" + safe(patientName)
                + ", patientAge=" + safe(patientAge)
                + ", patientGender=" + safe(patientGender)
                + ", status(raw)=" + safe(status)
                + ", doctorId=" + safe(doctorId)
                + ", doctorName=" + safe(doctorName)
                + ", pincode=" + safe(pincode)
                + ", isVetCase=" + isVetCase
                + ", animalName=" + safe(animalName)
                + ", vaccinationName=" + safe(vaccinationName)
                + ", vaccinationPrice=" + fmt2(vaccinationPrice));

        if (doctorId == null || doctorId.isEmpty()) {
            loaderutil.hideLoader();
            Toast.makeText(this, "Sorry, we could not find the doctor information. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("Request for visit".equals(status))      status = "Requested";
        else if ("Book Appointment".equals(status))  status = "Confirmed";

        d("Normalized status=" + safe(status));

        // User location for map link
        userLat = intent.getDoubleExtra("latitude", 0.0);
        userLng = intent.getDoubleExtra("longitude", 0.0);
        if (userLat != 0.0 && userLng != 0.0) {
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=" + userLat + "," + userLng;
        }
        d("User location: userLatLng=" + userLat + "," + userLng + " mapLink=" + safe(googleMapsLink));

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
        tvPricePerKm          = findViewById(R.id.price_km);

        tvGstValue            = findViewById(R.id.tv_gst_value);
        tvTotalPaidValue      = findViewById(R.id.tv_total_paid_value);
        btnOnlinePayment      = findViewById(R.id.btn_online_payment);
        btnOfflinePayment     = findViewById(R.id.btn_offline_payment);
        btnRechargeWallet     = findViewById(R.id.btn_recharge_wallet);
        payButton             = findViewById(R.id.pay_button);

        rowAnimalName        = findViewById(R.id.row_animal_name);
        rowAnimalAge         = findViewById(R.id.row_animal_age);
        rowAnimalGender      = findViewById(R.id.row_animal_gender);
        rowAnimalBreed       = findViewById(R.id.row_animal_breed);
        rowAnimalVaccination = findViewById(R.id.row_animal_vaccination);
        tvAnimalName         = findViewById(R.id.tv_bill_animal_name);
        tvAnimalAge          = findViewById(R.id.tv_bill_animal_age);
        tvAnimalGender       = findViewById(R.id.tv_bill_animal_gender);
        tvAnimalBreed        = findViewById(R.id.tv_bill_animal_breed);
        tvAnimalVaccination  = findViewById(R.id.tv_bill_animal_vaccination);

        // NEW: vaccination price row + value
        rowVaccinationPrice     = findViewById(R.id.row_vaccination_price);
        tvVaccinationPriceValue = findViewById(R.id.tv_vaccination_price_value);

        depositRow = tvDeposit != null ? (View) tvDeposit.getParent() : null;
        @SuppressLint("DiscouragedApi") int labelId = getResources().getIdentifier("tv_deposit_label", "id", getPackageName());
        depositLabelView = (labelId != 0) ? findViewById(labelId) : null;
        hideDepositRow();

        // Header info
        setTextOrDash(tvBillPatientName, isVetCase == 1 ? animalName : patientName);
        setTextOrDash(tvBillDoctorName, doctorName);
        String curDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(new Date());
        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvBillDate.setText(curDate);
        tvBillTime.setText(curTime);

        d("Header set: date=" + curDate + " time=" + curTime);

        // Vet rows visibility
        if (isVetCase == 1) {
            setRowVisibility(rowAnimalName,        true); setTextOrDash(tvAnimalName, animalName);
            setRowVisibility(rowAnimalAge,         true); setTextOrDash(tvAnimalAge, animalAge);
            setRowVisibility(rowAnimalGender,      true); setTextOrDash(tvAnimalGender, animalGender);
            setRowVisibility(rowAnimalBreed,       true); setTextOrDash(tvAnimalBreed, animalBreed);
            setRowVisibility(rowAnimalVaccination, true); setTextOrDash(tvAnimalVaccination, vaccinationName);
        } else {
            setRowVisibility(rowAnimalName,        false);
            setRowVisibility(rowAnimalAge,         false);
            setRowVisibility(rowAnimalGender,      false);
            setRowVisibility(rowAnimalBreed,       false);
            setRowVisibility(rowAnimalVaccination, false);
        }

        // NEW: vaccination price line
        if (isVetCase == 1 && vaccinationPrice > 0.0) {
            setRowVisibility(rowVaccinationPrice, true);
            tvVaccinationPriceValue.setText("₹ " + (int) Math.round(vaccinationPrice));
        } else {
            setRowVisibility(rowVaccinationPrice, false);
        }

        // Buttons default
        setButtonNeutralState();
        selectedPaymentMethod = "Online";
        stylePaymentButtons();
        disablePayButton();

        d("Initial paymentMethod=" + selectedPaymentMethod);

        // Load config + doctor location
        fetchAppConfig();
        fetchDoctorLocation(doctorId);

        // Wallet polling (lifecycle-aware)
        walletRunnable = () -> {
            if (!isDestroyedOrFinishing && !isFinishing() && !isDestroyed()) {
                d("Wallet polling tick...");
                fetchWalletBalance();
                walletHandler.postDelayed(walletRunnable, 30000);
            }
        };
        walletHandler.postDelayed(walletRunnable, 30000);

        btnOfflinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Offline";
            d("Selected payment method -> Offline");
            stylePaymentButtons();
            if (walletBalance < DEPOSIT) {
                Toast.makeText(this, "Offline booking के लिए Wallet में कम से कम ₹" + (int) DEPOSIT + " चाहिए.", Toast.LENGTH_SHORT).show();
                w("Offline selected but walletBalance(" + fmt2(walletBalance) + ") < DEPOSIT(" + fmt2(DEPOSIT) + ")");
            }
            recomputeTotalsAndUI();
        });

        btnOnlinePayment.setOnClickListener(v -> {
            selectedPaymentMethod = "Online";
            d("Selected payment method -> Online");
            stylePaymentButtons();
            recomputeTotalsAndUI();
        });

        btnRechargeWallet.setOnClickListener(v -> startActivity(new Intent(pending_bill.this, payments.class)));

        // payButton click → show dialog with UPI input + verify before proceeding
        payButton.setOnClickListener(v -> {
            d("Pay button clicked. current totalPayRupees=" + finalPayRupees + ", finalCost=" + fmt2(finalCost));
            logBillingState("payButtonClicked");
            showConfirmWithUpiDialog();
        });

        logBillingState("onCreate_end");
    }

    @Override
    protected void onPause() {
        super.onPause();
        d("onPause()");
        if (walletHandler != null && walletRunnable != null) {
            walletHandler.removeCallbacks(walletRunnable);
        }
        loaderutil.hideLoader();
    }

    @Override
    protected void onStop() {
        super.onStop();
        d("onStop()");
        loaderutil.hideLoader();
    }

    @Override
    protected void onDestroy() {
        d("onDestroy()");
        isDestroyedOrFinishing = true;
        if (walletHandler != null && walletRunnable != null) {
            walletHandler.removeCallbacks(walletRunnable);
        }
        loaderutil.hideLoader();
        super.onDestroy();
    }

    private void setTextOrDash(TextView tv, String val) {
        if (tv == null) return;
        if (val == null || val.trim().isEmpty() || val.trim().equals("0"))
            tv.setText("-");
        else
            tv.setText(val);
    }

    private void setupSystemBarScrims() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        View statusScrim = findViewById(R.id.status_bar_scrim);
        View navScrim    = findViewById(R.id.navigation_bar_scrim);
        View root        = findViewById(R.id.root_container);

        if (root == null || statusScrim == null || navScrim == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars    = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.LayoutParams lpTop = statusScrim.getLayoutParams();
            if (lpTop.height != statusBars.top) {
                lpTop.height = statusBars.top;
                statusScrim.setLayoutParams(lpTop);
            }
            statusScrim.setBackgroundColor(Color.BLACK);
            statusScrim.setVisibility(statusBars.top > 0 ? View.VISIBLE : View.GONE);

            ViewGroup.LayoutParams lpBottom = navScrim.getLayoutParams();
            if (lpBottom.height != navBars.bottom) {
                lpBottom.height = navBars.bottom;
                navScrim.setLayoutParams(lpBottom);
            }
            navScrim.setBackgroundColor(Color.BLACK);
            navScrim.setVisibility(navBars.bottom > 0 ? View.VISIBLE : View.GONE);

            return WindowInsetsCompat.CONSUMED;
        });

        root.requestApplyInsets();
    }

    private void setRowVisibility(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /* ---------------- Confirm dialog with UPI ---------------- */

    private void showConfirmWithUpiDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_upi_capture, null, false);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) EditText etUpi = content.findViewById(R.id.et_upi);

        String msg = "Are you sure?\n\nBooking appointment charge will be ₹" +
                String.format(Locale.getDefault(), "%.0f", DEPOSIT) + " if you cancel.\n\n" +
                "Please enter your refund UPI ID (e.g., name@bank).";

        d("UPI dialog opened. Deposit(Platform Charge)=" + fmt2(DEPOSIT));

        new AlertDialog.Builder(pending_bill.this)
                .setTitle("Confirm Appointment")
                .setMessage(msg)
                .setView(content)
                .setCancelable(false)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    Editable ed = etUpi.getText();
                    String vpa = (ed == null) ? "" : ed.toString().trim();

                    d("UPI entered (masked)=" + maskUpi(vpa) + " length=" + vpa.length());

                    if (!isLikelyValidUpi(vpa)) {
                        w("UPI failed local validation (masked)=" + maskUpi(vpa));
                        Toast.makeText(this, "Please enter a valid UPI like name@bank", Toast.LENGTH_LONG).show();
                        return;
                    }

                    d("UPI passed local validation. Verifying with backend...");
                    verifyUpi(vpa, () -> continueAfterUpiVerified(vpa));
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    d("UPI dialog cancelled by user");
                    d.dismiss();
                })
                .show();
    }

    private boolean isLikelyValidUpi(String vpa) {
        if (TextUtils.isEmpty(vpa)) return false;
        if (vpa.length() < 6 || vpa.length() > 300) return false;
        if (vpa.contains(" ") || vpa.contains("\\") || vpa.contains("/")) return false;
        return vpa.matches("^[a-zA-Z0-9._\\-]{2,256}@[a-zA-Z]{3,64}$");
    }

    private void verifyUpi(String vpa, Runnable onSuccess) {
        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this);

        d("verifyUpi() POST -> " + upiVerifyUrl + " vpa(masked)=" + maskUpi(vpa));

        StringRequest req = new StringRequest(
                Request.Method.POST,
                upiVerifyUrl,
                resp -> {
                    d("verifyUpi() response=" + resp);
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean ok = o.optBoolean("valid", false);
                        String msg = o.optString("message", "");
                        d("verifyUpi() parsed: valid=" + ok + ", message=" + msg);

                        if (!ok) {
                            loaderutil.hideLoader();
                            Toast.makeText(this, (msg.isEmpty() ? "UPI not supported" : msg), Toast.LENGTH_LONG).show();
                        } else {
                            if (onSuccess != null) onSuccess.run();
                        }
                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        e("UPI verification parse error.", e);
                        Toast.makeText(this, "UPI verification parse error.", Toast.LENGTH_LONG).show();
                    }
                },
                err -> {
                    loaderutil.hideLoader();
                    e("verifyUpi() network error", err);
                    Toast.makeText(this, "Network error during UPI verification.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("vpa", vpa);
                p.put("_ts", String.valueOf(System.currentTimeMillis()));
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    private void continueAfterUpiVerified(String vpa) {
        enteredUpiId = vpa;
        d("UPI verified OK. enteredUpiId(masked)=" + maskUpi(enteredUpiId));

        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this);

        if (selectedPaymentMethod.isEmpty()) {
            loaderutil.hideLoader();
            Toast.makeText(this, "Please choose a payment option to continue.", Toast.LENGTH_SHORT).show();
            w("continueAfterUpiVerified() blocked: selectedPaymentMethod empty");
            return;
        }

        if ("Offline".equals(selectedPaymentMethod) && walletBalance < DEPOSIT) {
            loaderutil.hideLoader();
            Toast.makeText(this, "Wallet में ₹" + (int) DEPOSIT + " होने पर ही Offline booking होगी.", Toast.LENGTH_LONG).show();
            w("Offline blocked: walletBalance(" + fmt2(walletBalance) + ") < DEPOSIT(" + fmt2(DEPOSIT) + ")");
            return;
        }

        lastConfirmedDepositMode = (walletBalance >= DEPOSIT) ? DepositMode.WALLET : DepositMode.BILL;
        d("Deposit decision: walletBalance=" + fmt2(walletBalance) + ", DEPOSIT=" + fmt2(DEPOSIT)
                + " => lastConfirmedDepositMode=" + lastConfirmedDepositMode);

        if ("Offline".equals(selectedPaymentMethod)) {
            if (lastConfirmedDepositMode == DepositMode.WALLET) {
                d("Offline flow: debiting deposit from wallet now. amount=" + fmt2(DEPOSIT));
                deductWalletCharge(DEPOSIT, "Platform charge for offline appointment booking");
                setDepositLine("Platform Charge debited from wallet: ₹" + (int) DEPOSIT, true);
            } else {
                d("Offline flow: deposit will be added to bill (no wallet debit). amount=" + fmt2(DEPOSIT));
                setDepositLine("Platform Charge added to bill: ₹" + (int) DEPOSIT, true);
            }
            logBillingState("before_saveBookingData_offline");
            saveBookingData(googleMapsLink);
        } else {
            if (lastConfirmedDepositMode == DepositMode.WALLET) {
                d("Online flow: deposit will be debited from wallet after payment completion. amount=" + fmt2(DEPOSIT));
                setDepositLine("Wallet will be debited: ₹" + (int) DEPOSIT, true);
            } else {
                d("Online flow: deposit will be added to bill. amount=" + fmt2(DEPOSIT));
                setDepositLine("Platform Charge added to bill: ₹" + (int) DEPOSIT, true);
            }
            logBillingState("before_startPhonePeCheckout");
            startPhonePeCheckout();
        }
    }

    /* ---------------- PhonePe ---------------- */

    private void startPhonePeCheckout() {
        d("startPhonePeCheckout() -> " + ppCreateOrderUrl);
        d("Gateway amount alignment: finalPayRupees=" + finalPayRupees + " => paise=" + (finalPayRupees * 100L));
        logBillingState("startPhonePeCheckout");

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ppCreateOrderUrl,
                resp -> {
                    d("create_order response=" + resp);
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

                        d("create_order parsed: merchantOrderId=" + ppMerchantOrderId
                                + ", tokenLen=" + token.length()
                                + ", orderId=" + orderId);

                        if (ppMerchantOrderId == null || token.isEmpty() || orderId.isEmpty()) {
                            loaderutil.hideLoader();
                            Toast.makeText(this, "Invalid order response.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Missing token/orderId/merchantOrderId: " + obj);
                            return;
                        }

                        PhonePeKt.startCheckoutPage(this, token, orderId, ppCheckoutLauncher);

                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        e("create_order parse error", e);
                        Toast.makeText(this, "Order parse error.", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    loaderutil.hideLoader();
                    e("create_order network error", err);
                    Toast.makeText(this, "Network error creating order.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                long paise = finalPayRupees * 100L;
                p.put("amount", String.valueOf(paise));
                p.put("purpose", "APPOINTMENT");
                p.put("_ts", String.valueOf(System.currentTimeMillis()));
                d("create_order params: patient_id=" + safe(patientId) + ", amount(paise)=" + paise + ", purpose=APPOINTMENT");
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    private void checkPhonePeStatus(String merchantOrderId) {
        final Handler h = new Handler(getMainLooper());
        final int[] attempts = {0};
        final int maxAttempts = 5;

        d("checkPhonePeStatus() start merchantOrderId=" + merchantOrderId + " maxAttempts=" + maxAttempts);

        Runnable check = new Runnable() {
            @Override public void run() {
                if (isFinishing() || isDestroyedOrFinishing || isDestroyed()) {
                    loaderutil.hideLoader();
                    w("checkPhonePeStatus() aborted due to lifecycle");
                    return;
                }

                attempts[0]++;
                String url = ppStatusUrl + "?merchantOrderId=" + Uri.encode(merchantOrderId) + "&skipWallet=1&ts=" + System.currentTimeMillis();
                d("checkPhonePeStatus() attempt " + attempts[0] + "/" + maxAttempts + " GET -> " + url);

                StringRequest req = new StringRequest(Request.Method.GET, url,
                        resp -> {
                            d("check_status response=" + resp);
                            try {
                                JSONObject obj = new JSONObject(resp);

                                String topStatus = obj.optString("status");
                                if (!"success".equalsIgnoreCase(topStatus) && !"ok".equalsIgnoreCase(topStatus)) {
                                    w("check_status top status not success/ok: " + topStatus);
                                    if (attempts[0] < maxAttempts) {
                                        h.postDelayed(this, 2000);
                                        return;
                                    }
                                    loaderutil.hideLoader();
                                    Toast.makeText(pending_bill.this, "Could not verify payment right now. Please try again.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                String state = obj.optString("state", "PENDING");
                                d("check_status parsed state=" + state + ", selectedPaymentMethod=" + selectedPaymentMethod);

                                if ("COMPLETED".equalsIgnoreCase(state)) {
                                    if ("Online".equals(selectedPaymentMethod) && lastConfirmedDepositMode == DepositMode.WALLET) {
                                        d("PhonePe COMPLETED: debiting deposit from wallet. amount=" + fmt2(DEPOSIT));
                                        deductWalletCharge(DEPOSIT, "Platform charge for online appointment (wallet debit)");
                                        setDepositLine("Platform Charge debited from wallet: ₹" + (int) DEPOSIT, true);
                                        recomputeTotalsAndUI();
                                    }

                                    fetchWalletBalance();

                                    Toast.makeText(pending_bill.this, "Payment successful.", Toast.LENGTH_SHORT).show();
                                    d("PhonePe COMPLETED, merchantOrderId=" + ppMerchantOrderId);
                                    loaderutil.hideLoader();

                                    logBillingState("before_saveBookingData_online_completed");
                                    saveBookingData(googleMapsLink);

                                } else if ("FAILED".equalsIgnoreCase(state)) {
                                    loaderutil.hideLoader();
                                    Toast.makeText(pending_bill.this, "Payment failed.", Toast.LENGTH_SHORT).show();
                                    w("PhonePe FAILED for merchantOrderId=" + merchantOrderId);

                                } else {
                                    if (attempts[0] < maxAttempts) {
                                        h.postDelayed(this, 2000);
                                    } else {
                                        loaderutil.hideLoader();
                                        Toast.makeText(pending_bill.this, "Payment pending. You can check again from history.", Toast.LENGTH_SHORT).show();
                                        w("PhonePe state still " + state + " after max attempts");
                                    }
                                }
                            } catch (Exception e) {
                                e("Status parse error", e);
                                if (attempts[0] < maxAttempts) {
                                    h.postDelayed(this, 2000);
                                } else {
                                    loaderutil.hideLoader();
                                    Toast.makeText(pending_bill.this, "Status parse error.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        },
                        err -> {
                            e("Server error while checking status.", err);
                            if (attempts[0] < maxAttempts) {
                                h.postDelayed(this, 2000);
                            } else {
                                loaderutil.hideLoader();
                                Toast.makeText(pending_bill.this, "Server error while checking status.", Toast.LENGTH_SHORT).show();
                            }
                        });
                req.setShouldCache(false);
                req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.5f));
                Volley.newRequestQueue(pending_bill.this).add(req);
            }
        };

        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this);
        check.run();
    }

    /* ---------------- Config & distance ---------------- */

    private void fetchAppConfig() {
        String url = ApiConfig.endpoint("get_app_config.php") + "?ts=" + System.currentTimeMillis();
        d("fetchAppConfig() GET -> " + url);

        @SuppressLint("DefaultLocale") JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    d("fetchAppConfig() response=" + resp);
                    if (!resp.optBoolean("success")) {
                        String err = resp.optString("error", "CONFIG_FAILED");
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Config error: " + err, Toast.LENGTH_LONG).show();
                        w("Config error: " + err);
                        finish();
                        return;
                    }

                    FREE_DISTANCE_KM = resp.optDouble("base_distance");
                    PER_KM_CHARGE    = resp.optDouble("extra_cost_per_km");
                    DEPOSIT          = resp.optDouble("platform_charge");
                    GST_PERCENT      = resp.optDouble("gst_percent");
                    cfgLoaded = true;

                    d("Config loaded: base_distance=" + fmt2(FREE_DISTANCE_KM)
                            + ", extra_cost_per_km=" + fmt2(PER_KM_CHARGE)
                            + ", platform_charge=" + fmt2(DEPOSIT)
                            + ", gst_percent=" + fmt2(GST_PERCENT));

                    tvPricePerKm.setText(String.format("₹ %.2f per kilometer", PER_KM_CHARGE));
                    fetchAppointmentCharge(doctorId);
                    logBillingState("after_fetchAppConfig");
                },
                err -> {
                    loaderutil.hideLoader();
                    e("Network error fetching config", err);
                    Toast.makeText(this, "Config network error", Toast.LENGTH_LONG).show();
                    finish();
                }
        );
        req.setShouldCache(false);
        Volley.newRequestQueue(this).add(req);
    }

    private void fetchAppointmentCharge(String doctorId) {
        String url = ApiConfig.endpoint("get_appointment_charge.php", "doctor_id", doctorId) + "&ts=" + System.currentTimeMillis();
        d("fetchAppointmentCharge() GET -> " + url);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    d("fetchAppointmentCharge() response=" + response);

                    if (response.optBoolean("success")) {
                        APPOINTMENT_CHARGE = response.optDouble("appointment_charge", 250.0);
                    } else {
                        APPOINTMENT_CHARGE = 250.0;
                    }

                    gstAmount     = APPOINTMENT_CHARGE * (GST_PERCENT / 100.0);
                    consultingFee = APPOINTMENT_CHARGE - DEPOSIT;
                    chargeLoaded  = true;

                    d("Charge loaded: APPOINTMENT_CHARGE=" + fmt2(APPOINTMENT_CHARGE)
                            + ", GST_PERCENT=" + fmt2(GST_PERCENT) + " => gstAmount=" + fmt2(gstAmount)
                            + ", DEPOSIT=" + fmt2(DEPOSIT) + " => consultingFee=" + fmt2(consultingFee));

                    recomputeTotalsAndUI();
                    logBillingState("after_fetchAppointmentCharge");
                },
                error -> {
                    e("Volley error fetching doctor charge", error);

                    APPOINTMENT_CHARGE = 250.0;
                    gstAmount     = APPOINTMENT_CHARGE * (GST_PERCENT / 100.0);
                    consultingFee = APPOINTMENT_CHARGE - DEPOSIT;
                    chargeLoaded  = true;

                    d("Charge fallback: APPOINTMENT_CHARGE=" + fmt2(APPOINTMENT_CHARGE)
                            + ", gstAmount=" + fmt2(gstAmount)
                            + ", consultingFee=" + fmt2(consultingFee));

                    recomputeTotalsAndUI();
                    logBillingState("after_fetchAppointmentCharge_errorFallback");
                }
        );
        req.setShouldCache(false);
        Volley.newRequestQueue(this).add(req);
    }

    private void fetchDoctorLocation(String docId) {
        String url = ApiConfig.endpoint("get_doctor_location.php", "doctor_id", docId) + "&ts=" + System.currentTimeMillis();
        d("fetchDoctorLocation() GET -> " + url);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    d("fetchDoctorLocation() response=" + resp);
                    try {
                        if (resp.getBoolean("success")) {
                            String loc = resp.getString("location");
                            d("Doctor location string=" + loc);
                            parseDoctorLatLng(loc);
                        } else {
                            w("fetchDoctorLocation(): success=false. Distance will be marked ready with 0km.");
                            distanceReady = true;
                            recomputeTotalsAndUI();
                        }
                    } catch (JSONException e) {
                        e("fetchDoctorLocation() JSON parse error", e);
                        distanceReady = true;
                        recomputeTotalsAndUI();
                    }
                },
                err -> {
                    e("fetchDoctorLocation() network error", err);
                    distanceReady = true;
                    recomputeTotalsAndUI();
                }
        );
        req.setShouldCache(false);
        Volley.newRequestQueue(this).add(req);
    }

    private void parseDoctorLatLng(String docUrl) {
        try {
            Uri uri = Uri.parse(docUrl);
            String q = uri.getQueryParameter("query");
            d("parseDoctorLatLng(): parsed query=" + q);

            if (q != null && q.contains(",")) {
                String[] p = q.split(",");
                docLat = Double.parseDouble(p[0]);
                docLng = Double.parseDouble(p[1]);
                d("Doctor lat/lng parsed: " + docLat + "," + docLng);

                if (userLat != 0.0 && userLng != 0.0 && docLat != 0.0 && docLng != 0.0) {
                    fetchDrivingDistance(userLat, userLng, docLat, docLng);
                } else {
                    w("Cannot fetch distance: missing coords. userLatLng=" + userLat + "," + userLng + " docLatLng=" + docLat + "," + docLng);
                    distanceReady = true;
                    recomputeTotalsAndUI();
                }
            } else {
                w("parseDoctorLatLng(): query missing/invalid in docUrl. Distance will be marked ready with 0km.");
                distanceReady = true;
                recomputeTotalsAndUI();
            }
        } catch (Exception e) {
            e("parseDoctorLatLng() error", e);
            distanceReady = true;
            recomputeTotalsAndUI();
        }
    }

    private void fetchDrivingDistance(double lat1, double lng1, double lat2, double lng2) {
        logDirectionsRequest(lat1, lng1, lat2, lng2);

        // PRIMARY: Backend route endpoint — API key stays on server.
        String url = ApiConfig.endpoint(
                "get_route_distance.php",
                "origin_lat", String.valueOf(lat1),
                "origin_lng",  String.valueOf(lng1),
                "destination_lat", String.valueOf(lat2),
                "destination_lng", String.valueOf(lng2),
                "mode", "driving"
        ) + "&ts=" + System.currentTimeMillis();

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        parseRoutesResponse(response);
                    } else {
                        w("Backend route API returned success=false: " + response.optString("message", ""));
                        // FALLBACK: Use Android string key when backend returns failure
                        fetchDrivingDistanceFallback(lat1, lng1, lat2, lng2);
                    }
                },
                err -> {
                    e("Backend route distance network error — trying Android key fallback", err);
                    // FALLBACK: Only reached if backend endpoint is unreachable
                    fetchDrivingDistanceFallback(lat1, lng1, lat2, lng2);
                }
        );
        req.setShouldCache(false);
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(8000, 1, 1.0f));
        Volley.newRequestQueue(this).add(req);
    }

    /**
     * FALLBACK ONLY — called only when backend get_route_distance.php is unreachable or fails.
     * Uses the Android string resource key (google_maps_key) directly.
     * Primary method is fetchDrivingDistance() which uses the backend.
     */
    private void fetchDrivingDistanceFallback(double lat1, double lng1, double lat2, double lng2) {
        w("Using Android key fallback for Directions API (backend unavailable)");
        try {
            String androidKey = getString(R.string.google_maps_key);
            if (androidKey == null || androidKey.isEmpty() || androidKey.startsWith("PASTE_")) {
                w("Android fallback key not set. Skipping distance calculation.");
                distanceReady = true;
                recomputeTotalsAndUI();
                return;
            }
            String fallbackUrl = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + lat1 + "," + lng1
                    + "&destination=" + lat2 + "," + lng2
                    + "&mode=driving"
                    + "&key=" + androidKey;
            // Note: we do NOT log the URL here (would expose key in logcat)
            d("Directions fallback request: origin=" + lat1 + "," + lng1
                    + " destination=" + lat2 + "," + lng2);

            JsonObjectRequest fallbackReq = new JsonObjectRequest(Request.Method.GET, fallbackUrl, null,
                    fallbackResp -> {
                        try {
                            String status = fallbackResp.optString("status", "");
                            if ("OK".equals(status)) {
                                JSONObject leg = fallbackResp
                                        .getJSONArray("routes").getJSONObject(0)
                                        .getJSONArray("legs").getJSONObject(0);
                                long meters = leg.getJSONObject("distance").getLong("value");
                                distanceKm = meters / 1000.0;
                                d("Fallback distance: meters=" + meters + " km=" + fmt2(distanceKm));

                                if (distanceKm <= FREE_DISTANCE_KM) {
                                    distanceCharge = 0.0;
                                    tvDistanceChargeValue.setText("Free under "
                                            + String.format(Locale.getDefault(), "%.0f", FREE_DISTANCE_KM) + " km");
                                } else {
                                    distanceCharge = distanceKm * PER_KM_CHARGE;
                                    tvDistanceChargeValue.setText(String.format(Locale.getDefault(),
                                            "₹ %.0f", distanceCharge));
                                }
                            } else {
                                w("Fallback Directions API status=" + status + ". Proceeding with 0km.");
                            }
                        } catch (Exception ex) {
                            e("Fallback parse error", ex);
                        }
                        distanceReady = true;
                        recomputeTotalsAndUI();
                    },
                    fallbackErr -> {
                        e("Fallback Directions API also failed", fallbackErr);
                        distanceReady = true;
                        recomputeTotalsAndUI();
                    }
            );
            fallbackReq.setShouldCache(false);
            Volley.newRequestQueue(this).add(fallbackReq);
        } catch (Exception ex) {
            e("fetchDrivingDistanceFallback exception", ex);
            distanceReady = true;
            recomputeTotalsAndUI();
        }
    }

    @SuppressLint("SetTextI18n")
    private void parseRoutesResponse(JSONObject response) {
        d("parseRoutesResponse() raw=" + response);
        try {
            boolean success = response.optBoolean("success", false);
            if (success) {
                long meters = response.optLong("distance_meters", 0L);
                distanceKm  = meters / 1000.0;

                d("Distance parsed from backend: meters=" + meters + " => distanceKm=" + fmt2(distanceKm)
                        + ", FREE_DISTANCE_KM=" + fmt2(FREE_DISTANCE_KM)
                        + ", PER_KM_CHARGE=" + fmt2(PER_KM_CHARGE));

                if (distanceKm <= FREE_DISTANCE_KM) {
                    distanceCharge = 0.0;
                    tvDistanceChargeValue.setText("Free under " + String.format(Locale.getDefault(),"%.0f", FREE_DISTANCE_KM) + " km");
                    d("Distance charge: FREE (<= base distance)");
                } else {
                    distanceCharge = distanceKm * PER_KM_CHARGE;
                    tvDistanceChargeValue.setText(String.format(Locale.getDefault(), "₹ %.0f", distanceCharge));
                    d("Distance charge computed: distanceKm(" + fmt2(distanceKm) + ") * perKm(" + fmt2(PER_KM_CHARGE) + ") = " + fmt2(distanceCharge));
                }
            } else {
                w("Route backend failed: " + response.optString("message", "No route found")
                        + " raw_status=" + response.optString("raw_status", ""));
            }
        } catch (Exception e) {
            e("Routes parse error", e);
        }
        distanceReady = true;
        recomputeTotalsAndUI();
        logBillingState("after_parseRoutesResponse");
    }
    /* ---------------- UI recompute ---------------- */

    @SuppressLint("SetTextI18n")
    private void recomputeTotalsAndUI() {
        boolean ready = cfgLoaded && chargeLoaded && distanceReady;

        double distanceChargeRaw = (distanceKm <= FREE_DISTANCE_KM) ? 0.0 : (distanceKm * PER_KM_CHARGE);
        distanceCharge = distanceChargeRaw;

        // Base total (consulting + GST + distance)
        double baseTotal = consultingFee + gstAmount + distanceChargeRaw;

        // Add vaccination price if any
        boolean addVaccine = (isVetCase == 1 && vaccinationPrice > 0.0);
        if (addVaccine) {
            baseTotal += vaccinationPrice;
            setRowVisibility(rowVaccinationPrice, true);
            if (tvVaccinationPriceValue != null) {
                tvVaccinationPriceValue.setText("₹ " + (int) Math.round(vaccinationPrice));
            }
        } else {
            setRowVisibility(rowVaccinationPrice, false);
        }

        boolean depositCoveredByWallet = walletBalance >= DEPOSIT;
        boolean addDepositToBill = !depositCoveredByWallet;
        String depositLine;

        if ("Offline".equals(selectedPaymentMethod) && walletBalance < DEPOSIT) {
            depositLine = "Offline booking के लिए wallet में ₹" + (int) DEPOSIT + " होना ज़रूरी है.";
        } else if (depositCoveredByWallet) {
            depositLine = "Wallet will be debited: ₹" + (int) DEPOSIT;
        } else {
            depositLine = "Platform Charge added to bill: ₹" + (int) DEPOSIT;
        }

        double finalCostRaw = baseTotal + (addDepositToBill ? DEPOSIT : 0.0);
        finalPayRupees = (long) Math.ceil(finalCostRaw);
        finalCost = finalCostRaw;

        // FULL PRICE BREAKDOWN LOG
        d("PRICE CALC:"
                + " consultingFee=" + fmt2(consultingFee)
                + " + gstAmount=" + fmt2(gstAmount)
                + " + distanceChargeRaw=" + fmt2(distanceChargeRaw)
                + (addVaccine ? (" + vaccinationPrice=" + fmt2(vaccinationPrice)) : "")
                + " => baseTotal=" + fmt2(baseTotal)
                + " | depositCoveredByWallet=" + depositCoveredByWallet
                + " | addDepositToBill=" + addDepositToBill + " (deposit=" + fmt2(DEPOSIT) + ")"
                + " => finalCostRaw=" + fmt2(finalCostRaw)
                + " => finalPayRupees(ceil)=" + finalPayRupees);

        tvAppointmentCharge.setText("₹ " + (int) APPOINTMENT_CHARGE);
        tvConsultingFeeValue.setText("₹ " + (int) consultingFee);
        tvDistanceKmValue.setText(String.format(Locale.getDefault(),"%.1f km", distanceKm));
        tvGstValue.setText("₹ " + (int) gstAmount);
        tvDistanceChargeValue.setText(distanceChargeRaw > 0
                ? "₹ " + (int) Math.round(distanceChargeRaw)
                : "Free under " + (int) FREE_DISTANCE_KM + " km");
        tvTotalPaidValue.setText("₹ " + finalPayRupees);
        tvWalletBalance.setText("₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));

        if (depositLine.isEmpty()) hideDepositRow(); else showDepositRow(depositLine);

        if ("Offline".equals(selectedPaymentMethod) && walletBalance < DEPOSIT) {
            btnRechargeWallet.setVisibility(View.VISIBLE);
            disablePayButton();
        } else {
            btnRechargeWallet.setVisibility(View.GONE);
            if (!ready) {
                disablePayButton();
                if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this);
            } else {
                loaderutil.hideLoader();
                enablePayButton();
            }
        }

        boolean canUseOffline = walletBalance >= DEPOSIT;
        btnOfflinePayment.setAlpha(canUseOffline ? 1f : 0.7f);

        // Always log after recompute
        logBillingState("recomputeTotalsAndUI");
    }

    private void hideDepositRow() {
        if (depositRow != null) depositRow.setVisibility(View.GONE);
        if (depositLabelView != null) depositLabelView.setVisibility(View.GONE);
        if (tvDeposit != null) {
            tvDeposit.setText("");
            tvDeposit.setVisibility(View.GONE);
        }
    }

    private void showDepositRow(String text) {
        if (depositLabelView != null) depositLabelView.setVisibility(View.GONE);
        if (depositRow != null) depositRow.setVisibility(View.VISIBLE);
        if (tvDeposit != null) {
            tvDeposit.setVisibility(View.VISIBLE);
            tvDeposit.setSingleLine(false);
            tvDeposit.setMaxLines(3);
            tvDeposit.setEllipsize(null);
            tvDeposit.setText(text);
        }
        d("Deposit line UI: " + text);
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

    /* ---------------- Wallet ---------------- */

    private void fetchWalletBalance() {
        String url = ApiConfig.endpoint("get_wallet_balance.php");
        d("fetchWalletBalance() POST -> " + url + " patientId=" + safe(patientId));

        @SuppressLint("SetTextI18n") StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    d("fetchWalletBalance() response=" + resp);
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if ("success".equals(obj.optString("status"))) {
                            double old = walletBalance;
                            walletBalance = obj.optDouble("wallet_balance", 0.0);
                            d("Wallet balance updated: " + fmt2(old) + " -> " + fmt2(walletBalance));
                            tvWalletBalance.setText("₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));
                            recomputeTotalsAndUI();
                        } else {
                            w("fetchWalletBalance(): status=" + obj.optString("status"));
                        }
                    } catch (JSONException e) {
                        e("fetchWalletBalance() parse error", e);
                    }
                },
                err -> e("Wallet fetch error", err)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("_ts", String.valueOf(System.currentTimeMillis()));
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    @SuppressLint("SetTextI18n")
    private void deductWalletCharge(double charge, String reason) {
        d("deductWalletCharge(): charge=" + fmt2(charge) + " reason=" + reason + " walletBefore=" + fmt2(walletBalance));

        walletBalance -= charge;
        if (walletBalance < 0) walletBalance = 0;

        d("walletAfter=" + fmt2(walletBalance));

        updateUserWallet(patientId, walletBalance);
        addWalletTransaction(Integer.parseInt(patientId), charge, "debit", reason);
        tvWalletBalance.setText("₹" + String.format(Locale.getDefault(),"%.2f", walletBalance));
        recomputeTotalsAndUI();
    }

    private void updateUserWallet(String userId, double newBalance) {
        String url = ApiConfig.endpoint("update_wallet.php");
        d("updateUserWallet() POST -> " + url + " user_id=" + safe(userId) + " newBalance=" + fmt2(newBalance));

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> d("Wallet updated response: " + resp),
                err -> e("Wallet update error", err)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", userId);
                p.put("wallet_balance", String.format(Locale.getDefault(), "%.2f", newBalance));
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    private void addWalletTransaction(int patientId, double amount, String type, String reason) {
        String url = ApiConfig.endpoint("add_wallet_transaction.php");
        d("addWalletTransaction() POST -> " + url
                + " patientId=" + patientId
                + " amount=" + fmt2(amount)
                + " type=" + type
                + " reason=" + reason);

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> d("Wallet txn added response: " + resp),
                err -> e("Wallet txn error", err)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", String.valueOf(patientId));
                // keeping your existing behavior unchanged:
                p.put("amount", String.valueOf(finalPayRupees));
                p.put("type", type);
                p.put("reason", reason);
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    /* ---------------- Save booking ---------------- */

    private void saveBookingData(String googleMapsLink) {
        String url = ApiConfig.endpoint("save_appointment.php");
        d("saveBookingData() POST -> " + url);
        d("saveBookingData() location=" + safe(googleMapsLink) + " userLatLng=" + userLat + "," + userLng);
        logBillingState("saveBookingData_start");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    d("saveBookingData() response=" + resp);
                    try {
                        JSONObject r = new JSONObject(resp);
                        String appointmentId = r.optString("appointment_id", "0");
                        d("Appointment saved. appointmentId=" + appointmentId + " -> inserting payment history...");
                        insertPaymentHistory(appointmentId);
                    } catch (JSONException e) {
                        loaderutil.hideLoader();
                        e("saveBookingData() parse error", e);
                    }
                    Toast.makeText(this, "Your appointment has been booked successfully!", Toast.LENGTH_SHORT).show();
                },
                err -> {
                    loaderutil.hideLoader();
                    e("saveBookingData() network error", err);
                    Toast.makeText(this, "Could not book your appointment. Please check your connection and try again.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("patient_name", patientName);
                p.put("age", patientAge);
                p.put("gender", patientGender);
                p.put("address", patientAddress);
                p.put("doctor_id", doctorId);
                p.put("reason_for_visit", patientProblem);

                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                p.put("appointment_date", dateFormat.format(now));
                p.put("time_slot", timeFormat.format(now));
                p.put("pincode", pincode);
                p.put("appointment_mode", "Online");
                p.put("payment_method", selectedPaymentMethod);
                p.put("status", status);
                p.put("location", googleMapsLink);

                p.put("is_vet_case", String.valueOf(isVetCase));
                if (isVetCase == 1) {
                    if (animalCategoryId != null && !animalCategoryId.trim().isEmpty())
                        p.put("animal_category_id", animalCategoryId);
                    if (animalName != null && !animalName.trim().isEmpty())
                        p.put("animal_name", animalName);
                    if (animalGender != null && !animalGender.trim().isEmpty())
                        p.put("animal_gender", animalGender);
                    if (animalAge != null && !animalAge.trim().isEmpty())
                        p.put("animal_age", animalAge);
                    if (animalBreed != null && !animalBreed.trim().isEmpty())
                        p.put("animal_breed", animalBreed);
                    if (vaccinationId != null && !vaccinationId.trim().isEmpty())
                        p.put("vaccination_id", vaccinationId);
                    if (vaccinationName != null && !vaccinationName.trim().isEmpty())
                        p.put("vaccination_name", vaccinationName);
                }

                d("saveBookingData() params summary:"
                        + " patient_id=" + safe(patientId)
                        + ", doctor_id=" + safe(doctorId)
                        + ", pincode=" + safe(pincode)
                        + ", payment_method=" + safe(selectedPaymentMethod)
                        + ", status=" + safe(status)
                        + ", is_vet_case=" + isVetCase
                        + ", location=" + safe(googleMapsLink));

                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    private void insertPaymentHistory(String appointmentId) {
        String url = ApiConfig.endpoint("payment_history.php");
        d("insertPaymentHistory() POST -> " + url + " appointmentId=" + appointmentId);
        logBillingState("insertPaymentHistory_start");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Your payment details have been saved.", Toast.LENGTH_SHORT).show();
                    d("Payment inserted => " + resp);
                    onBookingSuccess();
                },
                err -> {
                    loaderutil.hideLoader();
                    e("insertPaymentHistory() error", err);
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

                String resolvedPaymentStatus =
                        ("Online".equalsIgnoreCase(selectedPaymentMethod)) ? "Completed" : "Pending";
                p.put("payment_status", resolvedPaymentStatus);
                p.put("refund_status", "None");

                if (ppMerchantOrderId != null) {
                    p.put("payment_reference", ppMerchantOrderId);
                }

                if (isVetCase == 1 && vaccinationPrice > 0.0) {
                    p.put("notes", "Vaccine: " + vaccinationName + " | Price: ₹" + (int) Math.round(vaccinationPrice));
                } else {
                    p.put("notes", "None");
                }

                p.put("upi_id", (enteredUpiId == null ? "" : enteredUpiId));

                d("insertPaymentHistory() params summary:"
                        + " patient_id=" + safe(patientId)
                        + ", appointment_id=" + appointmentId
                        + ", doctor_id=" + safe(doctorId)
                        + ", amount(finalCost)=" + fmt2(finalCost)
                        + ", consultingFee=" + fmt2(consultingFee)
                        + ", deposit=" + fmt2(DEPOSIT)
                        + ", deposit_status=" + lastConfirmedDepositMode
                        + ", payment_method=" + safe(selectedPaymentMethod)
                        + ", payment_status=" + resolvedPaymentStatus
                        + ", distanceKm=" + fmt2(distanceKm)
                        + ", distanceCharge=" + fmt2(distanceCharge)
                        + ", gstAmount=" + fmt2(gstAmount)
                        + ", payment_reference=" + safe(ppMerchantOrderId)
                        + ", upi(masked)=" + maskUpi(enteredUpiId));

                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.5f));
        Volley.newRequestQueue(this).add(req);
    }

    private void onBookingSuccess() {
        d("onBookingSuccess() -> MainActivity open_fragment=2 ongoing_tab=" + (isVetCase == 1 ? 1 : 0));
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_fragment", 2);
        i.putExtra("ongoing_tab", (isVetCase == 1 ? 1 : 0));
        startActivity(i);
        finish();
    }

    private void enablePayButton() {
        payButton.setEnabled(true);
        payButton.setAlpha(1f);
        payButton.setBackgroundColor(getResources().getColor(R.color.navy_blue));
        d("Pay button ENABLED");
    }

    private void disablePayButton() {
        payButton.setEnabled(false);
        payButton.setAlpha(0.5f);
        payButton.setBackgroundColor(getResources().getColor(R.color.custom_gray));
        d("Pay button DISABLED");
    }
}
