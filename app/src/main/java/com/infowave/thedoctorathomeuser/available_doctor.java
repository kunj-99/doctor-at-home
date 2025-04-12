package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.DoctorAdapter;

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
    private final ArrayList<String> Duration = new ArrayList<>();

    private EditText edtPincode;
    private ImageButton btnSearch;
    private TextView tvNoDoctors;
    private ImageButton btnBack;
    private View loaderLayout;

    private String categoryId, categoryName;
    private String userPincode = ""; // pincode will now be fetched from the server

    private final Handler handler = new Handler();
    private static final int DOCTOR_STATUS_REFRESH_INTERVAL = 2000;
    private static final int DOCTOR_LIST_REFRESH_INTERVAL = 15000;
    private boolean isActivityVisible = false;

    private final Runnable autoUpdateStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (isActivityVisible) {
                // Call update with a null callback for silent updates
                updateDoctorAutoStatus(null);
                handler.postDelayed(this, DOCTOR_STATUS_REFRESH_INTERVAL);
            }
        }
    };

    private final Runnable autoRefreshDoctorsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isActivityVisible) {
                // Ensure we have a valid pincode before fetching
                if (!userPincode.isEmpty()) {
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                }
                handler.postDelayed(this, DOCTOR_LIST_REFRESH_INTERVAL);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        loaderLayout = findViewById(R.id.loaderLayout);
        loaderLayout.setVisibility(View.VISIBLE); // Show loader immediately

        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);

        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        // On back button click, go to MainActivity with fragment 1.
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(available_doctor.this, MainActivity.class);
            intent.putExtra("open_fragment", 1);
            startActivity(intent);
            finish();
        });

        // Instead of a fixed delay, update doctor status and then load UI
        updateDoctorAutoStatus(new Runnable() {
            @Override
            public void run() {
                // After background status update, fetch the user's pincode
                SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String userId = sp.getString("user_id", "");

                if (!userId.isEmpty()) {
                    fetchUserPincode(userId);
                } else {
                    Toast.makeText(available_doctor.this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show();
                }

                // Hide loader and display the actual UI
                loaderLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                isActivityVisible = true;
                handler.postDelayed(autoUpdateStatusRunnable, DOCTOR_STATUS_REFRESH_INTERVAL);
                handler.postDelayed(autoRefreshDoctorsRunnable, DOCTOR_LIST_REFRESH_INTERVAL);
            }
        });

        btnSearch.setOnClickListener(v -> {
            String pincode = edtPincode.getText().toString().trim();
            if (!pincode.isEmpty() && categoryId != null) {
                userPincode = pincode;
                fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
            } else {
                if (isActivityVisible) {
                    Toast.makeText(available_doctor.this, "Please enter a valid pincode", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchUserPincode(String userId) {
        String url = "http://sxm.a58.mytemp.website/user_pincode.php?user_id=" + userId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("pincode") && !response.getString("pincode").isEmpty()) {
                            userPincode = response.getString("pincode");
                        } else {
                            Toast.makeText(available_doctor.this, "No pincode found for the user", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        Toast.makeText(available_doctor.this, "Error parsing pincode", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                },
                error -> {
                    Toast.makeText(available_doctor.this, "Error fetching pincode", Toast.LENGTH_SHORT).show();
                }
        );
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        String url = "http://sxm.a58.mytemp.website/getDoctorsByCategory.php?pincode=" + pincode + "&category_id=" + categoryId;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
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
                        if (isActivityVisible) {
                            if (!userSearch) {
                                Toast.makeText(available_doctor.this,
                                        "No doctors found in your location. Try another pincode.",
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

                            String profilePicUrl = doctor.optString("profile_picture", "");
                            if (profilePicUrl.isEmpty() || profilePicUrl.equals("null")) {
                                profilePicUrl = "http://sxm.a58.mytemp.website/doctor_images/default.png";
                            }
                            imageUrls.add(profilePicUrl);

                            Duration.add(doctor.getString("experience_duration"));
                        }
                        adapter = new DoctorAdapter(available_doctor.this, doctorIds, names, specialties, hospitals, ratings, imageUrls, Duration);
                        recyclerView.setAdapter(adapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (isActivityVisible) {
                            Toast.makeText(available_doctor.this, "Data parsing error", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> {
                    tvNoDoctors.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if (isActivityVisible) {
                        Toast.makeText(available_doctor.this, "No Doctor available, try another pincode", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    /**
     * Updates doctor status and executes the onComplete callback after the request finishes.
     * If onComplete is null, the callback is simply ignored.
     */
    private void updateDoctorAutoStatus(final Runnable onComplete) {
        String updateUrl = "http://sxm.a58.mytemp.website/update_doctor_status.php";
        StringRequest updateRequest = new StringRequest(Request.Method.GET, updateUrl,
                response -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                },
                error -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
        );
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(updateRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        handler.removeCallbacks(autoUpdateStatusRunnable);
        handler.removeCallbacks(autoRefreshDoctorsRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoUpdateStatusRunnable);
        handler.removeCallbacks(autoRefreshDoctorsRunnable);
    }

    // Override the system back press to go to MainActivity with fragment 1
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(available_doctor.this, MainActivity.class);
        intent.putExtra("open_fragment", 1);
        startActivity(intent);
        finish();
    }
}
