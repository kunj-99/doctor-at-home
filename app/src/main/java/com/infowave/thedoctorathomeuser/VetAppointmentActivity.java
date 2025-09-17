package com.infowave.thedoctorathomeuser;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.infowave.thedoctorathomeuser.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class VetAppointmentActivity extends AppCompatActivity {

    // UI References
    private TextInputEditText etPetName, etBreed, etAge, etOwnerName, etPhone, etEmail, etAddress, etNotes;
    private AutoCompleteTextView actvPetType, actvGender, actvReason;
    private TextView tvPickDate, tvPickTime;
    private Button btnSubmit;
    private ProgressBar progressSubmit;

    // Date and time variables
    private Calendar selectedDateTime;

    // Dropdown options
    private final String[] petTypes = {"Dog", "Cat", "Bird", "Rabbit", "Other"};
    private final String[] genders = {"Male", "Female"};
    private final String[] reasons = {"Routine Checkup", "Vaccination", "Illness", "Injury", "Surgery", "Dental Care", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_appointment);

        // Initialize UI components
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup dropdowns
        setupDropdowns();

        // Setup date and time pickers
        setupDateTimePickers();

        // Setup submit button
        setupSubmitButton();
    }

    private void initViews() {
        // Toolbar already handled in setupToolbar()

        // Pet Information
        etPetName = findViewById(R.id.etPetName);
        actvPetType = findViewById(R.id.actvPetType);
        etBreed = findViewById(R.id.etBreed);
        etAge = findViewById(R.id.etAge);
        actvGender = findViewById(R.id.actvGender);

        // Owner Information
        etOwnerName = findViewById(R.id.etOwnerName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        // Appointment Details
        tvPickDate = findViewById(R.id.tvPickDate);
        tvPickTime = findViewById(R.id.tvPickTime);
        actvReason = findViewById(R.id.actvReason);
        etAddress = findViewById(R.id.etAddress);
        etNotes = findViewById(R.id.etNotes);

        // Submit button and progress
        btnSubmit = findViewById(R.id.btnSubmit);
        progressSubmit = findViewById(R.id.progressSubmit);

        // Initialize calendar
        selectedDateTime = Calendar.getInstance();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set back button click listener
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDropdowns() {
        // Pet type dropdown
        ArrayAdapter<String> petTypeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, petTypes);
        actvPetType.setAdapter(petTypeAdapter);

        // Gender dropdown
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, genders);
        actvGender.setAdapter(genderAdapter);

        // Reason dropdown
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, reasons);
        actvReason.setAdapter(reasonAdapter);
    }

    private void setupDateTimePickers() {
        // Date picker
        tvPickDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        tvPickTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDateTime.set(selectedYear, selectedMonth, selectedDay);

                    // Format date and update TextView
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
                    tvPickDate.setText(dateFormat.format(selectedDateTime.getTime()));

                    // If time hasn't been set yet, set a default time
                    if (tvPickTime.getText().toString().equals("Select time")) {
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, 9);
                        selectedDateTime.set(Calendar.MINUTE, 0);

                        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                        tvPickTime.setText(timeFormat.format(selectedDateTime.getTime()));
                    }
                },
                year, month, day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        // Show the dialog
        datePickerDialog.show();
    }

    private void showTimePicker() {
        // If no date is selected, use today
        if (tvPickDate.getText().toString().equals("Select date")) {
            selectedDateTime = Calendar.getInstance();

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            tvPickDate.setText(dateFormat.format(selectedDateTime.getTime()));
        }

        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedDateTime.set(Calendar.MINUTE, selectedMinute);

                    // Format time and update TextView
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    tvPickTime.setText(timeFormat.format(selectedDateTime.getTime()));
                },
                hour, minute, false // false for 12-hour format, true for 24-hour
        );

        // Show the dialog
        timePickerDialog.show();
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitAppointment();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Pet Name validation
        if (TextUtils.isEmpty(etPetName.getText().toString().trim())) {
            etPetName.setError("Pet name is required");
            isValid = false;
        } else {
            etPetName.setError(null);
        }

        // Pet Type validation
        if (TextUtils.isEmpty(actvPetType.getText().toString().trim())) {
            actvPetType.setError("Please select a pet type");
            isValid = false;
        } else {
            actvPetType.setError(null);
        }

        // Owner Name validation
        if (TextUtils.isEmpty(etOwnerName.getText().toString().trim())) {
            etOwnerName.setError("Owner name is required");
            isValid = false;
        } else {
            etOwnerName.setError(null);
        }

        // Phone validation
        if (TextUtils.isEmpty(etPhone.getText().toString().trim())) {
            etPhone.setError("Phone number is required");
            isValid = false;
        } else {
            etPhone.setError(null);
        }

        // Date validation
        if (tvPickDate.getText().toString().equals("Select date")) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Time validation
        if (tvPickTime.getText().toString().equals("Select time")) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Reason validation
        if (TextUtils.isEmpty(actvReason.getText().toString().trim())) {
            actvReason.setError("Please select a reason for visit");
            isValid = false;
        } else {
            actvReason.setError(null);
        }

        // Address validation
        if (TextUtils.isEmpty(etAddress.getText().toString().trim())) {
            etAddress.setError("Address is required");
            isValid = false;
        } else {
            etAddress.setError(null);
        }

        return isValid;
    }

    private void submitAppointment() {
        // Show progress
        progressSubmit.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // Collect form data
        String petName = etPetName.getText().toString().trim();
        String petType = actvPetType.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String date = tvPickDate.getText().toString();
        String time = tvPickTime.getText().toString();
        String reason = actvReason.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Format the selected date and time
        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDateTime = apiDateFormat.format(selectedDateTime.getTime());

        // Here you would typically send the data to your backend API
        // For now, we'll simulate an API call with a delay

        new android.os.Handler().postDelayed(
                () -> {
                    // Hide progress
                    progressSubmit.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);

                    // Show success message
                    Toast.makeText(
                            VetAppointmentActivity.this,
                            "Appointment booked successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    // You might want to finish the activity or navigate to a confirmation screen
                    // finish();
                },
                2000 // 2 second delay to simulate network request
        );
    }
}