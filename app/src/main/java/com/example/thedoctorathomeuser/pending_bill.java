package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class pending_bill extends AppCompatActivity {

    private static final String TAG = "PendingBill";

    // UPI Payment
    private static final int UPI_PAYMENT_REQUEST_CODE = 123;
    private static final String MERCHANT_UPI_ID   = "mohitvatiya3333@oksbi";
    private static final String MERCHANT_NAME     = "the doctor at home";
    private static final String TRANSACTION_NOTE  = "Payment for appointment";
    private static final String CURRENCY          = "INR";

    // Base Payment + Fees
    private final double baseCost      = 1.0;     // Payment Amount
    private final double consultingFee = 0.0;     // Consulting Fee
    private final double gst           = 0.0;     // GST
    private double distanceCharge      = 0.0;
    private double distanceKm          = 0.0;     // Driving distance in km
    private double finalCost           = 0.0;

    // If the route is > 3 km, each extra km is 7 Rs
    private static final double BASE_DISTANCE      = 3.0;
    private static final double EXTRA_COST_PER_KM  = 7.0;

    // Google Routes API Key (use your valid API key here)
    private static final String GOOGLE_API_KEY = "AIzaSyCg79KiSpncjitTJDiOAmpc5SjjTHtcc24";

    // Booking Data
    private String patientName, age, gender, problem, address, doctorId, doctorName, Status, selectedPaymentMethod;
    private String patientId;
    private String pincode;

    // UI references
    private Button payButton;
    private TextView tvBillDate, tvBillTime, tvBillPatientName, tvBillDoctorName;

    // Payment details in layout
    private TextView tvPaymentAmountValue;
    private TextView tvConsultingFeeValue;
    private TextView tvDistanceKmValue;
    private TextView tvDistanceChargeValue;
    private TextView tvGstValue;
    private TextView tvTotalPaidValue;

    // Coordinates
    private double userLat;
    private double userLng;
    private double docLat;  // from doctor's location
    private double docLng;  // from doctor's location

    private String googleMapsLink = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Gather data from Intent
        Intent intent = getIntent();
        patientName  = intent.getStringExtra("patient_name");
        age          = String.valueOf(intent.getIntExtra("age", 0));
        gender       = intent.getStringExtra("gender");
        problem      = intent.getStringExtra("problem");
        address      = intent.getStringExtra("address");
        doctorId     = intent.getStringExtra("doctor_id");
        doctorName   = intent.getStringExtra("doctorName");
        Status       = intent.getStringExtra("appointment_status");
        pincode      = intent.getStringExtra("pincode");

        if ("Request for visit".equals(Status)) {
            Status = "Requested";
        } else if ("Book Appointment".equals(Status)) {
            Status = "Confirmed";
        }
        Log.d(TAG, "Booking details => " +
                "patientName=" + patientName + ", age=" + age +
                ", gender=" + gender + ", problem=" + problem +
                ", address=" + address + ", doctorId=" + doctorId +
                ", Status=" + Status);

        userLat = intent.getDoubleExtra("latitude", 0.0);
        userLng = intent.getDoubleExtra("longitude", 0.0);
        if (userLat != 0.0 && userLng != 0.0) {
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=" + userLat + "," + userLng;
        }
        Log.d(TAG, "User lat,lng => " + userLat + "," + userLng);

        // Patient ID from SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link Bill Info textviews
        tvBillPatientName = findViewById(R.id.tv_bill_patient_name);
        tvBillDoctorName  = findViewById(R.id.tv_bill_doctor_name);
        tvBillDate        = findViewById(R.id.tv_bill_date);
        tvBillTime        = findViewById(R.id.tv_bill_time);

        if (patientName != null) tvBillPatientName.setText(patientName);
        if (doctorName != null)  tvBillDoctorName.setText(doctorName);

        // Date/Time
        String curDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(new Date());
        tvBillDate.setText(curDate);

        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvBillTime.setText(curTime);

        // Payment detail references
        tvPaymentAmountValue   = findViewById(R.id.tv_payment_amount_value);
        tvConsultingFeeValue   = findViewById(R.id.tv_consulting_fee_value);
        tvDistanceKmValue      = findViewById(R.id.tv_distance_km_value);
        tvDistanceChargeValue  = findViewById(R.id.tv_distance_charge_value);
        tvGstValue             = findViewById(R.id.tv_gst_value);
        tvTotalPaidValue       = findViewById(R.id.tv_total_paid_value);

        // initial finalCost calculation
        distanceCharge = 0.0;
        distanceKm     = 0.0;
        finalCost      = baseCost + consultingFee + gst + distanceCharge;
        updatePaymentUI();

        RadioGroup paymentMethodGroup = findViewById(R.id.payment_method_group);
        payButton = findViewById(R.id.pay_button);

        if (!areRequiredParametersPresent()) {
            payButton.setEnabled(false);
            payButton.setAlpha(0.5f);
            Toast.makeText(this, "Some booking details are missing.", Toast.LENGTH_LONG).show();
        }

        // Step: fetch doctor location, parse lat/lng, then fetch driving distance via the Routes API
        fetchDoctorLocation(doctorId);

        payButton.setOnClickListener(v -> {
            int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.payment_online) {
                selectedPaymentMethod = "Online";
                startUpiPayment();
            } else if (selectedId == R.id.payment_offline) {
                selectedPaymentMethod = "Offline";
                saveBookingData(googleMapsLink);
            } else {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean areRequiredParametersPresent() {
        return (patientName != null && !patientName.trim().isEmpty() &&
                age        != null && !age.trim().isEmpty() &&
                gender     != null && !gender.trim().isEmpty() &&
                problem    != null && !problem.trim().isEmpty() &&
                address    != null && !address.trim().isEmpty() &&
                doctorId   != null && !doctorId.trim().isEmpty() &&
                doctorName != null && !doctorName.trim().isEmpty() &&
                Status     != null && !Status.trim().isEmpty());
    }

    private void fetchDoctorLocation(String docId) {
        // Replace with your actual endpoint
        String fetchUrl = "http://sxm.a58.mytemp.website/get_doctor_location.php?doctor_id=" + docId;
        Log.d(TAG, "Fetching doc location => " + fetchUrl);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                fetchUrl,
                null,
                response -> {
                    Log.d(TAG, "Doc location => " + response);
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            String docLocationUrl = response.getString("location");
                            parseDoctorLatLng(docLocationUrl);
                        } else {
                            Log.e(TAG, "Doc location not success => " + response);
                            updatePaymentUI();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse => " + e.getMessage());
                        updatePaymentUI();
                    }
                },
                error -> {
                    Log.e(TAG, "fetchDoctorLocation error => " + error.getMessage());
                    updatePaymentUI();
                }
        );
        queue.add(request);
    }

    private void parseDoctorLatLng(String docUrl) {
        Log.d(TAG, "Parsing doc lat,lng => " + docUrl);
        try {
            Uri uri = Uri.parse(docUrl);
            String qParam = uri.getQueryParameter("query");
            if (qParam != null && qParam.contains(",")) {
                String[] parts = qParam.split(",");
                docLat = Double.parseDouble(parts[0]);
                docLng = Double.parseDouble(parts[1]);
                Log.d(TAG, "Doc lat,lng => " + docLat + "," + docLng);

                // Step 2) Fetch driving distance via the new Routes API
                fetchDrivingDistance(userLat, userLng, docLat, docLng);
            } else {
                Log.e(TAG, "No valid lat,lng => " + qParam);
                updatePaymentUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "parseDoctorLatLng => " + e.getMessage());
            updatePaymentUI();
        }
    }

    private void fetchDrivingDistance(double lat1, double lng1, double lat2, double lng2) {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes?key=" + GOOGLE_API_KEY;
        JSONObject requestBody = new JSONObject();
        try {
            // Build origin object
            JSONObject originObj = new JSONObject();
            JSONObject originLocation = new JSONObject();
            JSONObject originLatLng = new JSONObject();
            originLatLng.put("latitude", lat1);
            originLatLng.put("longitude", lng1);
            originLocation.put("latLng", originLatLng);
            originObj.put("location", originLocation);
            requestBody.put("origin", originObj);

            // Build destination object
            JSONObject destinationObj = new JSONObject();
            JSONObject destinationLocation = new JSONObject();
            JSONObject destinationLatLng = new JSONObject();
            destinationLatLng.put("latitude", lat2);
            destinationLatLng.put("longitude", lng2);
            destinationLocation.put("latLng", destinationLatLng);
            destinationObj.put("location", destinationLocation);
            requestBody.put("destination", destinationObj);

            // Set travel and routing parameters
            requestBody.put("travelMode", "DRIVE");
            requestBody.put("routingPreference", "TRAFFIC_AWARE");
            requestBody.put("computeAlternativeRoutes", false);
            requestBody.put("languageCode", "en-US");
            requestBody.put("units", "METRIC");
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON request: " + e.getMessage());
            updatePaymentUI();
            return;
        }

        Log.d(TAG, "fetchDrivingDistance (Routes API) => " + url + " request: " + requestBody.toString());

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest routeReq = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    Log.d(TAG, "Routes API response => " + response);
                    parseRoutesResponse(response);
                },
                error -> {
                    String errorMessage = error.getMessage();
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMessage = new String(error.networkResponse.data);
                    }
                    Log.e(TAG, "Routes API error => " + errorMessage);
                    updatePaymentUI();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                // Required header: specify which fields you need in the response.
                headers.put("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline");
                return headers;
            }
        };
        queue.add(routeReq);
    }


    private void parseRoutesResponse(JSONObject response) {
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject firstRoute = routes.getJSONObject(0);
                // Get the distance directly from the route object instead of looking for legs.
                long distanceMeters = firstRoute.getLong("distanceMeters");
                distanceKm = distanceMeters / 1000.0; // Convert meters to km
                Log.d(TAG, "Driving distance (Routes API) => " + distanceKm + " km");

                // Calculate distance charge based on extra km over BASE_DISTANCE
                if (distanceKm > BASE_DISTANCE) {
                    double extraDist = distanceKm - BASE_DISTANCE;
                    distanceCharge = extraDist * EXTRA_COST_PER_KM;
                } else {
                    distanceCharge = 0.0;
                }
                finalCost = baseCost + consultingFee + gst + distanceCharge;
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseRoutesResponse error => " + e.getMessage());
        }
        // Always update the UI once distance is determined
        updatePaymentUI();
    }


    private void updatePaymentUI() {
        Log.d(TAG, "updateUI => distance=" + distanceKm + " km, distCharge=" + distanceCharge + ", finalCost=" + finalCost);

        // Payment Amount => baseCost
        String baseStr = "₹ " + String.format(Locale.getDefault(), "%.0f", baseCost);
        tvPaymentAmountValue.setText(baseStr);

        // Consulting Fee
        String consultStr = "₹ " + String.format(Locale.getDefault(), "%.0f", consultingFee);
        tvConsultingFeeValue.setText(consultStr);

        // Distance in km
        String distStr = String.format(Locale.getDefault(), "%.1f km", distanceKm);
        tvDistanceKmValue.setText(distStr);

        // Distance charge
        String distChargeStr = "₹ " + String.format(Locale.getDefault(), "%.0f", distanceCharge);
        tvDistanceChargeValue.setText(distChargeStr);

        // GST
        String gstStr = "₹ " + String.format(Locale.getDefault(), "%.0f", gst);
        tvGstValue.setText(gstStr);

        // Final cost
        String finalStr = "₹ " + String.format(Locale.getDefault(), "%.0f", finalCost);
        tvTotalPaidValue.setText(finalStr);
    }

    // Start UPI Payment
    private void startUpiPayment() {
        String amtStr = String.format(Locale.getDefault(), "%.2f", finalCost);
        String upiUri = "upi://pay?pa=" + MERCHANT_UPI_ID +
                "&pn=" + Uri.encode(MERCHANT_NAME) +
                "&tn=" + Uri.encode(TRANSACTION_NOTE) +
                "&am=" + amtStr +
                "&cu=" + CURRENCY;

        Log.d(TAG, "UPI Payment => " + upiUri);
        Intent upiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(upiUri));
        Intent chooser = Intent.createChooser(upiIntent, "Pay via UPI");
        if (upiIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No UPI app found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, @Nullable Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = (data != null) ? data.getStringExtra("response") : null;
            processUpiPaymentResponse(response);
        }
    }

    private void processUpiPaymentResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "UPI Payment => " + response);

        String status = "";
        String approvalRefNo = "";
        String[] arr = response.split("&");
        for (String part : arr) {
            String[] kv = part.split("=");
            if (kv.length >= 2) {
                String key = kv[0].toLowerCase();
                String val = kv[1].toLowerCase();
                if ("status".equals(key)) {
                    status = val;
                } else if ("txnref".equals(key) || "txnrefno".equals(key) || "approvalrefno".equals(key)) {
                    approvalRefNo = val;
                }
            }
        }
        Log.d(TAG, "UPI Payment status => " + status + ", ref => " + approvalRefNo);

        if ("success".equals(status)) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            saveBookingData(googleMapsLink);
        } else {
            Toast.makeText(this, "Payment failed or cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBookingData(String googleMapsLink) {
        String url = "http://sxm.a58.mytemp.website/save_appointment.php";
        Log.d(TAG, "saveBookingData => " + url);

        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {
                    Log.d(TAG, "Booking saved => " + resp);
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show();
                    onBookingSuccess();
                },
                err -> {
                    Log.e(TAG, "Error => " + err.getMessage());
                    Toast.makeText(this, "Error saving appointment!", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("patient_name", patientName);
                p.put("age", age);
                p.put("gender", gender);
                p.put("address", address);
                p.put("doctor_id", doctorId);
                p.put("reason_for_visit", problem);

                String apptDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                p.put("appointment_date", apptDate);
                p.put("time_slot", "10:00 AM");
                p.put("pincode", pincode);
                p.put("appointment_mode", "Online");
                p.put("payment_method", selectedPaymentMethod);
                p.put("status", Status);

                p.put("location", googleMapsLink);

                String costStr = String.format(Locale.getDefault(), "%.2f", finalCost);
                p.put("final_cost", costStr);

                return p;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/x-www-form-urlencoded");
                return h;
            }
        };

        RequestQueue q = Volley.newRequestQueue(this);
        q.add(req);
    }

    private void onBookingSuccess() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_fragment", 2);
        startActivity(i);
        finish();
    }
}
