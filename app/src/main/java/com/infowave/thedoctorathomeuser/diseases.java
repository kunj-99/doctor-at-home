package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
//import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class diseases extends AppCompatActivity {

    private static final String API_URL = "https://thedoctorathome.in/fetch_diseases.php?category_id=";
    private RecyclerView recyclerView;
    private DiseaseAdapter adapter;
    private List<String> diseaseList = new ArrayList<>();
    private Button confirm;
    private String categoryId, categoryName;
    private TextView title;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diseases);

        // Initialize UI components
        title = findViewById(R.id.title);
        confirm = findViewById(R.id.confirm_button);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        if (categoryName != null && !categoryName.isEmpty()) {
            title.setText(categoryName + "  General Physician");
        } else {
            title.setText("Select a Category");
        }

        // Fetch diseases only if categoryId is valid
        if (categoryId != null) {
            fetchDiseases(categoryId);
        } else {
            Toast.makeText(this, "Could not find the selected category. Please try again.", Toast.LENGTH_SHORT).show();
        }

        // Confirm button click listener
        confirm.setOnClickListener(v -> {
            Intent intent = new Intent(diseases.this, available_doctor.class);
            intent.putExtra("category_id", categoryId);
            intent.putExtra("category_name", categoryName);
            startActivity(intent);
        });
    }

    private void fetchDiseases(String categoryId) {
        String url = API_URL + categoryId;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        diseaseList.clear();

                        if (response.getBoolean("success")) {
                            JSONArray diseasesArray = response.getJSONArray("diseases");

                            for (int i = 0; i < diseasesArray.length(); i++) {
                                JSONObject diseaseObj = diseasesArray.getJSONObject(i);
                                diseaseList.add(diseaseObj.getString("disease_name"));
                            }

                            adapter = new DiseaseAdapter(diseaseList);
                            recyclerView.setAdapter(adapter);

                        } else {
                            Toast.makeText(diseases.this, "No diseases found in this category.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(diseases.this, "Sorry, we could not load the diseases right now.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(diseases.this, "Unable to connect. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                });

        queue.add(jsonObjectRequest);
    }
}
