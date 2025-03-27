package com.example.thedoctorathomeuser;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONObject;

public class track_doctor extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String GET_LOCATION_URL = "http://sxm.a58.mytemp.website/get_live_location.php";

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker doctorMarker;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private String doctorId = "11"; // Example doctor ID
    private String appointmentId = "35"; // Example appointment ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_doctor);

        mapView = findViewById(R.id.mapView);
        Bundle mapBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapBundle);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLiveLocation();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(updateRunnable);
    }

    private void fetchLiveLocation() {
        String url = GET_LOCATION_URL + "?doctor_id=" + doctorId + "&appointment_id=" + appointmentId;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("live_latitude") && response.has("live_longitude")) {
                            double lat = response.getDouble("live_latitude");
                            double lon = response.getDouble("live_longitude");
                            updateMarker(lat, lon);
                        } else {
                            Log.e(TAG, "Latitude or Longitude not found in response");
                            Toast.makeText(track_doctor.this, "Location data not available", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing location data", e);
                        Toast.makeText(track_doctor.this, "Error parsing location data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Fetch location error", error);
                    Toast.makeText(track_doctor.this, "Error fetching location data", Toast.LENGTH_SHORT).show();
                });
        queue.add(request);
    }

    private void updateMarker(double lat, double lon) {
        if (googleMap == null) return;
        LatLng position = new LatLng(lat, lon);
        if (doctorMarker == null) {
            doctorMarker = googleMap.addMarker(new MarkerOptions().position(position).title("Doctor Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        } else {
            doctorMarker.setPosition(position);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (googleMap != null && updateRunnable != null) handler.post(updateRunnable);
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
}
