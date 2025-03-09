package com.example.thedoctorathomeuser;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
    private ArrayList<String> doctorIds = new ArrayList<>();
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> specialties = new ArrayList<>();
    private ArrayList<String> hospitals = new ArrayList<>();
    private ArrayList<Float> ratings = new ArrayList<>();
    private ArrayList<String> imageUrls = new ArrayList<>();
    private EditText edtPincode;
    private ImageButton btnSearch;
    private String categoryId, categoryName;
    private static final String DEFAULT_PINCODE = "110001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_doctor);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        edtPincode = findViewById(R.id.edt_pincode);
        btnSearch = findViewById(R.id.btn_search);

        // Get Category ID from Intent
        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        // **Search default pincode first**
        fetchDoctorsByPincodeAndCategory(DEFAULT_PINCODE, categoryId, false);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pincode = edtPincode.getText().toString().trim();
                if (!pincode.isEmpty() && categoryId != null) {
                    fetchDoctorsByPincodeAndCategory(pincode, categoryId, true);
                } else {
                    Toast.makeText(available_doctor.this, "Please enter a valid pincode", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchDoctorsByPincodeAndCategory(String pincode, String categoryId, boolean userSearch) {
        String url = "http://sxm.a58.mytemp.website/getDoctorsByCategory.php?pincode=" + pincode + "&category_id=" + categoryId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        doctorIds.clear();
                        names.clear();
                        specialties.clear();
                        hospitals.clear();
                        ratings.clear();
                        imageUrls.clear();

                        if (response.length() == 0) {
                            if (!userSearch) {
                                Toast.makeText(available_doctor.this, "No doctors found in 364470. Please enter a different pincode.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(available_doctor.this, "No doctors found for this pincode", Toast.LENGTH_SHORT).show();
                            }
                            recyclerView.setAdapter(null);
                            return;
                        }

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject doctor = response.getJSONObject(i);

                                doctorIds.add(doctor.getString("doctor_id")); // Store doctor ID
                                names.add(doctor.getString("full_name"));
                                specialties.add(doctor.getString("specialization"));
                                hospitals.add(doctor.getString("hospital_affiliation"));
                                ratings.add((float) doctor.getDouble("rating"));
                                imageUrls.add(doctor.getString("profile_picture"));
                            }

                            adapter = new DoctorAdapter(available_doctor.this, doctorIds, names, specialties, hospitals, ratings, imageUrls);
                            recyclerView.setAdapter(adapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(available_doctor.this, "Data parsing error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(available_doctor.this, "No Doctor available, try another pincode ", Toast.LENGTH_SHORT).show();
                Log.e("VolleyError", error.toString());
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
