package com.infowave.thedoctorathomeuser;

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

public class VetAnimalsActivity extends AppCompatActivity implements AnimalAdapter.OnAnimalClickListener {

    private RecyclerView recyclerView;
    private AnimalAdapter adapter;
    private final List<Animal> animals = new ArrayList<>();
    private final List<Animal> filteredAnimals = new ArrayList<>();
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private LinearLayout llEmptyState;

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

        seedAnimals(); // add animals
        filteredAnimals.addAll(animals);
        adapter = new AnimalAdapter(filteredAnimals, this);
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        setupSearch();
    }

    private void setupSearch() {
        // Text watcher for search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
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

    private void filterAnimals(String query) {
        filteredAnimals.clear();

        if (query.isEmpty()) {
            filteredAnimals.addAll(animals);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Animal animal : animals) {
                if (animal.getName().toLowerCase().contains(lowerCaseQuery)) {
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

    private void seedAnimals() {
        // Add your drawables in res/drawable and reference here.
        // Replace sample names with your actual images.
        animals.add(new Animal("Dog", R.drawable.animal_dog));
        animals.add(new Animal("Cat", R.drawable.animal_cat));
        animals.add(new Animal("Cow", R.drawable.animal_cow));
        animals.add(new Animal("Buffalo", R.drawable.animal_buffalo));
        animals.add(new Animal("Goat", R.drawable.animal_goat));
        // Add more animals as needed
    }

    @Override
    public void onAnimalClick(Animal animal) {
        // Navigate to your vet appointment flow with selected animal
        Intent i = new Intent(this, VetAppointmentActivity.class);
        i.putExtra("animal_name", animal.getName());
        startActivity(i);
    }
}