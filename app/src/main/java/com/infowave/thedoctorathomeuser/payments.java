package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.TransactionAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class payments extends AppCompatActivity {

    TextView tvWalletBalance;
    Button btnRecharge;
    RecyclerView rvTransactions;

    List<TransactionAdapter.TransactionItem> transactionList = new ArrayList<>();
    TransactionAdapter adapter;

    String rechargeUrl = ApiConfig.endpoint("rechargewallet.php");


    String fetchBalanceUrl = ApiConfig.endpoint("get_wallet_balance.php");


    String fetchTransactionUrl = ApiConfig.endpoint("fetch_wallet_transactions.php");


    private String patientId;

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
        // Log: Patient ID loaded from shared preferences for wallet/payment actions
        Log.d("DEBUG", "Loaded patient_id: " + patientId);

        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new TransactionAdapter(transactionList, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        fetchWalletBalance();
        fetchTransactionHistory();

        btnRecharge.setOnClickListener(view -> showRechargeDialog());
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
                rechargeWallet(amountStr);
            } else {
                Toast.makeText(this, "Amount required!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void rechargeWallet(String amount) {
        @SuppressLint("SetTextI18n")
        StringRequest request = new StringRequest(Request.Method.POST, rechargeUrl,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
                            String updatedBalance = obj.getString("wallet_balance");
                            tvWalletBalance.setText("₹" + updatedBalance);
                            Toast.makeText(this, "Wallet recharged successfully", Toast.LENGTH_SHORT).show();
                            fetchTransactionHistory();
                        } else {
                            Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", String.valueOf(patientId));
                map.put("amount", amount);
                return map;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void fetchWalletBalance() {
        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(Request.Method.POST, fetchBalanceUrl,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
                            String balance = obj.getString("wallet_balance");
                            tvWalletBalance.setText("₹" + balance);
                        } else {
                            tvWalletBalance.setText("₹0.00");
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing balance", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error fetching balance", Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", String.valueOf(patientId));
                return map;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void fetchTransactionHistory() {
        StringRequest request = new StringRequest(Request.Method.POST, fetchTransactionUrl,
                response -> {
                    try {
                        // Log: Raw transaction history response received from server
                        Log.d("TRANSACTION_HISTORY", response);

                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("success")) {
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
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing transactions", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error fetching transactions", Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("patient_id", String.valueOf(patientId));
                return map;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}
