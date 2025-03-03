package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.Adapter.DiseaseAdapter;

public class diseases extends AppCompatActivity {

    Button confirm;

    // Array of diseases
    private String[] diseaseArray = {
            "Severe dizziness",
            "Sudden, Severe headache.",
            "Chest pain.",
            "Difficulty breathing.",
            "Numbness or paralysis of arms or legs.",
            "Fainting",
            "Double vision.",
            "Rapid or irregular heartbeat.",
            "Confusion or slurred speech.",
            "Headache, bodyache",
            "General weakness",
            "Nausea",
            "Vomiting",
            "Acidity",
            "Diarrhoea (Loose motion)",
            "Dysentery (Motion with pain in abdomen)",
            "Hypertension (high blood pressure)",
            "Hypotension (Low blood pressure)",
            "Hypoglycemia (Low sugar)",
            "Pain in abdomen",
            "Anaemia (Low Hb%)",
            "Fracture"
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diseases);

        confirm = findViewById(R.id.confirm_button);

        // Set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DiseaseAdapter(diseaseArray));


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(diseases.this,available_doctor.class);
                startActivity(intent);
            }
        });

    }
}
