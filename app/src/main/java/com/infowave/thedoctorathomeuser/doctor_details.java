package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details);

        // ===== PERFECT BLACK TOP & BOTTOM USING VIEW SCRIMS =====
        // Draw edge-to-edge so our scrim Views can occupy status/nav bar areas
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Paint system bars black to avoid any flash
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        // White system icons on dark bars
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        // Optional: reduce OEM contrast/divider effects
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(Color.BLACK);
        }

        // Size scrim views from WindowInsets
        final android.view.View statusScrim = findViewById(R.id.status_bar_scrim);
        final android.view.View navScrim    = findViewById(R.id.navigation_bar_scrim);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                statusScrim.getLayoutParams().height = sys.top;
                statusScrim.requestLayout();
                statusScrim.setVisibility(sys.top > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            if (navScrim != null) {
                navScrim.getLayoutParams().height = sys.bottom;
                navScrim.requestLayout();
                navScrim.setVisibility(sys.bottom > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            return insets;
        });

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
        backButton = findViewById(R.id.backButton);

        String doctorId = getIntent().getStringExtra("doctor_id");

        if (doctorId == null || doctorId.trim().isEmpty()) {
            Toast.makeText(this, "Unable to find this doctor. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            fetchDoctorDetails(doctorId);
        }

        // Back button
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void fetchDoctorDetails(String doctorId) {
        String url = ApiConfig.endpoint("fetch_doctor.php", "doctor_id", doctorId);

        @SuppressLint("SetTextI18n")
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
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
                            Toast.makeText(this, "Doctor details are currently unavailable.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Sorry, we could not load this doctor's details.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Could not connect. Please check your internet and try again.", Toast.LENGTH_SHORT).show());

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}