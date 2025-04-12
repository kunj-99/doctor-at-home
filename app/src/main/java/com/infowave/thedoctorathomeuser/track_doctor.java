package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.R;
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

    private static final String TAG = "TrackDoctor";
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String GET_LOCATION_URL = "http://sxm.a58.mytemp.website/get_live_location.php";
    // Replace with your actual Google Directions API key.
    private static final String API_KEY = "AIzaSyCkUxQSJ1jNt0q_CcugieFl5vezsNAUxe0";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker doctorMarker;
    private Marker userMarker;  // New marker for the user's location
    private Polyline currentPolyline;
    private Handler handler = new Handler();
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

        // Initialize TextViews.
        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);

        // Get doctor_id and appointment_id from Intent extras.
        doctorId = String.valueOf(getIntent().getIntExtra("doctor_id", -1));
        appointmentId = String.valueOf(getIntent().getIntExtra("appointment_id", -1));

        if (doctorId.equals("-1") || appointmentId.equals("-1")) {
            Toast.makeText(this, "Invalid Doctor or Appointment ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Doctor ID: " + doctorId + ", Appointment ID: " + appointmentId);

        // Initialize FusedLocationProviderClient for user location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Check for location permission and fetch user location if granted.
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
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // Optionally enable the My Location layer (if permission is granted).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        startLocationUpdates();
    }

    // Start periodic updates for doctor's live location (and refresh the user's location).
    private void startLocationUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLiveLocation();
                fetchUserLocation(); // Refresh user location as well.
                handler.postDelayed(this, 5000); // Update every 5 seconds.
            }
        };
        handler.post(updateRunnable);
    }

    // Fetch the doctor's live location from the server.
    private void fetchLiveLocation() {
        String url = GET_LOCATION_URL + "?doctor_id=" + doctorId + "&appointment_id=" + appointmentId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("live_latitude") && response.has("live_longitude")) {
                            double lat = response.getDouble("live_latitude");
                            double lon = response.getDouble("live_longitude");
                            updateDoctorMarker(lat, lon);
                        } else {
                            Log.e(TAG, "Latitude or Longitude not found in response");
                            Toast.makeText(track_doctor.this, "Doctor location data not available", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing doctor location data", e);
                        Toast.makeText(track_doctor.this, "Error parsing doctor location data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Fetch doctor location error", error);
                    Toast.makeText(track_doctor.this, "Error fetching doctor location data", Toast.LENGTH_SHORT).show();
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
                    // Set custom drawable icon for the doctor.
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.doctor)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(doctorPosition, 15));
        } else {
            doctorMarker.setPosition(doctorPosition);
        }
        // If we have the user's location, compute the route.
        if (userLocation != null) {
            calculateDistanceAndDuration(doctorPosition, userLocation);
        }
    }

    // Fetch the user's current location and update/add a red marker.
    private void fetchUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "User location: " + userLocation);
                // Update or create the user marker as a red marker.
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
                // If doctor's marker is available, recalc route (origin: doctor, destination: user).
                if (doctorMarker != null) {
                    calculateDistanceAndDuration(doctorMarker.getPosition(), userLocation);
                }
            } else {
                Log.e(TAG, "Unable to fetch user location");
            }
        });
    }

    // Use the Google Directions API to calculate driving distance, duration, and draw the route.
    // Here, the origin is the doctor's location and the destination is the user's location.
    private void calculateDistanceAndDuration(LatLng origin, LatLng destination) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + destination.latitude + "," + destination.longitude;
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&key=" + API_KEY;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject firstRoute = routes.getJSONObject(0);
                            // Update distance and duration from the first leg.
                            JSONArray legs = firstRoute.getJSONArray("legs");
                            if (legs.length() > 0) {
                                JSONObject firstLeg = legs.getJSONObject(0);
                                String distanceText = firstLeg.getJSONObject("distance").getString("text");
                                String durationText = firstLeg.getJSONObject("duration").getString("text");
                                tvDistance.setText("Distance: " + distanceText);
                                tvDuration.setText("Duration: " + durationText);
                            }
                            // Draw the route line using the overview_polyline.
                            JSONObject overviewPolyline = firstRoute.getJSONObject("overview_polyline");
                            String polylinePoints = overviewPolyline.getString("points");
                            List<LatLng> polylineList = decodePoly(polylinePoints);

                            // Remove previous polyline if it exists.
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
                        Log.e(TAG, "Error parsing directions response", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Directions API error", error);
                });
        requestQueue.add(request);
    }

    // Decodes an encoded polyline string into a list of LatLngs.
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

    // Handle runtime permission result for ACCESS_FINE_LOCATION.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
