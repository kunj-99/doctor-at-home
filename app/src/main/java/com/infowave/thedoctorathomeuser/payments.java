package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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
    private Button btnRecharge;
    private RecyclerView rvTransactions;

    private final List<TransactionAdapter.TransactionItem> transactionList = new ArrayList<>();
    private TransactionAdapter adapter;

    // API endpoints (production server paths)
    private final String createOrderUrl      = ApiConfig.endpoint("phonepe/public/create_order.php");
    private final String statusUrl           = ApiConfig.endpoint("phonepe/public/check_status.php");
    private final String fetchBalanceUrl     = ApiConfig.endpoint("get_wallet_balance.php");
    private final String fetchTransactionUrl = ApiConfig.endpoint("fetch_wallet_transactions.php");

    private String patientId;
    private String merchantOrderId;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean awaitingSdkResult = false;

    private ActivityResultLauncher<Intent> checkoutLauncher;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "=== onCreate: Payment Activity Started ===");
        setContentView(R.layout.activity_payments);

        // System UI adjustments
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        // Logging system bar scrims
        final android.view.View statusScrim = findViewById(R.id.status_bar_scrim);
        final android.view.View navScrim    = findViewById(R.id.navigation_bar_scrim);
        final ConstraintLayout root         = findViewById(R.id.root_container);
        Log.d(TAG, "Scrim views: statusScrim=" + statusScrim + ", navScrim=" + navScrim);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            if (navScrim != null) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            Log.d(TAG, "Applied system insets: top=" + sys.top + ", bottom=" + sys.bottom);
            return insets;
        });

        tvWalletBalance = findViewById(R.id.tvTotalBalance);
        btnRecharge = findViewById(R.id.btnRecharge);
        rvTransactions = findViewById(R.id.rvTransactions);

        tvWalletBalance.setText("₹0.00");

        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        Log.d(TAG, "[INIT] patientId loaded from prefs: " + patientId);

        if (patientId == null || patientId.isEmpty()) {
            Log.e(TAG, "[INIT] Patient ID is null or empty! Finishing Activity.");
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new TransactionAdapter(transactionList, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        Log.d(TAG, "[INIT] Endpoints - createOrderUrl: " + createOrderUrl
                + ", statusUrl: " + statusUrl
                + ", fetchBalanceUrl: " + fetchBalanceUrl
                + ", fetchTransactionUrl: " + fetchTransactionUrl);

        fetchWalletBalance();
        fetchTransactionHistory();

        checkoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onCheckoutResult
        );

        btnRecharge.setOnClickListener(v -> showRechargeDialog());
    }

    private void onCheckoutResult(ActivityResult result) {
        Log.d(TAG, "[CHECKOUT RESULT] Returned from PhonePe: resultCode=" + result.getResultCode()
                + ", merchantOrderId=" + merchantOrderId + ", awaitingSdkResult=" + awaitingSdkResult);
        if (merchantOrderId != null) {
            if (!awaitingSdkResult) {
                awaitingSdkResult = true;
                checkPaymentStatusWithBackoff(merchantOrderId);
            } else {
                checkPaymentStatus(merchantOrderId);
            }
        } else {
            Log.e(TAG, "[CHECKOUT RESULT] merchantOrderId is null after SDK return");
        }
        btnRecharge.setEnabled(true);
    }

    private void showRechargeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recharge Wallet");

        final EditText input = new EditText(this);
        input.setHint("Enter amount in ₹ (e.g. 150.50)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Recharge", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            Log.d(TAG, "[DIALOG] User entered amount: " + amountStr);
            if (!amountStr.isEmpty()) {
                try {
                    double rupees = Double.parseDouble(amountStr);
                    int paise = (int) Math.round(rupees * 100); // convert to paise
                    Log.d(TAG, "[DIALOG] Parsed rupees=" + rupees + ", paise=" + paise);
                    if (paise <= 0) {
                        Toast.makeText(this, "Amount must be greater than zero!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startRecharge(String.valueOf(paise)); // send paise
                } catch (Exception e) {
                    Log.e(TAG, "[DIALOG] Exception while parsing amount: " + e.getMessage(), e);
                    Toast.makeText(this, "Invalid amount!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "[DIALOG] User did not enter amount");
                Toast.makeText(this, "Amount required!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "[DIALOG] Recharge dialog cancelled by user");
            dialog.cancel();
        });
        builder.show();
    }

    private void startRecharge(String paiseAmount) {
        btnRecharge.setEnabled(false);
        awaitingSdkResult = false;

        // NEW: generate correlation id for this attempt
        final String attemptId = "APP-" + System.currentTimeMillis();
        Log.d(TAG, "[ORDER] startRecharge() | attemptId=" + attemptId
                + " | patientId=" + patientId + " | paiseAmount=" + paiseAmount
                + " | endpoint=" + createOrderUrl);

        StringRequest request = new StringRequest(
                Request.Method.POST,
                createOrderUrl,
                response -> {
                    Log.d(TAG, "[ORDER] createOrder response (attemptId=" + attemptId + "): " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        Log.d(TAG, "[ORDER] createOrder JSON: " + obj);

                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            Log.e(TAG, "[ORDER] createOrder failed (attemptId=" + attemptId + "): " + obj);
                            Toast.makeText(this, "Create order failed: " + obj.optString("message", ""), Toast.LENGTH_SHORT).show();
                            btnRecharge.setEnabled(true);
                            return;
                        }

                        merchantOrderId = obj.optString("merchantOrderId", null);
                        String token    = obj.optString("token", "");
                        String orderId  = obj.optString("orderId", "");
                        String env      = obj.optString("env", "SANDBOX");

                        Log.d(TAG, "[ORDER] Parsed (attemptId=" + attemptId + "): merchantOrderId=" + merchantOrderId
                                + ", token=" + token + ", orderId=" + orderId + ", env=" + env);

                        if (token.isEmpty() || orderId.isEmpty()) {
                            Log.e(TAG, "[ORDER] Missing token/orderId (attemptId=" + attemptId + "): " + obj);
                            Toast.makeText(this, "Invalid order response", Toast.LENGTH_SHORT).show();
                            btnRecharge.setEnabled(true);
                            return;
                        }

                        try {
                            Log.d(TAG, "[ORDER] Launching PhonePe (attemptId=" + attemptId + "): token=" + token + ", orderId=" + orderId + ", env=" + env);
                            PhonePeKt.startCheckoutPage(this, token, orderId, checkoutLauncher);
                            Log.d(TAG, "[ORDER] PhonePe Standard Checkout launched (attemptId=" + attemptId + ")");
                        } catch (Throwable t) {
                            Log.e(TAG, "[ORDER] startCheckoutPage failed (attemptId=" + attemptId + ")", t);
                            Toast.makeText(this, "Unable to open PhonePe UI", Toast.LENGTH_SHORT).show();
                            btnRecharge.setEnabled(true);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "[ORDER] createOrder parse error (attemptId=" + attemptId + ")", e);
                        Toast.makeText(this, "Create order parse error", Toast.LENGTH_SHORT).show();
                        btnRecharge.setEnabled(true);
                    }
                },
                error -> {
                    String msg = (error == null || error.getMessage() == null) ? "unknown" : error.getMessage();
                    Log.e(TAG, "[ORDER] createOrder network error (attemptId=" + attemptId + "): " + msg, error);
                    Toast.makeText(this, "Network error creating order", Toast.LENGTH_SHORT).show();
                    btnRecharge.setEnabled(true);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                map.put("amount", paiseAmount);      // integer paise
                map.put("purpose", "WALLET_TOPUP");
                map.put("attemptId", attemptId);     // correlation id sent to server
                Log.d(TAG, "[ORDER] Request parameters (attemptId=" + attemptId + "): " + map);
                return map;
            }
        };

        Log.d(TAG, "[ORDER] Adding createOrder request to queue (attemptId=" + attemptId + ")");
        Volley.newRequestQueue(this).add(request);
    }

    private void checkPaymentStatusWithBackoff(String moid) {
        Log.d(TAG, "[STATUS] checkPaymentStatusWithBackoff() for merchantOrderId=" + moid);
        final int[] attempts = {0};
        final Runnable poll = new Runnable() {
            @Override public void run() {
                attempts[0]++;
                Log.d(TAG, "[STATUS] Poll attempt #" + attempts[0] + " | merchantOrderId=" + moid);
                checkPaymentStatus(moid);
                if (attempts[0] < 5) {
                    int next = attempts[0] * 2000; // 2s, 4s, 6s, 8s, 10s
                    mainHandler.postDelayed(this, next);
                } else {
                    Log.d(TAG, "[STATUS] Max poll attempts reached");
                    awaitingSdkResult = false;
                }
            }
        };
        mainHandler.post(poll);
    }

    private void checkPaymentStatus(String moid) {
        String url = statusUrl + "?merchantOrderId=" + Uri.encode(moid);
        Log.d(TAG, "[STATUS] checkPaymentStatus() URL: " + url);
        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    Log.d(TAG, "[STATUS] checkStatus response: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        Log.d(TAG, "[STATUS] checkStatus JSON: " + obj);

                        // ---- ONLY CHANGE WE'RE MAKING: accept both "ok" and "success"
                        String apiStatus = obj.optString("status");
                        if (!"success".equalsIgnoreCase(apiStatus) && !"ok".equalsIgnoreCase(apiStatus)) {
                            Log.e(TAG, "[STATUS] Status check failed: " + obj);
                            Toast.makeText(this, "Status check failed: " + obj.optString("message", ""), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String state   = obj.optString("state", "PENDING");
                        String balance = obj.optString("wallet_balance", "0.00");
                        tvWalletBalance.setText("₹" + balance);

                        Log.d(TAG, "[STATUS] Payment state=" + state + ", Wallet balance=" + balance);

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
                                Log.d(TAG, "[STATUS] Payment pending…");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[STATUS] status parse error", e);
                        Toast.makeText(this, "Status parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "[STATUS] Network error in checkStatus: " + error);
                    Toast.makeText(this, "Network error in status check", Toast.LENGTH_SHORT).show();
                }
        );

        Log.d(TAG, "[STATUS] Adding checkStatus request to queue | merchantOrderId=" + moid);
        Volley.newRequestQueue(this).add(request);
    }

    private void fetchWalletBalance() {
        Log.d(TAG, "[BALANCE] fetchWalletBalance() | patientId=" + patientId);
        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(
                Request.Method.POST,
                fetchBalanceUrl,
                response -> {
                    Log.d(TAG, "[BALANCE] response: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        Log.d(TAG, "[BALANCE] JSON: " + obj);
                        if ("success".equalsIgnoreCase(obj.optString("status"))) {
                            String balance = obj.optString("wallet_balance", "0.00");
                            tvWalletBalance.setText("₹" + balance);
                        } else {
                            tvWalletBalance.setText("₹0.00");
                            Log.e(TAG, "[BALANCE] Fetch balance failed: " + obj);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[BALANCE] parse error", e);
                    }
                },
                error -> Log.e(TAG, "[BALANCE] Network error: " + error)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                Log.d(TAG, "[BALANCE] params: " + map);
                return map;
            }
        };

        Log.d(TAG, "[BALANCE] Enqueue request");
        Volley.newRequestQueue(this).add(request);
    }

    private void fetchTransactionHistory() {
        Log.d(TAG, "[TXN] fetchTransactionHistory() | patientId=" + patientId);
        StringRequest request = new StringRequest(
                Request.Method.POST,
                fetchTransactionUrl,
                response -> {
                    Log.d(TAG, "[TXN] response: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        Log.d(TAG, "[TXN] JSON: " + obj);
                        if ("success".equalsIgnoreCase(obj.optString("status"))) {
                            List<TransactionAdapter.TransactionItem> tempList = new ArrayList<>();
                            JSONArray arr = obj.getJSONArray("data");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject item = arr.getJSONObject(i);
                                tempList.add(new TransactionAdapter.TransactionItem(
                                        item.getString("amount"),
                                        item.getString("type"),
                                        item.optString("reason", ""),
                                        item.getString("timestamp")
                                ));
                            }
                            Log.d(TAG, "[TXN] Transaction count=" + tempList.size());
                            adapter.updateTransactions(tempList);
                        } else {
                            Log.e(TAG, "[TXN] Failed: " + obj);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[TXN] parse error", e);
                    }
                },
                error -> Log.e(TAG, "[TXN] Network error: " + error)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                Log.d(TAG, "[TXN] params: " + map);
                return map;
            }
        };

        Log.d(TAG, "[TXN] Enqueue request");
        Volley.newRequestQueue(this).add(request);
    }
}
