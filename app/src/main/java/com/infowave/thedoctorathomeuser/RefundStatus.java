package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.loaderutil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RefundStatus extends AppCompatActivity {

    TextView tvTotalPaid, tvBillDate, tvDoctorName, tvPatientName;
    TextView tvRefundableAmount, tvDeposit, tvGst, tvPaymentMethod, tvRefundStatus;
    TextView tvCancelStatus, tvCancelReason;
    SeekBar seekBarRefundProgress;

    int appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_refund_status);

        View root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        appointmentId = getIntent().getIntExtra("appointment_id", 0);
        initViews();
        fetchRefundDetails();
        seekBarRefundProgress.setEnabled(false);

    }

    private void initViews() {
        tvTotalPaid = findViewById(R.id.tv_total_paid_value);
        tvBillDate = findViewById(R.id.tv_bill_date);
        tvDoctorName = findViewById(R.id.tv_bill_doctor_name);
        tvPatientName = findViewById(R.id.tv_bill_patient_name);
        tvRefundableAmount = findViewById(R.id.tv_refundable_amount);
        tvDeposit = findViewById(R.id.tv_deposit);
        tvGst = findViewById(R.id.tv_gst_value);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvRefundStatus = findViewById(R.id.tv_refund_status);
        tvCancelStatus = findViewById(R.id.tv_cancel_status);
        tvCancelReason = findViewById(R.id.tv_cancel_reason);
        seekBarRefundProgress = findViewById(R.id.seekBarRefundProgress);
    }

    private void fetchRefundDetails() {
        String url = "http://sxm.a58.mytemp.website/get_refund_status.php";

        loaderutil.showLoader(this);

        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    loaderutil.hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            JSONObject data = obj.getJSONObject("data");

                            tvTotalPaid.setText("₹ " + data.getString("total_paid"));
                            tvRefundableAmount.setText("₹ " + data.getString("refundable_amount"));
                            tvBillDate.setText(data.getString("payment_date"));
                            tvDoctorName.setText(data.getString("doctor_name"));
                            tvPatientName.setText(data.getString("patient_name"));
                            tvDeposit.setText("₹ " + data.getString("deposit"));
                            tvGst.setText("₹ " + data.getString("gst"));
                            tvPaymentMethod.setText(data.getString("payment_method"));
                            tvRefundStatus.setText(data.getString("refund_status"));
                            tvCancelStatus.setText(data.getString("cancel_status"));
                            tvCancelReason.setText(data.getString("cancel_reason"));

                            int progress = data.getInt("refund_progress");
                            seekBarRefundProgress.setProgress(progress);

                        } else {
                            Toast.makeText(this, "No refund data found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                param.put("appointment_id", String.valueOf(appointmentId));
                return param;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}
