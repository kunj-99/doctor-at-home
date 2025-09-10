package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
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

public class cancle_appintment extends AppCompatActivity {

    private TextInputEditText reasonInput, upiIdInput;
    private MaterialCheckBox confirmationCheckbox;
    private MaterialButton btnBack, btnConfirm;
    private TextView doctorName, doctorQualification, patientName, appointmentDate;
    // TextView to display error messages in UI
    private TextView tvErrorMessage;

    private String appointmentId = "";
    private static final String API_URL = ApiConfig.endpoint("cancel_appointment.php");

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancle_appintment);

        // Retrieve Appointment ID safely
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

        // Initialize Views
        doctorName = findViewById(R.id.doctoName1);
        doctorQualification = findViewById(R.id.doctorQualification1);
        patientName = findViewById(R.id.patientName1);
        appointmentDate = findViewById(R.id.appointment_date1);
        reasonInput = findViewById(R.id.reasonInput);
        upiIdInput = findViewById(R.id.upi_id_input);
        confirmationCheckbox = findViewById(R.id.confirmationCheckbox);
        btnBack = findViewById(R.id.btn_back);
        btnConfirm = findViewById(R.id.btn_confirm);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);

        // Fetch appointment details
        fetchAppointmentDetails();

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        // Confirm Button - validate EditText inputs first, then check checkbox
        btnConfirm.setOnClickListener(v -> {
            // Clear previous error message
            tvErrorMessage.setText("");
            StringBuilder errorBuilder = new StringBuilder();

            // Validate cancellation reason
            String reason = Objects.requireNonNull(reasonInput.getText()).toString().trim();
            if (TextUtils.isEmpty(reason)) {
                reasonInput.setError("Please enter a reason for cancellation.");
                errorBuilder.append("Please enter a reason for cancellation.\n");
            }

            // Validate UPI ID
            String upi = upiIdInput.getText().toString().trim();
            if (TextUtils.isEmpty(upi)) {
                upiIdInput.setError("Please enter your UPI ID.");
                errorBuilder.append("Please enter your UPI ID.\n");
            } else if (!isValidUpi(upi)) {
                upiIdInput.setError("Please enter a valid UPI ID.");
                errorBuilder.append("Please enter a valid UPI ID.\n");
            }

            // If any errors exist in the EditText fields, display them and stop processing
            if (errorBuilder.length() > 0) {
                tvErrorMessage.setText(errorBuilder.toString());
                return;
            }

            // Now, check the confirmation checkbox
            if (!confirmationCheckbox.isChecked()) {
                Toast.makeText(cancle_appintment.this, "Please check the box to confirm cancellation.", Toast.LENGTH_SHORT).show();
                return;
            }

            // All validations passed, update UPI and cancel the appointment
            updateUpi(upi, () -> cancelAppointment(reason));
        });
    }

    // Validate UPI ID using regex (format: username@bank)
    private boolean isValidUpi(String upi) {
        return upi.matches("^[a-zA-Z0-9._-]+@[a-zA-Z]{2,}$");
    }

    // Fetch appointment details
    private void fetchAppointmentDetails() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (!jsonObject.getBoolean("success")) {
                            tvError("Could not load appointment details. Please try again.");
                            return;
                        }
                        JSONObject appointment = jsonObject.getJSONObject("appointment");
                        if (doctorName != null) doctorName.setText(appointment.getString("doctor_name"));
                        if (doctorQualification != null) doctorQualification.setText(appointment.getString("qualification"));
                        if (patientName != null) patientName.setText(appointment.getString("patient_name"));
                        if (appointmentDate != null) appointmentDate.setText(appointment.getString("appointment_date"));
                    } catch (JSONException e) {
                        tvError("Sorry, we could not load your appointment details right now.");
                    }
                },
                error -> tvError("No internet connection. Please check and try again.")) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("appointment_id", appointmentId);
                params.put("action", "fetch");
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // Update UPI ID for the appointment
    private void updateUpi(String upi, final Runnable onSuccess) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            onSuccess.run();
                        } else {
                            tvError("Could not update UPI ID. Please check and try again.");
                        }
                    } catch (JSONException e) {
                        tvError("Sorry, something went wrong while updating your UPI ID.");
                    }
                },
                error -> tvError("Network error while updating UPI ID. Please check your connection.")) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("appointment_id", appointmentId);
                params.put("action", "update_upi");
                params.put("upi_id", upi);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // Cancel appointment
    private void cancelAppointment(String reason) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            tvError("Your appointment has been cancelled successfully.");
                            finish();
                        } else {
                            tvError("Could not cancel the appointment. Please try again.");
                        }
                    } catch (JSONException e) {
                        tvError("Something went wrong. Please try again.");
                    }
                },
                error -> tvError("No internet connection. Please check and try again.")) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("appointment_id", appointmentId);
                params.put("action", "cancel");
                params.put("reason", reason);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // Helper method to display error messages in UI
    private void tvError(String message) {
        if (tvErrorMessage != null) {
            tvErrorMessage.setText(message);
        }
    }
}
