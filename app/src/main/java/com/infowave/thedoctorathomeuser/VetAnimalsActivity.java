package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.AnimalAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VetAnimalsActivity extends AppCompatActivity implements AnimalAdapter.OnAnimalClickListener {

    private static final String TAG = "VET_FLOW";

    // Intent keys
    public static final String EXTRA_VET_CATEGORY_ID   = "vet_category_id";
    public static final String EXTRA_VET_CATEGORY_NAME = "vet_category_name";
    public static final String EXTRA_ANIMAL_CATEGORY_ID   = "animal_category_id";
    public static final String EXTRA_ANIMAL_CATEGORY_NAME = "animal_category_name";
    public static final String EXTRA_DOCTOR_TYPE = "doctor_type"; // pass through

    private RecyclerView recyclerView;
    private AnimalAdapter adapter;
    private final List<Animal> animals = new ArrayList<>();
    private final List<Animal> filteredAnimals = new ArrayList<>();
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private LinearLayout llEmptyState;

    // Scrims
    private View statusScrim, navScrim;

    // Volley
    private RequestQueue requestQueue;

    // From DegreeSelection
    private int vetCategoryIdFromDegree = -1;
    private String vetCategoryNameFromDegree = "";
    private String doctorTypeFromDegree = "";   // NEW: carry forward!

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_animals);

        // === Read minimal extras from DegreeSelection ===
        vetCategoryIdFromDegree   = getIntent().getIntExtra(DegreeSelectionActivity.EXTRA_VET_CATEGORY_ID, -1);
        vetCategoryNameFromDegree = getIntent().getStringExtra(DegreeSelectionActivity.EXTRA_VET_CATEGORY_NAME);
        doctorTypeFromDegree      = getIntent().getStringExtra(DegreeSelectionActivity.EXTRA_DOCTOR_TYPE);

        Log.d(TAG, "VetAnimalsActivity.onCreate | vet_category_id=" + vetCategoryIdFromDegree
                + ", vet_category_name=" + vetCategoryNameFromDegree
                + ", doctor_type=" + doctorTypeFromDegree);

        // Edge-to-edge scrims
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        statusScrim = findViewById(R.id.status_bar_scrim);
        navScrim = findViewById(R.id.navigation_bar_scrim);
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(vetCategoryNameFromDegree == null ? "Choose Animal" : vetCategoryNameFromDegree);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Views
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        llEmptyState = findViewById(R.id.llEmptyState);

        recyclerView = findViewById(R.id.rvAnimals);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridSpacingDecoration(16));

        // Volley
        requestQueue = Volley.newRequestQueue(this);

        // Adapter
        adapter = new AnimalAdapter(filteredAnimals, this);
        recyclerView.setAdapter(adapter);

        // Load animal categories
        fetchAnimalCategories();

        // Search handlers
        setupSearch();
    }

    private void setupSearch() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterAnimals(s.toString());
                updateClearButtonVisibility();
            }
        };
        etSearch.addTextChangedListener(watcher);

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });
        updateClearButtonVisibility();
    }

    private void updateClearButtonVisibility() {
        btnClearSearch.setVisibility(etSearch.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterAnimals(String query) {
        filteredAnimals.clear();
        if (query == null || query.isEmpty()) {
            filteredAnimals.addAll(animals);
        } else {
            String q = query.toLowerCase();
            for (Animal a : animals) {
                if (a.getName() != null && a.getName().toLowerCase().contains(q)) {
                    filteredAnimals.add(a);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredAnimals.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // === API CALL: fetch animal categories (ANIMAL types) ===
    private void fetchAnimalCategories() {
        llEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        final String url = ApiConfig.endpoint("Animal/get_animal_category.php");
        Log.d(TAG, "GET " + url);

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        boolean success = root.optBoolean("success", false);

                        animals.clear();
                        filteredAnimals.clear();

                        if (success) {
                            JSONArray data = root.optJSONArray("data");
                            if (data != null) {
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject item = data.getJSONObject(i);
                                    int id = item.optInt("category_id");
                                    String name = item.optString("category_name", "");
                                    String imageUrl = item.optString("category_image", "");
                                    Animal a = new Animal(id, name, imageUrl);
                                    animals.add(a);
                                }
                            }
                        }

                        // Sort by category_id ascending, first card always lowest id
                        animals.sort(Comparator.comparingInt(Animal::getId));

                        filteredAnimals.addAll(animals);
                        Log.d(TAG, "animal categories count=" + filteredAnimals.size());
                        for (Animal a : animals) {
                            Log.d(TAG, "Animal: id=" + a.getId() + ", name=" + a.getName());
                        }
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } catch (JSONException e) {
                        showEmptyWithMessage();
                    }
                },
                error -> showEmptyWithMessage()
        );

        requestQueue.add(req);
    }

    private void showEmptyWithMessage() {
        filteredAnimals.clear();
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    @Override
    public void onAnimalClick(Animal animal) {
        // Pass all details, including doctor_type!
        Intent intent = new Intent(this, VetDoctorsActivity.class);
        intent.putExtra(EXTRA_VET_CATEGORY_ID, vetCategoryIdFromDegree);          // doctor_categories.category_id
        intent.putExtra(EXTRA_ANIMAL_CATEGORY_ID, animal.getId());                // animal_categories.category_id
        intent.putExtra(EXTRA_VET_CATEGORY_NAME, vetCategoryNameFromDegree);
        intent.putExtra(EXTRA_ANIMAL_CATEGORY_NAME, animal.getName());
        intent.putExtra(EXTRA_DOCTOR_TYPE, doctorTypeFromDegree);                 // <<==== THIS IS IMPORTANT!

        Log.d(TAG, "VetAnimals -> VetDoctors | vet_category_id=" + vetCategoryIdFromDegree
                + ", animal_category_id=" + animal.getId()
                + ", doctor_type=" + doctorTypeFromDegree);

        startActivity(intent);
    }
}
