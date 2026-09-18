package com.infowave.thedoctorathomeuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.infowave.thedoctorathomeuser.adapter.DoctorAdapter;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;

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
    private TextView tvListStatus;
    private ImageButton btnBack;

    private String categoryId, categoryName;
    private String userPincode = "";
    private String defaultPincode = "";

    private RequestQueue queue;
    private static final String TAG_PINCODE_REQUEST = "available_doctor_pincode";
    private static final String TAG_DOCTOR_LIST_REQUEST = "available_doctor_list";
    private String inFlightDoctorRequestKey = "";

    // === Low-frequency list refresh controls (live busy state is handled separately by batch status polling) ===
    private static final long REFRESH_INTERVAL_MS = 20_000L;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private boolean isPollingActive = false;
    private boolean isFetching = false;      // prevent overlapping calls
    private boolean isQuietRefresh = false;  // suppress loader/toast during background refresh
    private static final long SLOW_NETWORK_HINT_MS = 2500L;
    private Runnable slowNetworkHintRunnable;

    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            if (!isPollingActive) return;
            if (TextUtils.isEmpty(userPincode) || TextUtils.isEmpty(categoryId)) {
                scheduleNext();
                return;
            }
            if (isFetching) { // skip this tick if a request is running
                scheduleNext();
                return;
            }
            isQuietRefresh = true; // background: no loader / no toast
            fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
            scheduleNext();
        }
        private void scheduleNext() { refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS); }
    };

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


        queue = VolleySingleton.getInstance(this).getRequestQueue();

        // Browse screens use inline refresh/status UI instead of blocking the whole screen.

        // Views
        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefreshDoctors);
        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);
        tvNoDoctors = findViewById(R.id.tv_no_doctors);
        tvListStatus = findViewById(R.id.tv_list_status);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // subtle change animations on item updates
        if (recyclerView.getItemAnimator() instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(true);
        }

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

        // Pull-to-refresh (manual)
        swipeRefresh.setOnRefreshListener(() -> {
            if (!userPincode.isEmpty() && categoryId != null) {
                isQuietRefresh = false; // user intent → show normal UI
                fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
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
                        isQuietRefresh = false;
                        fetchDoctorsByPincodeAndCategory(userPincode, categoryId, true);
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty() && !defaultPincode.isEmpty()) {
                    userPincode = defaultPincode;
                    swipeRefresh.setRefreshing(true);
                    isQuietRefresh = false;
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
                isQuietRefresh = false;
                fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
            } else {
                setListStatus("Enter a valid 6-digit pincode to search for doctors.");
                Toast.makeText(available_doctor.this, "Please enter a valid 6-digit pincode.", Toast.LENGTH_SHORT).show();
            }
        });

        // First load: resolve the user's pincode, then load the doctor list.
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sp.getString("user_id", "");
        if (!userId.isEmpty()) {
            swipeRefresh.setRefreshing(true);
            setListStatus("Loading your saved area…");
            fetchUserPincode(userId);
        } else {
            try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
            Toast.makeText(available_doctor.this,
                    "Could not find your user profile. Please log in again.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        startPolling();
    }

    @Override protected void onPause() {
        super.onPause();
        stopPolling();
    }

    private void startPolling() {
        if (isPollingActive) return;
        isPollingActive = true;
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    private void stopPolling() {
        isPollingActive = false;
        refreshHandler.removeCallbacksAndMessages(null);
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
                            setListStatus("No pincode is saved in your profile. Enter a 6-digit pincode to continue.");
                            Toast.makeText(this, "No pincode is saved in your profile. Please enter one manually.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        setListStatus("We could not read your saved pincode. Enter it manually or try again.");
                        Toast.makeText(this, "Could not read your saved pincode. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    swipeRefresh.setRefreshing(true);
                    isQuietRefresh = false;
                    fetchDoctorsByPincodeAndCategory(userPincode, categoryId, false);
                },
                error -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    setListStatus("Could not load your saved area. Enter a 6-digit pincode to continue.");
                    Toast.makeText(this, com.infowave.thedoctorathomeuser.network.NetworkErrorUtil.userMessage(
                            this, error, "Could not load your saved area. Enter your pincode manually."), Toast.LENGTH_LONG).show();
                }
        );
        req.setTag(TAG_PINCODE_REQUEST);
        queue.cancelAll(TAG_PINCODE_REQUEST);
        queue.add(req);
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        final String safePincode = pincode == null ? "" : pincode.trim();
        final String safeCategoryId = categoryId == null ? "" : categoryId.trim();
        final String requestKey = safeCategoryId + "|" + safePincode;

        if (safePincode.length() != 6 || TextUtils.isEmpty(safeCategoryId)) {
            if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            return;
        }

        // Same request already running: do not duplicate it. If the user changed the
        // pincode/category, cancel the stale request so an old response cannot replace
        // the newly requested list.
        if (isFetching) {
            if (requestKey.equals(inFlightDoctorRequestKey)) return;
            cancelSlowNetworkHint();
            queue.cancelAll(TAG_DOCTOR_LIST_REQUEST);
            isFetching = false;
        }

        String url = ApiConfig.endpoint("getDoctorsByCategory.php",
                "pincode", safePincode,
                "category_id", safeCategoryId);

        isFetching = true;
        inFlightDoctorRequestKey = requestKey;
        if (!isQuietRefresh) {
            setListStatus("Checking doctors and latest availability in " + safePincode + "…");
            scheduleSlowNetworkHint(requestKey);
        }

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    cancelSlowNetworkHint();
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

                            String raw = d.optString("profile_picture", "");
                            String profilePicUrl = cleanUrl(raw);
                            newImageUrls.add(profilePicUrl);

                            newDuration.add(d.optString("experience_duration", ""));
                            newAutoStatuses.add(d.optString("auto_status", "Inactive"));
                        }

                        boolean listChanged =
                                !newDoctorIds.equals(doctorIds) ||
                                        !newNames.equals(names) ||
                                        !newSpecialties.equals(specialties) ||
                                        !newHospitals.equals(hospitals) ||
                                        !newRatings.equals(ratings) ||
                                        !newImageUrls.equals(imageUrls) ||
                                        !newDuration.equals(Duration);

                        boolean onlyStatusChanged = !newAutoStatuses.equals(autoStatuses) && !listChanged;

                        if (listChanged) {
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

                            // subtle fade only on big swaps (first load / major change)
                            if (!isQuietRefresh) {
                                recyclerView.setAlpha(0f);
                                recyclerView.animate().alpha(1f).setDuration(250).start();
                            }
                        } else if (onlyStatusChanged) {
                            // Fast path: only the status labels changed → rebind all items
                            autoStatuses.clear(); autoStatuses.addAll(newAutoStatuses);
                            adapter.notifyItemRangeChanged(0, autoStatuses.size());
                        }

                        tvNoDoctors.setVisibility(doctorIds.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(doctorIds.isEmpty() ? View.GONE : View.VISIBLE);

                        if (doctorIds.isEmpty()) {
                            tvNoDoctors.setText("No doctors are currently serving pincode " + safePincode + ". Try another pincode or pull down to refresh later.");
                            setListStatus("No doctors are currently available in " + safePincode + ".");
                        } else {
                            setListStatus("Showing " + doctorIds.size() + " doctor" + (doctorIds.size() == 1 ? "" : "s") + ". Availability updates automatically.");
                        }

                    } catch (JSONException e) {
                        if (!isQuietRefresh) {
                            setListStatus("Doctor information could not be read. Pull down to try again.");
                            Toast.makeText(this, "Could not read the doctor list. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        if (requestKey.equals(inFlightDoctorRequestKey)) {
                            isFetching = false;
                            inFlightDoctorRequestKey = "";
                        }
                        try { if (!isQuietRefresh) loaderutil.hideLoader(); } catch (Throwable ignored) { }
                        if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                        isQuietRefresh = false; // reset
                    }
                },
                error -> {
                    cancelSlowNetworkHint();
                    if (requestKey.equals(inFlightDoctorRequestKey)) {
                        isFetching = false;
                        inFlightDoctorRequestKey = "";
                    }
                    if (!isQuietRefresh) {
                        if (doctorIds.isEmpty()) {
                            tvNoDoctors.setText("Could not load doctors right now. Check your internet and pull down to retry.");
                            tvNoDoctors.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            setListStatus("Could not refresh doctors. Check your connection and try again.");
                        } else {
                            tvNoDoctors.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            setListStatus("Could not refresh right now. Showing the last loaded doctors.");
                        }
                        Toast.makeText(this, com.infowave.thedoctorathomeuser.network.NetworkErrorUtil.userMessage(
                                this, error, "Could not refresh doctors. Please try again."), Toast.LENGTH_LONG).show();
                        try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
                    }
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                    isQuietRefresh = false;
                }
        );

        req.setTag(TAG_DOCTOR_LIST_REQUEST);
        req.setShouldCache(false);
        queue.add(req);
    }

    private void scheduleSlowNetworkHint(String requestKey) {
        cancelSlowNetworkHint();
        slowNetworkHintRunnable = () -> {
            if (!isFinishing() && isFetching && requestKey.equals(inFlightDoctorRequestKey) && !isQuietRefresh) {
                boolean online = com.infowave.thedoctorathomeuser.network.NetworkErrorUtil.isConnected(this);
                setListStatus(online
                        ? "Internet is slow. Still checking doctors — loaded results will appear automatically."
                        : "No internet connection. Pull down to retry when you are online.");
            }
        };
        refreshHandler.postDelayed(slowNetworkHintRunnable, SLOW_NETWORK_HINT_MS);
    }

    private void cancelSlowNetworkHint() {
        if (slowNetworkHintRunnable != null) {
            refreshHandler.removeCallbacks(slowNetworkHintRunnable);
            slowNetworkHintRunnable = null;
        }
    }

    private void setListStatus(String message) {
        if (tvListStatus != null) {
            tvListStatus.setText(message == null ? "" : message);
        }
    }

    /**
     * Returns a clean, absolute URL for an image without any double-prefix.
     */
    private String cleanUrl(String raw) {
        if (raw == null) return DEFAULT_DOCTOR_IMAGE_URL;
        String u = raw.trim();
        if ((u.startsWith("\"") && u.endsWith("\"")) || (u.startsWith("'") && u.endsWith("'"))) {
            u = u.substring(1, u.length() - 1).trim();
        }
        if (u.isEmpty() || "null".equalsIgnoreCase(u)) {
            return DEFAULT_DOCTOR_IMAGE_URL;
        }
        if (u.startsWith("http://") || u.startsWith("https://")) {
            int secondHttps = u.indexOf("https://", 8);
            int secondHttp  = u.indexOf("http://", 7);
            int idx = -1;
            if (secondHttps >= 0) idx = secondHttps;
            else if (secondHttp >= 0) idx = secondHttp;
            if (idx > 0) {
                return u.substring(idx);
            }
            return u;
        }
        return u;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && edtPincode != null) {
            imm.hideSoftInputFromWindow(edtPincode.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        cancelSlowNetworkHint();
        stopPolling();
        if (queue != null) {
            queue.cancelAll(TAG_PINCODE_REQUEST);
            queue.cancelAll(TAG_DOCTOR_LIST_REQUEST);
        }
        try { loaderutil.hideLoader(); } catch (Throwable ignored) { }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(available_doctor.this, MainActivity.class);
        intent.putExtra("open_fragment", 1);
        startActivity(intent);
        finish();
    }

    // Last Updated: 2026-09-18 14:42 IST (Phase 4 slow-network discovery UX)
}
