package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipeRefresh;

    private final ArrayList<JSONObject> doctors = new ArrayList<>();
    private final ArrayList<JSONObject> filteredDoctors = new ArrayList<>();

    private View statusScrim, navScrim;
    private RequestQueue queue;

    // === Inputs from Intent ===
    private int vetCategoryId = -1;         // from doctor_categories
    private int animalCategoryId = -1;      // from animal_categories (selected animal)
    private String animalName = "";
    private String doctorType = "";         // from intent

    // === Pincode Management ===
    private String defaultPincode = "";
    private String activePincode  = "";

    // === SharedPreferences ===
    private static final String PREFS       = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PINCODE = "pincode"; // still used to persist after user edits

    // === Toast management (avoid spam) ===
    private Toast currentToast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_doctors);

        Log.d(TAG, "VetDoctorsActivity.onCreate() initialized");

        initViews();
        setupEdgeToEdge();
        setupSearchBar();
        setupSwipeToRefresh();

        // --- Read intent from VetAnimalsActivity ---
        vetCategoryId    = getIntent().getIntExtra("vet_category_id", -1);
        animalCategoryId = getIntent().getIntExtra("animal_category_id", -1);
        animalName       = getIntent().getStringExtra("animal_category_name");
        doctorType       = getIntent().getStringExtra("doctor_type");

        Log.d(TAG, "Received from VetAnimalsActivity → "
                + "vet_category_id=" + vetCategoryId
                + ", animal_category_id=" + animalCategoryId
                + ", animal_name=" + animalName
                + ", doctor_type=" + doctorType);

        if (vetCategoryId <= 0 || animalCategoryId <= 0 || doctorType == null || doctorType.isEmpty()) {
            showToastSafe("Invalid category selection");
            Log.e(TAG, "Invalid IDs or missing doctorType → vetCategoryId=" + vetCategoryId
                    + ", animalCategoryId=" + animalCategoryId
                    + ", doctorType=" + doctorType);
            finish();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VetDoctorsAdapter(this, filteredDoctors, animalCategoryId, this);
        recyclerView.setAdapter(adapter);

        queue = Volley.newRequestQueue(this);

        // --- Startup: fetch pincode using user_id from SharedPreferences ---
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String userId = sp.getString(KEY_USER_ID, "");
        if (userId != null && !userId.isEmpty()) {
            resolvePincodeFromServer(userId);
        } else {
            // No user id → we cannot resolve pincode; show "no pincode" state
            resetListForNoPincode(); // no network call
        }
    }

    private void initViews() {
        etSearch       = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        llEmptyState   = findViewById(R.id.llEmptyState);
        tvDoctorsCount = findViewById(R.id.tvDoctorsCount);
        recyclerView   = findViewById(R.id.rvDoctors);
        swipeRefresh   = findViewById(R.id.swipeRefresh);
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

    private void setupSwipeToRefresh() {
        if (swipeRefresh == null) return;
        swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.primary_200,
                R.color.blue
        );
        swipeRefresh.setOnRefreshListener(() -> {
            String pin = safePin(etSearch.getText() == null ? "" : etSearch.getText().toString());
            if (pin.length() == 6) {
                // Strict: refresh only that pincode
                fetchVets(pin, /*allowFallback=*/false);
            } else {
                // No/invalid pincode → just reset list, no network call
                resetListForNoPincode();
                setRefreshing(false);
            }
        });
    }

    private void setupSearchBar() {
        // Enforce numeric pincode, max 6 chars
        etSearch.setInputType(InputType.TYPE_CLASS_NUMBER);
        etSearch.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        etSearch.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(6) });

        // IME Done/Go → trigger fetch if 6 digits present
        etSearch.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String pin = safePin(v.getText().toString());
                if (pin.length() == 6 && !pin.equals(activePincode)) {
                    activePincode = pin;
                    persistPincode(activePincode); // optional persistence after user input
                    Log.d(TAG, "IME action → new pincode=" + activePincode);
                    setRefreshing(true);
                    fetchVets(activePincode, /*allowFallback=*/false);
                } else if (pin.length() < 6) {
                    // Incomplete pincode → just clear list, no toast spam
                    resetListForNoPincode();
                }
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                btnClearSearch.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);

                String pin = safePin(s.toString());

                // Auto-refresh the moment the 6th digit appears
                if (pin.length() == 6) {
                    if (!pin.equals(activePincode)) {
                        activePincode = pin;
                        persistPincode(activePincode); // optional persistence after user input
                        Log.d(TAG, "Search triggered (6 digits) → " + activePincode);
                        setRefreshing(true);
                        fetchVets(activePincode, /*allowFallback=*/false);
                    }
                    return;
                }

                // If cleared or 1–5 digits: show empty state but DO NOT toast
                if (pin.length() < 6) {
                    resetListForNoPincode();
                    setRefreshing(false);
                }
            }
        });
    }

    private String safePin(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private void persistPincode(String pin) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(KEY_PINCODE, pin).apply();
    }

    private void resolvePincodeFromServer(String userId) {
        String url = ApiConfig.endpoint("user_pincode.php", "user_id", userId);
        Log.d(TAG, "GET " + url);
        setRefreshing(true);
        loaderutil.showLoader(this);   // <<< SHOW LOADER
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    String pin = resp.optString("pincode", "");
                    Log.d(TAG, "user_pincode resp=" + pin);
                    if (pin != null && pin.trim().length() == 6) {
                        defaultPincode = pin.trim();
                        activePincode  = defaultPincode;

                        // Populate field (TextWatcher will NOT refetch because activePincode already matches)
                        etSearch.setText(defaultPincode);

                        // First render fetch (strict, no fallback)
                        fetchVets(defaultPincode, /*allowFallback=*/false);
                    } else {
                        resetListForNoPincode();
                        setRefreshing(false);
                        loaderutil.hideLoader();
                    }
                },
                err -> {
                    Log.e(TAG, "user_pincode error", err);
                    resetListForNoPincode();
                    setRefreshing(false);
                    loaderutil.hideLoader();
                    showToastSafe("Unable to fetch your area. Please enter pincode manually.");
                }
        );
        if (queue == null) queue = Volley.newRequestQueue(this);
        queue.add(req);
    }

    private void fetchVets(String pin, boolean allowFallback) {
        if (vetCategoryId <= 0 || animalCategoryId <= 0 || doctorType == null || doctorType.isEmpty()) {
            Log.w(TAG, "fetchVets aborted: vetCategoryId=" + vetCategoryId
                    + ", animalCategoryId=" + animalCategoryId
                    + ", doctorType=" + doctorType);
            setRefreshing(false);
            showToastSafe("Invalid category selection");
            return;
        }

        // Strict behavior: if pincode provided but invalid (<6), treat as "no pincode"
        if (pin != null && !pin.isEmpty() && pin.length() < 6) {
            resetListForNoPincode();
            setRefreshing(false);
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
        loaderutil.showLoader(this);   // <<< SHOW LOADER

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    Log.d(TAG, "fetchVets → Response: " + resp);
                    boolean ok = resp.optBoolean("success", false);
                    JSONArray arr = resp.optJSONArray("data");

                    if (!ok) {
                        Log.w(TAG, "API success=false");
                        applyResult(null);
                    } else if (arr == null || arr.length() == 0) {
                        Log.w(TAG, "No results for pin=" + (pin == null ? "" : pin));
                        applyResult(new JSONArray());
                    } else {
                        Log.d(TAG, "Doctors found → count=" + arr.length());
                        applyResult(arr);
                    }
                    setRefreshing(false);
                    loaderutil.hideLoader();   // <<< HIDE LOADER
                },
                err -> {
                    Log.e(TAG, "fetchVets error: " + (err.getMessage() == null ? "unknown" : err.getMessage()));
                    applyResult(null); // show empty
                    setRefreshing(false);
                    loaderutil.hideLoader();   // <<< HIDE LOADER
                    showToastSafe("Failed to load vets. Please try again.");
                }
        );

        if (queue == null) queue = Volley.newRequestQueue(this);
        queue.add(req);
    }

    private void setRefreshing(boolean refreshing) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(refreshing);
        }
    }

    /**
     * Reset list & UI when there is no valid pincode (or field cleared).
     * No toast here – this is called very frequently while typing.
     */
    private void resetListForNoPincode() {
        doctors.clear();
        filteredDoctors.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (tvDoctorsCount != null) tvDoctorsCount.setText("0");

        if (llEmptyState != null) llEmptyState.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);

        Log.d(TAG, "No pincode: showing empty state (no network call).");
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyResult(@Nullable JSONArray arr) {
        doctors.clear();
        filteredDoctors.clear();

        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {

                    // === NEW: normalize multiple animal IDs (CSV) for logging ===
                    String animalIdsCsv = o.optString("animal_category_ids", null);
                    if (animalIdsCsv == null || animalIdsCsv.isEmpty()) {
                        int single = o.optInt("animal_category_id", -1);
                        animalIdsCsv = (single > 0) ? String.valueOf(single) : "";
                    }

                    Log.d(TAG, "Parsed Doctor: " + o.optString("full_name", "NULL")
                            + ", id=" + o.optInt("doctor_id", 0)
                            + ", animal_category_ids=" + animalIdsCsv
                            + ", category_id=" + o.optInt("category_id", -1)
                            + ", doctor_type=" + o.optString("doctor_type", "")
                            + ", pincodes=" + o.optString("pincodes", ""));

                    doctors.add(o);
                }
            }
        }

        filteredDoctors.addAll(doctors);

        int count = filteredDoctors.size();
        Log.d(TAG, "applyResult() → total doctors=" + count);

        if (tvDoctorsCount != null) tvDoctorsCount.setText(String.valueOf(count));

        if (adapter != null) adapter.notifyDataSetChanged();

        boolean empty = (count == 0);
        if (llEmptyState != null) llEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            String pinMsg = activePincode != null && activePincode.length() == 6
                    ? (" in " + activePincode)
                    : "";
            Log.d(TAG, "No doctors found" + pinMsg);
            showToastSafe("No vets available" + pinMsg);
        }
    }

    /**
     * Use a single Toast instance so Android never queues multiple toasts.
     */
    private void showToastSafe(String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loaderutil.hideLoader();
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    }

    @Override
    public void onDoctorClick(JSONObject doctorJson) {
        // Your existing implementation (if any)
        // Handle click on vet doctor item.
    }
}
