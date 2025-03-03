package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.thedoctorathomeuser.Fragment.HistoryFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class track_doctor extends AppCompatActivity {
    private ViewPager vp;
    Button don , yourbill ;
    private MapView mapView;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_doctor);


        don = findViewById(R.id.button_done);
        yourbill = findViewById(R.id.button_bill);


        // Initialize the MapView
        mapView = findViewById(R.id.mapView);

        // Bundle for MapView initialization
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);

        // Set up the map when ready
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                // Set up a location for the doctor
                LatLng doctorLocation = new LatLng(-1.286389, 36.817223); // Replace with actual coordinates
                googleMap.addMarker(new MarkerOptions()
                        .position(doctorLocation)
                        .title("Dr. Tranquilli's Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(doctorLocation, 15));
            }
        });

        don.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(track_doctor.this, MainActivity.class);
                intent.putExtra("open_fragment", 2); // 2 = Ongoing Appointments
                startActivity(intent);
            }
        });

        yourbill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(track_doctor.this, complet_bill.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

        protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
