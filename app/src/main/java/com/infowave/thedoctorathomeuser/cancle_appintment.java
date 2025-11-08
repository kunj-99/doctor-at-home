package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// System bar scrims
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class cancle_appintment extends AppCompatActivity {

    private TextInputEditText reasonInput;
    private MaterialCheckBox confirmationCheckbox;
    private MaterialButton btnBack, btnConfirm;
    private TextView doctorName, doctorQualification, patientName, appointmentDate;
    private TextView tvErrorMessage;

    private String appointmentId = "";
    private static final String API_URL = ApiConfig.endpoint("cancel_appointment.php");

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancle_appintment);
        setupSystemBarScrims();

        if (getIntent().hasExtra("appointment_id")) {
            appointmentId = String.valueOf(getIntent().getIntExtra("appointment_id", -1));
            if (appointmentId.equals("-1")) {
                tvError("Something went wrong. Please try again.");
                finish();
                return;
            }
        } else {
            tvError("Could not find your appointment. Please try again.");
            finish();
            return;
        }

        doctorName          = findViewById(R.id.doctoName1);
        doctorQualification = findViewById(R.id.doctorQualification1);
        patientName         = findViewById(R.id.patientName1);
        appointmentDate     = findViewById(R.id.appointment_date1);
        reasonInput         = findViewById(R.id.reasonInput);
        confirmationCheckbox= findViewById(R.id.confirmationCheckbox);
        btnBack             = findViewById(R.id.btn_back);
        btnConfirm          = findViewById(R.id.btn_confirm);
        tvErrorMessage      = findViewById(R.id.tvErrorMessage);

        fetchAppointmentDetails();

        btnBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> {
            tvErrorMessage.setText("");
            StringBuilder errorBuilder = new StringBuilder();

            String reason = Objects.requireNonNull(reasonInput.getText()).toString().trim();
            if (TextUtils.isEmpty(reason)) {
                reasonInput.setError("Please enter a reason for cancellation.");
                errorBuilder.append("Please enter a reason for cancellation.\n");
            }

            if (errorBuilder.length() > 0) {
                tvErrorMessage.setText(errorBuilder.toString());
                return;
            }

            if (!confirmationCheckbox.isChecked()) {
                Toast.makeText(cancle_appintment.this, "Please check the box to confirm cancellation.", Toast.LENGTH_SHORT).show();
                return;
            }

            cancelAppointment(reason);
        });
    }

    private void fetchAppointmentDetails() {
        StringRequest req = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject o = new JSONObject(response);
                        if (!o.getBoolean("success")) {
                            tvError("Could not load appointment details. Please try again.");
                            return;
                        }
                        JSONObject a = o.getJSONObject("appointment");
                        if (doctorName != null)          doctorName.setText(a.optString("patient_name","")); // or doctor_name if you expose it
                        if (doctorQualification != null) doctorQualification.setText(a.optString("appointment_mode",""));
                        if (patientName != null)         patientName.setText(a.optString("patient_name",""));
                        if (appointmentDate != null)     appointmentDate.setText(a.optString("appointment_date",""));
                    } catch (JSONException e) {
                        tvError("Sorry, we could not load your appointment details right now.");
                    }
                },
                error -> tvError("No internet connection. Please check and try again.")) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("appointment_id", appointmentId);
                p.put("action", "fetch");
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void cancelAppointment(String reason) {
        StringRequest req = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject o = new JSONObject(response);
                        if (o.getBoolean("success")) {
                            tvError("Your appointment has been cancelled successfully.");
                            finish();
                        } else {
                            tvError(o.optString("error", "Could not cancel the appointment. Please try again."));
                        }
                    } catch (JSONException e) {
                        tvError("Something went wrong. Please try again.");
                    }
                },
                error -> tvError("No internet connection. Please check and try again.")) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("appointment_id", appointmentId);
                p.put("action", "cancel");
                p.put("reason", reason);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void tvError(String message) {
        if (tvErrorMessage != null) {
            tvErrorMessage.setText(message);
        }
    }

    /* ----- System bar scrims ----- */
    private void setupSystemBarScrims() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        FrameLayout content = findViewById(android.R.id.content);
        if (content == null) return;
        View root = (content.getChildCount() > 0) ? content.getChildAt(0) : null;
        if (root == null) return;

        View statusScrim = new View(this);
        View navScrim = new View(this);
        statusScrim.setBackgroundColor(Color.BLACK);
        navScrim.setBackgroundColor(Color.BLACK);

        FrameLayout.LayoutParams lpTop = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP);
        FrameLayout.LayoutParams lpBottom = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM);

        content.addView(statusScrim, lpTop);
        content.addView(navScrim, lpBottom);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.LayoutParams stLp = statusScrim.getLayoutParams();
            if (stLp.height != status.top) {
                stLp.height = status.top;
                statusScrim.setLayoutParams(stLp);
            }
            statusScrim.setVisibility(status.top > 0 ? View.VISIBLE : View.GONE);

            ViewGroup.LayoutParams nbLp = navScrim.getLayoutParams();
            if (nbLp.height != nav.bottom) {
                nbLp.height = nav.bottom;
                navScrim.setLayoutParams(nbLp);
            }
            navScrim.setVisibility(nav.bottom > 0 ? View.VISIBLE : View.GONE);

            v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), nav.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        root.requestApplyInsets();
    }
}
