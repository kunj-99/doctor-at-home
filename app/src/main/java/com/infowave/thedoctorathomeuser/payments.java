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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class payments extends AppCompatActivity {

    private static final String TAG = "PHONEPE_LOG";

    private TextView tvWalletBalance;
    private Button btnRecharge; // hidden in XML; kept for ID stability
    private RecyclerView rvTransactions;

    // Quick recharge
    private Button btnRecharge50, btnRecharge100;

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
        loaderutil.showLoader(this);
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
            loaderutil.showLoader(this);
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

        loaderutil.showLoader(this);
        StringRequest request = new StringRequest(
                Request.Method.POST,
                createOrderUrl,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            Toast.makeText(this, "Create order failed: " + obj.optString("message", ""), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, "Invalid order response", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                            return;
                        }
                        try {
                            PhonePeKt.startCheckoutPage(this, token, orderId, checkoutLauncher);
                        } catch (Throwable t) {
                            Log.e(TAG, "PhonePe launch error", t);
                            Toast.makeText(this, "Unable to open PhonePe UI", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                        }
                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Create order parse error", Toast.LENGTH_SHORT).show();
                        btnRecharge50.setEnabled(true);
                        btnRecharge100.setEnabled(true);
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Network error creating order", Toast.LENGTH_SHORT).show();
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
        Volley.newRequestQueue(this).add(request);
    }

    private void checkPaymentStatusWithBackoff(String moid) {
        final int[] attempts = {0};
        final Runnable poll = new Runnable() {
            @Override public void run() {
                attempts[0]++;
                checkPaymentStatus(moid);
                if (attempts[0] < 5) {
                    int next = attempts[0] * 2000; // 2s, 4s, 6s, 8s, 10s
                    mainHandler.postDelayed(this, next);
                } else {
                    awaitingSdkResult = false;
                }
            }
        };
        // Small initial delay so PG can settle
        mainHandler.postDelayed(poll, 1200);
    }

    private void checkPaymentStatus(String moid) {
        // cache-buster to avoid any intermediary caching
        String url = statusUrl + "?merchantOrderId=" + Uri.encode(moid) + "&ts=" + System.currentTimeMillis();

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        String apiStatus = obj.optString("status");
                        if (!"success".equalsIgnoreCase(apiStatus) && !"ok".equalsIgnoreCase(apiStatus)) {
                            Toast.makeText(this, "Status check failed: " + obj.optString("message", ""), Toast.LENGTH_SHORT).show();
                            loaderutil.hideLoader();
                            return;
                        }

                        String state = obj.optString("state", "PENDING");
                        // Do NOT trust wallet_balance from status; fetch separately.

                        switch (state) {
                            case "COMPLETED":
                                Toast.makeText(this, "Recharge successful!", Toast.LENGTH_SHORT).show();
                                fetchWalletBalance();       // authoritative: patients.wallet_balance
                                fetchTransactionHistory();
                                awaitingSdkResult = false;
                                merchantOrderId = null;     // stop further polls
                                updateButtonsForPending(false); // re-enable (threshold rule will apply on balance load)
                                break;

                            case "FAILED":
                                Toast.makeText(this, "Recharge failed", Toast.LENGTH_SHORT).show();
                                fetchWalletBalance();
                                loaderutil.hideLoader();
                                awaitingSdkResult = false;
                                merchantOrderId = null;
                                updateButtonsForPending(false);
                                break;

                            case "CANCELLED":
                            case "TIMED_OUT":
                                Toast.makeText(this, "Payment " + state.toLowerCase(), Toast.LENGTH_SHORT).show();
                                fetchWalletBalance();
                                loaderutil.hideLoader();
                                awaitingSdkResult = false;
                                merchantOrderId = null;
                                updateButtonsForPending(false);
                                break;

                            default:
                                // PENDING: keep UI responsive but keep buttons disabled to avoid duplicate topups
                                loaderutil.hideLoader();
                                updateButtonsForPending(true);
                        }
                    } catch (Exception e) {
                        loaderutil.hideLoader();
                        Toast.makeText(this, "Status parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Network error in status check", Toast.LENGTH_SHORT).show();
                }
        );
        request.setShouldCache(false);
        request.setRetryPolicy(ppRetry());
        Volley.newRequestQueue(this).add(request);
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
        Volley.newRequestQueue(this).add(request);
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
        Volley.newRequestQueue(this).add(request);
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
