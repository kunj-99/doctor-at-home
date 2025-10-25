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

    // Segments (same IDs as before; just visually segmented)
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

        // Load initial data
        fetchWalletBalance();
        fetchTransactionHistory();

        // PhonePe launcher
        checkoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onCheckoutResult
        );

        // Quick recharge
        btnRecharge50.setOnClickListener(v -> {
            btnRecharge50.setEnabled(false);
            btnRecharge100.setEnabled(false);
            startRecharge(String.valueOf(50 * 100));
        });
        btnRecharge100.setOnClickListener(v -> {
            btnRecharge50.setEnabled(false);
            btnRecharge100.setEnabled(false);
            startRecharge(String.valueOf(100 * 100));
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
    }

    /** Make the segments look like a professional toggle (no “card” feel). */
    private void styleActivePartition() {
        // Credit active?
        boolean creditActive = "CREDIT".equals(activeFilter);

        // Visuals: light fill for active, transparent for inactive; clear strokes
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
        if (merchantOrderId != null) {
            if (!awaitingSdkResult) {
                awaitingSdkResult = true;
                checkPaymentStatusWithBackoff(merchantOrderId);
            } else {
                checkPaymentStatus(merchantOrderId);
            }
        }
        btnRecharge50.setEnabled(true);
        btnRecharge100.setEnabled(true);
    }

    private void startRecharge(String paiseAmount) {
        btnRecharge50.setEnabled(false);
        btnRecharge100.setEnabled(false);
        awaitingSdkResult = false;

        final String attemptId = "APP-" + System.currentTimeMillis();
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
                            return;
                        }
                        merchantOrderId = obj.optString("merchantOrderId", null);
                        String token    = obj.optString("token", "");
                        String orderId  = obj.optString("orderId", "");
                        if (token.isEmpty() || orderId.isEmpty()) {
                            Toast.makeText(this, "Invalid order response", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                            return;
                        }
                        try {
                            PhonePeKt.startCheckoutPage(this, token, orderId, checkoutLauncher);
                        } catch (Throwable t) {
                            Toast.makeText(this, "Unable to open PhonePe UI", Toast.LENGTH_SHORT).show();
                            btnRecharge50.setEnabled(true);
                            btnRecharge100.setEnabled(true);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Create order parse error", Toast.LENGTH_SHORT).show();
                        btnRecharge50.setEnabled(true);
                        btnRecharge100.setEnabled(true);
                    }
                },
                error -> {
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
                map.put("attemptId", attemptId);
                return map;
            }
        };
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
        mainHandler.post(poll);
    }

    private void checkPaymentStatus(String moid) {
        String url = statusUrl + "?merchantOrderId=" + Uri.encode(moid);
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        String apiStatus = obj.optString("status");
                        if (!"success".equalsIgnoreCase(apiStatus) && !"ok".equalsIgnoreCase(apiStatus)) {
                            Toast.makeText(this, "Status check failed: " + obj.optString("message", ""), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String state   = obj.optString("state", "PENDING");
                        String balance = obj.optString("wallet_balance", "0.00");
                        tvWalletBalance.setText("₹" + balance);

                        switch (state) {
                            case "COMPLETED":
                                Toast.makeText(this, "Recharge successful!", Toast.LENGTH_SHORT).show();
                                fetchTransactionHistory();
                                awaitingSdkResult = false;
                                break;
                            case "FAILED":
                                Toast.makeText(this, "Recharge failed", Toast.LENGTH_SHORT).show();
                                awaitingSdkResult = false;
                                break;
                            case "CANCELLED":
                            case "TIMED_OUT":
                                Toast.makeText(this, "Payment " + state.toLowerCase(), Toast.LENGTH_SHORT).show();
                                awaitingSdkResult = false;
                                break;
                            default:
                                // pending
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Status parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error in status check", Toast.LENGTH_SHORT).show()
        );
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
                            String balance = obj.optString("wallet_balance", "0.00");
                            tvWalletBalance.setText("₹" + balance);
                        } else {
                            tvWalletBalance.setText("₹0.00");
                        }
                    } catch (Exception ignored) {}
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                return map;
            }
        };
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
                    } catch (Exception ignored) {}
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                return map;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }
}
