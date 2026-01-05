package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.loaderutil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RefundStatus extends AppCompatActivity {

    private TextView tvTotalPaid, tvBillDate, tvDoctorName, tvPatientName;
    private TextView tvRefundableAmount, tvDeposit, tvGst, tvPaymentMethod, tvRefundStatus;
    private TextView tvCancelStatus, tvCancelReason;
    private SeekBar seekBarRefundProgress;

    private View statusBarScrim;
    private View navBarScrim;
    private View rootContainer;
    private View main;

    private int appointmentId = 0;

    // IMPORTANT: matches the finalized PHP filename
    private static final String API_URL = ApiConfig.endpoint("get_refund_status.php");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge so we can control insets precisely
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Paint real system bars black (even if overlays fail on odd OEMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        setContentView(R.layout.activity_refund_status);

        // Force light icons on black bars
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        rootContainer   = findViewById(R.id.root_container);
        main            = findViewById(R.id.main);
        statusBarScrim  = findViewById(R.id.status_bar_scrim);
        navBarScrim     = findViewById(R.id.navigation_bar_scrim);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            main.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            setViewHeight(statusBarScrim, sys.top);
            setViewHeight(navBarScrim, sys.bottom);
            return insets;
        });

        appointmentId = getIntent().getIntExtra("appointment_id", 0);
        if (appointmentId <= 0) {
            Toast.makeText(this, "Missing appointment.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        fetchRefundDetails();
    }

    private void setViewHeight(@NonNull View view, int heightPx) {
        if (view.getLayoutParams() != null && view.getLayoutParams().height != heightPx) {
            view.getLayoutParams().height = heightPx;
            view.requestLayout();
        }
    }

    private void initViews() {
        tvTotalPaid       = findViewById(R.id.tv_total_paid_value);
        tvBillDate        = findViewById(R.id.tv_bill_date);
        tvDoctorName      = findViewById(R.id.tv_bill_doctor_name);
        tvPatientName     = findViewById(R.id.tv_bill_patient_name);
        tvRefundableAmount= findViewById(R.id.tv_refundable_amount);
        tvDeposit         = findViewById(R.id.tv_deposit);
        tvGst             = findViewById(R.id.tv_gst_value);
        tvPaymentMethod   = findViewById(R.id.tv_payment_method);
        tvRefundStatus    = findViewById(R.id.tv_refund_status);
        tvCancelStatus    = findViewById(R.id.tv_cancel_status);
        tvCancelReason    = findViewById(R.id.tv_cancel_reason);
        seekBarRefundProgress = findViewById(R.id.seekBarRefundProgress);

        if (seekBarRefundProgress != null) {
            seekBarRefundProgress.setEnabled(false);
            seekBarRefundProgress.setMax(100);
        }
    }

    private void fetchRefundDetails() {
        loaderutil.showLoader(this);

        @SuppressLint("SetTextI18n")
        StringRequest request = new StringRequest(
                Request.Method.POST,
                API_URL,
                response -> {
                    loaderutil.hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            JSONObject data = obj.getJSONObject("data");

                            if (tvTotalPaid != null)
                                tvTotalPaid.setText("₹ " + data.optString("total_paid", "0.00"));
                            if (tvRefundableAmount != null)
                                tvRefundableAmount.setText("₹ " + data.optString("refundable_amount", "0.00"));
                            if (tvBillDate != null)
                                tvBillDate.setText(data.optString("payment_date", ""));
                            if (tvDoctorName != null)
                                tvDoctorName.setText(data.optString("doctor_name", ""));
                            if (tvPatientName != null)
                                tvPatientName.setText(data.optString("patient_name", ""));
                            if (tvDeposit != null)
                                tvDeposit.setText("₹ " + data.optString("deposit", "0.00"));
                            if (tvGst != null)
                                tvGst.setText("₹ " + data.optString("gst", "0.00"));
                            if (tvPaymentMethod != null)
                                tvPaymentMethod.setText(data.optString("payment_method", ""));
                            if (tvRefundStatus != null)
                                tvRefundStatus.setText(data.optString("refund_status", "None"));
                            if (tvCancelStatus != null)
                                tvCancelStatus.setText(data.optString("cancel_status", ""));
                            if (tvCancelReason != null)
                                tvCancelReason.setText(data.optString("cancel_reason", "-"));

                            int progress = data.optInt("refund_progress", 0);
                            if (seekBarRefundProgress != null) {
                                seekBarRefundProgress.setProgress(Math.max(0, Math.min(progress, 100)));
                            }
                        } else {
                            Toast.makeText(this,
                                    obj.optString("message", "No refund information available at the moment."),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Sorry, we could not show your refund status right now.",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this,
                            "Unable to connect. Please check your internet and try again.",
                            Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                param.put("appointment_id", String.valueOf(appointmentId));
                return param;
            }
        };

        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(
                12000, // timeout
                1,     // one retry
                1.0f   // backoff
        ));
        Volley.newRequestQueue(this).add(request);
    }
}
