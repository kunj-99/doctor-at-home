package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.Adapter.DoctorAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class available_doctor extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DoctorAdapter adapter;
    private final ArrayList<String> doctorIds = new ArrayList<>();
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> specialties = new ArrayList<>();
    private final ArrayList<String> hospitals = new ArrayList<>();
    private final ArrayList<Float> ratings = new ArrayList<>();
    private final ArrayList<String> imageUrls = new ArrayList<>();
    // New list for experience duration
    private final ArrayList<String> Duration = new ArrayList<>();

    private EditText edtPincode;
    private ImageButton btnSearch;
    private TextView tvNoDoctors;
    private ImageButton btnBack;

    private String categoryId, categoryName;
    private static final String DEFAULT_PINCODE = "110001";

    private final Handler handler = new Handler();
    private Runnable autoRefreshRunnable;
    private static final int REFRESH_INTERVAL = 10000;

    // Flag to track if this activity is visible
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            // Redirect to the host activity with the Book appointment fragment
            Intent intent = new Intent(available_doctor.this, MainActivity.class);
            intent.putExtra("open_fragment", 1);
            startActivity(intent);
            finish();
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);

        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        // Fetch doctors for default pincode initially
        fetchDoctorsByPincodeAndCategory(DEFAULT_PINCODE, categoryId, false);

        // Define auto-refresh runnable
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Only auto-refresh if the activity is visible.
                if (isActivityVisible) {
                    fetchDoctorsByPincodeAndCategory(DEFAULT_PINCODE, categoryId, false);
                    handler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };

        btnSearch.setOnClickListener(v -> {
            String pincode = edtPincode.getText().toString().trim();
            if (!pincode.isEmpty() && categoryId != null) {
                fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
            } else {
                if (isActivityVisible) {
                    Toast.makeText(available_doctor.this, "Please enter a valid pincode", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        // Start auto-refresh when this activity is visible
        handler.postDelayed(autoRefreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        // Stop auto-refresh when this activity is not visible
        handler.removeCallbacks(autoRefreshRunnable);
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        String url = "http://sxm.a58.mytemp.website/getDoctorsByCategory.php?pincode="
                + pincode + "&category_id=" + categoryId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Clear all lists to avoid duplicate data
                        doctorIds.clear();
                        names.clear();
                        specialties.clear();
                        hospitals.clear();
                        ratings.clear();
                        imageUrls.clear();
                        Duration.clear();

                        if (response.length() == 0) {
                            tvNoDoctors.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            // Only show toast if activity is visible
                            if (isActivityVisible) {
                                if (!userSearch) {
                                    Toast.makeText(available_doctor.this,
                                            "No doctors found in default location. Try another pincode.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(available_doctor.this,
                                            "No doctors found for this pincode",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                            return;
                        } else {
                            tvNoDoctors.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject doctor = response.getJSONObject(i);
                                doctorIds.add(doctor.getString("doctor_id"));
                                names.add(doctor.getString("full_name"));
                                specialties.add(doctor.getString("specialization"));
                                hospitals.add(doctor.getString("hospital_affiliation"));
                                ratings.add((float) doctor.getDouble("rating"));
                                imageUrls.add(doctor.getString("profile_picture"));
                                // New: add experience duration from JSON response
                                Duration.add(doctor.getString("experience_duration"));
                            }

                            adapter = new DoctorAdapter(available_doctor.this, doctorIds, names, specialties, hospitals, ratings, imageUrls, Duration);
                            recyclerView.setAdapter(adapter);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (isActivityVisible) {
                                Toast.makeText(available_doctor.this, "Data parsing error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                tvNoDoctors.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                if (isActivityVisible) {
                    Toast.makeText(available_doctor.this, "No Doctor available, try another pincode", Toast.LENGTH_SHORT).show();
                }
                Log.e("VolleyError", error.toString());
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoRefreshRunnable);
    }
}
