package com.infowave.thedoctorathomeuser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.TransactionAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.*;

public class payments extends AppCompatActivity {

    // UI Components
    private TextView tvTotalBalance, tvEarned, tvSpent;
    private Button btnRecharge;
    private RecyclerView rvTransactions;
    private ProgressDialog progressDialog;

    // Adapter and Data
    private TransactionAdapter adapter;
    private RequestQueue requestQueue;

    // API endpoints
    private static final String RECHARGE_URL = "http://sxm.a58.mytemp.website/rechargewallet.php";
    private static final String BALANCE_URL = "http://sxm.a58.mytemp.website/get_wallet_balance.php";
    private static final String TRANSACTIONS_URL = "http://sxm.a58.mytemp.website/fetch_wallet_transactions.php";

    // User data
    private String patientId;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        initViews();
        setupCurrencyFormat();
        setupRecyclerView();
        loadUserData();
    }

    private void initViews() {
        try {
            tvTotalBalance = findViewById(R.id.tvTotalBalance);
            tvEarned = findViewById(R.id.tvEarned);
            tvSpent = findViewById(R.id.tvSpent);
            btnRecharge = findViewById(R.id.btnRecharge);
            rvTransactions = findViewById(R.id.rvTransactions);

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
        } catch (Exception e) {
            Toast.makeText(this, "UI initialization failed", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupCurrencyFormat() {
        currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setMaximumFractionDigits(2);
    }

    private void setupRecyclerView() {
        try {
            adapter = new TransactionAdapter(this);
            rvTransactions.setLayoutManager(new LinearLayoutManager(this));
            rvTransactions.setAdapter(adapter);
            requestQueue = Volley.newRequestQueue(this);
        } catch (Exception e) {
            Toast.makeText(this, "RecyclerView setup failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        try {
            SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            patientId = sp.getString("patient_id", "");

            if (patientId == null || patientId.isEmpty()) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            btnRecharge.setOnClickListener(view -> showRechargeDialog());
            fetchWalletBalance();
            fetchTransactionHistory();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRechargeDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Recharge Wallet");

            final EditText input = new EditText(this);
            input.setHint("Enter amount (₹)");
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            builder.setView(input);

            builder.setPositiveButton("Recharge", (dialog, which) -> {
                try {
                    String amountStr = input.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        if (amount > 0) {
                            rechargeWallet(amountStr);
                        } else {
                            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Amount required!", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to show recharge dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void rechargeWallet(String amount) {
        showProgress();

        StringRequest request = new StringRequest(Request.Method.POST, RECHARGE_URL,
                response -> {
                    dismissProgress();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
                            String updatedBalance = obj.getString("wallet_balance");
                            updateBalanceUI(updatedBalance);
                            Toast.makeText(this, "Wallet recharged successfully", Toast.LENGTH_SHORT).show();
                            fetchTransactionHistory();
                        } else {
                            String errorMsg = obj.optString("message", "Recharge failed");
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    dismissProgress();
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                params.put("amount", amount);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void fetchWalletBalance() {
        showProgress();

        StringRequest request = new StringRequest(Request.Method.POST, BALANCE_URL,
                response -> {
                    dismissProgress();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
                            String balance = obj.getString("wallet_balance");
                            updateBalanceUI(balance);
                        } else {
                            runOnUiThread(() -> tvTotalBalance.setText(formatCurrency(0)));
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            tvTotalBalance.setText(formatCurrency(0));
                            Toast.makeText(this, "Error parsing balance", Toast.LENGTH_SHORT).show();
                        });
                    }
                },
                error -> {
                    dismissProgress();
                    runOnUiThread(() -> {
                        tvTotalBalance.setText(formatCurrency(0));
                        Toast.makeText(this, "Error fetching balance", Toast.LENGTH_SHORT).show();
                    });
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void fetchTransactionHistory() {
        showProgress();

        StringRequest request = new StringRequest(Request.Method.POST, TRANSACTIONS_URL,
                response -> {
                    dismissProgress();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
                            processTransactionData(obj.getJSONArray("data"));
                        } else {
                            runOnUiThread(() -> adapter.updateTransactions(null));
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            adapter.updateTransactions(null);
                            Toast.makeText(this, "Error parsing transactions", Toast.LENGTH_SHORT).show();
                        });
                    }
                },
                error -> {
                    dismissProgress();
                    runOnUiThread(() -> {
                        adapter.updateTransactions(null);
                        Toast.makeText(this, "Error fetching transactions", Toast.LENGTH_SHORT).show();
                    });
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void processTransactionData(JSONArray transactions) {
        try {
            List<TransactionAdapter.TransactionItem> items = new ArrayList<>();
            double earnedTotal = 0;
            double spentTotal = 0;

            for (int i = 0; i < transactions.length(); i++) {
                JSONObject item = transactions.getJSONObject(i);
                String amount = item.getString("amount");
                String type = item.getString("type");
                String reason = item.optString("reason", "");
                String timestamp = item.getString("timestamp");

                // Calculate totals
                try {
                    double amountValue = Double.parseDouble(amount);
                    if ("credit".equalsIgnoreCase(type)) {
                        earnedTotal += amountValue;
                    } else {
                        spentTotal += amountValue;
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid amounts
                    continue;
                }

                items.add(new TransactionAdapter.TransactionItem(amount, type, reason, timestamp));
            }

            runOnUiThread(() -> {
                // Update adapter
                adapter.updateTransactions(items);

                // Update totals
//                tvEarned.setText(formatCurrency(earnedTotal));
//                tvSpent.setText(formatCurrency(spentTotal));

                // Debug log
                Log.d("TransactionDebug", "Processed " + items.size() + " transactions");
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Error processing transactions", Toast.LENGTH_SHORT).show();
                Log.e("TransactionError", "Error: " + e.getMessage());
            });
        }
    }
    private void updateBalanceUI(String balance) {
        try {
            double amount = Double.parseDouble(balance);
            runOnUiThread(() -> tvTotalBalance.setText(formatCurrency(amount)));
        } catch (NumberFormatException e) {
            runOnUiThread(() -> tvTotalBalance.setText(formatCurrency(0)));
        }
    }

    private String formatCurrency(double amount) {
        return "₹" + String.format("%,.2f", amount);
    }

    private void showProgress() {
        try {
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void dismissProgress() {
        try {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgress();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}