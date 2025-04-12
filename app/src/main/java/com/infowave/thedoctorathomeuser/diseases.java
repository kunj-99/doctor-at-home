package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.infowave.thedoctorathomeuser.adapter.DiseaseAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class diseases extends AppCompatActivity {

    private static final String API_URL = "http://sxm.a58.mytemp.website/fetch_diseases.php?category_id=";
    private RecyclerView recyclerView;
    private DiseaseAdapter adapter;
    private List<String> diseaseList = new ArrayList<>();
    private Button confirm;
    private String categoryId, categoryName;
    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diseases);

        // Initialize UI components
        title = findViewById(R.id.title);  // Make sure title exists in XML
        confirm = findViewById(R.id.confirm_button);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        Log.d("DISEASES_ACTIVITY", "Received categoryId: " + categoryId);
        Log.d("DISEASES_ACTIVITY", "Received categoryName: " + categoryName);

        if (categoryName != null && !categoryName.isEmpty()) {
            title.setText(categoryName+ "  General Physician");
        } else {
            title.setText("Select a Category");  // More user-friendly default
            Log.w("DISEASES_ACTIVITY", "Warning: categoryName is missing!");
        }


        // Fetch diseases only if categoryId is valid
        if (categoryId != null) {
            fetchDiseases(categoryId);
        } else {
            Toast.makeText(this, "Error: Category ID not found!", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(diseases.this, "No diseases found!", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Error parsing JSON: " + e.getMessage());
                        Toast.makeText(diseases.this, "Error parsing data!", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Volley Error: " + error.toString());
                    Toast.makeText(diseases.this, "Failed to load diseases. Try again!", Toast.LENGTH_SHORT).show();
                });

        queue.add(jsonObjectRequest);
    }
}
