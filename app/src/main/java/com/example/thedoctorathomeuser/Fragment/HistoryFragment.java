package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.Adapter.DoctorHistoryAdapter;
import com.example.thedoctorathomeuser.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private DoctorHistoryAdapter adapter;
    private ProgressBar progressBar;

    // Lists for only necessary data
    private List<String> doctorNames = new ArrayList<>();
    private List<String> doctorSpecialties = new ArrayList<>();
    private List<String> appointmentDates = new ArrayList<>();
    private List<String> appointmentPrices = new ArrayList<>();
    private List<Integer> doctorImages = new ArrayList<>();  // Placeholder images
    private List<Integer> doctorIds = new ArrayList<>();  // ✅ Store doctor_id

    private static final String API_URL = "http://sxm.a58.mytemp.website/get_history.php?patient_id=1";  // Replace with actual API URL

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar_history);  // Ensure this is added in your XML layout
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DoctorHistoryAdapter(requireContext(), doctorIds, doctorNames, doctorSpecialties, appointmentDates, appointmentPrices, doctorImages);
        recyclerView.setAdapter(adapter);

        fetchData();
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);  // Show loading

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);  // Hide loading

                        try {
                            if (response.getBoolean("success")) {
                                JSONArray appointments = response.getJSONArray("appointments");

                                // Clear old data
                                doctorIds.clear();
                                doctorNames.clear();
                                doctorSpecialties.clear();
                                appointmentDates.clear();
                                appointmentPrices.clear();
                                doctorImages.clear();

                                // Loop through appointments
                                for (int i = 0; i < appointments.length(); i++) {
                                    JSONObject obj = appointments.getJSONObject(i);

                                    // Get only necessary data
                                    doctorIds.add(obj.getInt("doctor_id"));  // ✅ Pass doctor_id
                                    doctorNames.add(obj.getString("doctor_name"));
                                    doctorSpecialties.add(obj.getString("specialty"));
                                    appointmentDates.add(obj.getString("appointment_date"));
                                    appointmentPrices.add("₹ " + obj.getString("fee") + " /-");

                                    // Use a placeholder image for now
                                    doctorImages.add(R.drawable.plasholder);
                                }

                                // Notify adapter
                                adapter.notifyDataSetChanged();

                            } else {
                                Toast.makeText(requireContext(), "No history found", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Error: " + e.getMessage());
                            Toast.makeText(requireContext(), "Data parsing error!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("VOLLEY_ERROR", "Error: " + error.getMessage());
                Toast.makeText(requireContext(), "Failed to fetch data!", Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(request);
    }
}
