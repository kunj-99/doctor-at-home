package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class book_form extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS         = 2001;

    // UI
    private TextView headerBook;
    private Spinner  daySpinner, monthSpinner, yearSpinner, pincodeSpinner;
    private RadioGroup genderGroup;
    private Button     bookButton;

    // Doctor
    private String doctorId, doctorName, appointmentStatus;

    // Location / Map
    private GoogleMap               mMap;
    private LatLng                  selectedLocation = null;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_form);

        //── bind views ────────────────────────────────────────────────────────────
        headerBook     = findViewById(R.id.header_book);
        daySpinner     = findViewById(R.id.day_spinner);
        monthSpinner   = findViewById(R.id.month_spinner);
        yearSpinner    = findViewById(R.id.year_spinner);
        pincodeSpinner = findViewById(R.id.spinner_pincode);
        genderGroup    = findViewById(R.id.gender_group);
        bookButton     = findViewById(R.id.book_button);

        //── get Intent data ────────────────────────────────────────────────────────
        Intent intent     = getIntent();
        doctorId          = intent.getStringExtra("doctor_id");
        doctorName        = intent.getStringExtra("doctorName");
        appointmentStatus = intent.getStringExtra("appointment_status");

        //── display doctor's name in header ───────────────────────────────────────
        headerBook.setText(doctorName != null ? doctorName : "Book Appointment");

        //── button label ───────────────────────────────────────────────────────────
        bookButton.setText(appointmentStatus != null ? appointmentStatus : "Book");

        //── prepare map fragment ──────────────────────────────────────────────────
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        //── initial spinner state: show placeholder & disable until data loads ───
        List<String> placeholder = new ArrayList<>();
        placeholder.add("Select pincode");
        ArrayAdapter<String> initAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, placeholder
        );
        initAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        pincodeSpinner.setAdapter(initAdapter);
        pincodeSpinner.setEnabled(false);

        //── fetch real pincodes (with loader) ────────────────────────────────────
        fetchPincodesForDoctor(doctorId);

        //── book button click ─────────────────────────────────────────────────────
        bookButton.setOnClickListener(v -> {
            String name    = ((EditText)findViewById(R.id.patient_name))
                    .getText().toString().trim();
            String address = ((EditText)findViewById(R.id.address))
                    .getText().toString().trim();
            String problem = ((EditText)findViewById(R.id.problem))
                    .getText().toString().trim();
            String pin     = pincodeSpinner.getSelectedItem() != null
                    ? pincodeSpinner.getSelectedItem().toString()
                    : "";

            boolean valid = true;
            if (name.isEmpty()) {
                ((EditText)findViewById(R.id.patient_name))
                        .setError("Enter patient's name");
                valid = false;
            }
            if (address.isEmpty()) {
                ((EditText)findViewById(R.id.address))
                        .setError("Enter address");
                valid = false;
            }
            if (problem.isEmpty()) {
                ((EditText)findViewById(R.id.problem))
                        .setError("Describe the problem");
                valid = false;
            }
            // placeholder check
            if (pin.isEmpty() || pin.equals("Select pincode")) {
                Toast.makeText(this,
                        "Please select a pincode", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            // date validation
            int day, month, year;
            try {
                day   = Integer.parseInt(daySpinner.getSelectedItem().toString());
                month = monthSpinner.getSelectedItemPosition() + 1;
                year  = Integer.parseInt(yearSpinner.getSelectedItem().toString());
            } catch (Exception e) {
                Toast.makeText(this,
                        "Please select a valid date", Toast.LENGTH_SHORT).show();
                return;
            }
            int age = calculateAge(year, month, day);
            if (age < 0 || age > 150) {
                Toast.makeText(this,
                        "Please select a valid date of birth", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            // gender
            int genderId = genderGroup.getCheckedRadioButtonId();
            String gender = "";
            if (genderId != -1) {
                gender = ((RadioButton)findViewById(genderId))
                        .getText().toString();
            } else {
                Toast.makeText(this,
                        "Please select gender", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            // map location
            if (selectedLocation == null) {
                Toast.makeText(this,
                        "Please select a location on the map", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            if (!valid) return;

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
        });
    }

    private void fetchPincodesForDoctor(String doctorId) {
        // show your custom loader if network is slow or unavailable
        loaderutil.showLoader(this);

        // check immediate connectivity; if none, we still show loader until retry
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Toast.makeText(this,
                    "No network—waiting to retry...", Toast.LENGTH_SHORT).show();
        }

        String url = "http://sxm.a58.mytemp.website/get_pincode.php?doctor_id=" + doctorId;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    // build list with placeholder first
                    List<String> pins = new ArrayList<>();
                    pins.add("Select pincode");
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            String pin = response.getString(i);
                            if (!pin.isEmpty()) pins.add(pin);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // populate spinner and enable it
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            book_form.this,
                            android.R.layout.simple_spinner_item,
                            pins
                    );
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    pincodeSpinner.setAdapter(adapter);
                    pincodeSpinner.setEnabled(true);

                    // hide loader now that data is in
                    loaderutil.hideLoader();
                },
                error -> {
                    // on any error, hide loader and let user retry
                    loaderutil.hideLoader();
                    Toast.makeText(book_form.this,
                            "Failed to load pincodes", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(jsonArrayRequest);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            showLocationPermissionDialog();
        } else {
            checkLocationSettings();
        }
        mMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15));
        });
    }

    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("This app needs location to auto-detect your position.")
                .setPositiveButton("OK", (d, w) ->
                        ActivityCompat.requestPermissions(
                                book_form.this,
                                new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                },
                                LOCATION_PERMISSION_REQUEST_CODE
                        ))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkLocationSettings() {
        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder().addLocationRequest(req);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task =
                client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(r -> setCurrentLocation())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException)e)
                                    .startResolutionForResult(
                                            book_form.this, REQUEST_CHECK_SETTINGS);
                        } catch (Exception ex) {
                            Toast.makeText(this,
                                    "Cannot resolve location settings.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this,
                                "Location settings cannot be fixed here.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void setCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        selectedLocation = new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        );
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions()
                                .position(selectedLocation)
                                .title("Current Location"));
                        mMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                    } else {
                        // if null, request an update
                        LocationRequest req = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(5000)
                                .setFastestInterval(2000);
                        fusedLocationClient.requestLocationUpdates(
                                req,
                                new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult result) {
                                        if (result == null) return;
                                        selectedLocation = new LatLng(
                                                result.getLastLocation().getLatitude(),
                                                result.getLastLocation().getLongitude()
                                        );
                                        mMap.clear();
                                        mMap.addMarker(new MarkerOptions()
                                                .position(selectedLocation)
                                                .title("Current Location"));
                                        mMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                                        fusedLocationClient.removeLocationUpdates(this);
                                    }
                                },
                                Looper.getMainLooper()
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error fetching location", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                Toast.makeText(this,
                        "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(
                    requestCode, permissions, grantResults
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data
    ) {
        if (requestCode == REQUEST_CHECK_SETTINGS
                && resultCode == RESULT_OK) {
            setCurrentLocation();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private int calculateAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        dob.set(year, month - 1, day);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)
                || (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age;
    }
}
