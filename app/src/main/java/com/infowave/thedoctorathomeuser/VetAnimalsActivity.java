package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.infowave.thedoctorathomeuser.adapter.AnimalAdapter;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VetAnimalsActivity extends AppCompatActivity implements AnimalAdapter.OnAnimalClickListener {

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_animals);

        // --- Edge-to-edge so scrims can occupy system bar areas ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Force true-black bars behind scrims (baseline)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        // Keep icons white on black bars
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // Hook scrims & size them from real insets
        statusScrim = findViewById(R.id.status_bar_scrim);
        navScrim = findViewById(R.id.navigation_bar_scrim);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top; // status bar height
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }

            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom; // nav bar height (0 on gesture nav)
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }

            return insets; // don’t consume; let child behaviors work
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Views
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        llEmptyState = findViewById(R.id.llEmptyState);

        recyclerView = findViewById(R.id.rvAnimals);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridSpacingDecoration(16)); // your spacing decorator

        // Volley
        requestQueue = Volley.newRequestQueue(this);

        // Adapter
        adapter = new AnimalAdapter(filteredAnimals, this);
        recyclerView.setAdapter(adapter);

        // Load data
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

    // === API CALL: get_animal_category.php ===
    private void fetchCategoriesFromApi() {
        llEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        final String url = ApiConfig.endpoint("get_animal_category.php");

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override public void onResponse(String response) {
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

                                        animals.add(new Animal(id, name, imageUrl));
                                    }
                                }
                            }

                            filteredAnimals.addAll(animals);
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        } catch (JSONException e) {
                            showEmptyWithMessage();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        showEmptyWithMessage();
                    }
                }
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
        Intent intent = new Intent(this, VetDoctorsActivity.class);
        intent.putExtra("category_id", animal.getId());
        intent.putExtra("animal_name", animal.getName());
        startActivity(intent);
    }
}
