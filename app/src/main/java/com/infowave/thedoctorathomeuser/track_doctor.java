package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
// import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class track_doctor extends AppCompatActivity implements OnMapReadyCallback {

    // private static final String TAG = "TrackDoctor";
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String GET_LOCATION_URL = ApiConfig.endpoint("get_live_location.php");

    private static final String API_KEY = "AIzaSyCkUxQSJ1jNt0q_CcugieFl5vezsNAUxe0";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker doctorMarker;
    private Marker userMarker;
    private Polyline currentPolyline;
    private final Handler handler = new Handler();
    private Runnable updateRunnable;

    private String doctorId;
    private String appointmentId;

    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation;

    private TextView tvDistance, tvDuration;
    private RequestQueue requestQueue;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_doctor);

        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);

        doctorId = String.valueOf(getIntent().getIntExtra("doctor_id", -1));
        appointmentId = String.valueOf(getIntent().getIntExtra("appointment_id", -1));

        if (doctorId.equals("-1") || appointmentId.equals("-1")) {
            Toast.makeText(this, "Could not load appointment. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Log.d(TAG, "Doctor ID: " + doctorId + ", Appointment ID: " + appointmentId);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocation();
        }

        mapView = findViewById(R.id.mapView);
        Bundle mapBundle = (savedInstanceState != null) ? savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapBundle);
        mapView.getMapAsync(this);

        requestQueue = Volley.newRequestQueue(this);

        Button buttonBill = findViewById(R.id.button_bill);
        Button buttonDone = findViewById(R.id.button_done);

        buttonBill.setOnClickListener(v -> {
            Intent intent = new Intent(track_doctor.this, complet_bill.class);
            intent.putExtra("appointment_id", Integer.parseInt(appointmentId));
            startActivity(intent);
        });

        buttonDone.setOnClickListener(v -> {
            Intent intent = new Intent(track_doctor.this, MainActivity.class);
            intent.putExtra("open_fragment", 2);
            startActivity(intent);
            finish();
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLiveLocation();
                fetchUserLocation();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(updateRunnable);
    }

    private void fetchLiveLocation() {
        String url = ApiConfig.endpoint("get_live_location.php", "doctor_id", doctorId) + "&appointment_id=" + appointmentId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("live_latitude") && response.has("live_longitude")) {
                            double lat = response.getDouble("live_latitude");
                            double lon = response.getDouble("live_longitude");
                            updateDoctorMarker(lat, lon);
                        } else {
                            // Log.e(TAG, "Latitude or Longitude not found in response");
                            Toast.makeText(track_doctor.this, "Doctor location not yet available.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        // Log.e(TAG, "Error parsing doctor location data", e);
                        Toast.makeText(track_doctor.this, "Could not update doctor location.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Log.e(TAG, "Fetch doctor location error", error);
                    Toast.makeText(track_doctor.this, "Unable to get doctor location. Please check your internet.", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    private void updateDoctorMarker(double lat, double lon) {
        if (googleMap == null) return;
        LatLng doctorPosition = new LatLng(lat, lon);
        if (doctorMarker == null) {
            doctorMarker = googleMap.addMarker(new MarkerOptions()
                    .position(doctorPosition)
                    .title("Doctor Location")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.doctor)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(doctorPosition, 15));
        } else {
            doctorMarker.setPosition(doctorPosition);
        }
        if (userLocation != null) {
            calculateDistanceAndDuration(doctorPosition, userLocation);
        }
    }

    private void fetchUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                // Log.d(TAG, "User location: " + userLocation);
                if (googleMap != null) {
                    if (userMarker == null) {
                        userMarker = googleMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("Your Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    } else {
                        userMarker.setPosition(userLocation);
                    }
                }
                if (doctorMarker != null) {
                    calculateDistanceAndDuration(doctorMarker.getPosition(), userLocation);
                }
            } else {
                // Log.e(TAG, "Unable to fetch user location");
                Toast.makeText(this, "Unable to access your location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateDistanceAndDuration(LatLng origin, LatLng destination) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + destination.latitude + "," + destination.longitude;
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&key=" + API_KEY;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        @SuppressLint("SetTextI18n") JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject firstRoute = routes.getJSONObject(0);
                            JSONArray legs = firstRoute.getJSONArray("legs");
                            if (legs.length() > 0) {
                                JSONObject firstLeg = legs.getJSONObject(0);
                                String distanceText = firstLeg.getJSONObject("distance").getString("text");
                                String durationText = firstLeg.getJSONObject("duration").getString("text");
                                tvDistance.setText("Distance: " + distanceText);
                                tvDuration.setText("Duration: " + durationText);
                            }
                            JSONObject overviewPolyline = firstRoute.getJSONObject("overview_polyline");
                            String polylinePoints = overviewPolyline.getString("points");
                            List<LatLng> polylineList = decodePoly(polylinePoints);

                            if (currentPolyline != null) {
                                currentPolyline.remove();
                            }
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(polylineList)
                                    .width(10)
                                    .color(Color.BLUE);
                            currentPolyline = googleMap.addPolyline(polylineOptions);
                        }
                    } catch (Exception e) {
                        // Log.e(TAG, "Error parsing directions response", e);
                        Toast.makeText(this, "Unable to show route.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Log.e(TAG, "Directions API error", error);
                    Toast.makeText(this, "Could not connect to directions service.", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (googleMap != null && updateRunnable != null) {
            handler.post(updateRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapBundle == null) {
            mapBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapBundle);
        }
        mapView.onSaveInstanceState(mapBundle);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required for tracking.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
