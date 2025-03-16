package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
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

import java.util.ArrayList;
import java.util.List;

public class medical_riport extends AppCompatActivity {

    // API endpoint URL (update with your actual server URL)
    // This endpoint expects an "appointment_id" parameter.
    private static final String GET_REPORT_URL = "http://sxm.a58.mytemp.website/get_medical_report.php?appointment_id=";

    // Appointment ID retrieved from the Intent extra
    private String appointmentId;

    // UI elements (make sure these IDs match those in your layout file activity_medical_riport.xml)
    private TextView tvHospitalName, tvHospitalAddress;
    private TextView tvPatientName, tvPatientAddress, tvVisitDate;
    private TextView tvPatientAge, tvPatientWeight, tvPatientSex;
    private TextView tvTemperature, tvPulse, tvSpo2, tvBloodPressure, tvRespiratory;
    private TextView tvSymptoms, tvInvestigations;
    private TextView tvDoctorName, tvDoctorDetails;

    private RequestQueue requestQueue;
    private static final String TAG = "MedicalReport";

    @SuppressLint("SetTextI18n")
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
        tvPatientAge = findViewById(R.id.tv_patient_age);
        tvPatientSex = findViewById(R.id.tv_patient_sex);
        tvPatientWeight = findViewById(R.id.tv_patient_weight);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvPulse = findViewById(R.id.tv_pulse);
        tvSpo2 = findViewById(R.id.tv_spo2);
        tvBloodPressure = findViewById(R.id.tv_blood_pressure);
        tvRespiratory = findViewById(R.id.tv_respiratory);
        tvSymptoms = findViewById(R.id.tv_symptoms);
        tvInvestigations = findViewById(R.id.tv_investigations_content);
        tvDoctorName = findViewById(R.id.tv_doctor_name);
//        tvDoctorDetails = findViewById(R.id.tv_doctor_details);

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
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "API Response received: " + response.toString());

                        try {
                            String status = response.optString("status", "");
                            Log.d(TAG, "Response status: " + status);

                            if (status.equalsIgnoreCase("success")) {
                                JSONObject data = response.optJSONObject("data");
                                if (data != null) {
                                    Log.d(TAG, "Data object received: " + data.toString());
                                    // Log individual fields for debugging
                                    Log.d(TAG, "patient_name: " + data.optString("patient_name", "N/A"));
                                    Log.d(TAG, "patient_address: " + data.optString("patient_address", "N/A"));
                                    Log.d(TAG, "visit_date: " + data.optString("visit_date", "N/A"));
                                    Log.d(TAG, "temperature: " + data.optString("temperature", "N/A"));
                                    Log.d(TAG, "pulse: " + data.optString("pulse", "N/A"));
                                    Log.d(TAG, "spo2: " + data.optString("spo2", "N/A"));
                                    Log.d(TAG, "blood_pressure: " + data.optString("blood_pressure", "N/A"));
                                    Log.d(TAG, "respiratory_system: " + data.optString("respiratory_system", "N/A"));
                                    Log.d(TAG, "symptoms: " + data.optString("symptoms", "N/A"));
                                    Log.d(TAG, "investigations: " + data.optString("investigations", "N/A"));
                                    Log.d(TAG, "doctor_name: " + data.optString("doctor_name", "N/A"));
                                    Log.d(TAG, "doctor_details: " + data.optString("doctor_details", "N/A"));
                                    Log.d(TAG, "age: " + data.optString("age", "N/A"));
                                    Log.d(TAG, "weight: " + data.optString("weight", "N/A"));
                                    Log.d(TAG, "medications: " + data.optString("medications", ""));
                                    Log.d(TAG, "dosage: " + data.optString("dosage", ""));

                                    // Update UI with fetched data
                                    tvPatientName.setText("Name: " + data.optString("patient_name", "N/A"));
                                    tvPatientAddress.setText("Address: " + data.optString("patient_address", "N/A"));
                                    tvVisitDate.setText("Date: " + data.optString("visit_date", "N/A"));
                                    tvTemperature.setText("Temperature: " + data.optString("temperature", "N/A"));
                                    tvPatientAge.setText("Age: " + data.optString("age", "N/A") + " Years");
                                    tvPatientWeight.setText("Weight: " + data.optString("weight", "N/A") + " kg");
                                    tvPatientSex.setText("Sex: " + data.optString("sex", "N/A"));

                                    tvPulse.setText("Pulse: " + data.optString("pulse", "N/A"));
                                    tvSpo2.setText("SP02: " + data.optString("spo2", "N/A"));
                                    tvBloodPressure.setText("Blood Pressure: " + data.optString("blood_pressure", "N/A"));
                                    tvRespiratory.setText("Respiratory: " + data.optString("respiratory_system", "N/A"));
                                    tvSymptoms.setText("Symptoms: " + data.optString("symptoms", "N/A"));
                                    tvInvestigations.setText("Investigations: " + data.optString("investigations", "N/A"));
                                    tvDoctorName.setText("Doctor: " + data.optString("doctor_name", "N/A"));
                                    // tvDoctorDetails.setText(data.optString("doctor_details", "N/A"));

                                    // --- Parse and update the Medications table ---
                                    String medicationsStr = data.optString("medications", "");
                                    String dosageStr = data.optString("dosage", "");

                                    Log.d(TAG, "Medications string: " + medicationsStr);
                                    Log.d(TAG, "Dosage string: " + dosageStr);

                                    // Split the strings by newline (\\n)
                                    String[] medicationsArray = medicationsStr.split("\\n");
                                    String[] dosageArray = dosageStr.split("\\n");

                                    // Filter out any empty lines
                                    List<String> medList = new ArrayList<>();
                                    for (String med : medicationsArray) {
                                        med = med.trim();
                                        if (!med.isEmpty()) {
                                            if (med.endsWith(",")) {
                                                med = med.substring(0, med.length() - 1);
                                            }
                                            medList.add(med);
                                        }
                                    }

                                    List<String> dosageList = new ArrayList<>();
                                    for (String dos : dosageArray) {
                                        dos = dos.trim();
                                        if (!dos.isEmpty()) {
                                            if (dos.endsWith(",")) {
                                                dos = dos.substring(0, dos.length() - 1);
                                            }
                                            dosageList.add(dos);
                                        }
                                    }

                                    // Use maximum count so that if there is a missing dosage, it will be defaulted to an empty string.
                                    int rowCount = Math.max(medList.size(), dosageList.size());
                                    Log.d(TAG, "Number of medication rows after filtering: " + rowCount);

                                    // Obtain reference to the TableLayout from XML
                                    TableLayout tableMedications = findViewById(R.id.table_medications);

                                    // Remove any existing rows except header (assuming header is at index 0)
                                    int childCount = tableMedications.getChildCount();
                                    if (childCount > 1) {
                                        tableMedications.removeViews(1, childCount - 1);
                                    }

                                    // Loop through each medication row
                                    for (int i = 0; i < rowCount; i++) {
                                        String medName = "";
                                        String dosage = "";
                                        if (i < medList.size()) {
                                            medName = medList.get(i);
                                        }
                                        if (i < dosageList.size()) {
                                            dosage = dosageList.get(i);
                                        }

                                        // Create a new TableRow
                                        TableRow row = new TableRow(medical_riport.this);
                                        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                                                TableRow.LayoutParams.WRAP_CONTENT,
                                                TableRow.LayoutParams.WRAP_CONTENT);
                                        row.setLayoutParams(lp);

                                        // Create a TextView for the row number
                                        TextView tvNo = new TextView(medical_riport.this);
                                        tvNo.setText(String.valueOf(i + 1));
                                        tvNo.setTextSize(16);
                                        tvNo.setPadding(4, 4, 4, 4);

                                        // Create a TextView for the medicine name
                                        TextView tvMedName = new TextView(medical_riport.this);
                                        tvMedName.setText(medName);
                                        tvMedName.setTextSize(16);
                                        tvMedName.setPadding(4, 4, 4, 4);

                                        // Create a TextView for the dosage
                                        TextView tvDosage = new TextView(medical_riport.this);
                                        tvDosage.setText(dosage);
                                        tvDosage.setTextSize(16);
                                        tvDosage.setPadding(4, 4, 4, 4);

                                        // Add the TextViews to the TableRow
                                        row.addView(tvNo);
                                        row.addView(tvMedName);
                                        row.addView(tvDosage);

                                        // Add the TableRow to the TableLayout
                                        tableMedications.addView(row);
                                    }
                                } else {
                                    Toast.makeText(medical_riport.this, "Report not found", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Report not found. Returned status: " + status);
                                }
                            } else {
                                Toast.makeText(medical_riport.this, "Report not found", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Report not found. Returned status: " + status);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception during parsing: " + e.getMessage(), e);
                            Toast.makeText(medical_riport.this, "Error parsing report data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            Log.e(TAG, "Volley error. HTTP Status Code: " + error.networkResponse.statusCode);
                            Log.e(TAG, "Volley error data: " + new String(error.networkResponse.data));
                        }
                        Log.e(TAG, "Volley error: " + error.getMessage(), error);
                        Toast.makeText(medical_riport.this, "Error fetching report", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }
}
