package com.infowave.thedoctorathomeuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipeRefresh;
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

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        queue = Volley.newRequestQueue(this);

        // Initial blocking loader (your project utility)
        loaderutil.showLoader(available_doctor.this);

        // Views
        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefreshDoctors);
        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        edtPincode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});

        // Intent extras
        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(available_doctor.this, MainActivity.class);
            intent.putExtra("open_fragment", 1);
            startActivity(intent);
            finish();
        });

        // Pull-to-refresh: refresh current list (silent loader; uses swipe spinner)
        swipeRefresh.setOnRefreshListener(() -> {
            if (!userPincode.isEmpty() && categoryId != null) {
                // Optionally ping auto-status before refreshing the list
                updateDoctorAutoStatus(() -> fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false));
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        // Pincode watcher
        edtPincode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    hideKeyboard();
                    userPincode = s.toString().trim();
                    if (categoryId != null) {
                        // Explicit user search → show swipe spinner briefly for consistency
                        swipeRefresh.setRefreshing(true);
                        fetchDoctorsByPincodeAndCategory(userPincode, categoryId, true);
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty() && !defaultPincode.isEmpty()) {
                    userPincode = defaultPincode;
                    swipeRefresh.setRefreshing(true);
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                }
            }
        });

        // Search button
        btnSearch.setOnClickListener(v -> {
            String pincode = edtPincode.getText().toString().trim();
            if (!pincode.isEmpty() && categoryId != null) {
                userPincode = pincode;
                swipeRefresh.setRefreshing(true);
                fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
            } else {
                Toast.makeText(available_doctor.this, "Please enter a valid pincode.", Toast.LENGTH_SHORT).show();
            }
        });

        // First-time flow: update auto-status, then resolve user's default pincode, finally load list
        updateDoctorAutoStatus(() -> {
            SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = sp.getString("user_id", "");
            if (!userId.isEmpty()) {
                fetchUserPincode(userId);
            } else {
                loaderutil.hideLoader();
                Toast.makeText(available_doctor.this, "Could not find your user profile. Please log in again.", Toast.LENGTH_SHORT).show();
            }
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
                            loaderutil.hideLoader();
                            Toast.makeText(available_doctor.this, "No pincode found for your profile.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        loaderutil.hideLoader();
                        Toast.makeText(available_doctor.this, "Could not read your pincode. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Initial list load (we use swipe spinner for consistency)
                    swipeRefresh.setRefreshing(true);
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(available_doctor.this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(request);
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
                            newRatings.add((float) doctor.optDouble("rating", 0));
                            String profilePicUrl = doctor.optString("profile_picture", "");
                            if (profilePicUrl.isEmpty() || "null".equalsIgnoreCase(profilePicUrl)) {
                                profilePicUrl = "http://sxm.a58.mytemp.website/doctor_images/default.png";
                            }
                            newImageUrls.add(profilePicUrl);
                            newDuration.add(doctor.optString("experience_duration", ""));
                            newAutoStatuses.add(doctor.optString("auto_status", "Inactive"));
                        }

                        boolean changed = !newDoctorIds.equals(doctorIds) || !newAutoStatuses.equals(autoStatuses);
                        if (changed) {
                            doctorIds.clear(); doctorIds.addAll(newDoctorIds);
                            names.clear(); names.addAll(newNames);
                            specialties.clear(); specialties.addAll(newSpecialties);
                            hospitals.clear(); hospitals.addAll(newHospitals);
                            ratings.clear(); ratings.addAll(newRatings);
                            imageUrls.clear(); imageUrls.addAll(newImageUrls);
                            Duration.clear(); Duration.addAll(newDuration);
                            autoStatuses.clear(); autoStatuses.addAll(newAutoStatuses);

                            if (adapter == null) {
                                adapter = new DoctorAdapter(
                                        available_doctor.this,
                                        doctorIds, names, specialties, hospitals,
                                        ratings, imageUrls, Duration, autoStatuses
                                );
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(300).start();
                        }

                        // Empty state + visibility
                        tvNoDoctors.setVisibility(doctorIds.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(doctorIds.isEmpty() ? View.GONE : View.VISIBLE);

                        if (doctorIds.isEmpty()) {
                            Toast.makeText(available_doctor.this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(available_doctor.this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Stop loaders
                        loaderutil.hideLoader();
                        if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                    }
                },
                error -> {
                    tvNoDoctors.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(available_doctor.this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();

                    // Stop loaders
                    loaderutil.hideLoader();
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                }
        );

        request.setShouldCache(false);
        queue.add(request);
    }

    private void updateDoctorAutoStatus(final Runnable onComplete) {
        String updateUrl = "http://sxm.a58.mytemp.website/update_doctor_status.php";
        StringRequest updateRequest = new StringRequest(Request.Method.GET, updateUrl,
                response -> { if (onComplete != null) onComplete.run(); },
                error -> { if (onComplete != null) onComplete.run(); }
        );
        queue.add(updateRequest);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edtPincode.getWindowToken(), 0);
        }
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
