package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

/** Wallet screen: creates SDK order, launches PhonePe UI, polls status (SANDBOX). */
public class payments extends AppCompatActivity {

    private static final String TAG = "PHONEPE";

    private TextView tvWalletBalance;
    private Button btnRecharge;
    private RecyclerView rvTransactions;

    private final List<TransactionAdapter.TransactionItem> transactionList = new ArrayList<>();
    private TransactionAdapter adapter;

    // API endpoints
    private final String createOrderUrl      = ApiConfig.endpoint("create_order.php");
    private final String statusUrl           = ApiConfig.endpoint("check_status.php");
    private final String fetchBalanceUrl     = ApiConfig.endpoint("get_wallet_balance.php");
    private final String fetchTransactionUrl = ApiConfig.endpoint("fetch_wallet_transactions.php");

    private String patientId;
    private String merchantOrderId;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean awaitingSdkResult = false;

    // Launcher for PhonePe checkout result
    private ActivityResultLauncher<Intent> checkoutLauncher;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        tvWalletBalance = findViewById(R.id.tvTotalBalance);
        btnRecharge = findViewById(R.id.btnRecharge);
        rvTransactions = findViewById(R.id.rvTransactions);

        tvWalletBalance.setText("₹0.00");

        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        Log.d(TAG, "Loaded patient_id: " + patientId);

        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new TransactionAdapter(transactionList, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        fetchWalletBalance();
        fetchTransactionHistory();

        checkoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onCheckoutResult
        );

        btnRecharge.setOnClickListener(v -> showRechargeDialog());
    }

    private void onCheckoutResult(ActivityResult result) {
        // Don’t trust result codes; always check server
        Log.d(TAG, "Returned from PhonePe (resultCode=" + result.getResultCode() + ")");
        if (merchantOrderId != null) {
            // prevent rapid double-polling if activity is recreated
            if (!awaitingSdkResult) {
                awaitingSdkResult = true;
                checkPaymentStatusWithBackoff(merchantOrderId);
            } else {
                checkPaymentStatus(merchantOrderId);
            }
        } else {
            Log.w(TAG, "Returned from PhonePe but merchantOrderId is null");
        }
        // allow new recharges after we started polling
        btnRecharge.setEnabled(true);
    }

    private void showRechargeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recharge Wallet");

        final EditText input = new EditText(this);
        input.setHint("Enter amount");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Recharge", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                startRecharge(amountStr);
            } else {
                Toast.makeText(this, "Amount required!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void startRecharge(String amount) {
        btnRecharge.setEnabled(false);
        awaitingSdkResult = false;

        StringRequest request = new StringRequest(
                Request.Method.POST,
                createOrderUrl,
                response -> {
                    try {
                        Log.d(TAG, "create_order response: " + response);
                        JSONObject obj = new JSONObject(response);

                        if (!"success".equalsIgnoreCase(obj.optString("status"))) {
                            Log.e(TAG, "create_order failed: " + obj);
                            Toast.makeText(this, "Create order failed", Toast.LENGTH_SHORT).show();
                            btnRecharge.setEnabled(true);
                            return;
                        }

                        merchantOrderId = obj.optString("merchantOrderId", null);
                        String token    = obj.optString("token", "");
                        String orderId  = obj.optString("orderId", "");
                        String env      = obj.optString("env", "SANDBOX");

                        if (token.isEmpty() || orderId.isEmpty()) {
                            Log.e(TAG, "Missing token/orderId in response: " + obj);
                            Toast.makeText(this, "Invalid order response", Toast.LENGTH_SHORT).show();
                            btnRecharge.setEnabled(true);
                            return;
                        }

                        try {
                            // Standard Checkout
                            PhonePeKt.startCheckoutPage(this, token, orderId, checkoutLauncher);
                            Log.d(TAG, "Launched Standard Checkout: orderId=" + orderId + ", env=" + env);
                        } catch (Throwable t) {
                            Log.e(TAG, "startCheckoutPage failed", t);
                            Toast.makeText(this, "Unable to open PhonePe UI", Toast.LENGTH_SHORT).show();
                            btnRecharge.setEnabled(true);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "create_order parse error", e);
                        Toast.makeText(this, "Create order parse error", Toast.LENGTH_SHORT).show();
                        btnRecharge.setEnabled(true);
                    }
                },
                error -> {
                    String msg = (error == null || error.getMessage() == null) ? "unknown" : error.getMessage();
                    Log.e(TAG, "create_order network error: " + msg, error);
                    Toast.makeText(this, "Network error creating order", Toast.LENGTH_SHORT).show();
                    btnRecharge.setEnabled(true);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", patientId);
                map.put("amount", amount);
                map.put("purpose", "WALLET_TOPUP"); // explicit for clarity
                return map;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void checkPaymentStatusWithBackoff(String moid) {
        // Poll up to ~5 times with 2s -> 4s -> 6s -> 8s -> 10s
        final int[] attempts = {0};
        final Runnable poll = new Runnable() {
            @Override public void run() {
                attempts[0]++;
                checkPaymentStatus(moid);
                if (attempts[0] < 5) {
                    int next = attempts[0] * 2000; // 2s,4s,6s,8s,10s
                    mainHandler.postDelayed(this, next);
                } else {
                    awaitingSdkResult = false;
                }
            }
        };
        mainHandler.post(poll);
    }

    private void checkPaymentStatus(String moid) {
        String url = statusUrl + "?merchantOrderId=" + moid;
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    Log.d(TAG, "check_status response: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!"success".equals(obj.optString("status"))) {
                            Log.e(TAG, "Status check failed: " + obj);
                            Toast.makeText(this, "Status check failed", Toast.LENGTH_SHORT).show();
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
                                // PENDING – keep polling via backoff loop
                                Log.d(TAG, "Payment pending…");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "status parse error", e);
                        Toast.makeText(this, "Status parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Network error in check_status: " + error);
                    Toast.makeText(this, "Network error in status check", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void fetchWalletBalance() {
        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(
                Request.Method.POST,
                fetchBalanceUrl,
                response -> {
                    Log.d(TAG, "fetch_balance response: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) {
                            String balance = obj.optString("wallet_balance", "0.00");
                            tvWalletBalance.setText("₹" + balance);
                        } else {
                            tvWalletBalance.setText("₹0.00");
                            Log.e(TAG, "Fetch balance failed: " + obj);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "balance parse error", e);
                    }
                },
                error -> Log.e(TAG, "Network error in fetch_balance: " + error)
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
                    Log.d(TAG, "txn_history response: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) {
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
                            adapter.updateTransactions(tempList);
                        } else {
                            Log.e(TAG, "Transaction history failed: " + obj);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "txn history parse error", e);
                    }
                },
                error -> Log.e(TAG, "Network error in txn_history: " + error)
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
