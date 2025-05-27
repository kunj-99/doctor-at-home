package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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

    private static final String API_URL = "http://sxm.a58.mytemp.website/fetch_doctor.php?doctor_id=";

//    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details); // Make sure this matches your XML file

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
        backButton = findViewById(R.id.backButton); // Optional

        String doctorId = getIntent().getStringExtra("doctor_id");

        if (doctorId == null || doctorId.trim().isEmpty()) {
            Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show();
            Log.e("ERROR", "Doctor ID is null or empty");
            finish();
        } else {
            Log.d("DOCTOR_ID", "Fetching details for Doctor ID: " + doctorId);
            fetchDoctorDetails(doctorId);
        }

        // Back button (optional)
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void fetchDoctorDetails(String doctorId) {
        String url = API_URL + doctorId;
        Log.d("API_REQUEST", "Fetching data from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
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

                            String imageUrl = doctorData.optString("profile_picture", "");
                            if (imageUrl.isEmpty() || imageUrl.equals("null")) {
                                doctorImage.setImageResource(R.drawable.main5);
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
                    Log.e("API_ERROR", "Request failed: " + error.getMessage());
                    Toast.makeText(this, "Failed to load doctor details. Please try again.", Toast.LENGTH_SHORT).show();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
