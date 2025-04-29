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
    private final ArrayList<String> autoStatuses = new ArrayList<>();

    private EditText edtPincode;
    private ImageButton btnSearch;
    private TextView tvNoDoctors;
    private ImageButton btnBack;

    private String categoryId, categoryName;
    private String userPincode = "";
    private String defaultPincode = "";

    private final Handler handler = new Handler();
    private static final int DOCTOR_STATUS_REFRESH_INTERVAL = 2000;
    private static final int DOCTOR_LIST_REFRESH_INTERVAL = 15000;
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

        loaderutil.showLoader(available_doctor.this);

        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        edtPincode = findViewById(R.id.edt_pincode);
        edtPincode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        btnSearch = findViewById(R.id.btn_search);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);

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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    hideKeyboard();
                    userPincode = s.toString();
                    if (categoryId != null && !userPincode.isEmpty()) {
                        fetchDoctorsByPincodeAndCategory(userPincode, categoryId, true);
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    if (!defaultPincode.isEmpty()) {
                        userPincode = defaultPincode;
                        fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                    }
                }
            }
        });

        btnSearch.setOnClickListener(v -> {
            String pincode = edtPincode.getText().toString().trim();
            if (!pincode.isEmpty() && categoryId != null) {
                userPincode = pincode;
                fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
            } else {
                Toast.makeText(available_doctor.this, "Please enter a valid pincode", Toast.LENGTH_SHORT).show();
            }
        });

        updateDoctorAutoStatus(() -> {
            SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = sp.getString("user_id", "");

            if (!userId.isEmpty()) {
                fetchUserPincode(userId);
            } else {
                Toast.makeText(available_doctor.this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show();
            }

            loaderutil.hideLoader();
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
                            Toast.makeText(available_doctor.this, "No pincode found for the user", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        Toast.makeText(available_doctor.this, "Error parsing pincode", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                },
                error -> Toast.makeText(available_doctor.this, "Error fetching pincode", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        String url = "http://sxm.a58.mytemp.website/getDoctorsByCategory.php?pincode=" + pincode + "&category_id=" + categoryId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    ArrayList<String> newDoctorIds = new ArrayList<>();
                    ArrayList<String> newNames = new ArrayList<>();
                    ArrayList<String> newSpecialties = new ArrayList<>();
                    ArrayList<String> newHospitals = new ArrayList<>();
                    ArrayList<Float> newRatings = new ArrayList<>();
                    ArrayList<String> newImageUrls = new ArrayList<>();
                    ArrayList<String> newDuration = new ArrayList<>();
                    ArrayList<String> newAutoStatuses = new ArrayList<>();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject doctor = response.getJSONObject(i);
                            newDoctorIds.add(doctor.getString("doctor_id"));
                            newNames.add(doctor.getString("full_name"));
                            newSpecialties.add(doctor.getString("specialization"));
                            newHospitals.add(doctor.getString("hospital_affiliation"));
                            newRatings.add((float) doctor.getDouble("rating"));

                            String profilePicUrl = doctor.optString("profile_picture", "");
                            if (profilePicUrl.isEmpty() || profilePicUrl.equals("null")) {
                                profilePicUrl = "http://sxm.a58.mytemp.website/doctor_images/default.png";
                            }
                            newImageUrls.add(profilePicUrl);
                            newDuration.add(doctor.getString("experience_duration"));
                            newAutoStatuses.add(doctor.optString("auto_status", "Inactive"));
                        }

                        if (!newDoctorIds.equals(doctorIds) || !newAutoStatuses.equals(autoStatuses)) {
                            doctorIds.clear(); doctorIds.addAll(newDoctorIds);
                            names.clear(); names.addAll(newNames);
                            specialties.clear(); specialties.addAll(newSpecialties);
                            hospitals.clear(); hospitals.addAll(newHospitals);
                            ratings.clear(); ratings.addAll(newRatings);
                            imageUrls.clear(); imageUrls.addAll(newImageUrls);
                            Duration.clear(); Duration.addAll(newDuration);
                            autoStatuses.clear(); autoStatuses.addAll(newAutoStatuses);

                            if (adapter == null) {
                                adapter = new DoctorAdapter(available_doctor.this, doctorIds, names, specialties,
                                        hospitals, ratings, imageUrls, Duration, autoStatuses);
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(400).start();
                        }

                        tvNoDoctors.setVisibility(doctorIds.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(doctorIds.isEmpty() ? View.GONE : View.VISIBLE);

                    } catch (JSONException e) {
                        Toast.makeText(available_doctor.this, "Data parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    tvNoDoctors.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(available_doctor.this, "Error fetching doctors.", Toast.LENGTH_SHORT).show();
                }
        );

        request.setShouldCache(false);
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
                }
        );
        Volley.newRequestQueue(this).add(updateRequest);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edtPincode.getWindowToken(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        handler.postDelayed(autoUpdateStatusRunnable, DOCTOR_STATUS_REFRESH_INTERVAL);
        handler.postDelayed(autoRefreshDoctorsRunnable, DOCTOR_LIST_REFRESH_INTERVAL);
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
        Intent intent = new Intent(available_doctor.this, MainActivity.class);
        intent.putExtra("open_fragment", 1);
        startActivity(intent);
        finish();
    }
}
