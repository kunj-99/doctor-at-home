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
import android.widget.ProgressBar;
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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.phonepe.intent.sdk.api.PhonePeKt;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;
import com.infowave.thedoctorathomeuser.billing.BookingQuote;
import com.infowave.thedoctorathomeuser.billing.MoneyUtil;

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
    private static final boolean DBG = false;

    // Phase 4C: financial state is server-authoritative and stored in integer paise only.
    private long gatewayPaise = 0L;
    private long   platformChargePaise = 0L;
    private long   walletBalancePaise = 0L;
    private BookingQuote activeQuote;

    // Booking destination sent to the server quote endpoint.
    private double userLat = 0.0, userLng = 0.0;

    // Appointment/person fields
    private String patientName, patientAge, patientGender, patientProblem, patientAddress, doctorId, doctorName, status;
    private String selectedPaymentMethod = "Online";
    private String patientId, pincode;

    // Vet case
    private int isVetCase = 0;
    private String animalName, animalGender, animalBreed, animalAge, vaccinationName;
    private String animalCategoryId = "";
    private String vaccinationId   = "";


    // ─── Reservation token (from DoctorAdapter / VetDoctorsAdapter) ──────────
    /** Token from reserve_doctor.php — bound into the server quote and finalized through finalize_booking.php. */
    private String reservationToken = "";
    // Patient ID is loaded from SharedPrefs at onCreate and sent only as a transitional ownership cross-check.

    // ─── Lock-release safety flags ────────────────────────────────────────────
    /** True once user commits to payment (PhonePe or offline pay button pressed) */
    private boolean paymentStarted          = false;
    /** True while finalize_booking.php confirmation is in flight. */
    private boolean appointmentSaveInProgress = false;
    /** True after server confirms appointment_id successfully */
    private boolean appointmentConfirmed    = false;
    /** True after we have already sent a release request (prevent duplicate) */
    private boolean lockReleased            = false;
    /** Blocks duplicate payment/finalization taps while a server operation is active. */
    private boolean bookingOperationInProgress = false;

    // UI
    private Button payButton, btnOnlinePayment, btnOfflinePayment, btnRechargeWallet, btnBillStatusAction;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName, tvBillPatientLabel;
    private TextView tvBillVerificationTitle, tvBillVerificationMessage;
    private ProgressBar progressBillVerification;
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
    private final String bookingQuoteUrl  = ApiConfig.endpoint("get_booking_quote.php");
    private final String finalizeBookingUrl = ApiConfig.endpoint("finalize_booking.php");

    private String ppMerchantOrderId;
    private String bookingQuoteToken = "";
    private ActivityResultLauncher<Intent> ppCheckoutLauncher;

    // Wallet
    private final Handler walletHandler = new Handler();
    private Runnable walletRunnable;
    private TextView tvPricePerKm;

    private enum DepositMode { NONE, WALLET, BILL }
    private DepositMode lastConfirmedDepositMode = DepositMode.NONE;

    private enum BillStatusAction {
        NONE,
        RETRY_QUOTE,
        RETRY_PAYMENT_SETUP,
        CHECK_PAYMENT,
        RETRY_FINALIZE,
        GO_BACK,
        VIEW_APPOINTMENTS
    }
    private BillStatusAction billStatusAction = BillStatusAction.NONE;
    private String billStatusPaymentReference = "";
    private boolean walletRechargeLaunched = false;

    private static final String RECOVERY_PREFS = "BookingRecoveryPrefs";
    private static final String RECOVERY_QUOTE = "quote_token";
    private static final String RECOVERY_MERCHANT = "merchant_order_id";
    private static final String RECOVERY_PATIENT = "patient_id";
    private static final String RECOVERY_DOCTOR = "doctor_id";
    private static final String RECOVERY_RESERVATION = "reservation_token";
    private static final String RECOVERY_STATUS = "booking_status";
    private static final String RECOVERY_PAYMENT_METHOD = "payment_method";
    private static final String RECOVERY_SAVED_AT = "saved_at";
    private static final long RECOVERY_MAX_AGE_MS = 24L * 60L * 60L * 1000L;

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

    private void logBillingState(String where) {
        if (!DBG) return;
        BookingQuote q = activeQuote;
        d("=== BILLING STATE @ " + where + " ===");
        d("Payment: selected=" + safe(selectedPaymentMethod)
                + ", depositMode=" + lastConfirmedDepositMode
                + ", paymentStarted=" + paymentStarted
                + ", operationInProgress=" + bookingOperationInProgress);
        d("Booking: patientId=" + safe(patientId) + ", doctorId=" + safe(doctorId)
                + ", pincode=" + safe(pincode) + ", quote=" + (q == null ? "none" : "verified-v" + q.pricingVersion));
        d("Location: userLatLng=" + userLat + "," + userLng);
        if (q != null) {
            d("Exact bill paise: appointment=" + q.appointmentChargePaise
                    + ", consultation=" + q.consultationFeePaise
                    + ", platform=" + q.platformChargePaise
                    + ", gst=" + q.gstPaise
                    + ", distance=" + q.distanceChargePaise
                    + ", vaccine=" + q.vaccinationPricePaise
                    + ", walletDeposit=" + q.walletDepositPaise
                    + ", final=" + q.finalExactPaise
                    + ", gateway=" + q.gatewayPaise);
        }
        d("Wallet balance paise=" + walletBalancePaise);
        d("=== END STATE ===");
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDestroyedOrFinishing = false;
        d("onResume()");
        fetchWalletBalance();
        if (walletRechargeLaunched && payButton != null && !paymentStarted && !bookingOperationInProgress) {
            walletRechargeLaunched = false;
            bookingQuoteToken = "";
            activeQuote = null;
            gatewayPaise = 0L;
            lastConfirmedDepositMode = DepositMode.NONE;
            setBookingUiState("VERIFYING", "Refreshing bill",
                    "Checking your updated wallet balance and final amount.");
            fetchAuthoritativeBookingQuote(false);
        } else if (activeQuote != null) {
            renderAuthoritativeQuote();
        }
        startWalletPolling();
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
        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this, "Preparing bill", "Verifying the latest booking details…");

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


        // ─── Reservation token (forwarded from adapter through form activity) ───
        reservationToken = intent.getStringExtra("reservation_token") != null
                         ? intent.getStringExtra("reservation_token") : "";

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
                + ", vaccinationName=" + safe(vaccinationName));

        if (doctorId == null || doctorId.isEmpty()) {
            loaderutil.hideLoader();
            Toast.makeText(this, "Sorry, we could not find the doctor information. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("Request for visit".equals(status))      status = "Requested";
        else if ("Book Appointment".equals(status))  status = "Confirmed";

        d("Normalized status=" + safe(status));

        // User location is quoted and canonicalized server-side.
        userLat = intent.getDoubleExtra("latitude", 0.0);
        userLng = intent.getDoubleExtra("longitude", 0.0);
        d("User location: userLatLng=" + userLat + "," + userLng);

        // Bind UI
        tvBillPatientName     = findViewById(R.id.tv_bill_patient_name);
        tvBillPatientLabel    = findViewById(R.id.tv_bill_patient_label);
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
        btnBillStatusAction   = findViewById(R.id.btn_bill_status_action);
        tvBillVerificationTitle = findViewById(R.id.tv_bill_verification_title);
        tvBillVerificationMessage = findViewById(R.id.tv_bill_verification_message);
        progressBillVerification = findViewById(R.id.progress_bill_verification);

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

        // Header info. For veterinary bookings keep owner and pet identity separate.
        if (tvBillPatientLabel != null) {
            tvBillPatientLabel.setText(isVetCase == 1 ? "Pet Owner" : "Patient Name");
        }
        setTextOrDash(tvBillPatientName, patientName);
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

        // Never display a client-provided vaccine price. The row is shown only after a verified server quote.
        setRowVisibility(rowVaccinationPrice, false);

        // Buttons default
        setButtonNeutralState();
        selectedPaymentMethod = "Online";
        stylePaymentButtons();
        disablePayButton();

        d("Initial paymentMethod=" + selectedPaymentMethod);

        // Recover an in-progress online payment before offering any new payment attempt.
        // This protects users after process death / lost PhonePe result callbacks.
        boolean recoveringPayment = restorePaymentRecoveryIfAny();
        if (!recoveringPayment) {
            setBookingUiState("VERIFYING", "Verifying bill securely",
                    "Checking doctor availability, service area and final amount.");
            fetchAuthoritativeBookingQuote(false);
        }

        // Wallet polling (lifecycle-aware)
        walletRunnable = () -> {
            if (!isDestroyedOrFinishing && !isFinishing() && !isDestroyed()) {
                d("Wallet polling tick...");
                fetchWalletBalance();
                walletHandler.postDelayed(walletRunnable, 30000);
            }
        };
        startWalletPolling();

        btnOfflinePayment.setOnClickListener(v -> selectPaymentMethod("Offline"));
        btnOnlinePayment.setOnClickListener(v -> selectPaymentMethod("Online"));

        btnRechargeWallet.setOnClickListener(v -> {
            if (paymentStarted || bookingOperationInProgress) return;
            walletRechargeLaunched = true;
            startActivity(new Intent(pending_bill.this, payments.class));
        });

        btnBillStatusAction.setOnClickListener(v -> handleBillStatusAction());

        // payButton click → show dialog with UPI input + verify before proceeding
        payButton.setOnClickListener(v -> {
            d("Pay button clicked. verified gatewayPaise=" + gatewayPaise);
            logBillingState("payButtonClicked");
            showConfirmWithUpiDialog();
        });

        logBillingState("onCreate_end");
    }

    @Override
    protected void onPause() {
        super.onPause();
        d("onPause()");
        stopWalletPolling();
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
        stopWalletPolling();
        loaderutil.hideLoader();
        // Only release lock if finishing before payment ever started
        // (e.g., user backs out, OS eviction). Never releases after payment committed.
        if (isFinishing()) {
            releaseReservationLockIfSafe("onDestroy_finishing");
        }
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

    private void startWalletPolling() {
        if (walletRunnable == null || isDestroyedOrFinishing || isFinishing() || isDestroyed()) return;
        walletHandler.removeCallbacks(walletRunnable);
        walletHandler.postDelayed(walletRunnable, 30000L);
    }

    private void stopWalletPolling() {
        if (walletRunnable != null) walletHandler.removeCallbacks(walletRunnable);
    }

    private boolean isValidBookingLocation() {
        return Double.isFinite(userLat) && Double.isFinite(userLng)
                && userLat >= -90.0 && userLat <= 90.0
                && userLng >= -180.0 && userLng <= 180.0
                && !(Math.abs(userLat) < 0.000001 && Math.abs(userLng) < 0.000001);
    }

    private void selectPaymentMethod(String method) {
        if (paymentStarted || bookingOperationInProgress) return;
        if (!"Online".equals(method) && !"Offline".equals(method)) return;

        selectedPaymentMethod = method;
        bookingQuoteToken = "";
        activeQuote = null;
        gatewayPaise = 0L;
        lastConfirmedDepositMode = DepositMode.NONE;
        stylePaymentButtons();
        disablePayButton();

        if ("Offline".equals(method) && platformChargePaise > 0L && walletBalancePaise < platformChargePaise) {
            setBookingUiState("VERIFYING", "Checking offline booking",
                    "We will verify whether your wallet can cover the platform charge.");
        } else {
            setBookingUiState("VERIFYING", "Updating verified bill",
                    "Checking the final amount for the selected payment option.");
        }
        fetchAuthoritativeBookingQuote(false);
    }

    private void setBillStatusAction(BillStatusAction action, String label, String paymentReference) {
        billStatusAction = action == null ? BillStatusAction.NONE : action;
        billStatusPaymentReference = paymentReference == null ? "" : paymentReference;
        if (btnBillStatusAction == null) return;

        if (billStatusAction == BillStatusAction.NONE || label == null || label.trim().isEmpty()) {
            btnBillStatusAction.setVisibility(View.GONE);
            btnBillStatusAction.setEnabled(false);
            return;
        }
        btnBillStatusAction.setText(label);
        btnBillStatusAction.setEnabled(true);
        btnBillStatusAction.setVisibility(View.VISIBLE);
    }

    private void handleBillStatusAction() {
        if (bookingOperationInProgress || appointmentSaveInProgress) return;
        BillStatusAction action = billStatusAction;
        String ref = billStatusPaymentReference;
        setBillStatusAction(BillStatusAction.NONE, "", "");

        switch (action) {
            case RETRY_QUOTE:
                fetchAuthoritativeBookingQuote(false);
                break;
            case RETRY_PAYMENT_SETUP:
                paymentStarted = true;
                startPhonePeCheckout();
                break;
            case CHECK_PAYMENT:
                if (!TextUtils.isEmpty(ref)) checkPhonePeStatus(ref);
                else if (!TextUtils.isEmpty(ppMerchantOrderId)) checkPhonePeStatus(ppMerchantOrderId);
                else startPhonePeCheckout();
                break;
            case RETRY_FINALIZE:
                finalizeBooking(ref);
                break;
            case GO_BACK:
                releaseReservationLockIfSafe("status_action_go_back");
                finish();
                break;
            case VIEW_APPOINTMENTS:
                onBookingSuccess();
                break;
            case NONE:
            default:
                break;
        }
    }

    private JSONObject parseVolleyErrorBody(VolleyError err) {
        if (err == null || err.networkResponse == null || err.networkResponse.data == null) return null;
        try {
            return new JSONObject(new String(err.networkResponse.data));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void handleQuoteFailure(String code, String message) {
        loaderutil.hideLoader();
        bookingOperationInProgress = false;
        bookingQuoteToken = "";
        activeQuote = null;
        gatewayPaise = 0L;
        lastConfirmedDepositMode = DepositMode.NONE;

        String safeCode = code == null ? "" : code.trim();
        String safeMessage = TextUtils.isEmpty(message)
                ? "We could not verify the booking right now. Please try again."
                : message.trim();
        String title = "Could not verify bill";
        BillStatusAction action = BillStatusAction.RETRY_QUOTE;
        String actionLabel = "Retry bill check";

        if ("RESERVATION_REQUIRED".equals(safeCode) || "RESERVATION_EXPIRED".equals(safeCode)) {
            title = "Doctor reservation expired";
            action = BillStatusAction.GO_BACK;
            actionLabel = "Go back";
        } else if ("DOCTOR_INACTIVE".equals(safeCode) || "DOCTOR_NOT_ACCEPTING".equals(safeCode)
                || "DOCTOR_NOT_FOUND".equals(safeCode)) {
            title = "Doctor unavailable";
            action = BillStatusAction.GO_BACK;
            actionLabel = "Go back";
        } else if ("PINCODE_NOT_SERVED".equals(safeCode) || "INVALID_PINCODE".equals(safeCode)) {
            title = "Service area needs updating";
            action = BillStatusAction.GO_BACK;
            actionLabel = "Change location";
        } else if ("BOOKING_DETAILS_REQUIRED".equals(safeCode) || "PET_DETAILS_REQUIRED".equals(safeCode)
                || "ANIMAL_BREED_REQUIRED".equals(safeCode) || "ANIMAL_BREED_NOT_AVAILABLE".equals(safeCode)
                || "VET_CATEGORY_MISMATCH".equals(safeCode) || "VACCINATION_NOT_AVAILABLE".equals(safeCode)
                || "VACCINATION_CATEGORY_MISMATCH".equals(safeCode)) {
            title = "Booking details need attention";
            action = BillStatusAction.GO_BACK;
            actionLabel = "Review details";
        } else if ("BOOKING_LOCATION_REQUIRED".equals(safeCode) || "INVALID_COORDINATES".equals(safeCode)) {
            title = "Location required";
            action = BillStatusAction.GO_BACK;
            actionLabel = "Go back";
        } else if ("OFFLINE_WALLET_INSUFFICIENT".equals(safeCode) || "WALLET_BALANCE_CHANGED".equals(safeCode)) {
            title = "Wallet recharge required";
            action = BillStatusAction.RETRY_QUOTE;
            actionLabel = "Verify after recharge";
        } else if ("QUOTE_FINAL_MISMATCH".equals(safeCode) || "QUOTE_GST_MISMATCH".equals(safeCode)
                || "QUOTE_DISTANCE_MISMATCH".equals(safeCode) || "QUOTE_SETTLEMENT_MISMATCH".equals(safeCode)
                || "MONEY_OVERFLOW".equals(safeCode) || "INVALID_DECIMAL_VALUE".equals(safeCode)) {
            title = "Bill verification failed";
            action = BillStatusAction.RETRY_QUOTE;
            actionLabel = "Verify bill again";
        }

        setBookingUiState("ERROR", title, safeMessage);
        setBillStatusAction(action, actionLabel, "");
        if (("OFFLINE_WALLET_INSUFFICIENT".equals(safeCode) || "WALLET_BALANCE_CHANGED".equals(safeCode))
                && btnRechargeWallet != null) {
            btnRechargeWallet.setVisibility(View.VISIBLE);
        }
    }

    private void savePaymentRecovery(String quoteToken, String merchantOrderId) {
        if (TextUtils.isEmpty(quoteToken)) return;
        getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE).edit()
                .putString(RECOVERY_QUOTE, quoteToken)
                .putString(RECOVERY_MERCHANT, merchantOrderId == null ? "" : merchantOrderId)
                .putString(RECOVERY_PATIENT, patientId == null ? "" : patientId)
                .putString(RECOVERY_DOCTOR, doctorId == null ? "" : doctorId)
                .putString(RECOVERY_RESERVATION, reservationToken == null ? "" : reservationToken)
                .putString(RECOVERY_STATUS, status == null ? "" : status)
                .putString(RECOVERY_PAYMENT_METHOD, selectedPaymentMethod == null ? "Online" : selectedPaymentMethod)
                .putLong(RECOVERY_SAVED_AT, System.currentTimeMillis())
                .apply();
    }

    private void clearPaymentRecovery() {
        getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE).edit().clear().apply();
    }

    private boolean restorePaymentRecoveryIfAny() {
        SharedPreferences recovery = getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE);
        String savedQuote = recovery.getString(RECOVERY_QUOTE, "");
        if (TextUtils.isEmpty(savedQuote)) return false;

        long savedAt = recovery.getLong(RECOVERY_SAVED_AT, 0L);
        boolean tooOld = savedAt <= 0L || System.currentTimeMillis() - savedAt > RECOVERY_MAX_AGE_MS;
        boolean sameContext = safe(patientId).equals(recovery.getString(RECOVERY_PATIENT, ""))
                && safe(doctorId).equals(recovery.getString(RECOVERY_DOCTOR, ""))
                && safe(reservationToken).equals(recovery.getString(RECOVERY_RESERVATION, ""))
                && safe(status).equals(recovery.getString(RECOVERY_STATUS, ""));
        if (tooOld || !sameContext) {
            clearPaymentRecovery();
            return false;
        }

        bookingQuoteToken = savedQuote;
        ppMerchantOrderId = recovery.getString(RECOVERY_MERCHANT, "");
        selectedPaymentMethod = recovery.getString(RECOVERY_PAYMENT_METHOD, "Online");
        if (!"Offline".equals(selectedPaymentMethod)) selectedPaymentMethod = "Online";
        paymentStarted = true;
        bookingOperationInProgress = true;
        stylePaymentButtons();

        if ("Offline".equals(selectedPaymentMethod)) {
            setBookingUiState("FINALIZING", "Restoring appointment confirmation",
                    "Checking the same wallet booking safely. No second wallet charge will be created.");
            finalizeBooking("");
        } else if (!TextUtils.isEmpty(ppMerchantOrderId)) {
            setBookingUiState("PAYMENT", "Restoring previous payment",
                    "Checking the same PhonePe payment. Please do not pay again.");
            checkPhonePeStatus(ppMerchantOrderId);
        } else {
            setBookingUiState("PAYMENT", "Restoring secure payment",
                    "A previous payment setup was interrupted. Reusing the same booking reference safely.");
            startPhonePeCheckout();
        }
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void setBookingUiState(String state, String title, String message) {
        if (tvBillVerificationTitle != null) tvBillVerificationTitle.setText(title);
        if (tvBillVerificationMessage != null) tvBillVerificationMessage.setText(message);
        setBillStatusAction(BillStatusAction.NONE, "", "");

        boolean busy = "VERIFYING".equals(state) || "PAYMENT".equals(state) || "FINALIZING".equals(state);
        if (progressBillVerification != null) progressBillVerification.setVisibility(busy ? View.VISIBLE : View.GONE);

        if (payButton != null) {
            if ("PAYMENT".equals(state)) {
                payButton.setText("Payment in progress…");
            } else if ("FINALIZING".equals(state)) {
                payButton.setText("Confirming appointment…");
            } else if ("SUCCESS".equals(state)) {
                payButton.setText("Appointment confirmed");
            } else {
                payButton.setText("Proceed to Payment");
            }
        }

        if (busy || "SUCCESS".equals(state)) {
            if (payButton != null) {
                payButton.setEnabled(false);
                payButton.setAlpha(0.65f);
            }
            if (btnOnlinePayment != null) btnOnlinePayment.setEnabled(false);
            if (btnOfflinePayment != null) btnOfflinePayment.setEnabled(false);
        } else {
            if (btnOnlinePayment != null) btnOnlinePayment.setEnabled(true);
            if (btnOfflinePayment != null) btnOfflinePayment.setEnabled(true);
            // Rendering is explicit. Do not re-enter renderAuthoritativeQuote() from state changes.
        }
    }

    private void resetBeforePaymentFailure(String title, String message) {
        paymentStarted = false;
        bookingOperationInProgress = false;
        ppMerchantOrderId = null;
        bookingQuoteToken = "";
        activeQuote = null;
        gatewayPaise = 0L;
        lastConfirmedDepositMode = DepositMode.NONE;
        clearPaymentRecovery();
        loaderutil.hideLoader();
        setBookingUiState("ERROR", title, message);
        setBillStatusAction(BillStatusAction.RETRY_QUOTE, "Verify latest bill", "");
    }

    private void handlePaymentSetupUncertain(String message) {
        loaderutil.hideLoader();
        bookingOperationInProgress = false;
        paymentStarted = true;
        savePaymentRecovery(bookingQuoteToken, ppMerchantOrderId);
        setBookingUiState("PAYMENT", "Payment setup interrupted",
                TextUtils.isEmpty(message)
                        ? "Do not start a new payment. Retry safely with the same booking reference."
                        : message);
        setBillStatusAction(BillStatusAction.RETRY_PAYMENT_SETUP, "Retry secure payment", "");
    }

    private void handleBookingConfirmed(int appointmentId) {
        appointmentConfirmed = true;
        appointmentSaveInProgress = false;
        bookingOperationInProgress = false;
        clearPaymentRecovery();
        setBookingUiState("SUCCESS", "Appointment confirmed",
                "Your booking is saved safely. Opening your ongoing appointments…");
        fetchWalletBalance();
        Toast.makeText(this, "Appointment booked successfully.", Toast.LENGTH_SHORT).show();
        onBookingSuccess();
    }

    /* ---------------- Confirm dialog with UPI ---------------- */

    private interface UpiVerificationCallback {
        void onSuccess();
        void onFailure(String message);
    }

    private void showConfirmWithUpiDialog() {
        if (paymentStarted || bookingOperationInProgress) return;

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_upi_capture, null, false);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) EditText etUpi = content.findViewById(R.id.et_upi);
        TextView tvUpiError = content.findViewById(R.id.tv_upi_error);
        ProgressBar progressUpi = content.findViewById(R.id.progress_upi_verification);

        String verifiedAmount = activeQuote == null ? "" : ("\nVerified amount: " + MoneyUtil.formatPaise(activeQuote.gatewayPaise) + "\n");
        String msg = "Please confirm the appointment details before continuing." + verifiedAmount + "\n"
                + "Your UPI ID is used only for eligible refunds if a refund is required later.";

        AlertDialog dialog = new AlertDialog.Builder(pending_bill.this)
                .setTitle("Confirm Appointment")
                .setMessage(msg)
                .setView(content)
                .setCancelable(false)
                .setPositiveButton("Verify & continue", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(ignored -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positive.setOnClickListener(v -> {
                Editable ed = etUpi.getText();
                String vpa = (ed == null) ? "" : ed.toString().trim();
                tvUpiError.setVisibility(View.GONE);
                etUpi.setError(null);

                if (!isLikelyValidUpi(vpa)) {
                    etUpi.setError("Enter a valid UPI ID, for example name@bank");
                    tvUpiError.setText("Please check the UPI ID and try again.");
                    tvUpiError.setVisibility(View.VISIBLE);
                    return;
                }

                positive.setEnabled(false);
                negative.setEnabled(false);
                positive.setText("Checking…");
                progressUpi.setVisibility(View.VISIBLE);
                tvUpiError.setTextColor(Color.parseColor("#5C6F82"));
                tvUpiError.setText("Verifying UPI ID…");
                tvUpiError.setVisibility(View.VISIBLE);

                verifyUpi(vpa, new UpiVerificationCallback() {
                    @Override
                    public void onSuccess() {
                        if (!dialog.isShowing()) return;
                        dialog.dismiss();
                        continueAfterUpiVerified(vpa);
                    }

                    @Override
                    public void onFailure(String message) {
                        if (!dialog.isShowing()) return;
                        progressUpi.setVisibility(View.GONE);
                        positive.setEnabled(true);
                        negative.setEnabled(true);
                        positive.setText("Verify & continue");
                        tvUpiError.setTextColor(Color.parseColor("#B3261E"));
                        tvUpiError.setText(TextUtils.isEmpty(message)
                                ? "UPI verification failed. Please check the ID and try again."
                                : message);
                        tvUpiError.setVisibility(View.VISIBLE);
                    }
                });
            });
        });
        dialog.show();
    }

    private boolean isLikelyValidUpi(String vpa) {
        if (TextUtils.isEmpty(vpa)) return false;
        if (vpa.length() < 6 || vpa.length() > 300) return false;
        if (vpa.contains(" ") || vpa.contains("\\") || vpa.contains("/")) return false;
        return vpa.matches("^[a-zA-Z0-9._\\-]{2,256}@[a-zA-Z]{3,64}$");
    }

    private void verifyUpi(String vpa, UpiVerificationCallback callback) {
        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this, "Verifying UPI ID", "Checking the refund UPI ID securely…");

        StringRequest req = new StringRequest(
                Request.Method.POST,
                upiVerifyUrl,
                resp -> {
                    loaderutil.hideLoader();
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean ok = o.optBoolean("valid", false);
                        String msg = o.optString("message", "");
                        if (ok) {
                            if (callback != null) callback.onSuccess();
                        } else if (callback != null) {
                            callback.onFailure(msg.isEmpty() ? "This UPI ID could not be verified." : msg);
                        }
                    } catch (Exception e) {
                        this.e("UPI verification parse error", e);
                        if (callback != null) callback.onFailure("We could not verify the UPI ID right now. Please try again.");
                    }
                },
                err -> {
                    loaderutil.hideLoader();
                    e("verifyUpi() network error", err);
                    if (callback != null) callback.onFailure("Network error while verifying UPI. Check your connection and try again.");
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
        VolleySingleton.getInstance(this).getRequestQueue().add(req);
    }

    private void continueAfterUpiVerified(String vpa) {
        enteredUpiId = vpa;
        d("UPI verified OK. enteredUpiId(masked)=" + maskUpi(enteredUpiId));

        if (selectedPaymentMethod == null || selectedPaymentMethod.isEmpty()) {
            loaderutil.hideLoader();
            Toast.makeText(this, "Please choose a payment option to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Phase 3: one authoritative server quote before any money action.
        fetchAuthoritativeBookingQuote(true);
    }

    private void fetchAuthoritativeBookingQuote(boolean continueAfterLoad) {
        if (!isValidBookingLocation()) {
            bookingOperationInProgress = false;
            handleQuoteFailure("BOOKING_LOCATION_REQUIRED",
                    "Please go back and select the appointment location again.");
            return;
        }
        bookingOperationInProgress = continueAfterLoad;
        setBookingUiState("VERIFYING", "Verifying bill securely",
                "Checking doctor availability, service pincode and final amount.");
        loaderutil.showLoader(this, "Verifying bill", "Checking availability and the latest server price…");

        StringRequest req = new StringRequest(Request.Method.POST, bookingQuoteUrl,
                resp -> {
                    try {
                        JSONObject root = new JSONObject(resp);
                        if (!root.optBoolean("success")) {
                            handleQuoteFailure(root.optString("code", "QUOTE_FAILED"),
                                    root.optString("message", "Unable to calculate booking price."));
                            return;
                        }
                        JSONObject q = root.getJSONObject("quote");
                        BookingQuote verifiedQuote = BookingQuote.fromJson(q);
                        verifiedQuote.validateForPaymentMethod(selectedPaymentMethod);
                        activeQuote = verifiedQuote;
                        bookingQuoteToken = verifiedQuote.quoteToken;

                        // Phase 4C: Android stores the immutable exact server quote; it does not recalculate money.
                        if (!verifiedQuote.vaccinationName.isEmpty()) vaccinationName = verifiedQuote.vaccinationName;
                        walletBalancePaise = verifiedQuote.walletBalancePaise;
                        platformChargePaise = verifiedQuote.platformChargePaise;
                        gatewayPaise = verifiedQuote.gatewayPaise;
                        lastConfirmedDepositMode = "Wallet Debited".equalsIgnoreCase(verifiedQuote.depositStatus)
                                ? DepositMode.WALLET : DepositMode.BILL;
                        renderAuthoritativeQuote();

                        setBookingUiState("READY", "Bill verified by server",
                                "Doctor availability, service area and amount are verified. The bill will be checked again before payment.");
                        if (continueAfterLoad) {
                            continueWithAuthoritativeQuote();
                        } else {
                            bookingOperationInProgress = false;
                            loaderutil.hideLoader();
                        }
                    } catch (Exception ex) {
                        e("Authoritative quote parse error", ex);
                        handleQuoteFailure("QUOTE_RESPONSE_INVALID",
                                "We could not read the verified bill. No payment has started; please try again.");
                    }
                },
                err -> {
                    e("Authoritative quote network error", err);
                    JSONObject body = parseVolleyErrorBody(err);
                    String code = body == null ? "QUOTE_NETWORK_ERROR" : body.optString("code", "QUOTE_NETWORK_ERROR");
                    String message = body == null
                            ? "Check your connection and retry the bill check. No payment has started."
                            : body.optString("message", "Could not verify the booking right now.");
                    handleQuoteFailure(code, message);
                }) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                // Explicit opt-in keeps the live v3 APK contract backward-compatible on the same backend.
                p.put("intent_version", "5");
                p.put("pricing_version", "5");
                p.put("patient_id", patientId);
                p.put("doctor_id", doctorId);
                p.put("user_lat", String.valueOf(userLat));
                p.put("user_lng", String.valueOf(userLng));
                p.put("is_vet_case", String.valueOf(isVetCase));
                p.put("booking_status", status == null ? "Pending" : status);
                p.put("patient_name", patientName == null ? "" : patientName);
                p.put("patient_age", patientAge == null ? "" : patientAge);
                p.put("patient_gender", patientGender == null ? "" : patientGender);
                p.put("address", patientAddress == null ? "" : patientAddress);
                p.put("pincode", pincode == null ? "" : pincode);
                p.put("reason_for_visit", patientProblem == null ? "" : patientProblem);
                p.put("appointment_mode", "Online");
                p.put("payment_method", selectedPaymentMethod == null ? "Online" : selectedPaymentMethod);
                p.put("upi_id", enteredUpiId == null ? "" : enteredUpiId);
                if (reservationToken != null && !reservationToken.isEmpty()) p.put("reservation_token", reservationToken);
                if (animalCategoryId != null && !animalCategoryId.trim().isEmpty()) p.put("animal_category_id", animalCategoryId);
                if (vaccinationId != null && !vaccinationId.trim().isEmpty()) p.put("vaccination_id", vaccinationId);
                if (isVetCase == 1) {
                    p.put("animal_name", animalName == null ? "" : animalName);
                    p.put("animal_gender", animalGender == null ? "" : animalGender);
                    p.put("animal_age", animalAge == null ? "" : animalAge);
                    p.put("animal_breed", animalBreed == null ? "" : animalBreed);
                }
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.5f));
        VolleySingleton.getInstance(this).getRequestQueue().add(req);
    }

    private void continueWithAuthoritativeQuote() {
        if (activeQuote == null || TextUtils.isEmpty(bookingQuoteToken)) {
            resetBeforePaymentFailure("Bill verification required", "Please verify the latest server bill before continuing.");
            return;
        }
        if ("Offline".equals(selectedPaymentMethod) && lastConfirmedDepositMode != DepositMode.WALLET) {
            loaderutil.hideLoader();
            bookingOperationInProgress = false;
            paymentStarted = false;
            bookingQuoteToken = "";
            activeQuote = null;
            gatewayPaise = 0L;
            lastConfirmedDepositMode = DepositMode.NONE;
            setBookingUiState("ERROR", "Wallet balance is too low",
                    "Offline booking needs at least " + MoneyUtil.formatPaise(platformChargePaise) + " in your wallet for the platform charge. Recharge the wallet, then verify the bill again.");
            renderAuthoritativeQuote();
            return;
        }

        if ("Offline".equals(selectedPaymentMethod)) {
            setDepositLine("Platform Charge will be debited safely during booking: " + MoneyUtil.formatPaise(platformChargePaise), true);
            paymentStarted = true;
            bookingOperationInProgress = true;
            savePaymentRecovery(bookingQuoteToken, "");
            setBookingUiState("FINALIZING", "Confirming appointment",
                    "Wallet charge and appointment are being saved together securely.");
            finalizeBooking("");
        } else {
            // v4 Online settlement intentionally keeps wallet out of the post-payment critical path.
            setDepositLine("Platform Charge included in secure online payment: " + MoneyUtil.formatPaise(platformChargePaise), true);
            paymentStarted = true;
            bookingOperationInProgress = true;
            savePaymentRecovery(bookingQuoteToken, "");
            setBookingUiState("PAYMENT", "Preparing secure payment",
                    "PhonePe will open once. If anything is interrupted, this same booking reference will be recovered safely.");
            startPhonePeCheckout();
        }
    }

    /* ---------------- PhonePe ---------------- */

    private void startPhonePeCheckout() {
        if (bookingQuoteToken == null || bookingQuoteToken.isEmpty()) {
            resetBeforePaymentFailure("Bill verification expired", "Please verify the latest amount before paying.");
            Toast.makeText(this, "Booking price expired. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        bookingOperationInProgress = true;
        paymentStarted = true;
        savePaymentRecovery(bookingQuoteToken, ppMerchantOrderId);
        setBookingUiState("PAYMENT", "Preparing secure payment",
                "Creating or restoring the same PhonePe payment securely. Please do not start another payment.");
        d("startPhonePeCheckout() -> " + ppCreateOrderUrl);
        d("Gateway amount alignment: exact server paise=" + gatewayPaise);
        logBillingState("startPhonePeCheckout");

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ppCreateOrderUrl,
                resp -> {
                    d("create_order response=" + resp);
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            String msg = obj.optString("message", "Payment could not be started.");
                            resetBeforePaymentFailure("Payment not started", msg);
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            return;
                        }

                        ppMerchantOrderId = obj.optString("merchantOrderId", null);
                        String token   = obj.optString("token", "");
                        String orderId = obj.optString("orderId", "");
                        long orderAmountPaise = obj.optLong("amountPaise", -1L);

                        if (activeQuote != null && orderAmountPaise >= 0L
                                && orderAmountPaise != activeQuote.gatewayPaise) {
                            resetBeforePaymentFailure("Payment amount mismatch",
                                    "The payment order did not match the verified bill. No checkout was opened. Please verify the latest bill again.");
                            return;
                        }

                        d("create_order parsed: merchantOrderId=" + ppMerchantOrderId
                                + ", tokenLen=" + token.length()
                                + ", orderId=" + orderId);

                        if (ppMerchantOrderId == null || token.isEmpty() || orderId.isEmpty()) {
                            handlePaymentSetupUncertain("The payment setup response was incomplete. Do not start a new payment; retry this same setup safely.");
                            return;
                        }

                        savePaymentRecovery(bookingQuoteToken, ppMerchantOrderId);
                        setBookingUiState("PAYMENT", "Secure payment ready",
                                "Complete this PhonePe payment once. Do not start another payment if confirmation is slow.");
                        PhonePeKt.startCheckoutPage(this, token, orderId, ppCheckoutLauncher);

                    } catch (Exception e) {
                        e("create_order parse error", e);
                        handlePaymentSetupUncertain("We could not read the payment setup response. Retry safely with the same booking reference.");
                    }
                },
                err -> {
                    e("create_order network error", err);
                    JSONObject body = parseVolleyErrorBody(err);
                    if (body != null && err.networkResponse != null && err.networkResponse.statusCode >= 400
                            && err.networkResponse.statusCode < 500) {
                        String msg = body.optString("message", "Payment could not be started. Please verify the bill again.");
                        resetBeforePaymentFailure("Payment not started", msg);
                    } else {
                        handlePaymentSetupUncertain("The network interrupted payment setup. Do not start a new payment; retry this same setup safely.");
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("purpose", "APPOINTMENT");
                p.put("quote_token", bookingQuoteToken);
                p.put("_ts", String.valueOf(System.currentTimeMillis()));
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.5f));
        VolleySingleton.getInstance(this).getRequestQueue().add(req);
    }

    private void checkPhonePeStatus(String merchantOrderId) {
        if (TextUtils.isEmpty(merchantOrderId)) {
            handlePaymentSetupUncertain("We do not have the payment reference yet. Retry the same secure payment setup.");
            return;
        }
        ppMerchantOrderId = merchantOrderId;
        paymentStarted = true;
        bookingOperationInProgress = true;
        savePaymentRecovery(bookingQuoteToken, merchantOrderId);

        final Handler h = new Handler(getMainLooper());
        final int[] attempts = {0};
        final int maxAttempts = 5;
        setBookingUiState("PAYMENT", "Verifying payment",
                "Please do not start another payment while we confirm this one.");

        Runnable check = new Runnable() {
            @Override public void run() {
                if (isFinishing() || isDestroyedOrFinishing || isDestroyed()) {
                    loaderutil.hideLoader();
                    return;
                }
                attempts[0]++;
                String url = ppStatusUrl + "?merchantOrderId=" + Uri.encode(merchantOrderId)
                        + "&skipWallet=1&ts=" + System.currentTimeMillis();
                StringRequest req = new StringRequest(Request.Method.GET, url,
                        resp -> {
                            try {
                                JSONObject obj = new JSONObject(resp);
                                if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                                    if (attempts[0] < maxAttempts) { h.postDelayed(this, 2000); return; }
                                    loaderutil.hideLoader();
                                    bookingOperationInProgress = false;
                                    setBookingUiState("PAYMENT", "Payment verification pending",
                                            "Do not pay again. Check the same payment status again when your connection is stable.");
                                    setBillStatusAction(BillStatusAction.CHECK_PAYMENT, "Check payment again", merchantOrderId);
                                    return;
                                }

                                String state = obj.optString("state", "PENDING");
                                if ("COMPLETED".equalsIgnoreCase(state)) {
                                    int recoveredAppointmentId = obj.optInt("appointment_id", 0);
                                    if (recoveredAppointmentId > 0 && "CONFIRMED".equalsIgnoreCase(obj.optString("booking_status"))) {
                                        loaderutil.hideLoader();
                                        handleBookingConfirmed(recoveredAppointmentId);
                                        return;
                                    }
                                    setBookingUiState("FINALIZING", "Payment received",
                                            "Payment is verified. Confirming your appointment now…");
                                    finalizeBooking(merchantOrderId);
                                    return;
                                }

                                if ("FAILED".equalsIgnoreCase(state)
                                        || "CANCELLED".equalsIgnoreCase(state)
                                        || "TIMED_OUT".equalsIgnoreCase(state)) {
                                    loaderutil.hideLoader();
                                    paymentStarted = false;
                                    bookingOperationInProgress = false;
                                    bookingQuoteToken = "";
                                    activeQuote = null;
                                    gatewayPaise = 0L;
                                    ppMerchantOrderId = null;
                                    lastConfirmedDepositMode = DepositMode.NONE;
                                    clearPaymentRecovery();

                                    String reason;
                                    if ("CANCELLED".equalsIgnoreCase(state)) {
                                        reason = "The PhonePe payment was cancelled. No appointment payment was confirmed.";
                                    } else if ("TIMED_OUT".equalsIgnoreCase(state)) {
                                        reason = "The PhonePe payment timed out. No appointment payment was confirmed.";
                                    } else {
                                        reason = "The PhonePe payment failed. No appointment payment was confirmed.";
                                    }
                                    setBookingUiState("ERROR", "Payment not completed",
                                            reason + " Your temporary doctor reservation is kept briefly so you can retry with a fresh verified bill.");
                                    setBillStatusAction(BillStatusAction.RETRY_QUOTE, "Verify bill & retry", "");
                                    return;
                                }

                                if (attempts[0] < maxAttempts) {
                                    h.postDelayed(this, 2000);
                                } else {
                                    loaderutil.hideLoader();
                                    bookingOperationInProgress = false;
                                    setBookingUiState("PAYMENT", "Payment verification pending",
                                            "Do not pay again. The same payment can be checked safely without creating a second payment.");
                                    setBillStatusAction(BillStatusAction.CHECK_PAYMENT, "Check payment again", merchantOrderId);
                                }
                            } catch (Exception e) {
                                if (attempts[0] < maxAttempts) h.postDelayed(this, 2000);
                                else {
                                    loaderutil.hideLoader();
                                    bookingOperationInProgress = false;
                                    setBookingUiState("PAYMENT", "Payment verification pending",
                                            "Do not pay again. We could not read the latest payment status, but this same payment can be checked safely.");
                                    setBillStatusAction(BillStatusAction.CHECK_PAYMENT, "Check payment again", merchantOrderId);
                                }
                            }
                        },
                        err -> {
                            if (attempts[0] < maxAttempts) h.postDelayed(this, 2000);
                            else {
                                loaderutil.hideLoader();
                                bookingOperationInProgress = false;
                                setBookingUiState("PAYMENT", "Payment verification pending",
                                        "Network is unstable. Do not pay again; check the same payment when the connection improves.");
                                setBillStatusAction(BillStatusAction.CHECK_PAYMENT, "Check payment again", merchantOrderId);
                            }
                        });
                req.setShouldCache(false);
                req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1.0f));
                VolleySingleton.getInstance(pending_bill.this).getRequestQueue().add(req);
            }
        };

        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this, "Checking payment", "Checking the same payment — please do not pay again.");
        check.run();
    }

    /**
     * v5 server-authoritative finalization. This endpoint is idempotent: retrying the same
     * quote is safe even when Android lost the original response after the server committed.
     */
    private void finalizeBooking(String paymentReference) {
        if (bookingQuoteToken == null || bookingQuoteToken.trim().isEmpty()) {
            loaderutil.hideLoader();
            bookingOperationInProgress = false;
            if (!TextUtils.isEmpty(paymentReference) || !TextUtils.isEmpty(ppMerchantOrderId)) {
                paymentStarted = true;
                setBookingUiState("PAYMENT", "Payment reference needs checking",
                        "Do not pay again. Check the existing PhonePe payment before doing anything else.");
                setBillStatusAction(BillStatusAction.CHECK_PAYMENT, "Check payment", !TextUtils.isEmpty(paymentReference) ? paymentReference : ppMerchantOrderId);
            } else {
                paymentStarted = false;
                clearPaymentRecovery();
                setBookingUiState("ERROR", "Booking reference expired",
                        "Please verify the latest bill before trying again.");
                setBillStatusAction(BillStatusAction.RETRY_QUOTE, "Verify latest bill", "");
            }
            return;
        }

        appointmentSaveInProgress = true;
        bookingOperationInProgress = true;
        setBookingUiState("FINALIZING", "Confirming appointment",
                "Saving your appointment securely. Please do not pay again or close the app yet.");
        if (!isFinishing() && !isDestroyed()) loaderutil.showLoader(this, "Confirming appointment", "Payment is safe. Finishing the same booking now…");

        StringRequest req = new StringRequest(Request.Method.POST, finalizeBookingUrl,
                resp -> {
                    loaderutil.hideLoader();
                    appointmentSaveInProgress = false;
                    try {
                        JSONObject obj = new JSONObject(resp);
                        if (obj.optBoolean("success", false)) {
                            int appointmentId = obj.optInt("appointment_id", 0);
                            if (appointmentId > 0) {
                                handleBookingConfirmed(appointmentId);
                                return;
                            }
                        }
                        bookingOperationInProgress = false;
                        setBookingUiState("FINALIZING", "Confirmation pending",
                                "Do not pay again. Retry confirmation with the same booking reference.");
                        setBillStatusAction(BillStatusAction.RETRY_FINALIZE, "Retry confirmation", paymentReference);
                        showSafeFinalizeRetryDialog(paymentReference,
                                obj.optString("message", "Booking confirmation is still pending."));
                    } catch (Exception ex) {
                        bookingOperationInProgress = false;
                        setBookingUiState("FINALIZING", "Confirmation pending",
                                "The server response was interrupted. Do not pay again; retrying confirmation is safe.");
                        setBillStatusAction(BillStatusAction.RETRY_FINALIZE, "Retry confirmation", paymentReference);
                        showSafeFinalizeRetryDialog(paymentReference,
                                "We could not read the confirmation response.");
                    }
                },
                err -> {
                    loaderutil.hideLoader();
                    appointmentSaveInProgress = false;

                    String code = "";
                    String message = "Booking confirmation was interrupted.";
                    if (err.networkResponse != null && err.networkResponse.data != null) {
                        try {
                            JSONObject body = new JSONObject(new String(err.networkResponse.data));
                            code = body.optString("code", "");
                            message = body.optString("message", message);
                        } catch (Exception ignored) { }
                    }

                    if ("PAYMENT_NOT_COMPLETED".equals(code)) {
                        // Payment state is still uncertain. Never release the doctor lock or invite a second payment.
                        bookingOperationInProgress = false;
                        paymentStarted = true;
                        setBookingUiState("PAYMENT", "Payment verification pending",
                                "Do not pay again. We are checking the same PhonePe payment.");
                        if (paymentReference != null && !paymentReference.isEmpty()) {
                            new Handler(getMainLooper()).postDelayed(
                                    () -> checkPhonePeStatus(paymentReference), 1500L);
                        } else {
                            setBillStatusAction(BillStatusAction.CHECK_PAYMENT, "Check payment", ppMerchantOrderId);
                        }
                        return;
                    }

                    boolean financialIntegrityIssue = "PHASE4C_MIGRATION_REQUIRED".equals(code)
                            || "QUOTE_CALCULATION_VERSION_INVALID".equals(code)
                            || "QUOTE_SETTLEMENT_MISMATCH".equals(code)
                            || "QUOTE_INTEGRITY_MISMATCH".equals(code)
                            || "PAYMENT_HISTORY_AUDIT_PREPARE_FAILED".equals(code)
                            || "PAYMENT_HISTORY_AUDIT_FAILED".equals(code);
                    boolean paidConflict = "DOCTOR_BUSY_AFTER_PAYMENT".equals(code)
                            || "DOCTOR_RESERVED_AFTER_PAYMENT".equals(code)
                            || "RESERVATION_UNAVAILABLE_AFTER_PAYMENT".equals(code)
                            || "PAYMENT_AMOUNT_MISMATCH".equals(code)
                            || ("Online".equals(selectedPaymentMethod) && financialIntegrityIssue);
                    if (paidConflict) {
                        bookingOperationInProgress = false;
                        paymentStarted = true;
                        setBookingUiState("FINALIZING", "Payment received — action required",
                                "Do not pay again. Your payment is recorded and this booking needs reconciliation.");
                        setBillStatusAction(BillStatusAction.VIEW_APPOINTMENTS, "Check ongoing appointments", "");
                        new AlertDialog.Builder(pending_bill.this)
                                .setTitle("Do not pay again")
                                .setMessage(message + "\n\nYour payment reference is already recorded. Check Ongoing Appointments first. If it still does not appear, contact support with the same payment reference.")
                                .setPositiveButton("Check appointments", (d, which) -> onBookingSuccess())
                                .setNegativeButton("Stay here", null)
                                .show();
                        return;
                    }

                    if ("OFFLINE_WALLET_REQUIRED".equals(code)
                            || "WALLET_BALANCE_CHANGED".equals(code)
                            || "QUOTE_EXPIRED".equals(code)
                            || "PINCODE_NOT_SERVED".equals(code)
                            || "DOCTOR_INACTIVE".equals(code)
                            || "DOCTOR_BUSY".equals(code)
                            || "RESERVATION_EXPIRED".equals(code)
                            || "RESERVATION_REQUIRED".equals(code)
                            || "ANIMAL_BREED_NOT_AVAILABLE".equals(code)
                            || "BOOKING_INTENT_INCOMPLETE".equals(code)
                            || ("Offline".equals(selectedPaymentMethod) && financialIntegrityIssue)) {
                        bookingOperationInProgress = false;
                        paymentStarted = false;
                        bookingQuoteToken = "";
                        activeQuote = null;
                        gatewayPaise = 0L;
                        lastConfirmedDepositMode = DepositMode.NONE;
                        clearPaymentRecovery();
                        setBookingUiState("ERROR", "Booking needs an update", message);
                        if ("DOCTOR_BUSY".equals(code) || "DOCTOR_INACTIVE".equals(code) || "PINCODE_NOT_SERVED".equals(code)
                                || "RESERVATION_EXPIRED".equals(code) || "RESERVATION_REQUIRED".equals(code)) {
                            releaseReservationLockIfSafe("finalize_business_rule_" + code);
                            setBillStatusAction(BillStatusAction.GO_BACK,
                                    "PINCODE_NOT_SERVED".equals(code) ? "Change location" : "Go back", "");
                        } else if ("ANIMAL_BREED_NOT_AVAILABLE".equals(code) || "BOOKING_INTENT_INCOMPLETE".equals(code)) {
                            setBillStatusAction(BillStatusAction.GO_BACK, "Review details", "");
                        } else {
                            setBillStatusAction(BillStatusAction.RETRY_QUOTE, "Verify latest bill", "");
                        }
                        fetchWalletBalance();
                        return;
                    }

                    // Ambiguous network/5xx response: the server may have committed already.
                    // Keep paymentStarted=true so onBack/onDestroy cannot release the reservation.
                    bookingOperationInProgress = false;
                    paymentStarted = true;
                    savePaymentRecovery(bookingQuoteToken, paymentReference);
                    setBookingUiState("FINALIZING", "Confirmation interrupted",
                            "Do not pay again. Retrying confirmation uses the same booking reference safely.");
                    setBillStatusAction(BillStatusAction.RETRY_FINALIZE, "Retry confirmation", paymentReference);
                    showSafeFinalizeRetryDialog(paymentReference, message);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("quote_token", bookingQuoteToken);
                p.put("patient_id", patientId == null ? "" : patientId);
                if (paymentReference != null && !paymentReference.isEmpty()) {
                    p.put("payment_reference", paymentReference);
                }
                return p;
            }
        };
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1.0f));
        VolleySingleton.getInstance(this).getRequestQueue().add(req);
    }

    private void showSafeFinalizeRetryDialog(String paymentReference, String detail) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Booking confirmation pending")
                .setMessage(detail + "\n\nDo not make another payment. Retrying only checks/finalizes the same booking reference.")
                .setNegativeButton("Check later", null)
                .setPositiveButton("Retry confirmation", (d, which) -> finalizeBooking(paymentReference))
                .show();
    }

    /* ---------------- Authoritative bill rendering ---------------- */


    @SuppressLint("SetTextI18n")
    private void renderAuthoritativeQuote() {
        BookingQuote q = activeQuote;
        boolean hasVerifiedQuote = q != null
                && bookingQuoteToken != null
                && bookingQuoteToken.equals(q.quoteToken)
                && lastConfirmedDepositMode != DepositMode.NONE;

        if (!hasVerifiedQuote) {
            disablePayButton();
            btnRechargeWallet.setVisibility(View.GONE);
            if (tvWalletBalance != null) tvWalletBalance.setText(MoneyUtil.formatPaise(walletBalancePaise));
            return;
        }

        // Phase 4C rule: render the immutable server quote only. No client-side billing math.
        platformChargePaise = q.platformChargePaise;
        gatewayPaise = q.gatewayPaise;

        boolean addVaccine = isVetCase == 1 && q.vaccinationPricePaise > 0L;
        setRowVisibility(rowVaccinationPrice, addVaccine);
        if (addVaccine && tvVaccinationPriceValue != null) {
            tvVaccinationPriceValue.setText(MoneyUtil.formatPaise(q.vaccinationPricePaise));
        }

        tvAppointmentCharge.setText(MoneyUtil.formatPaise(q.appointmentChargePaise));
        tvConsultingFeeValue.setText(MoneyUtil.formatPaise(q.consultationFeePaise));
        tvDistanceKmValue.setText(MoneyUtil.formatDistanceMeters(q.distanceMeters));
        tvGstValue.setText(MoneyUtil.formatPaise(q.gstPaise));
        tvDistanceChargeValue.setText(q.distanceChargePaise > 0L
                ? MoneyUtil.formatPaise(q.distanceChargePaise)
                : "Free under " + MoneyUtil.formatDistanceMeters(q.baseDistanceMeters));
        tvTotalPaidValue.setText(MoneyUtil.formatPaise(q.gatewayPaise));
        tvWalletBalance.setText(MoneyUtil.formatPaise(walletBalancePaise));
        if (tvPricePerKm != null) {
            tvPricePerKm.setText(MoneyUtil.formatPaise(q.extraCostPerKmPaise) + " per kilometer");
        }

        if (lastConfirmedDepositMode == DepositMode.WALLET) {
            showDepositRow("Platform Charge will be debited from wallet: " + MoneyUtil.formatPaise(q.platformChargePaise));
        } else {
            showDepositRow("Platform Charge included in online bill: " + MoneyUtil.formatPaise(q.platformChargePaise));
        }

        boolean offlineInsufficient = "Offline".equals(selectedPaymentMethod)
                && walletBalancePaise < q.platformChargePaise;
        if (offlineInsufficient) {
            btnRechargeWallet.setVisibility(View.VISIBLE);
            disablePayButton();
            setBookingUiState("ERROR", "Wallet balance changed",
                    "Offline booking needs " + MoneyUtil.formatPaise(q.platformChargePaise)
                            + " in your wallet. Recharge, then verify the bill again.");
            setBillStatusAction(BillStatusAction.RETRY_QUOTE, "Verify bill again", "");
        } else {
            btnRechargeWallet.setVisibility(View.GONE);
            loaderutil.hideLoader();
            if (!bookingOperationInProgress && !paymentStarted) enablePayButton();
            else disablePayButton();
        }

        boolean canUseOffline = platformChargePaise <= 0L || walletBalancePaise >= platformChargePaise;
        btnOfflinePayment.setAlpha(canUseOffline ? 1f : 0.7f);
        logBillingState("renderAuthoritativeQuote");
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
                            long oldPaise = walletBalancePaise;
                            Object rawBalance = obj.has("wallet_balance") ? obj.opt("wallet_balance") : 0;
                            walletBalancePaise = MoneyUtil.parseRupeesToPaise(rawBalance);
                            d("Wallet balance updated paise: " + oldPaise + " -> " + walletBalancePaise);
                            tvWalletBalance.setText(MoneyUtil.formatPaise(walletBalancePaise));

                            if (!paymentStarted && !bookingOperationInProgress
                                    && activeQuote != null
                                    && "Offline".equals(selectedPaymentMethod)
                                    && walletBalancePaise < activeQuote.platformChargePaise) {
                                // Do not let a stale offline quote proceed after the wallet fell below the locked platform charge.
                                bookingQuoteToken = "";
                                activeQuote = null;
                                gatewayPaise = 0L;
                                lastConfirmedDepositMode = DepositMode.NONE;
                                disablePayButton();
                                setBookingUiState("ERROR", "Wallet balance changed",
                                        "Your wallet no longer covers the platform charge. Recharge, then verify the bill again.");
                                setBillStatusAction(BillStatusAction.RETRY_QUOTE, "Verify bill again", "");
                            } else {
                                renderAuthoritativeQuote();
                            }
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
        VolleySingleton.getInstance(this).getRequestQueue().add(req);
    }

    /* Legacy client-side save_appointment flow removed in Phase 4C.
     * New builds finalize only through finalize_booking.php using the immutable quote token.
     */

    // ── Release reservation lock when user backs out of billing without paying ──
    @Override
    public void onBackPressed() {
        if (paymentStarted && !appointmentConfirmed) {
            boolean offline = "Offline".equals(selectedPaymentMethod);
            String message = offline
                    ? "Your wallet booking may already be confirming on the server. Do not create another booking or wallet charge for the same visit. If you leave, the same booking reference can be recovered when you return."
                    : "If PhonePe was opened or payment was completed, do not pay again. The same payment and booking reference can be recovered safely even if you leave this screen.";
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation is still in progress")
                    .setMessage(message)
                    .setNegativeButton("Stay here", null)
                    .setPositiveButton("Leave safely", (dlg, which) -> finish())
                    .show();
            return;
        }
        releaseReservationLockIfSafe("onBackPressed");
        super.onBackPressed();
    }


    /**
     * Safe, idempotent lock release.
     * Checks all safety flags before firing any network call.
     * Will NOT release if:
     *   - reservationToken or patientId is empty 
     *   - payment has already started (PhonePe or offline)
     *   - save_appointment.php call is currently in flight
     *   - appointment was already confirmed by server
     *   - release was already sent once
     */
    private void releaseReservationLockIfSafe(String reason) {
        if (reservationToken.isEmpty() || patientId.isEmpty()) return;
        if (paymentStarted)            { d("releaseReservationLockIfSafe blocked: paymentStarted [" + reason + "]");         return; }
        if (appointmentSaveInProgress) { d("releaseReservationLockIfSafe blocked: saveInProgress [" + reason + "]");          return; }
        if (appointmentConfirmed)      { d("releaseReservationLockIfSafe blocked: appointmentConfirmed [" + reason + "]");    return; }
        if (lockReleased)              { d("releaseReservationLockIfSafe blocked: alreadyReleased [" + reason + "]");          return; }

        lockReleased = true; // set before async call to prevent duplicate fires
        d("releaseReservationLockIfSafe: releasing lock, reason=" + reason);
        try {
            StringRequest rel = new StringRequest(
                    Request.Method.POST,
                    ApiConfig.RELEASE_DOCTOR_LOCK,
                    resp -> d("pending_bill lock released [" + reason + "]: " + resp),
                    err  -> w("pending_bill lock release failed (auto-expire ok) [" + reason + "]: " + err)
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> p = new HashMap<>();
                    p.put("reservation_token", reservationToken);
                    p.put("patient_id",        patientId);
                    return p;
                }
            };
            rel.setShouldCache(false);
            VolleySingleton.getInstance(this).getRequestQueue().add(rel);
        } catch (Exception e) {
            w("releaseReservationLockIfSafe exception [" + reason + "]: " + e.getMessage());
        }
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

// Last Updated: 2026-09-18 14:42 IST

// Last Updated: 2026-09-18 16:56 IST
