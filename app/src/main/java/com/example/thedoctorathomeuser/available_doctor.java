package com.example.thedoctorathomeuser;

import static java.security.AccessController.getContext;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.Adapter.DoctorAdapter;

public class available_doctor extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Fixed method name

        String[] names = {"Dr. Tranquilli", "Dr. Smith", "Dr. Patel"};
        String[] specialties = {"Specialist Medicine", "Dermatologist", "Cardiologist"};
        String[] hospitals = {"Patel Hospital", "City Hospital", "Metro Hospital"};
        float[] ratings = {4.0f, 4.5f, 4.2f};
        int[] imageResIds = {R.drawable.main1, R.drawable.main1, R.drawable.main3};

        DoctorAdapter adapter = new DoctorAdapter(this,names, specialties,hospitals, ratings, imageResIds); // Adjusted adapter constructor
        recyclerView.setAdapter(adapter);
    }
}
