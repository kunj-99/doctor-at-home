package com.example.thedoctorathomeuser;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class medical_riport extends AppCompatActivity {

    // API endpoint URL (update with your actual server URL)
    // This endpoint expects an "appointment_id" parameter.
    private static final String GET_REPORT_URL = "http://sxm.a58.mytemp.website/get_medical_report.php?appointment_id=";

    // Appointment ID retrieved from the Intent extra
    private String appointmentId;

    // UI elements (make sure these IDs match those in your layout file activity_medical_riport.xml)
    private TextView tvHospitalName, tvHospitalAddress;
    private TextView tvPatientName, tvPatientAddress, tvVisitDate;
    private TextView tvTemperature, tvPulse, tvSpo2, tvBloodPressure, tvRespiratory;
    private TextView tvSymptoms, tvInvestigations;
    private TextView tvDoctorName, tvDoctorDetails;

    private RequestQueue requestQueue;
    private static final String TAG = "MedicalReport";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_riport);

        // Retrieve appointment ID from the Intent extra
        appointmentId = getIntent().getStringExtra("appointment_id");
        Log.d(TAG, "Received appointment ID: " + appointmentId);
        if (appointmentId == null || appointmentId.isEmpty()) {
            Toast.makeText(this, "Appointment ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        tvHospitalName = findViewById(R.id.tv_hospital_name);
        tvHospitalAddress = findViewById(R.id.tv_hospital_address);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvPatientAddress = findViewById(R.id.tv_patient_address);
        tvVisitDate = findViewById(R.id.tv_visit_date);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvPulse = findViewById(R.id.tv_pulse);               // Ensure these exist in your XML
        tvSpo2 = findViewById(R.id.tv_spo2);
        tvBloodPressure = findViewById(R.id.tv_blood_pressure);
        tvRespiratory = findViewById(R.id.tv_respiratory);
        tvSymptoms = findViewById(R.id.tv_symptoms);
        tvInvestigations = findViewById(R.id.tv_investigations_content);
        tvDoctorName = findViewById(R.id.tv_doctor_name);
        tvDoctorDetails = findViewById(R.id.tv_doctor_details);

        // Optionally, set static header texts
        tvHospitalName.setText("VRAJ HOSPITAL");
        tvHospitalAddress.setText("150 Feet Ring Road, Rajkot - 360 004");

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch the medical report data using the appointment ID
        fetchMedicalReport();
    }

    private void fetchMedicalReport() {
        String url = GET_REPORT_URL + appointmentId;
        Log.d(TAG, "Fetching report from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "API Response: " + response.toString());
                        try {
                            if (response.getString("status").equals("success")) {
                                JSONObject data = response.getJSONObject("data");

                                // Update UI with fetched data
                                tvPatientName.setText("Name: " + data.optString("patient_name", "N/A"));
                                tvPatientAddress.setText("Address: " + data.optString("patient_address", "N/A"));
                                tvVisitDate.setText("Date: " + data.optString("visit_date", "N/A"));
                                tvTemperature.setText("Temperature: " + data.optString("temperature", "N/A"));

                                // Update additional vital signs if available
                                tvPulse.setText("Pulse: " + data.optString("pulse", "N/A"));
                                tvSpo2.setText("SP02: " + data.optString("spo2", "N/A"));
                                tvBloodPressure.setText("Blood Pressure: " + data.optString("blood_pressure", "N/A"));
                                tvRespiratory.setText("Respiratory: " + data.optString("respiratory_system", "N/A"));

                                tvSymptoms.setText("Symptoms: " + data.optString("symptoms", "N/A"));
                                tvInvestigations.setText("Investigations: " + data.optString("investigations", "N/A"));
                                tvDoctorName.setText("Doctor: " + data.optString("doctor_name", "N/A"));
                                tvDoctorDetails.setText(data.optString("doctor_details", "N/A"));
                            } else {
                                Toast.makeText(medical_riport.this, "Report not found", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Report not found in API response");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parsing error: " + e.getMessage(), e);
                            Toast.makeText(medical_riport.this, "Error parsing report data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error", error);
                        Toast.makeText(medical_riport.this, "Error fetching report", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }
}
