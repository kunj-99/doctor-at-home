package com.infowave.thedoctorathomeuser;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class VetAppointmentActivity extends AppCompatActivity {

    private TextView tvPickDate, tvPickTime;
    private EditText etAddress, etNotes;
    private Button btnSubmit;

    private String animalId;
    private String animalName;

    // If you keep userId/token in SharedPreferences, retrieve here.
    // For demo, we keep userId as a placeholder.
    private String userId = "123"; // TODO: replace with real logged-in user id

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_appointment);

        // Read animal info from Intent
        Intent i = getIntent();
        animalId   = i.getStringExtra("animal_id");
        animalName = i.getStringExtra("animal_name");

        bindViews();
        bindClicks();

//        if (!TextUtils.isEmpty(animalName)) {
//            tvHeaderAnimal.setText("Book Appointment • " + animalName);
//        }
    }

    private void bindViews() {
//        tvHeaderAnimal = findViewById(R.id.tvHeaderAnimal);
        tvPickDate     = findViewById(R.id.tvPickDate);
        tvPickTime     = findViewById(R.id.tvPickTime);
        etAddress      = findViewById(R.id.etAddress);
        etNotes        = findViewById(R.id.etNotes);
        btnSubmit      = findViewById(R.id.btnSubmit);
    }

    private void bindClicks() {
        tvPickDate.setOnClickListener(v -> openDatePicker());
        tvPickTime.setOnClickListener(v -> openTimePicker());
        btnSubmit.setOnClickListener(v -> trySubmit());
    }

    private void openDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String mm = String.format("%02d", month + 1);
            String dd = String.format("%02d", dayOfMonth);
            tvPickDate.setText(year + "-" + mm + "-" + dd); // yyyy-MM-dd
        }, y, m, d);

        // Optional: disallow past dates
        dlg.getDatePicker().setMinDate(System.currentTimeMillis());
        dlg.show();
    }

    private void openTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hh = c.get(Calendar.HOUR_OF_DAY);
        int mm = c.get(Calendar.MINUTE);

        TimePickerDialog dlg = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String H = String.format("%02d", hourOfDay);
            String M = String.format("%02d", minute);
            tvPickTime.setText(H + ":" + M); // HH:mm (24h)
        }, hh, mm, true);

        dlg.show();
    }

    private void trySubmit() {
        String date = tvPickDate.getText().toString().trim();
        String time = tvPickTime.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(animalId)) {
            toast("Animal not selected.");
            return;
        }
        if (TextUtils.isEmpty(date)) {
            toast("Please select appointment date.");
            return;
        }
        if (TextUtils.isEmpty(time)) {
            toast("Please select appointment time.");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            toast("Please enter address.");
            return;
        }

        submitAppointment(animalId, animalName, date, time, address, notes);
    }

    private void submitAppointment(String animalId, String animalName, String date, String time, String address, String notes) {
        // Progress indicator
        ProgressBar progress = findViewById(R.id.progressSubmit);
        progress.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // ✅ Uses your required ApiConfig.endpoint("file.php","param",value) format
        final String url = ApiConfig.endpoint("book_vet_appointment.php", "action", "create");

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    progress.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean ok = obj.optBoolean("status", false);
                        String msg = obj.optString("message", ok ? "Booked!" : "Failed.");
                        String appointmentId = obj.optString("appointment_id", "");

                        toast(msg);

                        if (ok) {
                            // Optionally navigate to details/success screen with appointmentId
                            // finish current screen:
                            finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("Unexpected response.");
                    }
                },
                error -> {
                    progress.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    toast("Network error, please try again.");
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put("user_id", userId);
                map.put("animal_id", animalId);
                map.put("animal_name", animalName != null ? animalName : "");
                map.put("date", date);      // yyyy-MM-dd
                map.put("time", time);      // HH:mm
                map.put("address", address);
                map.put("notes", notes);
                // Add any additional params needed by your backend (e.g., token)
                return map;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
