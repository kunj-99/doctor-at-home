package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
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

    // UI
    private RecyclerView recyclerView;
    private VetDoctorsAdapter adapter;
    private EditText etSearch;              // pincode search (6 digits)
    private ImageButton btnClearSearch;
    private LinearLayout llEmptyState;
    private TextView tvDoctorsCount;

    // Data
    private final ArrayList<JSONObject> doctors = new ArrayList<>();
    private final ArrayList<JSONObject> filteredDoctors = new ArrayList<>();

    private View statusScrim, navScrim;
    private RequestQueue queue;

    // Inputs
    private int categoryId = -1;
    private String animalName = "";
    private double catPrice = 0.0;
    private String catImage = "";

    // pincodes
    private String defaultPincode = "";
    private String activePincode  = "";

    // prefs
    private static final String PREFS       = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PINCODE = "pincode";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_doctors);

        Log.d(TAG, "VetDoctorsActivity.onCreate()");

        initViews();
        setupEdgeToEdge();

        // Read intent from VetAnimalsActivity
        categoryId = getIntent().getIntExtra("category_id", -1);
        animalName = getIntent().getStringExtra("animal_name");
        catPrice   = getIntent().getDoubleExtra("category_price", 0.0);
        catImage   = getIntent().getStringExtra("category_image");

        Log.d(TAG, "RECEIVED VetDoctorsActivity <- VetAnimalsActivity: " +
                "category_id=" + categoryId + ", animal_name=" + animalName +
                ", price=" + catPrice + ", image=" + catImage);
        Toast.makeText(this, "Category: " + animalName + " (" + categoryId + ")", Toast.LENGTH_SHORT).show();

        setupSearchBar();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VetDoctorsAdapter(filteredDoctors, this);
        recyclerView.setAdapter(adapter);

        queue = Volley.newRequestQueue(this);

        // default pincode resolve
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
            if (!hasFocus) { // history साफ रखें
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
                        fetchVets(activePincode, true);
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_PINCODE, activePincode).apply();
                    }
                }
                if (pin.length() == 0 && !defaultPincode.isEmpty()) {
                    activePincode = defaultPincode;
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
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_PINCODE, defaultPincode).apply();
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

    private void fetchVets(String pin, boolean allowFallback) {
        if (categoryId <= 0) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "fetchVets aborted: invalid categoryId=" + categoryId);
            return;
        }

        String url = ApiConfig.endpoint(
                "Animal/vet_doctors_by_category.php",
                "category_id", String.valueOf(categoryId),
                "pincode", pin == null ? "" : pin
        );
        Log.d(TAG, "GET " + url + " (allowFallback=" + allowFallback + ")");

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    Log.d(TAG, "fetchVets resp=" + resp);
                    boolean ok = resp.optBoolean("success", false);
                    JSONArray arr = resp.optJSONArray("data");

                    if (!ok || arr == null || arr.length() == 0) {
                        Log.w(TAG, "No results for pin=" + pin);
                        if (allowFallback && defaultPincode.length() == 6 && !pin.equals(defaultPincode)) {
                            Log.d(TAG, "fallback → default pin=" + defaultPincode);
                            fetchVets(defaultPincode, false);
                        } else {
                            applyResult(arr); // empty UI
                        }
                    } else {
                        applyResult(arr);
                    }
                },
                err -> {
                    Log.e(TAG, "fetchVets error", err);
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
        doctors.clear(); filteredDoctors.clear();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) doctors.add(o);
            }
        }
        filteredDoctors.addAll(doctors);
        Log.d(TAG, "applyResult -> doctors=" + doctors.size());
        if (tvDoctorsCount != null) tvDoctorsCount.setText(String.valueOf(filteredDoctors.size()));

        if (adapter != null) adapter.notifyDataSetChanged();

        boolean empty = filteredDoctors.isEmpty();
        if (llEmptyState != null) llEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            Toast.makeText(this,
                    "No vets for " + (activePincode.isEmpty()? "your area" : activePincode),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDoctorClick(JSONObject doctor) {
        Log.d(TAG, "onDoctorClick: " + doctor.optString("full_name",""));
    }

    @Override
    public void onBookNowClick(JSONObject doctor) {
        Log.d(TAG, "onBookNowClick DoctorId=" + doctor.optInt("doctor_id")
                + ", name=" + doctor.optString("full_name","")
                + ", fee=" + doctor.optDouble("consultation_fee",0.0));
    }
}
