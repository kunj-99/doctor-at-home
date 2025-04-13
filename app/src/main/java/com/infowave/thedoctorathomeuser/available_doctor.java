package com.infowave.thedoctorathomeuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    private ImageButton btnSearch, btnBack;
    private TextView tvNoDoctors;
    private View loaderLayout;

    private String categoryId, categoryName;
    private String userPincode = "";
    private String defaultPincode = "";

    private final Handler handler = new Handler();
    private static final int DOCTOR_STATUS_REFRESH_INTERVAL = 1000;
    private static final int DOCTOR_LIST_REFRESH_INTERVAL = 1500;
    private boolean isActivityVisible = false;

    private final Runnable autoUpdateStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (isActivityVisible) {
                updateDoctorAutoStatus(null);
                handler.postDelayed(this, DOCTOR_STATUS_REFRESH_INTERVAL);
            }
        }
    };

    private final Runnable autoRefreshDoctorsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isActivityVisible) {
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
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutAnimation(android.view.animation.AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fade_in));

        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);
        btnBack = findViewById(R.id.btn_back);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);

        edtPincode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(available_doctor.this, MainActivity.class);
            intent.putExtra("open_fragment", 1);
            startActivity(intent);
            finish();
        });

        edtPincode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(edtPincode.getWindowToken(), 0);
                    userPincode = s.toString();
                    if (categoryId != null) {
                        loaderLayout.setVisibility(View.VISIBLE);
                        fetchDoctorsByPincodeAndCategory(userPincode, categoryId, true);
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty() && !defaultPincode.isEmpty()) {
                    userPincode = defaultPincode;
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                }
            }
        });

        btnSearch.setOnClickListener(v -> {
            String pincode = edtPincode.getText().toString().trim();
            if (!pincode.isEmpty() && categoryId != null) {
                loaderLayout.setVisibility(View.VISIBLE);
                userPincode = pincode;
                fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
            } else {
                Toast.makeText(this, "Please enter a valid pincode", Toast.LENGTH_SHORT).show();
            }
        });

        updateDoctorAutoStatus(() -> {
            SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = sp.getString("user_id", "");
            if (!userId.isEmpty()) {
                fetchUserPincode(userId);
            } else {
                Toast.makeText(this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show();
            }

            loaderLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            isActivityVisible = true;
            handler.postDelayed(autoUpdateStatusRunnable, DOCTOR_STATUS_REFRESH_INTERVAL);
            handler.postDelayed(autoRefreshDoctorsRunnable, DOCTOR_LIST_REFRESH_INTERVAL);
        });
    }

    private void fetchUserPincode(String userId) {
        String url = "http://sxm.a58.mytemp.website/user_pincode.php?user_id=" + userId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("pincode") && !response.getString("pincode").isEmpty()) {
                            defaultPincode = response.getString("pincode");
                            userPincode = defaultPincode;
                        } else {
                            Toast.makeText(this, "No pincode found for the user", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing pincode", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                },
                error -> Toast.makeText(this, "Error fetching pincode", Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(request);
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        String url = "http://sxm.a58.mytemp.website/getDoctorsByCategory.php?pincode=" + pincode + "&category_id=" + categoryId;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() == 0) {
                        tvNoDoctors.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        loaderLayout.setVisibility(View.GONE);
                        return;
                    } else {
                        tvNoDoctors.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    // Step 1: Check if data actually changed
                    boolean isChanged = response.length() != doctorIds.size();
                    ArrayList<String> tempDoctorIds = new ArrayList<>();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject doctor = response.getJSONObject(i);
                            tempDoctorIds.add(doctor.getString("doctor_id"));
                        }

                        if (!isChanged) {
                            for (int i = 0; i < tempDoctorIds.size(); i++) {
                                if (!tempDoctorIds.get(i).equals(doctorIds.get(i))) {
                                    isChanged = true;
                                    break;
                                }
                            }
                        }

                        // Step 2: If changed, refresh data
                        if (isChanged) {
                            doctorIds.clear();
                            names.clear();
                            specialties.clear();
                            hospitals.clear();
                            ratings.clear();
                            imageUrls.clear();
                            Duration.clear();

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

                            if (adapter == null) {
                                adapter = new DoctorAdapter(this, doctorIds, names, specialties, hospitals, ratings, imageUrls, Duration);
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.notifyDataSetChanged(); // Smooth update
                            }
                            recyclerView.scheduleLayoutAnimation();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Data parsing error", Toast.LENGTH_SHORT).show();
                    }

                    loaderLayout.setVisibility(View.GONE);
                },
                error -> {
                    tvNoDoctors.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    loaderLayout.setVisibility(View.GONE);
                    Toast.makeText(this, "No Doctor available, try another pincode", Toast.LENGTH_SHORT).show();
                });
        Volley.newRequestQueue(this).add(request);
    }


    private void updateDoctorAutoStatus(final Runnable onComplete) {
        String updateUrl = "http://sxm.a58.mytemp.website/update_doctor_status.php";
        StringRequest updateRequest = new StringRequest(Request.Method.GET, updateUrl,
                response -> {
                    if (onComplete != null) onComplete.run();
                },
                error -> {
                    if (onComplete != null) onComplete.run();
                });
        Volley.newRequestQueue(this).add(updateRequest);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_fragment", 1);
        startActivity(intent);
        finish();
    }
}
