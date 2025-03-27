package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

public class doctor_details extends AppCompatActivity {

    private TextView doctorName, doctorSpecialty, doctorHospital, doctorExperience, doctorFee, doctorAvailability, doctorQualification;
    private RatingBar doctorRating;
    private ImageView doctorImage, backButton;
    private Button bookButton;
    private ProgressBar progressBar;

    private static final String API_URL = "http://sxm.a58.mytemp.website/fetch_doctor.php?doctor_id=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details);

        // Initialize UI elements
        doctorName = findViewById(R.id.doctorName);
        doctorSpecialty = findViewById(R.id.doctor_specialty);
        doctorHospital = findViewById(R.id.doctor_hospital);
        doctorExperience = findViewById(R.id.experienceDetails);
        doctorFee = findViewById(R.id.doctor_fee);
        doctorAvailability = findViewById(R.id.doctor_availability);
        doctorQualification = findViewById(R.id.degreeDetails);
        doctorRating = findViewById(R.id.doctor_rating);
        doctorImage = findViewById(R.id.doctorImage);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backButton);

        // Get doctor_id from Intent
        String doctorId = getIntent().getStringExtra("doctor_id");

        if (doctorId == null || doctorId.trim().isEmpty()) {
            Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show();
            Log.e("ERROR", "Doctor ID is null or empty");
            finish();
        } else {
            Log.d("DOCTOR_ID", "Fetching details for Doctor ID: " + doctorId);
            fetchDoctorDetails(doctorId);
        }

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

//        // Handle Book Appointment button click
//        bookButton.setOnClickListener(v -> {
//            Intent intent = new Intent(doctor_details.this, book_form.class);
//            intent.putExtra("doctor_id", doctorId);
//            startActivity(intent);
//        });
    }

    // Fetch doctor details from API
    private void fetchDoctorDetails(String doctorId) {
        progressBar.setVisibility(View.VISIBLE);

        String url = API_URL + doctorId;
        Log.d("API_REQUEST", "Fetching data from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Log.d("API_RESPONSE", "Full response: " + response.toString());

                    if (response.optBoolean("success")) {
                        JSONObject doctorData = response.optJSONObject("data");

                        if (doctorData != null) {
                            doctorName.setText(doctorData.optString("full_name", "Not Available"));
                            doctorSpecialty.setText(doctorData.optString("specialization", "Not Available"));
                            doctorHospital.setText(doctorData.optString("hospital_affiliation", "Not Available"));
                            doctorExperience.setText(doctorData.optInt("experience_years", 0) + " years");
                            doctorFee.setText("₹" + doctorData.optDouble("consultation_fee", 0.00));
                            doctorAvailability.setText(doctorData.optString("availability_schedule", "Not Available"));
                            doctorQualification.setText(doctorData.optString("qualification", "Not Provided"));
                            doctorRating.setRating((float) doctorData.optDouble("rating", 0.0));

                            // Load doctor profile image using Glide
                            String imageUrl = doctorData.optString("profile_picture", "");
                            if (imageUrl.isEmpty() || imageUrl.equals("null")) {
                                doctorImage.setImageResource(R.drawable.main5); // Default image
                            } else {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.plasholder)
                                        .error(R.drawable.plaseholder_error)
                                        .into(doctorImage);
                            }
                        } else {
                            Log.e("API_ERROR", "Doctor data is null");
                            Toast.makeText(this, "Doctor details not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "API returned success=false", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("API_ERROR", "Request failed: " + error.getMessage());
                    Toast.makeText(this, "Failed to load doctor details. Please try again.", Toast.LENGTH_SHORT).show();
                });

        // Set timeout policy to avoid slow responses causing errors
        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
