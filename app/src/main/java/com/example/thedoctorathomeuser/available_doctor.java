package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.Adapter.DoctorAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class available_doctor extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DoctorAdapter adapter;
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> specialties = new ArrayList<>();
    private ArrayList<String> hospitals = new ArrayList<>();
    private ArrayList<Float> ratings = new ArrayList<>();
    private ArrayList<String> imageUrls = new ArrayList<>(); // URLs instead of drawable IDs

    private String categoryId, categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        Log.d("AvailableDoctor", "Received categoryId: " + categoryId);
        Log.d("AvailableDoctor", "Received categoryName: " + categoryName);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchDoctors(categoryId);
    }

    private void fetchDoctors(String categoryId) {
        String url = "http://sxm.a58.mytemp.website/getDoctorsByCategory.php?category_id=" + categoryId; // Replace with your API URL

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject doctor = response.getJSONObject(i);
                                names.add(doctor.getString("full_name"));
                                specialties.add(doctor.getString("specialization"));
                                hospitals.add(doctor.getString("hospital_affiliation"));
                                ratings.add((float) doctor.getDouble("rating"));
                                imageUrls.add(doctor.getString("profile_picture")); // URL instead of drawable

                            }
                            // Update RecyclerView adapter
                            adapter = new DoctorAdapter(available_doctor.this, names, specialties, hospitals, ratings, imageUrls);
                            recyclerView.setAdapter(adapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(available_doctor.this, "Data parsing error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(available_doctor.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                Log.e("VolleyError", error.toString());
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
