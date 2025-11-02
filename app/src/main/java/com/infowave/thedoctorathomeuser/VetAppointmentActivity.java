package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VetAppointmentActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "VetAppointmentActivity";
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // Intent params
    private int doctorId = -1;
    private int animalCategoryId = -1;
    private String doctorName;
    private String doctorAutoStatus;           // carries availability from list (auto_status)
    private String adapterProvidedCTA = null;  // "Book Appointment" / "Request for visit" (from adapter)
    private String derivedInitialStatus = null;// "Confirmed"/"Requested" after mapping

    // UI refs
    private Spinner spPincode, spBreed, spVaccination;
    private ArrayAdapter<String> pincodeAdapter, breedAdapter, vaccinationAdapter;

    // Pincode & breed simple lists
    private final List<String> pincodeItems = new ArrayList<>();
    private final List<String> breedItems = new ArrayList<>();

    // Vaccination: keep DISPLAY and SOURCE lists separate (index-aligned)
    // index 0 is placeholder in all lists
    private final List<String> vaccinationDisplayItems = new ArrayList<>(); // "Name — ₹Price"
    private final List<String> vaccinationNameList     = new ArrayList<>(); // raw names
    private final List<Integer> vaccinationIdList      = new ArrayList<>(); // ids
    private final List<Double>  vaccinationPriceList   = new ArrayList<>(); // prices

    private TextInputEditText etOwnerName, etOwnerPhone, etOwnerAddress;
    private TextInputEditText etAnimalName, etAnimalAge, etReason;
    private MaterialButton btnVisit, btnVaccination, btnPickLocation, btnConfirm;
    private MaterialCardView cardReason, cardVaccination;
    private RadioGroup rgAnimalGender;
    private TextView tvLatLng;
    private ProgressBar progressSubmit;

    // Embedded MapView (small map)
    private MapView mapView;
    private GoogleMap googleMap;
    private Marker marker;
    private View mapClickCatcherVet;

    // Fullscreen overlay map
    private View overlayContainer;
    private MaterialButton btnCancelOverlay, btnSelectOverlay;
    private SupportMapFragment fullscreenMapFragment;
    private GoogleMap fullscreenMap;
    private Marker overlayMarker;
    private LatLng tempOverlayLocation = null;  // while overlay is open

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private LatLng currentLatLng = null;

    // Volley
    private RequestQueue requestQueue;

    // Mode
    private enum Mode { VISIT, VACCINATION }
    private Mode currentMode = Mode.VISIT;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_appointment);

        // ============ INTENT EXTRAS ============
        doctorId         = getIntent().getIntExtra("doctor_id", -1);
        animalCategoryId = getIntent().getIntExtra("animal_category_id", -1);
        doctorName       = getIntent().getStringExtra("doctor_name");
        doctorAutoStatus = getIntent().getStringExtra("auto_status"); // may be null
        adapterProvidedCTA = getIntent().getStringExtra("appointment_status"); // from adapter, if sent

        if (doctorName == null) doctorName = "";

        Log.d(TAG, "[onCreate] doctorId=" + doctorId
                + ", animalCategoryId=" + animalCategoryId
                + ", doctorName=" + doctorName
                + ", auto_status=" + doctorAutoStatus
                + ", adapterProvidedCTA=" + adapterProvidedCTA);

        if (doctorId == -1 || animalCategoryId == -1) {
            Toast.makeText(this, "Invalid booking parameters. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "[onCreate] Invalid parameters, finishing.");
            finish(); return;
        }

        // Decide initial status (HUMAN parity) — Prefer adapter CTA if present, else derive from auto_status
        derivedInitialStatus = decideInitialStatus(adapterProvidedCTA, doctorAutoStatus);
        Log.d(TAG, "[onCreate] derivedInitialStatus=" + derivedInitialStatus);

        // ============ SYSTEM BARS ============
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsController controller = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ? getWindow().getInsetsController() : null;
        if (controller != null) {
            controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        View statusScrim = findViewById(R.id.status_bar_scrim);
        View navScrim    = findViewById(R.id.navigation_bar_scrim);
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

        // ============ BIND / INIT ============
        initViews();
        setupToolbar();
        setupDropdowns();
        setupModeToggle();
        applyMode(Mode.VISIT);
        setupLocation();
        setupConfirm();
        loadDropdownsMaster();

        // MapView lifecycle
        Bundle mapViewBundle = (savedInstanceState != null) ? savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Volley
        requestQueue = Volley.newRequestQueue(this);
    }

    // --- Decide initial status with logs ---
    private String decideInitialStatus(@Nullable String ctaFromAdapter, @Nullable String autoStatusRaw) {
        // 1) If adapter already sent CTA text, map that to final DB values
        if (!TextUtils.isEmpty(ctaFromAdapter)) {
            String cta = ctaFromAdapter.trim();
            if (cta.equalsIgnoreCase("Request for visit")) {
                Log.d(TAG, "[decideInitialStatus] adapterProvidedCTA='Request for visit' → status='Requested'");
                return "Requested";
            }
            if (cta.equalsIgnoreCase("Book Appointment")) {
                Log.d(TAG, "[decideInitialStatus] adapterProvidedCTA='Book Appointment' → status='Confirmed'");
                return "Confirmed";
            }
            // If adapter sent direct DB status, accept it
            if (cta.equalsIgnoreCase("Requested") || cta.equalsIgnoreCase("Confirmed")) {
                Log.d(TAG, "[decideInitialStatus] adapterProvidedCTA as direct DB status → " + cta);
                return toTitleCase(cta);
            }
            Log.w(TAG, "[decideInitialStatus] Unknown CTA from adapter: " + cta + " → fallback to auto_status");
        }

        // 2) Else, derive from auto_status
        String auto = (autoStatusRaw == null) ? "" : autoStatusRaw.trim().toLowerCase(Locale.ROOT);
        boolean isActive = auto.equals("active") || auto.equals("online") || auto.equals("available");
        String derived = isActive ? "Confirmed" : "Requested";
        Log.d(TAG, "[decideInitialStatus] auto_status='" + autoStatusRaw + "' → status='" + derived + "'");
        return derived;
    }

    private String toTitleCase(String s) {
        if (s == null) return "";
        String low = s.toLowerCase(Locale.ROOT);
        if ("requested".equals(low)) return "Requested";
        if ("confirmed".equals(low)) return "Confirmed";
        return s;
    }

    private void initViews() {
        spPincode      = findViewById(R.id.spPincode);
        spBreed        = findViewById(R.id.spBreed);
        spVaccination  = findViewById(R.id.spVaccination);
        rgAnimalGender = findViewById(R.id.rgAnimalGender);

        etOwnerName    = findViewById(R.id.etOwnerName);
        etOwnerPhone   = findViewById(R.id.etOwnerPhone);
        etOwnerAddress = findViewById(R.id.etOwnerAddress);

        etAnimalName   = findViewById(R.id.etAnimalName);
        etAnimalAge    = findViewById(R.id.etAnimalAge);

        btnVisit       = findViewById(R.id.btnVisit);
        btnVaccination = findViewById(R.id.btnVaccination);
        cardReason     = findViewById(R.id.cardReason);
        cardVaccination= findViewById(R.id.cardVaccination);
        etReason       = findViewById(R.id.etReason);

        btnPickLocation= findViewById(R.id.btnPickLocation);
        tvLatLng       = findViewById(R.id.tvLatLng);
        mapView        = findViewById(R.id.mapView);
        mapClickCatcherVet = findViewById(R.id.mapClickCatcherVet);

        btnConfirm     = findViewById(R.id.btnConfirm);
        progressSubmit = findViewById(R.id.progressSubmit);

        overlayContainer   = findViewById(R.id.fullscreen_map_overlay_vet);
        btnCancelOverlay   = findViewById(R.id.btn_cancel_map_vet);
        btnSelectOverlay   = findViewById(R.id.btn_select_map_vet);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Book Vet Appointment");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDropdowns() {
        // Pincode & Breed placeholders
        pincodeItems.clear();       pincodeItems.add("Select Pincode");
        breedItems.clear();         breedItems.add("Select Breed");

        // Vaccination: clear & add placeholders IN ALL LISTS (keep index alignment)
        vaccinationDisplayItems.clear();
        vaccinationNameList.clear();
        vaccinationIdList.clear();
        vaccinationPriceList.clear();

        vaccinationDisplayItems.add("Select Vaccination");
        vaccinationNameList.add("");         // placeholder
        vaccinationIdList.add(0);            // placeholder
        vaccinationPriceList.add(0.0);       // placeholder

        pincodeAdapter     = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pincodeItems);
        spPincode.setAdapter(pincodeAdapter);

        breedAdapter       = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, breedItems);
        spBreed.setAdapter(breedAdapter);

        vaccinationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vaccinationDisplayItems);
        spVaccination.setAdapter(vaccinationAdapter);
    }

    private void setupModeToggle() {
        btnVisit.setOnClickListener(v -> applyMode(Mode.VISIT));
        btnVaccination.setOnClickListener(v -> applyMode(Mode.VACCINATION));
    }

    private void applyMode(Mode mode) {
        currentMode = mode;
        Log.d(TAG, "[applyMode] mode=" + mode);
        if (mode == Mode.VISIT) {
            cardReason.setVisibility(View.VISIBLE);
            cardVaccination.setVisibility(View.GONE);
            btnVisit.setChecked(true); btnVisit.setStrokeWidth(3);
            btnVaccination.setChecked(false); btnVaccination.setStrokeWidth(1);
        } else {
            cardReason.setVisibility(View.GONE);
            cardVaccination.setVisibility(View.VISIBLE);
            btnVaccination.setChecked(true); btnVaccination.setStrokeWidth(3);
            btnVisit.setChecked(false); btnVisit.setStrokeWidth(1);
        }
    }

    private void loadDropdownsMaster() {
        String url = ApiConfig.endpoint(
                "Animal/vet_appointment_data.php",
                "doctor_id", String.valueOf(doctorId),
                "animal_category_id", String.valueOf(animalCategoryId)
        );

        Log.d(TAG, "[loadDropdownsMaster] GET " + url);

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean success = obj.optBoolean("success");
                        Log.d(TAG, "[loadDropdownsMaster] success=" + success);
                        if (success) {
                            JSONArray arrPincode      = obj.optJSONArray("pincodes");
                            JSONArray arrBreed        = obj.optJSONArray("breeds");
                            JSONArray arrVaccinations = obj.optJSONArray("vaccinations");

                            // ----- Pincodes -----
                            pincodeItems.clear(); pincodeItems.add("Select Pincode");
                            if (arrPincode != null) {
                                for (int i = 0; i < arrPincode.length(); i++) {
                                    String pin = arrPincode.getString(i);
                                    pincodeItems.add(pin);
                                }
                            }
                            Log.d(TAG, "[loadDropdownsMaster] pincodes=" + pincodeItems.size());

                            // ----- Breeds -----
                            breedItems.clear(); breedItems.add("Select Breed");
                            if (arrBreed != null) {
                                for (int i = 0; i < arrBreed.length(); i++) {
                                    String b = arrBreed.getString(i);
                                    breedItems.add(b);
                                }
                            }
                            Log.d(TAG, "[loadDropdownsMaster] breeds=" + breedItems.size());

                            // ----- Vaccinations (id, name, price) -----
                            vaccinationDisplayItems.clear();
                            vaccinationNameList.clear();
                            vaccinationIdList.clear();
                            vaccinationPriceList.clear();

                            vaccinationDisplayItems.add("Select Vaccination");
                            vaccinationNameList.add("");
                            vaccinationIdList.add(0);
                            vaccinationPriceList.add(0.0);

                            if (arrVaccinations != null) {
                                for (int i = 0; i < arrVaccinations.length(); i++) {
                                    Object itm = arrVaccinations.get(i);
                                    if (itm instanceof JSONObject) {
                                        JSONObject v = (JSONObject) itm;

                                        int    id    = v.optInt("id", v.optInt("vaccination_id", 0));
                                        String name  = v.optString("name",
                                                v.optString("vaccination_name",
                                                        v.optString("title",
                                                                v.optString("vaccine_name", "Unknown"))));
                                        double price = v.optDouble("price", 0.0);

                                        vaccinationNameList.add(name);
                                        vaccinationIdList.add(id);
                                        vaccinationPriceList.add(price);

                                        String display = name + " — " + formatPrice(price);
                                        vaccinationDisplayItems.add(display);

                                    } else {
                                        String name = arrVaccinations.getString(i);
                                        vaccinationNameList.add(name);
                                        vaccinationIdList.add(0);
                                        vaccinationPriceList.add(0.0);
                                        String display = name + " — " + formatPrice(0.0);
                                        vaccinationDisplayItems.add(display);
                                    }
                                }
                            }
                            Log.d(TAG, "[loadDropdownsMaster] vaccinations=" + (vaccinationDisplayItems.size()-1));

                            pincodeAdapter.notifyDataSetChanged();
                            breedAdapter.notifyDataSetChanged();
                            vaccinationAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "Failed to load options", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "[loadDropdownsMaster] API success=false");
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "[loadDropdownsMaster] parse error", e);
                    }
                },
                error -> {
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "[loadDropdownsMaster] network error", error);
                }
        );

        if (requestQueue == null) requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(req);
    }

    private String formatPrice(double price) {
        long rounded = Math.round(price);
        if (Math.abs(price - rounded) < 0.005) {
            return "₹ " + rounded;
        } else {
            return String.format(Locale.getDefault(), "₹ %.2f", price);
        }
    }

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    Log.d(TAG, "[locationPerm] granted=" + isGranted);
                    if (isGranted) fetchAndCenterOnLocation();
                    else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                });

        btnPickLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                loaderutil.showLoader(this);
                fetchAndCenterOnLocation();
            } else {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        mapClickCatcherVet.setOnClickListener(v -> openMapOverlay());
    }

    // ---------- Embedded MapView callbacks ----------
    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;
        Log.d(TAG, "[onMapReady] map ready");
        enableMyLocationLayerIfPermitted();

        LatLng indiaCenter = new LatLng(22.9734, 78.6569);
        setMarkerAndCamera(indiaCenter, false);

        googleMap.setOnMapClickListener(latLng -> setMarkerAndCamera(latLng, true));
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(@NonNull Marker m) {}
            @Override public void onMarkerDrag(@NonNull Marker m) {
                LatLng p = m.getPosition();
                tvLatLng.setText(String.format(Locale.getDefault(),"Lat: %.6f, Lng: %.6f", p.latitude, p.longitude));
            }
            @Override public void onMarkerDragEnd(@NonNull Marker m) {
                LatLng p = m.getPosition();
                setMarkerAndCamera(p, true);
            }
        });

        // first map load → try current location
        loaderutil.showLoader(this);
        fetchAndCenterOnLocation();
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationLayerIfPermitted() {
        if (googleMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            Log.d(TAG, "[enableMyLocationLayer] enabled");
        } else {
            Log.d(TAG, "[enableMyLocationLayer] permission not granted");
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchAndCenterOnLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    loaderutil.hideLoader();
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "[fetchAndCenterOnLocation] got last location: " + latLng);
                        setMarkerAndCamera(latLng, true);
                    } else {
                        Log.w(TAG, "[fetchAndCenterOnLocation] location null");
                        Toast.makeText(this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loaderutil.hideLoader();
                    Log.e(TAG, "[fetchAndCenterOnLocation] error", e);
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setMarkerAndCamera(LatLng latLng, boolean animate) {
        currentLatLng = latLng;
        tvLatLng.setText(String.format(Locale.getDefault(),
                "Lat: %.6f, Lng: %.6f", latLng.latitude, latLng.longitude));

        if (googleMap == null) return;

        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .draggable(true));
        } else {
            marker.setPosition(latLng);
        }

        if (animate) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        else         googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));

        Log.d(TAG, "[setMarkerAndCamera] lat=" + latLng.latitude + ", lng=" + latLng.longitude + ", animate=" + animate);
    }

    // ---------- Fullscreen overlay map ----------
    private void openMapOverlay() {
        overlayContainer.setVisibility(View.VISIBLE);
        Log.d(TAG, "[openMapOverlay] open");
        if (fullscreenMapFragment == null) {
            FragmentManager fm = getSupportFragmentManager();
            fullscreenMapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction()
                    .replace(R.id.map_fullscreen_container_vet, fullscreenMapFragment)
                    .commitNowAllowingStateLoss();

            loaderutil.showLoader(this);
            fullscreenMapFragment.getMapAsync(m -> {
                fullscreenMap = m;
                setupOverlayMap(fullscreenMap);
                loaderutil.hideLoader();
            });
        } else if (fullscreenMap != null) {
            setupOverlayMap(fullscreenMap);
        }

        btnCancelOverlay.setOnClickListener(v -> closeMapOverlay(false));
        btnSelectOverlay.setOnClickListener(v -> closeMapOverlay(true));
    }

    private void setupOverlayMap(GoogleMap map) {
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);

        LatLng start = (currentLatLng != null) ? currentLatLng : new LatLng(22.9734, 78.6569);
        tempOverlayLocation = start;

        if (overlayMarker != null) overlayMarker.remove();
        overlayMarker = map.addMarker(new MarkerOptions().position(start).title("Selected Location"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 16f));
        Log.d(TAG, "[setupOverlayMap] start=" + start);

        map.setOnMapClickListener(latLng -> {
            tempOverlayLocation = latLng;
            if (overlayMarker != null) overlayMarker.remove();
            overlayMarker = map.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
            Log.d(TAG, "[setupOverlayMap] picked=" + latLng);
        });
    }

    private void closeMapOverlay(boolean useSelected) {
        overlayContainer.setVisibility(View.GONE);
        Log.d(TAG, "[closeMapOverlay] useSelected=" + useSelected + ", temp=" + tempOverlayLocation);
        if (useSelected && tempOverlayLocation != null) {
            setMarkerAndCamera(tempOverlayLocation, true);
        }
    }

    // ---------- Confirm ----------
    private void setupConfirm() {
        btnConfirm.setOnClickListener(v -> {
            if (validateForm()) submitAppointment();
        });
    }

    private boolean validateForm() {
        boolean ok = true;
        clearErrors();

        if (isEmpty(etOwnerName)) {
            etOwnerName.setError("Name is required"); ok = false;
        }
        String phone = valueOf(etOwnerPhone);
        if (TextUtils.isEmpty(phone)) {
            etOwnerPhone.setError("Phone number is required"); ok = false;
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            etOwnerPhone.setError("Enter a valid phone number"); ok = false;
        }

        if (spPincode.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select pincode", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        if (isEmpty(etOwnerAddress)) {
            etOwnerAddress.setError("Address is required"); ok = false;
        }
        if (isEmpty(etAnimalName)) {
            etAnimalName.setError("Animal name is required"); ok = false;
        }
        if (isEmpty(etAnimalAge)) {
            etAnimalAge.setError("Animal age is required"); ok = false;
        }
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

        Log.d(TAG, "[validateForm] ok=" + ok);
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

        // Owner
        String ownerName      = valueOf(etOwnerName);
        String ownerPhone     = valueOf(etOwnerPhone);
        String ownerAddress   = valueOf(etOwnerAddress);

        // Animal
        String animalName     = valueOf(etAnimalName);
        String animalAgeStr   = valueOf(etAnimalAge);
        int animalAgeInt = 0;
        try { animalAgeInt = Integer.parseInt(animalAgeStr.trim()); } catch (NumberFormatException ignore) { animalAgeInt = 0; }

        String animalBreed    = (spBreed.getSelectedItemPosition() > 0 && spBreed.getSelectedItemPosition() < breedItems.size())
                ? breedItems.get(spBreed.getSelectedItemPosition())
                : "";

        String animalGender = "Unknown";
        if (rgAnimalGender != null) {
            int checkedId = rgAnimalGender.getCheckedRadioButtonId();
            if (checkedId != -1) {
                RadioButton selectedBtn = findViewById(checkedId);
                if (selectedBtn != null) animalGender = selectedBtn.getText().toString().trim();
            }
        }

        // Vaccination selections (index-aligned lists)
        int vPos = spVaccination.getSelectedItemPosition();
        String vaccinationName = (vPos > 0 && vPos < vaccinationNameList.size())
                ? vaccinationNameList.get(vPos)
                : "";
        int vaccinationId = (vPos > 0 && vPos < vaccinationIdList.size())
                ? (vaccinationIdList.get(vPos) == null ? 0 : vaccinationIdList.get(vPos))
                : 0;
        double vaccinationPrice = (vPos > 0 && vPos < vaccinationPriceList.size())
                ? vaccinationPriceList.get(vPos)
                : 0.0;

        String reasonForVisit = (currentMode == Mode.VISIT) ? valueOf(etReason) : vaccinationName;

        String pincode = (spPincode.getSelectedItemPosition() > 0 && spPincode.getSelectedItemPosition() < pincodeItems.size())
                ? pincodeItems.get(spPincode.getSelectedItemPosition())
                : "";

        double latitude  = (currentLatLng != null) ? currentLatLng.latitude  : 0.0;
        double longitude = (currentLatLng != null) ? currentLatLng.longitude : 0.0;

        Log.d(TAG, "[submitAppointment] adapterProvidedCTA=" + adapterProvidedCTA
                + ", auto_status=" + doctorAutoStatus
                + ", finalStatus=" + derivedInitialStatus);

        // ---- Launch pending_bill (same keys you already use) ----
        Intent intent = new Intent(this, pending_bill.class);

        // Patient/owner
        intent.putExtra("patient_name", ownerName);
        intent.putExtra("age", animalAgeInt);                      // (re-using 'age' field for vet flow)
        intent.putExtra("gender", animalGender);
        intent.putExtra("problem", reasonForVisit);
        intent.putExtra("address", ownerAddress);

        // Doctor
        intent.putExtra("doctor_id", String.valueOf(doctorId));
        intent.putExtra("doctorName", doctorName);

        // *** CRITICAL: status must follow human flow parity ***
        intent.putExtra("appointment_status", derivedInitialStatus);
        intent.putExtra("status", derivedInitialStatus);

        // Location & pin
        intent.putExtra("pincode", pincode);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);

        // Vet block
        intent.putExtra("is_vet_case", 1);
        intent.putExtra("animal_category_id", String.valueOf(animalCategoryId));
        intent.putExtra("animal_name", animalName);
        intent.putExtra("animal_gender", animalGender);
        intent.putExtra("animal_age", animalAgeInt);
        intent.putExtra("animal_breed", animalBreed);
        intent.putExtra("vaccination_id", vaccinationId == 0 ? "" : String.valueOf(vaccinationId));
        intent.putExtra("vaccination_name", vaccinationName);
        intent.putExtra("vaccination_price", vaccinationPrice); // exact selected price

        Log.d(TAG, "[submitAppointment] → launching pending_bill with extras:"
                + " doctor_id=" + doctorId
                + ", doctorName=" + doctorName
                + ", is_vet_case=1"
                + ", animal_category_id=" + animalCategoryId
                + ", appointment_status=" + derivedInitialStatus
                + ", pincode=" + pincode
                + ", lat=" + latitude + ", lng=" + longitude
                + ", vaccination_id=" + vaccinationId
                + ", vaccination_name=" + vaccinationName
                + ", vaccination_price=" + vaccinationPrice);

        startActivity(intent);
        finish();
    }

    // ---------- MapView lifecycle ----------
    @Override protected void onStart()  { super.onStart();  mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); enableMyLocationLayerIfPermitted(); }
    @Override protected void onPause()  { mapView.onPause();  super.onPause(); }
    @Override protected void onStop()   { mapView.onStop();   super.onStop(); }
    @Override protected void onDestroy(){ mapView.onDestroy();super.onDestroy(); }
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
