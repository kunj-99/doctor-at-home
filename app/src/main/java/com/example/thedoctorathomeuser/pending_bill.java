package com.example.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class pending_bill extends AppCompatActivity {

    private static final int UPI_PAYMENT_REQUEST_CODE = 123;
    private static final int REQUEST_CHECK_SETTINGS = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 102;
    private static final String TAG = "PendingBill";

    // Merchant UPI details (ideally, store these securely)
    private static final String MERCHANT_UPI_ID = "abhitadhani98244-2@okicici";
    private static final String MERCHANT_NAME = " the doctor at home";
    private static final String TRANSACTION_NOTE = "Payment for appointment";
    private static final String AMOUNT = "500.00";
    private static final String CURRENCY = "INR";

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod;
    private String patientId;  // Retrieved from SharedPreferences
    private Button payButton;

    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName, tvAppointmentId;

    // FusedLocationProviderClient to get user's current location
    private FusedLocationProviderClient fusedLocationClient;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Initialize the fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Retrieve data from Intent extras
        Intent intent = getIntent();
        patientName = intent.getStringExtra("patient_name");
        age = String.valueOf(intent.getIntExtra("age", 0));
        gender = intent.getStringExtra("gender");
        problem = intent.getStringExtra("problem");
        address = intent.getStringExtra("address");
        doctorId = intent.getStringExtra("doctor_id");
        doctorName = intent.getStringExtra("doctorName");
        Status = intent.getStringExtra("appointment_status");
        if ("Request for visit".equals(Status)) {
            Status = "Requested";
        } else if ("Book Appointment".equals(Status)) {
            Status = "Confirmed";
        }
        Log.d(TAG, "Booking details: patientName=" + patientName + ", age=" + age +
                ", gender=" + gender + ", problem=" + problem + ", address=" + address +
                ", doctorId=" + doctorId + ", Status=" + Status);

        // Retrieve patient_id from SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Patient ID not available in SharedPreferences");
            finish();
            return;
        }
        Log.d(TAG, "Patient ID: " + patientId);

        // Retrieve dynamic bill info TextViews from the layout
        tvBillPatientName = findViewById(R.id.tv_bill_patient_name);
        tvBillDoctorName = findViewById(R.id.tv_bill_doctor_name);
        tvBillDate = findViewById(R.id.tv_bill_date);
        tvBillTime = findViewById(R.id.tv_bill_time);

        // Set dynamic values:
        if (patientName != null) {
            tvBillPatientName.setText(patientName);
        }
        if (doctorName != null) {
            tvBillDoctorName.setText(doctorName);
        }
        // Set current date as the bill date in the format "dd MMMM, yyyy"
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        String currentDate = sdfDate.format(new Date());
        tvBillDate.setText(currentDate);

        // Set current time in the format "hh:mm a"
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = sdfTime.format(new Date());
        tvBillTime.setText(currentTime);

        // Optionally, set appointment id if available from intent (or generate one)
        String dynamicAppointmentId = intent.getStringExtra("appointment_id");
        if (dynamicAppointmentId != null && !dynamicAppointmentId.isEmpty()) {
            tvAppointmentId.setText("#" + dynamicAppointmentId);
        }

        // Initialize remaining views
        RadioGroup paymentMethodGroup = findViewById(R.id.payment_method_group);
        payButton = findViewById(R.id.pay_button);

        // Validate if all required parameters are available; if not, disable the booking button.
        if (!areRequiredParametersPresent()) {
            payButton.setEnabled(false);
            payButton.setAlpha(0.5f); // Visual indication of disabled state.
            Toast.makeText(this, "Some booking details are missing. Please check your inputs.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Required booking parameters are missing.");
        }

        payButton.setOnClickListener(v -> {
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.payment_online) {
                selectedPaymentMethod = "Online";
                Log.d(TAG, "Selected payment method: Online");
                startUpiPayment();
            } else if (selectedId == R.id.payment_offline) {
                selectedPaymentMethod = "Offline";
                Log.d(TAG, "Selected payment method: Offline");
                // Before checking location settings, check if we have location permission.
                if (hasLocationPermissions()) {
                    checkLocationSettingsAndFetchLocation();
                } else {
                    requestLocationPermissions();
                }
            } else {
                Log.e(TAG, "No payment method selected.");
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Checks whether all required parameters are present.
     */
    private boolean areRequiredParametersPresent() {
        return (patientName != null && !patientName.trim().isEmpty() &&
                age != null && !age.trim().isEmpty() &&
                gender != null && !gender.trim().isEmpty() &&
                problem != null && !problem.trim().isEmpty() &&
                address != null && !address.trim().isEmpty() &&
                doctorId != null && !doctorId.trim().isEmpty() &&
                doctorName != null && !doctorName.trim().isEmpty() &&
                Status != null && !Status.trim().isEmpty());
    }

    private void startUpiPayment() {
        // Build secure UPI URI using merchant details
        String upiUri = "upi://pay?pa=" + MERCHANT_UPI_ID +
                "&pn=" + Uri.encode(MERCHANT_NAME) +
                "&tn=" + Uri.encode(TRANSACTION_NOTE) +
                "&am=" + AMOUNT +
                "&cu=" + CURRENCY;
        Log.d(TAG, "Initiating UPI Payment with URI: " + upiUri);
        Intent upiIntent = new Intent(Intent.ACTION_VIEW);
        upiIntent.setData(Uri.parse(upiUri));

        // Use an intent chooser to allow the user to select their UPI app securely
        Intent chooser = Intent.createChooser(upiIntent, "Pay via UPI");

        if (upiIntent.resolveActivity(getPackageManager()) != null) {
            try {
                startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
            } catch (Exception e) {
                Log.e(TAG, "Exception while starting UPI payment: ", e);
                Toast.makeText(this, "Error starting payment. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No UPI app found for handling payment.");
            Toast.makeText(this, "No UPI app found. Please install one to proceed.", Toast.LENGTH_SHORT).show();
        }
    }

    // Check if the location permissions are granted.
    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Request location permissions at runtime.
    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Location permissions granted.");
                checkLocationSettingsAndFetchLocation();
            } else {
                Log.e(TAG, "onRequestPermissionsResult: Location permissions denied.");
                Toast.makeText(this, "Location permission is required to fetch your location.", Toast.LENGTH_SHORT).show();
                // Optionally, save booking data without location.
                saveBookingData("");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = (data != null) ? data.getStringExtra("response") : null;
            Log.d(TAG, "UPI Payment Response: " + response);
            processUpiPaymentResponse(response);
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            Log.d(TAG, "onActivityResult: Handling location settings result.");
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: User enabled location settings.");
                fetchLocationAndSaveBookingData();
            } else {
                Log.e(TAG, "onActivityResult: User did not enable location settings. Result code: " + resultCode);
                Toast.makeText(this, "Location is required to proceed.", Toast.LENGTH_SHORT).show();
                saveBookingData("");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processUpiPaymentResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.e(TAG, "UPI Payment cancelled or failed: response is null or empty");
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Raw UPI response: " + response);
        String status = "";
        String approvalRefNo = "";
        String[] responseArray = response.split("&");
        for (String resp : responseArray) {
            String[] keyValue = resp.split("=");
            if (keyValue.length >= 2) {
                String key = keyValue[0].toLowerCase();
                String value = keyValue[1].toLowerCase();
                if ("status".equals(key)) {
                    status = value;
                } else if ("txnref".equals(key) || "txnrefno".equals(key) || "approvalrefno".equals(key)) {
                    approvalRefNo = value;
                }
            } else {
                Log.e(TAG, "Invalid key-value pair in UPI response: " + resp);
            }
        }
        Log.d(TAG, "Processed UPI Payment Status: " + status + ", ApprovalRefNo: " + approvalRefNo);
        if ("success".equals(status)) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            // After a successful UPI payment, check location settings
            if (hasLocationPermissions()) {
                checkLocationSettingsAndFetchLocation();
            } else {
                requestLocationPermissions();
            }
        } else {
            Log.e(TAG, "Payment failed or cancelled. Status: " + status);
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    // Check device location settings and prompt user if necessary.
    private void checkLocationSettingsAndFetchLocation() {
        Log.d(TAG, "checkLocationSettingsAndFetchLocation: Initiating location settings check.");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Log.d(TAG, "checkLocationSettingsAndFetchLocation: Built LocationSettingsRequest: " + builder.build().toString());

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.d(TAG, "checkLocationSettingsAndFetchLocation: Location settings are satisfied: " + locationSettingsResponse.toString());
                fetchLocationAndSaveBookingData();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "checkLocationSettingsAndFetchLocation: Failed to meet location settings requirements. Exception: " + e.getMessage());
                if (e instanceof ResolvableApiException) {
                    try {
                        Log.d(TAG, "checkLocationSettingsAndFetchLocation: Attempting to resolve location settings.");
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(pending_bill.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        Log.e(TAG, "checkLocationSettingsAndFetchLocation: Error during resolution: " + sendEx.getMessage());
                        Toast.makeText(pending_bill.this, "Error checking location settings.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "checkLocationSettingsAndFetchLocation: Non-resolvable location settings failure: " + e.getMessage());
                    Toast.makeText(pending_bill.this, "Location settings are inadequate and cannot be fixed here.", Toast.LENGTH_LONG).show();
                    saveBookingData("");
                }
            }
        });
    }

    // Fetch the user's current location and then save booking data with the Google Maps link.
    @SuppressLint("MissingPermission")
    private void fetchLocationAndSaveBookingData() {
        Log.d(TAG, "fetchLocationAndSaveBookingData: Attempting to fetch last known location.");
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d(TAG, "fetchLocationAndSaveBookingData: Location fetched successfully. Latitude: " + latitude + ", Longitude: " + longitude);
                String googleMapsLink = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
                Log.d(TAG, "fetchLocationAndSaveBookingData: Constructed Google Maps link: " + googleMapsLink);
                saveBookingData(googleMapsLink);
            } else {
                Log.e(TAG, "fetchLocationAndSaveBookingData: Location is null.");
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                saveBookingData("");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "fetchLocationAndSaveBookingData: Failed to fetch location: " + e.getMessage());
            Toast.makeText(this, "Failed to fetch location", Toast.LENGTH_SHORT).show();
            saveBookingData("");
        });
    }

    // Save booking data and include the Google Maps link as one of the parameters.
    private void saveBookingData(String googleMapsLink) {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php"; // Use HTTPS in production
        Log.d(TAG, "Saving booking data to: " + url);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Booking data saved successfully. Server response: " + response);
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                    onBookingSuccess();
                },
                error -> {
                    Log.e(TAG, "Error saving booking data: " + error.getMessage());
                    Toast.makeText(this, "Error saving appointment. Contact support.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                params.put("patient_name", patientName);
                params.put("age", age);
                params.put("gender", gender);
                params.put("address", address);
                params.put("doctor_id", doctorId);
                params.put("reason_for_visit", problem);
                String appointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                params.put("appointment_date", appointmentDate);
                params.put("time_slot", "10:00 AM");
                params.put("pincode", "112345");
                params.put("appointment_mode", "Online");
                params.put("payment_method", selectedPaymentMethod);
                params.put("status", Status);
                // Send the Google Maps link (or empty string if not available) to the database.
                params.put("location", googleMapsLink);
                Log.d(TAG, "Booking data params: " + params.toString());
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // Redirects to MainActivity with ongoing appointment fragment.
    public void onBookingSuccess() {
        Intent intent = new Intent(pending_bill.this, MainActivity.class);
        intent.putExtra("open_fragment", 2);
        startActivity(intent);
        finish();
    }
}
