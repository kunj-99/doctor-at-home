package com.infowave.thedoctorathomeuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

    // Absolute fallback image (no ApiConfig prefixing)
    private static final String DEFAULT_DOCTOR_IMAGE_URL =
            "https://thedoctorathome.in/doctor_images/default.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        // ---- Black bars via scrim views (null-safe) ----
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);
        try {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            WindowInsetsControllerCompat ctrl =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            ctrl.setAppearanceLightStatusBars(false);
            ctrl.setAppearanceLightNavigationBars(false);

            View root = findViewById(android.R.id.content);
            if (root != null) {
                ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                    Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    if (statusScrim != null) {
                        ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                        lp.height = sb.top;
                        statusScrim.setLayoutParams(lp);
                        statusScrim.setVisibility(sb.top > 0 ? View.VISIBLE : View.GONE);
                    }
                    if (navScrim != null) {
                        ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                        lp.height = sb.bottom;
                        navScrim.setLayoutParams(lp);
                        navScrim.setVisibility(sb.bottom > 0 ? View.VISIBLE : View.GONE);
                    }
                    return insets;
                });
            }
        } catch (Throwable ignored) { }
        // -----------------------------------------------

        queue = Volley.newRequestQueue(this);

        // Loader (guard against crashes if not present)
        try { loaderutil.showLoader(available_doctor.this); } catch (Throwable ignored) { }

        // Views
        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefreshDoctors);
        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Attach an EMPTY adapter immediately to avoid: “No adapter attached; skipping layout”
        attachEmptyAdapter();

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

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> {
            if (!userPincode.isEmpty() && categoryId != null) {
                updateDoctorAutoStatus(() -> fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false));
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        // Pincode watcher
        edtPincode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    hideKeyboard();
                    userPincode = s.toString().trim();
                    if (categoryId != null) {
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

        // First load: update auto-status → resolve default pincode → load list
        updateDoctorAutoStatus(() -> {
            SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = sp.getString("user_id", "");
            if (!userId.isEmpty()) {
                fetchUserPincode(userId);
            } else {
                try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                Toast.makeText(available_doctor.this, "Could not find your user profile. Please log in again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachEmptyAdapter() {
        if (adapter == null) {
            adapter = new DoctorAdapter(
                    available_doctor.this,
                    doctorIds, names, specialties, hospitals,
                    ratings, imageUrls, Duration, autoStatuses
            );
            recyclerView.setAdapter(adapter);
        }
    }

    private void fetchUserPincode(String userId) {
        String url = ApiConfig.endpoint("user_pincode.php", "user_id", userId);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String pin = response.has("pincode") ? response.getString("pincode") : "";
                        if (pin != null && !pin.isEmpty()) {
                            defaultPincode = pin;
                            userPincode = defaultPincode;
                        } else {
                            try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                            Toast.makeText(this, "No pincode found for your profile.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                        Toast.makeText(this, "Could not read your pincode. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    swipeRefresh.setRefreshing(true);
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                },
                error -> {
                    try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                    Toast.makeText(this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(req);
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        String url = ApiConfig.endpoint("getDoctorsByCategory.php", "pincode", pincode, "category_id", categoryId);

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
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
                            JSONObject d = response.getJSONObject(i);
                            newDoctorIds.add(d.optString("doctor_id", ""));
                            newNames.add(d.optString("full_name", ""));
                            newSpecialties.add(d.optString("specialization", ""));
                            newHospitals.add(d.optString("hospital_affiliation", ""));
                            newRatings.add((float) d.optDouble("rating", 0));

                            // Use image exactly as provided by API; clean up any accidental double-prefix
                            String raw = d.optString("profile_picture", "");
                            String profilePicUrl = cleanUrl(raw);
                            newImageUrls.add(profilePicUrl);

                            newDuration.add(d.optString("experience_duration", ""));
                            newAutoStatuses.add(d.optString("auto_status", "Inactive"));
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
                                attachEmptyAdapter();
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(300).start();
                        }

                        tvNoDoctors.setVisibility(doctorIds.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(doctorIds.isEmpty() ? View.GONE : View.VISIBLE);

                        if (doctorIds.isEmpty()) {
                            Toast.makeText(this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();
                    } finally {
                        try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                        if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                    }
                },
                error -> {
                    tvNoDoctors.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(this, "Currently no doctors at your pincode.", Toast.LENGTH_SHORT).show();

                    try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                }
        );

        req.setShouldCache(false);
        queue.add(req);
    }

    /**
     * Returns a clean, absolute URL for an image without any double-prefix.
     * Rules:
     *  - If empty → use DEFAULT_DOCTOR_IMAGE_URL
     *  - If already starts with http/https → return as-is
     *  - If it accidentally contains ".../doctor_images/https://..." → strip the leading part
     *  - Otherwise return as-is (no prefixing here by design)
     */
    private String cleanUrl(String raw) {
        if (raw == null) return DEFAULT_DOCTOR_IMAGE_URL;
        String u = raw.trim();
        // strip quotes if any (rare server bugs)
        if ((u.startsWith("\"") && u.endsWith("\"")) || (u.startsWith("'") && u.endsWith("'"))) {
            u = u.substring(1, u.length() - 1).trim();
        }
        if (u.isEmpty() || "null".equalsIgnoreCase(u)) {
            return DEFAULT_DOCTOR_IMAGE_URL;
        }
        // If it already has a scheme, return as-is (FULL URL case)
        if (u.startsWith("http://") || u.startsWith("https://")) {
            // Fix the specific double-prefix pattern seen in logs:
            // https://domain/doctor_images/https://domain/doctor_images/file.jpg
            int secondHttps = u.indexOf("https://", 8); // find after the first scheme
            int secondHttp  = u.indexOf("http://", 7);
            int idx = -1;
            if (secondHttps >= 0) idx = secondHttps;
            else if (secondHttp >= 0) idx = secondHttp;
            if (idx > 0) {
                // Keep only from the second scheme onward
                return u.substring(idx);
            }
            return u;
        }
        // Relative/filename case: leave untouched (backend should now send full URLs)
        return u;
    }

    private void updateDoctorAutoStatus(final Runnable onComplete) {
        String updateUrl = ApiConfig.endpoint("update_doctor_status.php");

        StringRequest req = new StringRequest(Request.Method.GET, updateUrl,
                response -> { if (onComplete != null) onComplete.run(); },
                error -> { if (onComplete != null) onComplete.run(); }
        );
        queue.add(req);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && edtPincode != null) {
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
