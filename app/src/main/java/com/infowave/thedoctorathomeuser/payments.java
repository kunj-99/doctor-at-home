package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.infowave.thedoctorathomeuser.adapter.TransactionAdapter;
import com.phonepe.intent.sdk.api.PhonePeKt;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class payments extends AppCompatActivity {

    private static final String TAG = "PHONEPE_LOG";

    private TextView tvWalletBalance;
    private TextView tvRechargeStatus;
    private Button btnRecharge; // hidden in XML; kept for ID stability
    private RecyclerView rvTransactions;

    // Quick recharge
    private Button btnRecharge50, btnRecharge100;
    private Button btnCheckRechargeStatus;

    // Segments
    private MaterialCardView cardCredit, cardDebit;

    // IDs kept but hidden
    private TextView tvCreditTotal, tvDebitTotal;

    private final List<TransactionAdapter.TransactionItem> transactionList = new ArrayList<>();
    private final List<TransactionAdapter.TransactionItem> allTransactions = new ArrayList<>();
    private TransactionAdapter adapter;

    // Endpoints
    private final String createOrderUrl      = ApiConfig.endpoint("phonepe/public/create_order.php");
    private final String statusUrl           = ApiConfig.endpoint("phonepe/public/check_status.php");
    private final String fetchBalanceUrl     = ApiConfig.endpoint("get_wallet_balance.php");
    private final String fetchTransactionUrl = ApiConfig.endpoint("fetch_wallet_transactions.php");

    private String patientId;
    private String merchantOrderId;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean awaitingSdkResult = false;
    private boolean paymentStatusCheckInFlight = false;
    private int statusPollAttempts = 0;
    private Runnable scheduledStatusPoll;
    private static final int MAX_STATUS_POLLS = 5;
    private ActivityResultLauncher<Intent> checkoutLauncher;

    private String activeFilter = "CREDIT";

    // ===== Recharge disable rule =====
    private static final double RECHARGE_DISABLE_THRESHOLD_RS = 400.0;
    private double currentWalletBalanceRs = 0.0;
    private boolean thresholdToastShown = false; // throttle the ≥₹400 toast once per session

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "=== onCreate: Payment Activity Started ===");
        setContentView(R.layout.activity_payments);

        // System UI
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        // Scrims
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim    = findViewById(R.id.navigation_bar_scrim);
        final ConstraintLayout root = findViewById(R.id.root_container);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });

        // Views
        tvWalletBalance = findViewById(R.id.tvTotalBalance);
        btnRecharge     = findViewById(R.id.btnRecharge);
        rvTransactions  = findViewById(R.id.rvTransactions);

        btnRecharge50   = findViewById(R.id.btnRecharge50);
        btnRecharge100  = findViewById(R.id.btnRecharge100);
        btnCheckRechargeStatus = findViewById(R.id.btnCheckRechargeStatus);
        tvRechargeStatus = findViewById(R.id.tvRechargeStatus);

        cardCredit      = findViewById(R.id.cardCredit);
        cardDebit       = findViewById(R.id.cardDebit);

        tvCreditTotal   = findViewById(R.id.tvEarned);
        tvDebitTotal    = findViewById(R.id.tvSpent);
        tvCreditTotal.setVisibility(View.GONE);
        tvDebitTotal.setVisibility(View.GONE);

        tvWalletBalance.setText("₹0.00");

        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new TransactionAdapter(transactionList, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        // Load initial data (show loader during parallel fetches)
        loaderutil.showLoader(this, "Loading wallet", "Getting your balance and recent transactions…");
        fetchWalletBalance();       // each call will hide loader on completion
        fetchTransactionHistory();  // each call will hide loader on completion

        // PhonePe launcher
        checkoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onCheckoutResult
        );

        // Quick recharge values (currently ₹1 and ₹2 for testing)
        // To switch to ₹50/₹100, change to 50*100 and 100*100.
        btnRecharge50.setOnClickListener(v -> {
            btnRecharge50.setEnabled(false);
            btnRecharge100.setEnabled(false);
            startRecharge(String.valueOf(50 * 100));   // <- change to 50*100 for ₹50
        });
        btnRecharge100.setOnClickListener(v -> {
            btnRecharge50.setEnabled(false);
            btnRecharge100.setEnabled(false);
            startRecharge(String.valueOf(100 * 100));   // <- change to 100*100 for ₹100
        });

        btnCheckRechargeStatus.setOnClickListener(v -> {
            if (merchantOrderId == null || merchantOrderId.trim().isEmpty()) {
                setRechargeStatus("No pending recharge was found.", false);
                return;
            }
            setRechargeStatus("Checking the same payment. Please do not start another recharge…", false);
            loaderutil.showLoader(this, "Checking payment", "Checking the same payment — please do not pay again.");
            checkPaymentStatus(merchantOrderId);
        });

        // Segmented partition behavior
        cardCredit.setOnClickListener(v -> {
            activeFilter = "CREDIT";
            styleActivePartition();
            applyFilter();
        });
        cardDebit.setOnClickListener(v -> {
            activeFilter = "DEBIT";
            styleActivePartition();
            applyFilter();
        });

        styleActivePartition();

        // Restore instance state (if activity recreated)
        if (savedInstanceState != null) {
            merchantOrderId = savedInstanceState.getString("merchantOrderId", merchantOrderId);
        }
        if (merchantOrderId != null && !merchantOrderId.trim().isEmpty()) {
            updateButtonsForPending(true);
            setRechargeStatus("A recharge is still being verified. Do not pay again.", true);
        }
    }

    /** Make the segments look like a professional toggle (no “card” feel). */
    private void styleActivePartition() {
        boolean creditActive = "CREDIT".equals(activeFilter);

        cardCredit.setCardElevation(0f);
        cardDebit.setCardElevation(0f);

        cardCredit.setStrokeWidth(creditActive ? 2 : 1);
        cardDebit.setStrokeWidth(creditActive ? 1 : 2);

        cardCredit.setCardBackgroundColor(creditActive ? Color.parseColor("#142196F3") : Color.TRANSPARENT);
        cardDebit.setCardBackgroundColor(!creditActive ? Color.parseColor("#14EF5350") : Color.TRANSPARENT);
    }

    /** Filter RecyclerView items based on active segment. */
    private void applyFilter() {
        transactionList.clear();
        for (TransactionAdapter.TransactionItem item : allTransactions) {
            String t = item.type == null ? "" : item.type.trim().toUpperCase();
            if ("CREDIT".equals(activeFilter) && "CREDIT".equals(t)) {
                transactionList.add(item);
            } else if ("DEBIT".equals(activeFilter) && "DEBIT".equals(t)) {
                transactionList.add(item);
            }
        }
        adapter.updateTransactions(transactionList);
    }

    private void onCheckoutResult(ActivityResult result) {
        // user returned from PhonePe
        if (merchantOrderId != null) {
            setRechargeStatus("Checking payment status. Please do not pay again…", false);
            loaderutil.showLoader(this, "Checking payment", "Confirming the result of the same recharge…");
            if (!awaitingSdkResult) {
                awaitingSdkResult = true;
                updateButtonsForPending(true); // keep disabled during polling
                checkPaymentStatusWithBackoff(merchantOrderId);
            } else {
                checkPaymentStatus(merchantOrderId);
            }
        }
        // DO NOT re-enable buttons here. They are re-enabled only after a terminal status.
    }

    private void startRecharge(String paiseAmount) {
        awaitingSdkResult = false;

        setRechargeStatus("Starting secure recharge…", false);
        loaderutil.showLoader(this, "Starting secure recharge", "Preparing the payment securely…");
        StringRequest request = new StringRequest(
                Request.Method.POST,
                createOrderUrl,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            setRechargeStatus("Could not start recharge. No money was charged. Please try again.", false);
                            Toast.makeText(this, "Could not start recharge. Please try again.", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                            loaderutil.hideLoader();
                            return;
                        }
                        merchantOrderId = obj.optString("merchantOrderId", null);
                        String token    = obj.optString("token", "");
                        String orderId  = obj.optString("orderId", "");
                        loaderutil.hideLoader();

                        if (token.isEmpty() || orderId.isEmpty()) {
                            setRechargeStatus("Could not start recharge. No money was charged. Please try again.", false);
                            Toast.makeText(this, "Invalid payment response. Please try again.", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                            return;
                        }
                        try {
                            PhonePeKt.startCheckoutPage(this, token, orderId, checkoutLauncher);
                        } catch (Throwable t) {
                            Log.e(TAG, "PhonePe launch error", t);
                            setRechargeStatus("PhonePe could not be opened. No recharge was started.", false);
                            Toast.makeText(this, "Unable to open PhonePe. Please try again.", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                        }
                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        setRechargeStatus("Could not read the payment response. No money was charged by this attempt.", false);
                        Toast.makeText(this, "Could not start recharge. Please try again.", Toast.LENGTH_SHORT).show();
                        btnRecharge50.setEnabled(true);
                        btnRecharge100.setEnabled(true);
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    setRechargeStatus("Could not contact the payment server. No money was charged by this attempt.", false);
                    Toast.makeText(this, "Network error while starting recharge.", Toast.LENGTH_SHORT).show();
                    btnRecharge50.setEnabled(true);
                    btnRecharge100.setEnabled(true);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                map.put("amount", paiseAmount);
                map.put("purpose", "WALLET_TOPUP");
                map.put("attemptId", "APP-" + System.currentTimeMillis());
                map.put("_ts", String.valueOf(System.currentTimeMillis())); // cache buster
                return map;
            }
        };
        request.setShouldCache(false);               // prevent Volley caching
        request.setRetryPolicy(ppRetry());           // robust retry
        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }

    private void checkPaymentStatusWithBackoff(String moid) {
        cancelScheduledStatusPoll();
        statusPollAttempts = 0;
        scheduleStatusPoll(moid, 1200L);
    }

    private void scheduleStatusPoll(String moid, long delayMs) {
        if (moid == null || moid.trim().isEmpty()) return;
        cancelScheduledStatusPoll();
        scheduledStatusPoll = () -> {
            scheduledStatusPoll = null;
            if (isFinishing() || isDestroyed()) return;
            if (merchantOrderId == null || !moid.equals(merchantOrderId)) return;
            if (paymentStatusCheckInFlight) {
                scheduleStatusPoll(moid, 1000L);
                return;
            }
            statusPollAttempts++;
            checkPaymentStatusInternal(moid, true);
        };
        mainHandler.postDelayed(scheduledStatusPoll, Math.max(0L, delayMs));
    }

    private void cancelScheduledStatusPoll() {
        if (scheduledStatusPoll != null) {
            mainHandler.removeCallbacks(scheduledStatusPoll);
            scheduledStatusPoll = null;
        }
    }

    private void checkPaymentStatus(String moid) {
        checkPaymentStatusInternal(moid, false);
    }

    private void checkPaymentStatusInternal(String moid, boolean continueBackoff) {
        if (moid == null || moid.trim().isEmpty()) return;
        if (paymentStatusCheckInFlight) {
            if (!continueBackoff) {
                setRechargeStatus("Already checking this payment. Please wait…", true);
            }
            return;
        }

        paymentStatusCheckInFlight = true;
        String url = statusUrl + "?merchantOrderId=" + Uri.encode(moid) + "&ts=" + System.currentTimeMillis();

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    paymentStatusCheckInFlight = false;
                    boolean shouldPollAgain = false;
                    try {
                        JSONObject obj = new JSONObject(response);
                        String apiStatus = obj.optString("status");
                        if (!"success".equalsIgnoreCase(apiStatus) && !"ok".equalsIgnoreCase(apiStatus)) {
                            setRechargeStatus("Payment status is not confirmed yet. Do not pay again; check the same payment status.", true);
                            Toast.makeText(this, "Payment status is not confirmed yet.", Toast.LENGTH_SHORT).show();
                            loaderutil.hideLoader();
                            shouldPollAgain = continueBackoff;
                        } else {
                            String state = obj.optString("state", "PENDING");
                            switch (state) {
                                case "COMPLETED":
                                    cancelScheduledStatusPoll();
                                    setRechargeStatus("Recharge successful. Your wallet balance is being updated.", false);
                                    Toast.makeText(this, "Recharge successful.", Toast.LENGTH_SHORT).show();
                                    fetchWalletBalance();
                                    fetchTransactionHistory();
                                    awaitingSdkResult = false;
                                    merchantOrderId = null;
                                    updateButtonsForPending(false);
                                    break;

                                case "FAILED":
                                    cancelScheduledStatusPoll();
                                    setRechargeStatus("Recharge failed. No wallet credit was added. You can try again.", false);
                                    Toast.makeText(this, "Recharge failed. You can try again.", Toast.LENGTH_SHORT).show();
                                    fetchWalletBalance();
                                    loaderutil.hideLoader();
                                    awaitingSdkResult = false;
                                    merchantOrderId = null;
                                    updateButtonsForPending(false);
                                    break;

                                case "CANCELLED":
                                case "TIMED_OUT":
                                    cancelScheduledStatusPoll();
                                    setRechargeStatus("Payment was " + state.toLowerCase() + ". No wallet credit was added. You can try again.", false);
                                    Toast.makeText(this, "Payment " + state.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                                    fetchWalletBalance();
                                    loaderutil.hideLoader();
                                    awaitingSdkResult = false;
                                    merchantOrderId = null;
                                    updateButtonsForPending(false);
                                    break;

                                default:
                                    loaderutil.hideLoader();
                                    updateButtonsForPending(true);
                                    setRechargeStatus("Payment is still pending. Do not start another recharge; check the same payment status.", true);
                                    shouldPollAgain = continueBackoff;
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        setRechargeStatus("Could not verify the payment yet. Do not pay again; check the same payment status.", true);
                        Toast.makeText(this, "Could not verify payment status yet.", Toast.LENGTH_SHORT).show();
                        shouldPollAgain = continueBackoff;
                    }

                    if (shouldPollAgain) {
                        if (statusPollAttempts < MAX_STATUS_POLLS && moid.equals(merchantOrderId)) {
                            scheduleStatusPoll(moid, Math.min(10000L, statusPollAttempts * 2000L));
                        } else {
                            awaitingSdkResult = false;
                            setRechargeStatus("Payment is not final yet. Use Check Payment Status for this same payment — do not pay again.", true);
                        }
                    }
                },
                error -> {
                    paymentStatusCheckInFlight = false;
                    loaderutil.hideLoader();
                    setRechargeStatus("Could not check payment right now. Do not pay again; use Check Payment Status.", true);
                    Toast.makeText(this, "Could not check payment status. Please try again.", Toast.LENGTH_SHORT).show();

                    if (continueBackoff && statusPollAttempts < MAX_STATUS_POLLS && moid.equals(merchantOrderId)) {
                        scheduleStatusPoll(moid, Math.min(10000L, statusPollAttempts * 2000L));
                    } else if (continueBackoff) {
                        awaitingSdkResult = false;
                    }
                }
        );
        request.setShouldCache(false);
        request.setRetryPolicy(VolleySingleton.policy(VolleySingleton.Profile.BACKGROUND));
        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }

    private void fetchWalletBalance() {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                fetchBalanceUrl,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equalsIgnoreCase(obj.optString("status"))) {
                            String balanceStr = obj.optString("wallet_balance", "0.00");
                            tvWalletBalance.setText("₹" + balanceStr);
                            updateRechargeControlsForBalance(parseAmountOrZero(balanceStr));
                        } else {
                            tvWalletBalance.setText("₹0.00");
                            updateRechargeControlsForBalance(0.0);
                        }
                    } catch (Exception ignored) {
                        // keep previous button state
                    } finally {
                        loaderutil.hideLoader();
                    }
                },
                error -> {
                    // keep previous button state
                    loaderutil.hideLoader();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                map.put("_ts", String.valueOf(System.currentTimeMillis())); // cache buster
                return map;
            }
        };
        request.setShouldCache(false);
        request.setRetryPolicy(ppRetry());
        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }

    private void fetchTransactionHistory() {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                fetchTransactionUrl,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equalsIgnoreCase(obj.optString("status"))) {
                            List<TransactionAdapter.TransactionItem> temp = new ArrayList<>();
                            JSONArray arr = obj.getJSONArray("data");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject it = arr.getJSONObject(i);
                                temp.add(new TransactionAdapter.TransactionItem(
                                        it.getString("amount"),
                                        it.getString("type"),
                                        it.optString("reason", ""),
                                        it.getString("timestamp")
                                ));
                            }
                            allTransactions.clear();
                            allTransactions.addAll(temp);
                            applyFilter(); // show current partition
                        } else {
                            allTransactions.clear();
                            transactionList.clear();
                            adapter.updateTransactions(transactionList);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        loaderutil.hideLoader();
                    }
                },
                error -> loaderutil.hideLoader()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                map.put("_ts", String.valueOf(System.currentTimeMillis())); // cache buster
                return map;
            }
        };
        request.setShouldCache(false);
        request.setRetryPolicy(ppRetry());
        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }

    /* ===== Helpers for the recharge disable rule ===== */

    private static double parseAmountOrZero(String s) {
        if (s == null) return 0.0;
        try { return Double.parseDouble(s.trim()); } catch (Exception ignored) { return 0.0; }
    }

    private void updateRechargeControlsForBalance(double balanceRs) {
        currentWalletBalanceRs = balanceRs;

        boolean shouldDisable = balanceRs >= RECHARGE_DISABLE_THRESHOLD_RS;

        btnRecharge50.setEnabled(!shouldDisable);
        btnRecharge100.setEnabled(!shouldDisable);

        float alpha = shouldDisable ? 0.5f : 1.0f;
        btnRecharge50.setAlpha(alpha);
        btnRecharge100.setAlpha(alpha);

        if (shouldDisable && !thresholdToastShown) {
            thresholdToastShown = true;
            Toast.makeText(this, "Wallet ≥ ₹400 — recharge disabled.", Toast.LENGTH_SHORT).show();
        }
        if (!shouldDisable) {
            // allow toast again next time threshold is crossed
            thresholdToastShown = false;
        }
    }

    /** Centralized control when a payment is pending vs. finalized */
    private void updateButtonsForPending(boolean isPending) {
        btnRecharge50.setEnabled(!isPending);
        btnRecharge100.setEnabled(!isPending);
        btnRecharge50.setAlpha(isPending ? 0.5f : 1.0f);
        btnRecharge100.setAlpha(isPending ? 0.5f : 1.0f);
    }

    private void setRechargeStatus(String message, boolean showCheckButton) {
        if (tvRechargeStatus != null) {
            tvRechargeStatus.setText(message == null ? "" : message);
            tvRechargeStatus.setVisibility((message == null || message.trim().isEmpty()) ? View.GONE : View.VISIBLE);
        }
        if (btnCheckRechargeStatus != null) {
            btnCheckRechargeStatus.setVisibility(showCheckButton ? View.VISIBLE : View.GONE);
            btnCheckRechargeStatus.setEnabled(showCheckButton);
        }
    }

    @Override
    protected void onDestroy() {
        cancelScheduledStatusPoll();
        paymentStatusCheckInFlight = false;
        super.onDestroy();
    }

    // ===== Persist important state across rotation/process death =====
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("merchantOrderId", merchantOrderId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        merchantOrderId = savedInstanceState.getString("merchantOrderId", merchantOrderId);
    }

    // ===== Volley retry policy for PG calls =====
    private static DefaultRetryPolicy ppRetry() {
        return new DefaultRetryPolicy(
                15000, // 15s timeout
                1,     // 1 retry
                1.5f   // backoff multiplier
        );
    }
}

// Last Updated: 2026-09-18 15:21 IST
