package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.toolbox.StringRequest;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VetAppointmentActivity extends AppCompatActivity implements OnMapReadyCallback {

    private int doctorId = -1;
    private int animalCategoryId = -1;

    // UI
    private Spinner spPincode, spBreed, spVaccination;
    private ArrayAdapter<String> pincodeAdapter, breedAdapter, vaccinationAdapter;
    private final List<String> pincodeItems = new ArrayList<>();
    private final List<String> breedItems = new ArrayList<>();
    private final List<String> vaccinationItems = new ArrayList<>();


    // Owner Details
    private TextInputEditText etOwnerName, etOwnerPhone, etOwnerAddress;
    // Animal Details
    private TextInputEditText etAnimalName, etAnimalAge;
    // Visit/Vaccination
    private MaterialButton btnVisit, btnVaccination;
    private MaterialCardView cardReason, cardVaccination;
    private TextInputEditText etReason;
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

    private RequestQueue requestQueue;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private enum Mode { VISIT, VACCINATION }
    private Mode currentMode = Mode.VISIT;



    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_appointment);

        doctorId = getIntent().getIntExtra("doctor_id", -1);
        animalCategoryId = getIntent().getIntExtra("animal_category_id", -1);
        if (doctorId == -1 || animalCategoryId == -1) {
            Toast.makeText(this, "Invalid booking parameters. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        View statusScrim = findViewById(R.id.status_bar_scrim);
        View navScrim = findViewById(R.id.navigation_bar_scrim);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
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

        initViews();
        setupToolbar();
        setupDropdowns();
        setupModeToggle();
        setupLocation();
        setupConfirm();

        applyMode(Mode.VISIT);

        // ---- DYNAMIC DATA LOADING ----
        loadDropdownsMaster();
        Bundle mapViewBundle = (savedInstanceState != null) ? savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        requestQueue = Volley.newRequestQueue(this);


    }

    private void initViews() {
        spPincode = findViewById(R.id.spPincode);
        spBreed = findViewById(R.id.spBreed);
        spVaccination = findViewById(R.id.spVaccination);

        etOwnerName    = findViewById(R.id.etOwnerName);
        etOwnerPhone   = findViewById(R.id.etOwnerPhone);
        etOwnerAddress = findViewById(R.id.etOwnerAddress);

        etAnimalName = findViewById(R.id.etAnimalName);
        etAnimalAge  = findViewById(R.id.etAnimalAge);

        btnVisit        = findViewById(R.id.btnVisit);
        btnVaccination  = findViewById(R.id.btnVaccination);
        cardReason      = findViewById(R.id.cardReason);
        cardVaccination = findViewById(R.id.cardVaccination);
        etReason        = findViewById(R.id.etReason);

        btnPickLocation = findViewById(R.id.btnPickLocation);
        tvLatLng        = findViewById(R.id.tvLatLng);
        mapView         = findViewById(R.id.mapView);

        btnConfirm     = findViewById(R.id.btnConfirm);
        progressSubmit = findViewById(R.id.progressSubmit);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Book Vet Appointment");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadDropdownsMaster() {
        String url = ApiConfig.endpoint("Animal/vet_appointment_data.php",
                "doctor_id", String.valueOf(doctorId),
                "animal_category_id", String.valueOf(animalCategoryId));
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success")) {
                            // Pincode
                            JSONArray arrPincode = obj.optJSONArray("pincodes");
                            pincodeItems.clear();
                            if (arrPincode != null) {
                                for (int i = 0; i < arrPincode.length(); i++)
                                    pincodeItems.add(arrPincode.getString(i));
                            } else {
                                pincodeItems.add("Select Pincode");
                            }
                            pincodeAdapter.notifyDataSetChanged();

                            // Breed
                            JSONArray arrBreed = obj.optJSONArray("breeds");
                            breedItems.clear();
                            if (arrBreed != null) {
                                for (int i = 0; i < arrBreed.length(); i++)
                                    breedItems.add(arrBreed.getString(i));
                            } else {
                                breedItems.add("Select Breed");
                            }
                            breedAdapter.notifyDataSetChanged();

                            // Vaccination
                            JSONArray arrVacc = obj.optJSONArray("vaccinations");
                            vaccinationItems.clear();
                            if (arrVacc != null) {
                                for (int i = 0; i < arrVacc.length(); i++)
                                    vaccinationItems.add(arrVacc.getString(i));
                            } else {
                                vaccinationItems.add("Select Vaccination");
                            }
                            vaccinationAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "Failed to load options", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        );
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(req);
    }

    private void setupDropdowns() {
        pincodeItems.clear(); pincodeItems.add("Select Pincode");
        pincodeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pincodeItems);
        spPincode.setAdapter(pincodeAdapter);

        breedItems.clear(); breedItems.add("Select Breed");
        breedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, breedItems);
        spBreed.setAdapter(breedAdapter);

        vaccinationItems.clear(); vaccinationItems.add("Select Vaccination");
        vaccinationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vaccinationItems);
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
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
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

    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;
        enableMyLocationLayerIfPermitted();

        LatLng indiaCenter = new LatLng(22.9734, 78.6569);
        setMarkerAndCamera(indiaCenter, false);

        googleMap.setOnMapClickListener(latLng -> setMarkerAndCamera(latLng, true));
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

        if (isEmpty(etOwnerName)) { etOwnerName.setError("Name is required"); ok = false; }
        String phone = valueOf(etOwnerPhone);
        if (TextUtils.isEmpty(phone)) { etOwnerPhone.setError("Phone number is required"); ok = false; }
        else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            etOwnerPhone.setError("Enter a valid phone number"); ok = false;
        }

        if (spPincode.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select pincode", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        if (isEmpty(etOwnerAddress)) { etOwnerAddress.setError("Address is required"); ok = false; }
        if (isEmpty(etAnimalName)) { etAnimalName.setError("Animal name is required"); ok = false; }
        if (isEmpty(etAnimalAge)) { etAnimalAge.setError("Animal age is required"); ok = false; }

        if (spBreed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select animal breed", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        if (currentMode == Mode.VISIT) {
            if (isEmpty(etReason)) { etReason.setError("Reason is required for Visit"); ok = false; }
        } else {
            if (spVaccination.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select vaccination type", Toast.LENGTH_SHORT).show();
                ok = false;
            }
        }

        if (currentLatLng == null) {
            Toast.makeText(this, "Please select location on map", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        etOwnerName.setError(null);
        etOwnerPhone.setError(null);
        etOwnerAddress.setError(null);
        etAnimalName.setError(null);
        etAnimalAge.setError(null);
        if (etReason != null) etReason.setError(null);
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

        // TODO: integrate with backend - collect all field values and send to your API

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressSubmit.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);
            Toast.makeText(this, "Appointment submitted.", Toast.LENGTH_LONG).show();
        }, 1000);
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); enableMyLocationLayerIfPermitted(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

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
