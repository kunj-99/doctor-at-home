package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class book_form extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 2001;

    // UI
    private TextView headerBook;
    private Spinner daySpinner, monthSpinner, yearSpinner, pincodeSpinner;
    private RadioGroup genderGroup;
    private Button bookButton, btnUseCurrent;
    private EditText dobInput, etName, etAddress, etProblem;
    private View mapClickCatcher;
    private View overlayContainer;
    private View btnCancelMap, btnSelectMap;

    // Intent
    private String doctorId, doctorName, appointmentStatus;

    // Maps
    private GoogleMap embeddedMap, fullscreenMap;
    private SupportMapFragment embeddedMapFragment, fullscreenMapFragment;
    private Marker embeddedMarker, overlayMarker;
    private FusedLocationProviderClient fusedLocationClient;

    // Points
    private LatLng selectedLocation = null;
    private LatLng tempOverlayLocation = null;

    // DOB
    private Calendar selectedDob = null;

    // state
    private boolean isLocating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_form);

        // Edge-to-edge + black bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.BLACK);
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) getWindow().setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) getWindow().setNavigationBarDividerColor(android.graphics.Color.BLACK);

        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                statusScrim.getLayoutParams().height = sys.top; statusScrim.requestLayout();
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
                navScrim.getLayoutParams().height = sys.bottom; navScrim.requestLayout();
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
                return insets;
            });
        }

        // bind
        headerBook     = findViewById(R.id.header_book);
        daySpinner     = findViewById(R.id.day_spinner);
        monthSpinner   = findViewById(R.id.month_spinner);
        yearSpinner    = findViewById(R.id.year_spinner);
        pincodeSpinner = findViewById(R.id.spinner_pincode);
        genderGroup    = findViewById(R.id.gender_group);
        bookButton     = findViewById(R.id.book_button);
        btnUseCurrent  = findViewById(R.id.btn_use_current_location);
        dobInput       = findViewById(R.id.dob_input);
        etName         = findViewById(R.id.patient_name);
        etAddress      = findViewById(R.id.address);
        etProblem      = findViewById(R.id.problem);
        mapClickCatcher = findViewById(R.id.map_click_catcher);

        overlayContainer = findViewById(R.id.fullscreen_map_overlay);
        btnCancelMap     = findViewById(R.id.btn_cancel_map);
        btnSelectMap     = findViewById(R.id.btn_select_map);

        Intent intent = getIntent();
        doctorId          = intent.getStringExtra("doctor_id");
        doctorName        = intent.getStringExtra("doctorName");
        appointmentStatus = intent.getStringExtra("appointment_status");
        headerBook.setText(doctorName != null ? doctorName : "Book Appointment");
        bookButton.setText(appointmentStatus != null ? appointmentStatus : "Book Appointment");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // pincode list (only selection; no geo)
        List<String> placeholder = new ArrayList<>();
        placeholder.add("Select pincode");
        ArrayAdapter<String> initAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, placeholder);
        initAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pincodeSpinner.setAdapter(initAdapter);
        pincodeSpinner.setEnabled(false);
        fetchPincodesForDoctor(doctorId);

        // embedded map via callback
        embeddedMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (embeddedMapFragment != null) {
            loaderutil.showLoader(this);
            embeddedMapFragment.getMapAsync(this);
        }

        dobInput.setOnClickListener(v -> openDatePicker());
        btnUseCurrent.setOnClickListener(v -> {
            startLocating();
            setCurrentLocation();
        });
        mapClickCatcher.setOnClickListener(v -> openMapOverlay());
        btnCancelMap.setOnClickListener(v -> closeMapOverlay(false));
        btnSelectMap.setOnClickListener(v -> closeMapOverlay(true));

        bookButton.setOnClickListener(v -> onClickBook());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        embeddedMap = googleMap;
        setupMapCommon(embeddedMap, false);
        startLocating();
        setCurrentLocation();
    }

    // ---------------- Booking ----------------
    private void onClickBook() {
        String name    = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String problem = etProblem.getText().toString().trim();
        String pin     = pincodeSpinner.getSelectedItem() != null ? pincodeSpinner.getSelectedItem().toString() : "";

        boolean valid = true;
        if (name.isEmpty())   { etName.setError("Please enter the patient's name."); valid = false; }
        if (address.isEmpty()){ etAddress.setError("Please enter the address.");     valid = false; }
        if (problem.isEmpty()){ etProblem.setError("Please describe the problem.");  valid = false; }

        if (pin.isEmpty() || "Select pincode".equals(pin)) {
            Toast.makeText(this, "Please select a pincode to continue.", Toast.LENGTH_SHORT).show();
            valid = false;
        } else if (!pin.matches("\\d{6}")) {
            Toast.makeText(this, "Please enter a valid 6-digit pincode.", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (selectedDob == null) {
            Toast.makeText(this, "Please select your Date of Birth.", Toast.LENGTH_SHORT).show();
            valid = false;
        } else {
            int ageCheck = calculateAge(selectedDob.get(Calendar.YEAR),
                    selectedDob.get(Calendar.MONTH) + 1,
                    selectedDob.get(Calendar.DAY_OF_MONTH));
            if (ageCheck < 0 || ageCheck > 150) {
                Toast.makeText(this, "Please select a valid date of birth.", Toast.LENGTH_SHORT).show();
                valid = false;
            }
        }

        int genderId = genderGroup.getCheckedRadioButtonId();
        String gender = (genderId != -1) ? ((RadioButton)findViewById(genderId)).getText().toString() : "";
        if (gender.isEmpty()) { Toast.makeText(this, "Please select a gender.", Toast.LENGTH_SHORT).show(); valid = false; }

        if (selectedLocation == null) { Toast.makeText(this, "Please select your location on the map.", Toast.LENGTH_SHORT).show(); valid = false; }
        if (!valid) return;

        int age = calculateAge(
                selectedDob.get(Calendar.YEAR),
                selectedDob.get(Calendar.MONTH) + 1,
                selectedDob.get(Calendar.DAY_OF_MONTH)
        );

        // Logging all extras being sent
        Log.d("BOOK_FORM_INTENT", "Passing intent to pending_bill with:");
        Log.d("BOOK_FORM_INTENT", "patient_name=" + name);
        Log.d("BOOK_FORM_INTENT", "age=" + age);
        Log.d("BOOK_FORM_INTENT", "gender=" + gender);
        Log.d("BOOK_FORM_INTENT", "problem=" + problem);
        Log.d("BOOK_FORM_INTENT", "address=" + address);
        Log.d("BOOK_FORM_INTENT", "pincode=" + pin);
        Log.d("BOOK_FORM_INTENT", "doctor_id=" + doctorId);
        Log.d("BOOK_FORM_INTENT", "doctorName=" + doctorName);
        Log.d("BOOK_FORM_INTENT", "appointment_status=" + appointmentStatus);
        Log.d("BOOK_FORM_INTENT", "latitude=" + (selectedLocation != null ? selectedLocation.latitude : "null"));
        Log.d("BOOK_FORM_INTENT", "longitude=" + (selectedLocation != null ? selectedLocation.longitude : "null"));

        Intent next = new Intent(book_form.this, pending_bill.class);
        next.putExtra("patient_name", name);
        next.putExtra("age", age);
        next.putExtra("gender", gender);
        next.putExtra("problem", problem);
        next.putExtra("address", address);
        next.putExtra("pincode", pin);
        next.putExtra("doctor_id", doctorId);
        next.putExtra("doctorName", doctorName);
        next.putExtra("appointment_status", appointmentStatus);
        next.putExtra("latitude", selectedLocation.latitude);
        next.putExtra("longitude", selectedLocation.longitude);
        startActivity(next);
    }

    // ---------------- Pincode list (no geo) ----------------
    private void fetchPincodesForDoctor(String doctorId) {
        loaderutil.showLoader(this);
        String url = ApiConfig.endpoint("get_pincode.php", "doctor_id", doctorId);
        RequestQueue q = Volley.newRequestQueue(this);

        JsonArrayRequest r = new JsonArrayRequest(Request.Method.GET, url, null,
                resp -> {
                    List<String> pins = new ArrayList<>();
                    pins.add("Select pincode");
                    try {
                        for (int i = 0; i < resp.length(); i++) {
                            String pin = resp.getString(i);
                            if (!TextUtils.isEmpty(pin)) pins.add(pin);
                        }
                    } catch (Exception ignored) { }

                    ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pins);
                    ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    pincodeSpinner.setAdapter(ad);
                    pincodeSpinner.setEnabled(true);

                    pincodeSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
                        @Override public void onItemSelected(int position) {
                            // Only selection; no geo action.
                        }
                    });

                    loaderutil.hideLoader();
                },
                err -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Pincode list unavailable. You can still proceed.", Toast.LENGTH_SHORT).show();
                });
        q.add(r);
    }

    // ---------------- Overlay (popup) ----------------
    private void openMapOverlay() {
        overlayContainer.setVisibility(View.VISIBLE);

        if (fullscreenMapFragment == null) {
            FragmentManager fm = getSupportFragmentManager();
            fullscreenMapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction()
                    .replace(R.id.map_fullscreen_container, fullscreenMapFragment)
                    .commitNowAllowingStateLoss();
            loaderutil.showLoader(this);
            fullscreenMapFragment.getMapAsync(m -> {
                fullscreenMap = m;
                setupMapCommon(fullscreenMap, true);
                if (selectedLocation != null) {
                    overlayMarker = fullscreenMap.addMarker(new MarkerOptions()
                            .position(selectedLocation)
                            .title("Selected Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    fullscreenMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 16f));
                    tempOverlayLocation = selectedLocation;
                }
                loaderutil.hideLoader();
            });
        } else if (fullscreenMap != null) {
            setupMapCommon(fullscreenMap, true);
        }
    }

    private void closeMapOverlay(boolean useSelected) {
        overlayContainer.setVisibility(View.GONE);
        if (useSelected && tempOverlayLocation != null) {
            selectedLocation = tempOverlayLocation;
            placeEmbeddedMarker(selectedLocation, true);
        }
    }

    // ---------------- Map common setup ----------------
    private void setupMapCommon(GoogleMap map, boolean isOverlay) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showLocationPermissionDialog();
        }
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        if (selectedLocation != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15f));
        map.setOnMapClickListener(latLng -> {
            if (isOverlay) {
                tempOverlayLocation = latLng;
                if (overlayMarker != null) overlayMarker.remove();
                overlayMarker = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Selected Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
            } else {
                selectedLocation = latLng;
                placeEmbeddedMarker(selectedLocation, true);
            }
        });
    }

    private void placeEmbeddedMarker(LatLng latLng, boolean animate) {
        if (embeddedMap == null) return;
        if (embeddedMarker != null) embeddedMarker.remove();
        embeddedMarker = embeddedMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Selected Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        if (animate) embeddedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        else embeddedMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }

    // ---------------- Permission & settings ----------------
    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("Location is needed to detect your position automatically.")
                .setPositiveButton("OK", (d, w) ->
                        ActivityCompat.requestPermissions(
                                book_form.this,
                                new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                                LOCATION_PERMISSION_REQUEST_CODE
                        ))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startLocating() {
        if (isLocating) return;
        isLocating = true;
        loaderutil.showLoader(this);
        checkLocationSettings();
    }

    private void stopLocating() {
        isLocating = false;
        loaderutil.hideLoader();
    }

    private void checkLocationSettings() {
        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(req);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(r -> setCurrentLocation())
                .addOnFailureListener(e -> {
                    stopLocating();
                    if (e instanceof ResolvableApiException) {
                        try { ((ResolvableApiException) e).startResolutionForResult(book_form.this, REQUEST_CHECK_SETTINGS); }
                        catch (Exception ignored) { Toast.makeText(this, "Unable to update your location settings.", Toast.LENGTH_SHORT).show(); }
                    } else {
                        Toast.makeText(this, "Your device's location settings could not be updated.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void setCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopLocating();
            showLocationPermissionDialog();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        onNewLocation(loc);
                        stopLocating();
                    } else {
                        // request fresh update
                        LocationRequest req = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(5000)
                                .setFastestInterval(2000);
                        fusedLocationClient.requestLocationUpdates(req, new LocationCallback() {
                            @Override public void onLocationResult(LocationResult result) {
                                if (result == null || result.getLastLocation() == null) return;
                                onNewLocation(result.getLastLocation());
                                fusedLocationClient.removeLocationUpdates(this);
                                stopLocating();
                            }
                        }, Looper.getMainLooper());
                    }
                })
                .addOnFailureListener(e -> {
                    stopLocating();
                    Toast.makeText(this, "Could not get your current location.", Toast.LENGTH_SHORT).show();
                });
    }

    private void onNewLocation(Location l) {
        LatLng here = new LatLng(l.getLatitude(), l.getLongitude());
        selectedLocation = here;
        placeEmbeddedMarker(here, true); // RED pin
    }

    // ---------------- DatePicker & age ----------------
    private void openDatePicker() {
        final Calendar now = Calendar.getInstance();
        int y = now.get(Calendar.YEAR), m = now.get(Calendar.MONTH), d = now.get(Calendar.DAY_OF_MONTH);
        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDob = Calendar.getInstance();
                    selectedDob.set(year, month, dayOfMonth);
                    @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    dobInput.setText(df.format(selectedDob.getTime()));
                },
                y, m, d
        );
        Calendar min = Calendar.getInstance(); min.add(Calendar.YEAR, -150);
        dlg.getDatePicker().setMinDate(min.getTimeInMillis());
        dlg.getDatePicker().setMaxDate(now.getTimeInMillis());
        dlg.show();
    }

    private int calculateAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance(); dob.set(year, month - 1, day);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                        today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age;
    }

    private abstract static class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override public void onNothingSelected(AdapterView<?> parent) {}
        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { onItemSelected(position); }
        public abstract void onItemSelected(int position);
    }
}
