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
import java.util.List;

public class VetAnimalsActivity extends AppCompatActivity implements AnimalAdapter.OnAnimalClickListener {

    private static final String TAG = "VET_FLOW";

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

    // 🔴 Doctor/Vet category coming from DegreeSelection (e.g., 11 = Veterinary General)
    private int vetCategoryIdFromDegree = -1;
    private String vetCategoryNameFromDegree = "";
    private double vetCategoryPriceFromDegree = 0.0;
    private String vetCategoryImageFromDegree = null;
    private String vetCategoryDiseaseFromDegree = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_animals);

        // --- read extras from DegreeSelection (DOCTOR category) ---
        vetCategoryIdFromDegree    = getIntent().getIntExtra(DegreeSelectionActivity.EXTRA_CATEGORY_ID, -1);
        vetCategoryNameFromDegree  = getIntent().getStringExtra(DegreeSelectionActivity.EXTRA_CATEGORY_NAME);
        vetCategoryPriceFromDegree = getIntent().getDoubleExtra(DegreeSelectionActivity.EXTRA_CATEGORY_PRICE, 0.0);
        vetCategoryImageFromDegree = getIntent().getStringExtra(DegreeSelectionActivity.EXTRA_CATEGORY_IMAGE);
        vetCategoryDiseaseFromDegree = getIntent().getStringExtra(DegreeSelectionActivity.EXTRA_CATEGORY_DISEASE);

        Log.d(TAG, "VetAnimalsActivity.onCreate");
        Log.d(TAG, "VetAnimalsActivity received from DegreeSelection -> "
                + "DOCTOR_CAT_ID=" + vetCategoryIdFromDegree
                + ", name=" + vetCategoryNameFromDegree
                + ", price=" + vetCategoryPriceFromDegree
                + ", image=" + vetCategoryImageFromDegree);

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
            // Title can reflect doctor category if you want
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

        // Load categories (ANIMAL types)
        fetchCategoriesFromApi();

        // Search
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

    // === API CALL: get_animal_category.php (ANIMAL types) ===
    private void fetchCategoriesFromApi() {
        llEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        final String url = ApiConfig.endpoint("get_animal_category.php");
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
                                    int id = item.optInt("category_id");              // <-- ANIMAL category id
                                    String name = item.optString("category_name", "");
                                    String imageUrl = item.optString("category_image", "");
                                    double price = item.optDouble("price", 0.0);      // optional

                                    Animal a = new Animal(id, name, imageUrl);
                                    a.setPrice(price);
                                    animals.add(a);
                                }
                            }
                        }

                        filteredAnimals.addAll(animals);
                        Log.d(TAG, "categories count=" + filteredAnimals.size());
                        for (int i = 0; i < filteredAnimals.size(); i++) {
                            Log.d(TAG, "cat[" + i + "] id=" + filteredAnimals.get(i).getId()
                                    + ", name=" + filteredAnimals.get(i).getName()
                                    + ", price=" + filteredAnimals.get(i).getPrice());
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
        // ✅ FIX: API को DOCTOR category चाहिए → वही “category_id” भेजें जो DegreeSelection से आया था.
        // Animal की जानकारी अलग keys से भेजो.
        Intent intent = new Intent(this, VetDoctorsActivity.class);

        // DOCTOR category for API:
        intent.putExtra("category_id", vetCategoryIdFromDegree); // <-- IMPORTANT

        // Animal selection for UI/filters:
        intent.putExtra("animal_type_id", animal.getId());       // (new key)
        intent.putExtra("animal_name", animal.getName());        // (name)
        intent.putExtra("animal_image", animal.getImageUrl());   // (image)

        // Optional: pass doctor category meta (if needed further)
        intent.putExtra("vet_category_name", vetCategoryNameFromDegree);
        intent.putExtra("vet_category_price", vetCategoryPriceFromDegree);
        intent.putExtra("vet_category_image", vetCategoryImageFromDegree);
        intent.putExtra("vet_category_disease", vetCategoryDiseaseFromDegree);

        Log.d(TAG, "CLICK VetAnimals -> going to VetDoctorsActivity with "
                + "DOCTOR_CAT_ID=" + vetCategoryIdFromDegree
                + ", animalTypeId=" + animal.getId()
                + ", animalName=" + animal.getName());

        startActivity(intent);
    }
}
