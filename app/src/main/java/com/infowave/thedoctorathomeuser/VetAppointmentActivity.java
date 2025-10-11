package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class VetAppointmentActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Intent extras
    private int categoryId = -1;
    private String categoryNameFromIntent = "";

    // Owner Details
    private TextInputEditText etOwnerName, etOwnerPhone, etOwnerPincode, etOwnerAddress;

    // Animal Details
    private TextInputEditText etAnimalName, etAnimalAge;
    private Spinner spBreed;

    // Visit/Vaccination
    private MaterialButton btnVisit, btnVaccination;
    private MaterialCardView cardReason, cardVaccination;
    private TextInputEditText etReason;
    private Spinner spVaccination;

    // Location
    private MaterialButton btnPickLocation;
    private TextView tvLatLng;
    private MapView mapView;
    private GoogleMap googleMap;
    private Marker marker;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private LatLng currentLatLng = null;

    // Submit
    private MaterialButton btnConfirm;
    private ProgressBar progressSubmit;

    // State
    private enum Mode { VISIT, VACCINATION }
    private Mode currentMode = Mode.VISIT;

    private Calendar now = Calendar.getInstance();

    // Dropdown data
    private final List<String> breedItems = new ArrayList<>(); // filled from API
    private ArrayAdapter<String> breedAdapter;

    private final List<String> vaccinationItems = Arrays.asList(
            "Select Vaccination", "Anti-Rabies (ARV)", "DHPPi", "Leptospirosis",
            "Feline Trivalent", "Booster Dose"
    );

    // MapView key
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // Volley
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_appointment);

        // Read intent
        categoryId = getIntent().getIntExtra("category_id", -1);
        categoryNameFromIntent = getIntent().getStringExtra("animal_name");
        if (categoryNameFromIntent == null) categoryNameFromIntent = "";

        initViews();
        setupToolbar();
        setupDropdowns();         // init adapters
        setupModeToggle();
        setupLocation();
        setupConfirm();

        // Default mode
        applyMode(Mode.VISIT);

        // MapView init
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Volley
        requestQueue = Volley.newRequestQueue(this);

        // Pre-fill animal name from category (optional UX)
        if (!TextUtils.isEmpty(categoryNameFromIntent)) {
            etAnimalName.setText(categoryNameFromIntent);
        }

        // Load breeds for selected category
        loadBreedsForCategory();
    }

    private void initViews() {
        // Owner
        etOwnerName    = findViewById(R.id.etOwnerName);
        etOwnerPhone   = findViewById(R.id.etOwnerPhone);
        etOwnerPincode = findViewById(R.id.etOwnerPincode);
        etOwnerAddress = findViewById(R.id.etOwnerAddress);

        // Animal
        etAnimalName = findViewById(R.id.etAnimalName);
        etAnimalAge  = findViewById(R.id.etAnimalAge);
        spBreed      = findViewById(R.id.spBreed);

        // Visit/Vaccination controls
        btnVisit        = findViewById(R.id.btnVisit);
        btnVaccination  = findViewById(R.id.btnVaccination);
        cardReason      = findViewById(R.id.cardReason);
        cardVaccination = findViewById(R.id.cardVaccination);
        etReason        = findViewById(R.id.etReason);
        spVaccination   = findViewById(R.id.spVaccination);

        // Location
        btnPickLocation = findViewById(R.id.btnPickLocation);
        tvLatLng        = findViewById(R.id.tvLatLng);
        mapView         = findViewById(R.id.mapView);

        // Submit
        btnConfirm     = findViewById(R.id.btnConfirm);
        progressSubmit = findViewById(R.id.progressSubmit);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Optionally show selected category in title
            if (!TextUtils.isEmpty(categoryNameFromIntent)) {
                getSupportActionBar().setTitle("Book Vet - " + categoryNameFromIntent);
            }
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDropdowns() {
        // Breed spinner (start with "Select Breed" until API loads)
        breedItems.clear();
        breedItems.add("Select Breed");
        breedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, breedItems);
        spBreed.setAdapter(breedAdapter);

        // Vaccination spinner
        ArrayAdapter<String> vaccinationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, vaccinationItems);
        spVaccination.setAdapter(vaccinationAdapter);
    }

    private void setupModeToggle() {
        btnVisit.setOnClickListener(v -> applyMode(Mode.VISIT));
        btnVaccination.setOnClickListener(v -> applyMode(Mode.VACCINATION));
    }

    private void applyMode(Mode mode) {
        currentMode = mode;

        if (mode == Mode.VISIT) {
            cardReason.setVisibility(View.VISIBLE);
            cardVaccination.setVisibility(View.GONE);

            btnVisit.setChecked(true);
            btnVisit.setStrokeWidth(3);
            btnVaccination.setChecked(false);
            btnVaccination.setStrokeWidth(1);
        } else {
            cardReason.setVisibility(View.GONE);
            cardVaccination.setVisibility(View.VISIBLE);

            btnVaccination.setChecked(true);
            btnVaccination.setStrokeWidth(3);
            btnVisit.setChecked(false);
            btnVisit.setStrokeWidth(1);
        }
    }

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        fetchAndCenterOnLocation();
                    } else {
                        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        btnPickLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fetchAndCenterOnLocation();
            } else {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationLayerIfPermitted() {
        if (googleMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchAndCenterOnLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                setMarkerAndCamera(latLng, true);
            } else {
                Toast.makeText(this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setMarkerAndCamera(LatLng latLng, boolean animate) {
        currentLatLng = latLng;
        tvLatLng.setText(String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f",
                latLng.latitude, latLng.longitude));

        if (googleMap == null) return;

        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Selected Location"));
        } else {
            marker.setPosition(latLng);
        }

        if (animate) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        }
    }

    // --- OnMapReadyCallback ---
    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;
        enableMyLocationLayerIfPermitted();

        // Initial camera: India center-ish if no location yet
        LatLng indiaCenter = new LatLng(22.9734, 78.6569);
        setMarkerAndCamera(indiaCenter, false);

        // Tap to move marker
        googleMap.setOnMapClickListener(latLng -> setMarkerAndCamera(latLng, true));

        // Drag marker to refine position
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(@NonNull Marker m) {}
            @Override public void onMarkerDrag(@NonNull Marker m) {
                LatLng p = m.getPosition();
                tvLatLng.setText(String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f",
                        p.latitude, p.longitude));
            }
            @Override public void onMarkerDragEnd(@NonNull Marker m) {
                LatLng p = m.getPosition();
                setMarkerAndCamera(p, true);
            }
        });
    }

    private void setupConfirm() {
        btnConfirm.setOnClickListener(v -> {
            if (validateForm()) {
                submitAppointment();
            }
        });
    }

    private boolean validateForm() {
        boolean ok = true;

        clearErrors();

        // Owner validations
        if (isEmpty(etOwnerName)) {
            etOwnerName.setError("Name is required");
            ok = false;
        }
        String phone = valueOf(etOwnerPhone);
        if (TextUtils.isEmpty(phone)) {
            etOwnerPhone.setError("Phone number is required");
            ok = false;
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            etOwnerPhone.setError("Enter a valid phone number");
            ok = false;
        }

        String pin = valueOf(etOwnerPincode);
        if (TextUtils.isEmpty(pin)) {
            etOwnerPincode.setError("Pincode is required");
            ok = false;
        } else if (pin.length() < 5) {
            etOwnerPincode.setError("Enter a valid pincode");
            ok = false;
        }

        if (isEmpty(etOwnerAddress)) {
            etOwnerAddress.setError("Address is required");
            ok = false;
        }

        // Animal validations
        if (isEmpty(etAnimalName)) {
            etAnimalName.setError("Animal name is required");
            ok = false;
        }
        if (isEmpty(etAnimalAge)) {
            etAnimalAge.setError("Animal age is required");
            ok = false;
        }
        if (spBreed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select animal breed", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        // Mode-specific validations
        if (currentMode == Mode.VISIT) {
            if (isEmpty(etReason)) {
                etReason.setError("Reason is required for Visit");
                ok = false;
            }
        } else { // VACCINATION
            if (spVaccination.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select vaccination type", Toast.LENGTH_SHORT).show();
                ok = false;
            }
        }

        // Location validation (ensure chosen)
        if (currentLatLng == null) {
            Toast.makeText(this, "Please select location on map", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        etOwnerName.setError(null);
        etOwnerPhone.setError(null);
        etOwnerPincode.setError(null);
        etOwnerAddress.setError(null);
        etAnimalName.setError(null);
        etAnimalAge.setError(null);
        etReason.setError(null);
    }

    private boolean isEmpty(TextInputEditText et) {
        return TextUtils.isEmpty(valueOf(et));
    }

    private String valueOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void submitAppointment() {
        progressSubmit.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        // Collect data
        String ownerName    = valueOf(etOwnerName);
        String ownerPhone   = valueOf(etOwnerPhone);
        String ownerPincode = valueOf(etOwnerPincode);
        String ownerAddress = valueOf(etOwnerAddress);

        String animalName   = valueOf(etAnimalName);
        String animalAge    = valueOf(etAnimalAge);
        String breed        = (String) spBreed.getSelectedItem();

        String type         = (currentMode == Mode.VISIT) ? "Visit" : "Vaccination";
        String reason       = (currentMode == Mode.VISIT) ? valueOf(etReason) : "";
        String vaccination  = (currentMode == Mode.VACCINATION) ? (String) spVaccination.getSelectedItem() : "";

        String latLngShown  = (currentLatLng != null)
                ? String.format(Locale.getDefault(), "%.6f,%.6f", currentLatLng.latitude, currentLatLng.longitude)
                : "-,-";

        // TODO: integrate with backend (Volley/Retrofit) and send lat/lng and categoryId.

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressSubmit.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);

            String msg = (currentMode == Mode.VISIT)
                    ? ("Visit booked for " + animalName)
                    : ("Vaccination booked (" + vaccination + ") for " + animalName);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }, 1200);
    }

    // --- Load breeds for selected category via API ---
    private void loadBreedsForCategory() {
        // Guard
        if (categoryId <= 0) {
            // no category id; keep only default option
            breedItems.clear();
            breedItems.add("Select Breed");
            if (breedAdapter != null) breedAdapter.notifyDataSetChanged();
            return;
        }

        // 🔴 Old
        // String url = "https://yourdomain.com/get_breeds.php?category_id=" + categoryId;

        // 🟢 New (per your rule)
        final String url = ApiConfig.endpoint("get_animal_breed.php", "category_id", String.valueOf(categoryId));

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject root = new JSONObject(response);
                            boolean success = root.optBoolean("success", false);

                            breedItems.clear();
                            breedItems.add("Select Breed"); // index 0

                            if (success) {
                                JSONArray data = root.optJSONArray("data");
                                if (data != null) {
                                    for (int i = 0; i < data.length(); i++) {
                                        JSONObject item = data.getJSONObject(i);
                                        String breedName = item.optString("breed_name", "");
                                        if (!TextUtils.isEmpty(breedName)) {
                                            breedItems.add(breedName);
                                        }
                                    }
                                }
                            }

                            if (breedAdapter != null) {
                                breedAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // keep default list
                            if (breedAdapter != null) breedAdapter.notifyDataSetChanged();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        // keep default list
                        if (breedAdapter != null) breedAdapter.notifyDataSetChanged();
                    }
                }
        );

        if (requestQueue == null) requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(req);
    }

    // --- MapView lifecycle passthrough ---
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        enableMyLocationLayerIfPermitted();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}
