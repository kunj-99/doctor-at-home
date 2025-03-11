package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.os.Bundle;
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

public class cancle_appintment extends AppCompatActivity {

    private TextInputEditText reasonInput;
    private MaterialCheckBox confirmationCheckbox;
    private MaterialButton btnBack, btnConfirm;
    private TextView doctorName, doctorQualification, patientName, appointmentDate;

    private String appointmentId = "";
    private static final String API_URL = "http://sxm.a58.mytemp.website/cancel_appointment.php";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancle_appintment);

        // Retrieve Appointment ID safely
        if (getIntent().hasExtra("appointment_id")) {
            appointmentId = String.valueOf(getIntent().getIntExtra("appointment_id", -1));
            if (appointmentId.equals("-1")) {
                showToast("Invalid appointment ID");
                finish();
                return;
            }
        } else {
            showToast("No appointment ID received");
            finish();
            return;
        }

        // Initialize Views
        doctorName = findViewById(R.id.doctoName1);
        doctorQualification = findViewById(R.id.doctorQualification1);
        patientName = findViewById(R.id.patientName1);
        appointmentDate = findViewById(R.id.appointment_date1);
        reasonInput = findViewById(R.id.reasonInput);
        confirmationCheckbox = findViewById(R.id.confirmationCheckbox);
        btnBack = findViewById(R.id.btn_back);
        btnConfirm = findViewById(R.id.btn_confirm);

        // Fetch appointment details
        fetchAppointmentDetails();

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        // Confirm Button
        btnConfirm.setOnClickListener(v -> {
            if (!confirmationCheckbox.isChecked()) {
                showToast("Please confirm cancellation");
                return;
            }

            String reason = reasonInput.getText().toString().trim();
            if (reason.isEmpty()) {
                showToast("Please enter a cancellation reason");
                return;
            }

            cancelAppointment(reason);
        });
    }

    // Fetch appointment details
    private void fetchAppointmentDetails() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (!jsonObject.getBoolean("success")) {
                            showToast(jsonObject.getString("error"));
                            return;
                        }

                        JSONObject appointment = jsonObject.getJSONObject("appointment");

                        if (doctorName != null) doctorName.setText(appointment.getString("doctor_name"));
                        if (doctorQualification != null) doctorQualification.setText(appointment.getString("qualification"));
                        if (patientName != null) patientName.setText(appointment.getString("patient_name"));
                        if (appointmentDate != null) appointmentDate.setText(appointment.getString("appointment_date"));

                    } catch (JSONException e) {
                        showToast("Error loading appointment details.");
                    }
                },
                error -> showToast("Network Error! Please try again.")) {
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

    // Cancel appointment
    private void cancelAppointment(String reason) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            showToast("Appointment cancelled successfully!");
                            finish();
                        } else {
                            showToast("Cancellation failed: " + jsonObject.getString("error"));
                        }
                    } catch (JSONException e) {
                        showToast("Error processing request.");
                    }
                },
                error -> showToast("Network Error! Please try again.")) {
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

    // Utility method for showing toast messages
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
