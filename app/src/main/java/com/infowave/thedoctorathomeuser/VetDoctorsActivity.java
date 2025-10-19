package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.VetDoctorsAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class VetDoctorsActivity extends AppCompatActivity implements VetDoctorsAdapter.OnDoctorClickListener {

    private static final String TAG = "VET_FLOW";

    private RecyclerView recyclerView;
    private VetDoctorsAdapter adapter;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private LinearLayout llEmptyState;
    private TextView tvDoctorsCount;

    private final ArrayList<JSONObject> doctors = new ArrayList<>();
    private final ArrayList<JSONObject> filteredDoctors = new ArrayList<>();

    private View statusScrim, navScrim;
    private RequestQueue queue;

    // === Inputs from Intent ===
    private int vetCategoryId = -1;         // from doctor_categories
    private int animalCategoryId = -1;      // from animal_categories
    private String animalName = "";
    private String doctorType = "";         // from intent

    // === Pincode Management ===
    private String defaultPincode = "";
    private String activePincode  = "";

    // === SharedPreferences ===
    private static final String PREFS       = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PINCODE = "pincode";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_doctors);

        Log.d(TAG, "VetDoctorsActivity.onCreate() initialized");

        initViews();
        setupEdgeToEdge();

        // --- Read intent from VetAnimalsActivity ---
        vetCategoryId    = getIntent().getIntExtra("vet_category_id", -1);
        animalCategoryId = getIntent().getIntExtra("animal_category_id", -1);
        animalName       = getIntent().getStringExtra("animal_category_name"); // CORRECT key
        doctorType       = getIntent().getStringExtra("doctor_type"); // CORRECT key

        Log.d(TAG, "Received from VetAnimalsActivity → "
                + "vet_category_id=" + vetCategoryId
                + ", animal_category_id=" + animalCategoryId
                + ", animal_name=" + animalName
                + ", doctor_type=" + doctorType);

        if (vetCategoryId <= 0 || animalCategoryId <= 0 || doctorType == null || doctorType.isEmpty()) {
            Toast.makeText(this, "Invalid category selection", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Invalid IDs or missing doctorType → vetCategoryId=" + vetCategoryId
                    + ", animalCategoryId=" + animalCategoryId
                    + ", doctorType=" + doctorType);
            finish();
            return;
        }

        setupSearchBar();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VetDoctorsAdapter(filteredDoctors, this);
        recyclerView.setAdapter(adapter);

        queue = Volley.newRequestQueue(this);

        // --- Resolve default pincode ---
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedPin = sp.getString(KEY_PINCODE, "");
        if (savedPin != null && savedPin.trim().length() == 6) {
            defaultPincode = savedPin.trim();
            activePincode  = defaultPincode;
            etSearch.setText(defaultPincode);
            fetchVets(defaultPincode, true);
        } else {
            String userId = sp.getString(KEY_USER_ID, "");
            if (userId != null && !userId.isEmpty()) {
                resolvePincodeFromServer(userId);
            } else {
                fetchVets("", true);
            }
        }
    }

    private void initViews() {
        etSearch       = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        llEmptyState   = findViewById(R.id.llEmptyState);
        tvDoctorsCount = findViewById(R.id.tvDoctorsCount);
        recyclerView   = findViewById(R.id.rvDoctors);
        statusScrim    = findViewById(R.id.status_bar_scrim);
        navScrim       = findViewById(R.id.navigation_bar_scrim);
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }
        WindowInsetsControllerCompat c =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);
        c.setAppearanceLightNavigationBars(false);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top; statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom; navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });
    }

    private void setupSearchBar() {
        etSearch.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(6) });

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                etSearch.setText("");
                btnClearSearch.setVisibility(View.GONE);
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                btnClearSearch.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
                String pin = s.toString().replaceAll("\\D", "");
                if (pin.length() == 6) {
                    if (!pin.equals(activePincode)) {
                        activePincode = pin;
                        Log.d(TAG, "Search triggered → new pincode=" + activePincode);
                        fetchVets(activePincode, true);
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit().putString(KEY_PINCODE, activePincode).apply();
                    }
                }
                if (pin.length() == 0 && !defaultPincode.isEmpty()) {
                    activePincode = defaultPincode;
                    Log.d(TAG, "Resetting to default pincode=" + defaultPincode);
                    fetchVets(defaultPincode, true);
                }
            }
        };
        etSearch.addTextChangedListener(watcher);

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });
    }

    private void resolvePincodeFromServer(String userId) {
        String url = ApiConfig.endpoint("user_pincode.php", "user_id", userId);
        Log.d(TAG, "GET " + url);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    String pin = resp.optString("pincode", "");
                    Log.d(TAG, "user_pincode resp=" + pin);
                    if (pin != null && pin.trim().length() == 6) {
                        defaultPincode = pin.trim();
                        activePincode  = defaultPincode;
                        etSearch.setText(defaultPincode);
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit().putString(KEY_PINCODE, defaultPincode).apply();
                        fetchVets(defaultPincode, true);
                    } else {
                        fetchVets("", true);
                    }
                },
                err -> {
                    Log.e(TAG, "user_pincode error", err);
                    fetchVets("", true);
                }
        );
        if (queue == null) queue = Volley.newRequestQueue(this);
        queue.add(req);
    }

    // === Fetch Doctors (with Vet & Animal & doctor_type filters) ===
    private void fetchVets(String pin, boolean allowFallback) {
        if (vetCategoryId <= 0 || animalCategoryId <= 0 || doctorType == null || doctorType.isEmpty()) {
            Toast.makeText(this, "Invalid category selection", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "fetchVets aborted: vetCategoryId=" + vetCategoryId
                    + ", animalCategoryId=" + animalCategoryId
                    + ", doctorType=" + doctorType);
            return;
        }

        String url = ApiConfig.endpoint(
                "Animal/vet_doctors_by_category.php",
                "vet_category_id", String.valueOf(vetCategoryId),
                "animal_category_id", String.valueOf(animalCategoryId),
                "doctor_type", doctorType,
                "pincode", pin == null ? "" : pin
        );

        Log.d(TAG, "GET " + url + " (allowFallback=" + allowFallback + ")");

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    Log.d(TAG, "fetchVets → Response: " + resp); // DEBUG: Log full JSON response
                    boolean ok = resp.optBoolean("success", false);
                    JSONArray arr = resp.optJSONArray("data");

                    if (!ok || arr == null || arr.length() == 0) {
                        Log.w(TAG, "No results for pin=" + pin);
                        if (allowFallback && defaultPincode.length() == 6 && !pin.equals(defaultPincode)) {
                            Log.d(TAG, "Fallback → default pin=" + defaultPincode);
                            fetchVets(defaultPincode, false);
                        } else {
                            applyResult(arr);
                        }
                    } else {
                        Log.d(TAG, "Doctors found → count=" + arr.length());
                        applyResult(arr);
                    }
                },
                err -> {
                    Log.e(TAG, "fetchVets error: " + err.getMessage());
                    if (allowFallback && defaultPincode.length() == 6 && !pin.equals(defaultPincode)) {
                        fetchVets(defaultPincode, false);
                    } else {
                        applyResult(null);
                    }
                }
        );

        if (queue == null) queue = Volley.newRequestQueue(this);
        queue.add(req);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyResult(@Nullable JSONArray arr) {
        doctors.clear();
        filteredDoctors.clear();

        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    // DEBUG: Log each doctor as parsed
                    Log.d(TAG, "Parsed Doctor: " + o.optString("full_name", "NULL")
                            + ", id=" + o.optInt("doctor_id", 0)
                            + ", animal_category_id=" + o.optInt("animal_category_id", -1)
                            + ", category_id=" + o.optInt("category_id", -1)
                            + ", doctor_type=" + o.optString("doctor_type", "")
                            + ", pincodes=" + o.optString("pincodes", ""));
                    doctors.add(o);
                }
            }
        }

        filteredDoctors.addAll(doctors);

        Log.d(TAG, "applyResult() → total doctors=" + filteredDoctors.size()); // DEBUG

        if (tvDoctorsCount != null)
            tvDoctorsCount.setText(String.valueOf(filteredDoctors.size()));

        if (adapter != null)
            adapter.notifyDataSetChanged();

        boolean empty = filteredDoctors.isEmpty();
        if (llEmptyState != null)
            llEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null)
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            Log.d(TAG, "No doctors found in filteredDoctors!"); // DEBUG
            Toast.makeText(this,
                    "No vets available for " + animalName +
                            " in " + (activePincode.isEmpty() ? "your area" : activePincode),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDoctorClick(JSONObject doctor) {
        Log.d(TAG, "onDoctorClick → " + doctor.optString("full_name",""));
    }

    // --- CHANGED: Pass intent extras on Book Now ---
    @Override
    public void onBookNowClick(JSONObject doctor) {
        Log.d(TAG, "onBookNowClick → DoctorId=" + doctor.optInt("doctor_id")
                + ", name=" + doctor.optString("full_name","")
                + ", fee=" + doctor.optDouble("consultation_fee",0.0));

        int doctorId = doctor.optInt("doctor_id", -1);
        // animal_category_id is already available as a field in this activity

        Intent intent = new Intent(VetDoctorsActivity.this, VetAppointmentActivity.class);
        intent.putExtra("doctor_id", doctorId);
        intent.putExtra("animal_category_id", animalCategoryId); // use the field from this activity
        startActivity(intent);
    }
}
