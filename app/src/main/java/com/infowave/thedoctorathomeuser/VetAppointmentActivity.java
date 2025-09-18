package com.infowave.thedoctorathomeuser; // Ensure this matches your package

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar; // Assuming you'll add a ProgressBar for submission
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
// No AutoCompleteTextView needed based on the new XML

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class VetAppointmentActivity extends AppCompatActivity {

    // UI References
    private TextInputEditText etDate, etTime, etReason;
    private TextInputEditText etFullName, etPhone, etEmail;
    private TextInputEditText etPetName, etBreed, etAge;
    private MaterialButton btnConfirm;
    private ProgressBar progressSubmit; // Declare ProgressBar

    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_appointment);

        initViews();
        setupToolbar();
        setupDateTimePickers();
        setupConfirmButton();

        selectedDateTime = Calendar.getInstance(); // Initialize calendar
    }

    private void initViews() {
        // Appointment Details
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etReason = findViewById(R.id.etReason);

        // Your Details
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        // Animal Details
        etPetName = findViewById(R.id.etPetName);
        etBreed = findViewById(R.id.etBreed);
        etAge = findViewById(R.id.etAge);

        btnConfirm = findViewById(R.id.btnConfirm);


        progressSubmit = findViewById(R.id.progressSubmit); // Ensure this ID exists in XML
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDateTimePickers() {
        // Make the TextInputEditText fields clickable to show pickers
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());

        // Alternatively, to make the whole card clickable:
        // ((View) etDate.getParent().getParent()).setOnClickListener(v -> showDatePicker());
        // ((View) etTime.getParent().getParent()).setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        Calendar currentDate = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()); // e.g., 24 October 2023
                    etDate.setText(dateFormat.format(selectedDateTime.getTime()));
                    etDate.setError(null); // Clear error after selection

                    // If time hasn't been set yet, you might set a default or leave it
                    if (TextUtils.isEmpty(etTime.getText())) {
                        // Default time e.g. 9 AM
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, 9);
                        selectedDateTime.set(Calendar.MINUTE, 0);
                        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                        etTime.setText(timeFormat.format(selectedDateTime.getTime()));
                    }
                },
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // Allow today
        datePickerDialog.show();
    }

    private void showTimePicker() {
        // If no date is selected, you might want to prompt or use current time for picker
        if (TextUtils.isEmpty(etDate.getText())) {
            // Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            // return; // Or default to today for the picker's initial state
        }

        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedDateTime.set(Calendar.MINUTE, selectedMinute);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault()); // e.g., 10:00 AM
                    etTime.setText(timeFormat.format(selectedDateTime.getTime()));
                    etTime.setError(null); // Clear error
                },
                hour, minute, false // false for 12-hour format with AM/PM
        );
        timePickerDialog.show();
    }

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(v -> {
            if (validateForm()) {
                submitAppointmentData();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Clear previous errors
        etDate.setError(null);
        etTime.setError(null);
        etReason.setError(null);
        etFullName.setError(null);
        etPhone.setError(null);
        etEmail.setError(null);
        etPetName.setError(null);
        etBreed.setError(null);
        etAge.setError(null);

        // Date validation
        if (TextUtils.isEmpty(etDate.getText().toString().trim())) {
            etDate.setError("Please select a date");
            // You might want to focus this field or its parent card
            isValid = false;
        }

        // Time validation
        if (TextUtils.isEmpty(etTime.getText().toString().trim())) {
            etTime.setError("Please select a time");
            isValid = false;
        }

        // Reason for Visit validation
        if (TextUtils.isEmpty(etReason.getText().toString().trim())) {
            etReason.setError("Reason for visit is required");
            isValid = false;
        }

        // Full Name validation
        if (TextUtils.isEmpty(etFullName.getText().toString().trim())) {
            etFullName.setError("Your full name is required");
            isValid = false;
        }

        // Phone Number validation
        String phoneInput = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneInput)) {
            etPhone.setError("Phone number is required");
            isValid = false;
        } else if (!android.util.Patterns.PHONE.matcher(phoneInput).matches()) {
            etPhone.setError("Enter a valid phone number");
            isValid = false;
        }

        // Email Address validation (optional or required based on your needs)
        String emailInput = etEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(emailInput) && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            etEmail.setError("Enter a valid email address");
            isValid = false;
        } else if (TextUtils.isEmpty(emailInput)) { // Example: If email is required
            etEmail.setError("Email address is required");
            isValid = false;
        }


        // Pet's Name validation
        if (TextUtils.isEmpty(etPetName.getText().toString().trim())) {
            etPetName.setError("Pet's name is required");
            isValid = false;
        }

        // Breed validation (making it required here, adjust if optional)
        if (TextUtils.isEmpty(etBreed.getText().toString().trim())) {
            etBreed.setError("Pet's breed is required");
            isValid = false;
        }

        // Age validation
        if (TextUtils.isEmpty(etAge.getText().toString().trim())) {
            etAge.setError("Pet's age is required");
            isValid = false;
        }

        return isValid;
    }

    private void submitAppointmentData() {
        if (progressSubmit == null) {
            Toast.makeText(this, "ProgressBar not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressSubmit.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        // Collect all data
        String date = etDate.getText().toString();
        String time = etTime.getText().toString();
        String reason = etReason.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String petName = etPetName.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String age = etAge.getText().toString().trim();

        // Use selectedDateTime for a combined, accurate date-time for backend
        SimpleDateFormat apiDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedApiDateTime = apiDateTimeFormat.format(selectedDateTime.getTime());

        // --- Log data for verification (remove in production) ---
        System.out.println("Booking Vet Appointment:");
        System.out.println("Date (Display): " + date);
        System.out.println("Time (Display): " + time);
        System.out.println("Formatted API DateTime: " + formattedApiDateTime);
        System.out.println("Reason: " + reason);
        System.out.println("Full Name: " + fullName);
        System.out.println("Phone: " + phone);
        System.out.println("Email: " + email);
        System.out.println("Pet Name: " + petName);
        System.out.println("Breed: " + breed);
        System.out.println("Age: " + age);
        // --- End Log ---


        // Simulate API call / data processing
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressSubmit.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);

            Toast.makeText(this, "Appointment for " + petName + " confirmed!", Toast.LENGTH_LONG).show();

            // Optionally, navigate away or clear the form
            // finish();
        }, 2000); // 2-second delay
    }
}
