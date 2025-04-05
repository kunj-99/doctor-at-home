package com.example.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import java.util.Calendar;

public class book_form extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "BookForm";

    EditText patientName, address, problem, edtPincode;
    Spinner daySpinner, monthSpinner, yearSpinner;
    RadioGroup genderGroup;
    Button bookButton;

    // Doctor details from Intent
    String doctorId, doctorName, appointmentStatus;

    // Google Maps objects
    private GoogleMap mMap;
    private LatLng selectedLocation = null; // To store user's selected location

    // FusedLocationProviderClient for user's current location
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_form);

        // Initialize views
        patientName = findViewById(R.id.patient_name);
        address = findViewById(R.id.address);
        daySpinner = findViewById(R.id.day_spinner);
        monthSpinner = findViewById(R.id.month_spinner);
        yearSpinner = findViewById(R.id.year_spinner);
        genderGroup = findViewById(R.id.gender_group);
        problem = findViewById(R.id.problem);
        edtPincode = findViewById(R.id.edt_pincode);  // New pincode field
        bookButton = findViewById(R.id.book_button);

        // Fetch Doctor ID & Name from Intent
        Intent intent = getIntent();
        doctorId = intent.getStringExtra("doctor_id");
        doctorName = intent.getStringExtra("doctorName");
        appointmentStatus = intent.getStringExtra("appointment_status");

        // Set the status text on the Book Button
        if (appointmentStatus != null) {
            bookButton.setText(appointmentStatus);
        } else {
            bookButton.setText("Book");
        }

        // Initialize the Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Handle Book Button Click with detailed validation
        bookButton.setOnClickListener(v -> {
            // Retrieve values and trim whitespace
            String name = patientName.getText().toString().trim();
            String addressText = address.getText().toString().trim();
            String selectedProblem = problem.getText().toString().trim();
            String pincode = edtPincode.getText().toString().trim();

            boolean valid = true;

            // Validate patient's name
            if (name.isEmpty()) {
                patientName.setError("Please enter patient's name");
                valid = false;
            }

            // Validate address
            if (addressText.isEmpty()) {
                address.setError("Please enter address");
                valid = false;
            }

            // Validate problem description
            if (selectedProblem.isEmpty()) {
                problem.setError("Please describe the problem");
                valid = false;
            }
            if (pincode.isEmpty()) {
                edtPincode.setError("Please enter pincode");
                valid = false;
            } else if (!pincode.matches("\\d{6}")) {
                edtPincode.setError("Please enter a valid 6-digit pincode");
                valid = false;
            }

            // Validate date fields
            int day, month, year;
            try {
                day = Integer.parseInt(daySpinner.getSelectedItem().toString());
                month = monthSpinner.getSelectedItemPosition() + 1;
                year = Integer.parseInt(yearSpinner.getSelectedItem().toString());
            } catch (Exception e) {
                Toast.makeText(book_form.this, "Please select a valid date", Toast.LENGTH_SHORT).show();
                return;
            }

            int calculatedAge = calculateAge(year, month, day);
            if (calculatedAge < 0 || calculatedAge > 150) {
                Toast.makeText(book_form.this, "Please select a valid date of birth", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            // Validate gender selection
            int selectedGenderId = genderGroup.getCheckedRadioButtonId();
            String gender = "";
            if (selectedGenderId != -1) {
                RadioButton selectedGender = findViewById(selectedGenderId);
                gender = selectedGender.getText().toString();
            } else {
                Toast.makeText(book_form.this, "Please select gender", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            // Validate that a location has been selected on the map
            if (selectedLocation == null) {
                Toast.makeText(book_form.this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
                valid = false;
            }

            // If any validation fails, stop further processing
            if (!valid) {
                return;
            }

            // If all validations pass, pass all data (including pincode and location) to the next activity
            Intent intentNext = new Intent(book_form.this, pending_bill.class);
            intentNext.putExtra("patient_name", name);
            intentNext.putExtra("age", calculatedAge);
            intentNext.putExtra("gender", gender);
            intentNext.putExtra("problem", selectedProblem);
            intentNext.putExtra("address", addressText);
            intentNext.putExtra("pincode", pincode);
            intentNext.putExtra("doctor_id", doctorId);
            intentNext.putExtra("doctorName", doctorName);
            intentNext.putExtra("appointment_status", appointmentStatus);
            intentNext.putExtra("latitude", selectedLocation.latitude);
            intentNext.putExtra("longitude", selectedLocation.longitude);
            startActivity(intentNext);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            if (selectedLocation == null) {
                checkLocationSettings();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            showLocationPermissionDialog();
        } else {
            checkLocationSettings();
        }
        mMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        });
    }

    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("This app requires access to your device's location to auto-detect your current position on the map. Please grant location permission.")
                .setPositiveButton("OK", (dialog, which) ->
                        ActivityCompat.requestPermissions(
                                book_form.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                LOCATION_PERMISSION_REQUEST_CODE))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(locationSettingsResponse -> setCurrentLocation());
        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(book_form.this, REQUEST_CHECK_SETTINGS);
                } catch (Exception ex) {
                    Toast.makeText(book_form.this, "Unable to resolve location settings.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(book_form.this, "Location settings are inadequate and cannot be fixed here.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void setCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Successfully retrieved last known location
                selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(selectedLocation).title("Current Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
            } else {
                // If last location is null, request a fresh location update
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(5000);
                locationRequest.setFastestInterval(2000);
                fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            Toast.makeText(book_form.this, "Unable to fetch current location", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Use the latest location from the update
                        android.location.Location loc = locationResult.getLastLocation();
                        if (loc != null) {
                            selectedLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(selectedLocation).title("Current Location"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                            // Remove updates after a location is obtained
                            fusedLocationClient.removeLocationUpdates(this);
                        }
                    }
                }, Looper.getMainLooper());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(book_form.this, "Error fetching location", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                setCurrentLocation();
            } else {
                Toast.makeText(this, "Please enable location to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private int calculateAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        dob.set(year, month - 1, day);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                        today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age;
    }
}
