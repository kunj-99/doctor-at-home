package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class book_form extends AppCompatActivity {

    EditText patientName, address,problem;
    Spinner daySpinner, monthSpinner, yearSpinner;
    RadioGroup genderGroup;

    Button bookButton;

    String doctorId, doctorName, appointmentStatus; // Doctor details from Intent

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_form);

        // Initialize views
        patientName = findViewById(R.id.patient_name);
        address = findViewById(R.id.address);
        daySpinner = findViewById(R.id.day_spinner);
        monthSpinner = findViewById(R.id.month_spinner);
        yearSpinner = findViewById(R.id.year_spinner);
        genderGroup = findViewById(R.id.gender_group);
        problem = findViewById(R.id.problem);
        bookButton = findViewById(R.id.book_button);

        // Fetch Doctor ID & Name from Intent
        Intent intent = getIntent();
        doctorId = intent.getStringExtra("doctor_id");
        doctorName = intent.getStringExtra("doctorName");
        appointmentStatus = intent.getStringExtra("appointment_status"); // Get

        Log.d("BookForm", "Doctor ID: " + doctorId);
        Log.d("BookForm", "Doctor Name: " + doctorName);
        Log.d("BookForm", "Appointment Status: " + appointmentStatus);

        // Set the status text
        if (appointmentStatus != null) {
            bookButton.setText(appointmentStatus);
        } else {
            bookButton.setText("book ");
        }

        // Handle Book Button Click
        bookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Data
                String name = patientName.getText().toString().trim();
                String addressText = address.getText().toString().trim();
                String selectedProblem = problem.getText().toString().trim();

                // Get selected values from Spinners
                try {
                    int day = Integer.parseInt(daySpinner.getSelectedItem().toString());
                    int month = monthSpinner.getSelectedItemPosition() + 1; // Month starts from 0 index
                    int year = Integer.parseInt(yearSpinner.getSelectedItem().toString());

                    // Calculate age
                    int calculatedAge = calculateAge(year, month, day);

                    // Get selected gender
                    int selectedGenderId = genderGroup.getCheckedRadioButtonId();
                    String gender = "";
                    if (selectedGenderId != -1) {
                        RadioButton selectedGender = findViewById(selectedGenderId);
                        gender = selectedGender.getText().toString();
                    }

                    // Validate required fields
                    if (name.isEmpty() || addressText.isEmpty() || selectedProblem.isEmpty() || gender.isEmpty()) {
                        Toast.makeText(book_form.this, "Please fill all details", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Move to next activity with all data
                    Intent intent = new Intent(book_form.this, pending_bill.class);
                    intent.putExtra("patient_name", name);
                    intent.putExtra("age", calculatedAge);
                    intent.putExtra("gender", gender);
                    intent.putExtra("problem", selectedProblem);
                    intent.putExtra("address", addressText);
                    intent.putExtra("doctor_id", doctorId);
                    intent.putExtra("doctorName", doctorName);
                    intent.putExtra("appointment_status", appointmentStatus);

                    startActivity(intent);

                } catch (Exception e) {
                    Toast.makeText(book_form.this, "Please select a valid date", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Function to calculate age based on selected date
    private int calculateAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        dob.set(year, month - 1, day); // Month is 0-based

        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                        today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    }
}
