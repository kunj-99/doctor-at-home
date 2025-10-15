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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.adapter.VetDoctorsAdapter;

import java.util.ArrayList;
import java.util.List;

public class VetDoctorsActivity extends AppCompatActivity implements VetDoctorsAdapter.OnDoctorClickListener {

    private RecyclerView recyclerView;
    private VetDoctorsAdapter adapter;
    private final List<VetDoctor> doctors = new ArrayList<>();
    private final List<VetDoctor> filteredDoctors = new ArrayList<>();
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private LinearLayout llEmptyState;
    private TextView tvDoctorsCount;

    // Scrims
    private View statusScrim, navScrim;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_doctors);

        // Initialize views first
        initViews();

        // Edge-to-edge setup
        setupEdgeToEdge();

        // Get animal category from intent
        int categoryId = getIntent().getIntExtra("category_id", -1);
        String animalName = getIntent().getStringExtra("animal_name");

        // Setup toolbar
        setupToolbar(animalName);

        // Setup search
        setupSearch();

        // Load sample data
        loadSampleDoctors();

        // Setup adapter
        setupAdapter();
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        statusScrim = findViewById(R.id.status_bar_scrim);
        navScrim = findViewById(R.id.navigation_bar_scrim);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

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
    }

    private void setupToolbar(String animalName) {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            if (animalName != null) {
                getSupportActionBar().setTitle("Vets for " + animalName);
            } else {
                getSupportActionBar().setTitle("Select Veterinarian");
            }
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvDoctorsCount = findViewById(R.id.tvDoctorsCount);
        recyclerView = findViewById(R.id.rvDoctors);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize scrims
        statusScrim = findViewById(R.id.status_bar_scrim);
        navScrim = findViewById(R.id.navigation_bar_scrim);
    }

    private void setupSearch() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterDoctors(s.toString());
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
        if (btnClearSearch != null) {
            btnClearSearch.setVisibility(etSearch.getText().toString().isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterDoctors(String query) {
        filteredDoctors.clear();
        if (query == null || query.isEmpty()) {
            filteredDoctors.addAll(doctors);
        } else {
            String q = query.toLowerCase();
            for (VetDoctor doctor : doctors) {
                if ((doctor.getName() != null && doctor.getName().toLowerCase().contains(q)) ||
                        (doctor.getZipCode() != null && doctor.getZipCode().contains(q)) ||
                        (doctor.getSpecialization() != null && doctor.getSpecialization().toLowerCase().contains(q))) {
                    filteredDoctors.add(doctor);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateEmptyState();
        updateDoctorsCount();
    }

    private void updateEmptyState() {
        if (llEmptyState != null && recyclerView != null) {
            if (filteredDoctors.isEmpty()) {
                llEmptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                llEmptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateDoctorsCount() {
        if (tvDoctorsCount != null) {
            tvDoctorsCount.setText(String.valueOf(filteredDoctors.size()));
        }
    }

    private void setupAdapter() {
        adapter = new VetDoctorsAdapter(filteredDoctors, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadSampleDoctors() {
        // Clear existing data
        doctors.clear();
        filteredDoctors.clear();

        // Sample data - using placeholder images
        doctors.add(new VetDoctor(
                1,
                "Dr. Sarah Wilson",
                "Small Animal Specialist",
                "https://example.com/doctor1.jpg",
                4.8,
                8,
                "New York, NY",
                "10001",
                "DVM, Cornell University",
                49,
                true
        ));

        doctors.add(new VetDoctor(
                2,
                "Dr. Michael Chen",
                "Avian Veterinarian",
                "https://example.com/doctor2.jpg",
                4.9,
                12,
                "Brooklyn, NY",
                "11201",
                "DVM, University of Pennsylvania",
                59,
                true
        ));

        doctors.add(new VetDoctor(
                3,
                "Dr. Emily Rodriguez",
                "Exotic Animal Expert",
                "https://example.com/doctor3.jpg",
                4.7,
                6,
                "Queens, NY",
                "11354",
                "DVM, Ohio State University",
                55,
                true
        ));

        filteredDoctors.addAll(doctors);
        updateDoctorsCount();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDoctorClick(VetDoctor doctor) {
        // Handle doctor profile click
        // You can show a detailed profile dialog here
    }

    @Override
    public void onBookNowClick(VetDoctor doctor) {
        // Redirect to VetAppointmentActivity with doctor details
        Intent intent = new Intent(this, VetAppointmentActivity.class);
        intent.putExtra("category_id", getIntent().getIntExtra("category_id", -1));
        intent.putExtra("doctor_id", doctor.getId());
        intent.putExtra("doctor_name", doctor.getName());
        intent.putExtra("consultation_fee", doctor.getConsultationFee());
        startActivity(intent);
    }
}