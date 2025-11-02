package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
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

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String API_KEY = "AIzaSyCkUxQSJ1jNt0q_CcugieFl5vezsNAUxe0";
    private static final String DEFAULT_DOCTOR_IMAGE_URL =
            "https://thedoctorathome.in/doctor_images/default.png";

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker doctorMarker;
    private Marker userMarker;
    private Polyline currentPolyline;

    private final Handler handler = new Handler();
    private Runnable updateRunnable;

    // Read as String to avoid ClassCastException (you passed String extras)
    private String doctorId = "";
    private String appointmentId = "";

    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation;

    private TextView tvDistance, tvDuration, tvDoctorName;
    private ImageView ivDoctorPhoto;
    private RequestQueue requestQueue;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_doctor);

        // Perfect black system bars with scrims
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, android.R.color.black));
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);
        final View root = findViewById(R.id.root_container);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                ViewGroup.LayoutParams lpTop = statusScrim.getLayoutParams();
                lpTop.height = sys.top;
                statusScrim.setLayoutParams(lpTop);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ViewGroup.LayoutParams lpBot = navScrim.getLayoutParams();
                lpBot.height = sys.bottom;
                navScrim.setLayoutParams(lpBot);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });

        tvDistance    = findViewById(R.id.tvDistance);
        tvDuration    = findViewById(R.id.tvDuration);
        tvDoctorName  = findViewById(R.id.doctor_name);   // ensure exists in XML
        ivDoctorPhoto = findViewById(R.id.civ_profile);  // ensure exists in XML

        // Read extras as String (fixes ClassCastException from getIntExtra)
        Intent it = getIntent();
        if (it != null) {
            doctorId = nvl(it.getStringExtra("doctor_id"));
            appointmentId = nvl(it.getStringExtra("appointment_id"));
        }
        if (doctorId.isEmpty() || appointmentId.isEmpty()) {
            Toast.makeText(this, "Could not load appointment. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            fetchUserLocation();
        }

        mapView = findViewById(R.id.mapView);
        if (mapView != null) {
            Bundle mapBundle = (savedInstanceState != null)
                    ? savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY) : null;
            mapView.onCreate(mapBundle);
            mapView.getMapAsync(this);
        }

        requestQueue = Volley.newRequestQueue(this);

        Button buttonBill = findViewById(R.id.button_bill);
        Button buttonDone = findViewById(R.id.button_done);

        buttonBill.setOnClickListener(v -> {
            int apptIdInt = parseIntSafe(appointmentId, -1);
            if (apptIdInt <= 0) {
                Toast.makeText(this, "Invalid appointment ID.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(track_doctor.this, complet_bill.class);
            intent.putExtra("appointment_id", apptIdInt);
            startActivity(intent);
        });

        buttonDone.setOnClickListener(v -> {
            Intent intent = new Intent(track_doctor.this, MainActivity.class);
            intent.putExtra("open_fragment", 2);
            startActivity(intent);
            finish();
        });

        // Fetch doctor name + photo dynamically using your existing PHP
        fetchDoctorBriefUsingExistingApi();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try { googleMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
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

    /** Use existing fetch_doctor.php */
    private void fetchDoctorBriefUsingExistingApi() {
        String url = ApiConfig.endpoint("fetch_doctor.php", "doctor_id", doctorId);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                resp -> {
                    try {
                        if (!resp.optBoolean("success", false)) return;
                        JSONObject data = resp.optJSONObject("data");
                        if (data == null) return;

                        String name = data.optString("full_name", "");
                        String pic  = cleanUrlOrDefault(data.optString("profile_picture", ""));

                        if (tvDoctorName != null) tvDoctorName.setText(name);

                        if (ivDoctorPhoto != null) {
                            Glide.with(getApplicationContext())
                                    .load(pic)
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                    .signature(new ObjectKey(pic))
                                    .thumbnail(0.25f)
                                    .circleCrop()
                                    .dontAnimate()
                                    .placeholder(R.drawable.ic_doctor_placeholder)
                                    .error(R.drawable.ic_doctor_placeholder)
                                    .into(ivDoctorPhoto);
                        }
                    } catch (Exception ignored) { }
                },
                error -> { /* silent */ }
        );
        requestQueue.add(req);
    }

    private static String cleanUrlOrDefault(String raw) {
        if (raw == null) return DEFAULT_DOCTOR_IMAGE_URL;
        String u = raw.trim();
        if ((u.startsWith("\"") && u.endsWith("\"")) || (u.startsWith("'") && u.endsWith("'"))) {
            u = u.substring(1, u.length() - 1).trim();
        }
        if (u.isEmpty() || "null".equalsIgnoreCase(u)) return DEFAULT_DOCTOR_IMAGE_URL;
        if (u.startsWith("http://") || u.startsWith("https://")) {
            int secondHttps = u.indexOf("https://", 8);
            int secondHttp  = u.indexOf("http://", 7);
            int idx = -1;
            if (secondHttps >= 0) idx = secondHttps;
            else if (secondHttp >= 0) idx = secondHttp;
            if (idx > 0) return u.substring(idx); // fix double-prefix
            return u;
        }
        // If older rows store relative paths and you want to force absolute:
        // return "https://thedoctorathome.in/doctor_images/" + u;
        return DEFAULT_DOCTOR_IMAGE_URL;
    }

    private void fetchLiveLocation() {
        String url = ApiConfig.endpoint("get_live_location.php", "doctor_id", doctorId)
                + "&appointment_id=" + appointmentId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.has("live_latitude") && response.has("live_longitude")) {
                            double lat = response.getDouble("live_latitude");
                            double lon = response.getDouble("live_longitude");
                            updateDoctorMarker(lat, lon);
                        }
                    } catch (Exception ignored) { }
                },
                error -> { /* silent */ }
        );
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
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(doctorPosition, 15f));
        } else {
            doctorMarker.setPosition(doctorPosition);
        }
        if (userLocation != null) {
            calculateDistanceAndDuration(doctorPosition, userLocation);
        }
    }

    private void fetchUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
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
            }
        });
    }

    private void calculateDistanceAndDuration(LatLng origin, LatLng destination) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest   = "destination=" + destination.latitude + "," + destination.longitude;
        String mode       = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&key=" + API_KEY;
        String url        = "https://maps.googleapis.com/maps/api/directions/json?" + parameters;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
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

                            if (currentPolyline != null) currentPolyline.remove();
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(polylineList)
                                    .width(10f)
                                    .color(Color.BLUE);
                            if (googleMap != null) {
                                currentPolyline = googleMap.addPolyline(polylineOptions);
                            }
                        }
                    } catch (Exception ignored) { }
                },
                error -> { /* silent */ }
        );
        requestQueue.add(request);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; }
            while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0; result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; }
            while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(((double) lat / 1E5), ((double) lng / 1E5)));
        }
        return poly;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (updateRunnable != null) handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
            if (mapBundle == null) {
                mapBundle = new Bundle();
                outState.putBundle(MAPVIEW_BUNDLE_KEY, mapBundle);
            }
            mapView.onSaveInstanceState(mapBundle);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation();
                if (googleMap != null) {
                    try { googleMap.setMyLocationEnabled(true); } catch (SecurityException ignored) { }
                }
            } else {
                Toast.makeText(this, "Location permission is required for tracking.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static String nvl(String s) { return s == null ? "" : s.trim(); }

    private static int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(nvl(s)); } catch (Exception e) { return fallback; }
    }
}
