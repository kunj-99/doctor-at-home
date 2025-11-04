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

    private View statusBarScrim;
    private View navBarScrim;
    private View rootContainer;
    private View main;

    int appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force edge-to-edge so we can control the insets precisely
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Paint real system bars black (guaranteed, even if overlays fail on odd OEMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        setContentView(R.layout.activity_refund_status);

        // Light icons OFF (so icons are light on black bars)
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        rootContainer   = findViewById(R.id.root_container);
        main            = findViewById(R.id.main);
        statusBarScrim  = findViewById(R.id.status_bar_scrim);
        navBarScrim     = findViewById(R.id.navigation_bar_scrim);

        // Apply real system bar insets:
        // 1) Pad content so nothing hides under bars.
        // 2) Size scrims to exactly match bar heights (so bars are perfectly black).
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // content padding so header/scroll never sits under bars
            main.setPadding(sys.left, sys.top, sys.right, sys.bottom);

            // overlay heights precisely equal to bar sizes
            setViewHeight(statusBarScrim, sys.top);
            setViewHeight(navBarScrim, sys.bottom);

            return insets;
        });

        appointmentId = getIntent().getIntExtra("appointment_id", 0);
        initViews();
        fetchRefundDetails();
        seekBarRefundProgress.setEnabled(false);
    }

    private void setViewHeight(@NonNull View view, int heightPx) {
        if (view.getLayoutParams() != null && view.getLayoutParams().height != heightPx) {
            view.getLayoutParams().height = heightPx;
            view.requestLayout();
        }
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
        String url = ApiConfig.endpoint("get_refund_status.php");

        loaderutil.showLoader(this);

        @SuppressLint("SetTextI18n")
        StringRequest request = new StringRequest(Request.Method.POST, url,
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
                            Toast.makeText(this, "No refund information available at the moment.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Sorry, we could not show your refund status right now.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Unable to connect. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
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
