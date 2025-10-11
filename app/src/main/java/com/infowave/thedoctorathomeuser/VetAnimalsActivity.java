package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.adapter.AnimalAdapter;

import java.util.ArrayList;
import java.util.List;

// Volley
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Response;
import com.android.volley.VolleyError;

// JSON
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

    // Volley
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_animals);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize views
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        llEmptyState = findViewById(R.id.llEmptyState);

        recyclerView = findViewById(R.id.rvAnimals);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridSpacingDecoration(16)); // spacing between cards

        // Volley
        requestQueue = Volley.newRequestQueue(this);

        // Adapter (start empty; we’ll fill after API call)
        adapter = new AnimalAdapter(filteredAnimals, this);
        recyclerView.setAdapter(adapter);

        // Fetch categories from API
        fetchCategoriesFromApi();

        // Search
        setupSearch();
    }

    private void setupSearch() {
        // Text watcher for search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterAnimals(s.toString());
                updateClearButtonVisibility();
            }
        });

        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });

        // Show/hide clear button based on whether there's text
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateClearButtonVisibility();
            }
        });
    }

    private void updateClearButtonVisibility() {
        if (etSearch.getText().toString().isEmpty()) {
            btnClearSearch.setVisibility(View.GONE);
        } else {
            btnClearSearch.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterAnimals(String query) {
        filteredAnimals.clear();

        if (query.isEmpty()) {
            filteredAnimals.addAll(animals);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Animal animal : animals) {
                if (animal.getName() != null && animal.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredAnimals.add(animal);
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
                    @Override
                    public void onResponse(String response) {
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
                                        String imageUrl = item.optString("category_image", ""); // full URL from PHP

                                        // Build Animal with id + image URL (ensure Animal has these)
                                        Animal a = new Animal(id, name, imageUrl); // requires the 3-arg constructor
                                        animals.add(a);
                                    }
                                }
                            }

                            filteredAnimals.addAll(animals);
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showEmptyWithMessage();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
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
        // Navigate to your vet appointment flow with selected animal
        Intent i = new Intent(this, VetAppointmentActivity.class);
        i.putExtra("category_id", animal.getId());   // ✅ pass category_id
       // i.putExtra("animal_name", animal.getName()); // optional: name
        startActivity(i);
    }
}
