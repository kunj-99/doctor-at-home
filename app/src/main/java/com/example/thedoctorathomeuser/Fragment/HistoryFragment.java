package com.example.thedoctorathomeuser.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.android.volley.toolbox.StringRequest;
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
    private RequestQueue requestQueue;

    private List<String> doctorNames = new ArrayList<>();
    private List<String> doctorSpecialties = new ArrayList<>();
    private List<String> appointmentDates = new ArrayList<>();
    private List<String> appointmentPrices = new ArrayList<>();
    // New list to hold profile picture URLs instead of image resource IDs
    private List<String> doctorProfilePictures = new ArrayList<>();
    private List<Integer> doctorIds = new ArrayList<>();
    private List<Integer> appointmentIds = new ArrayList<>();
    private List<String> appointmentStatuses = new ArrayList<>();  // Track appointment status

    private String apiUrl;
    private String patientId;
    private Handler handler;
    private final int REFRESH_INTERVAL = 10000;

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchData();
            handler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());

        // Get Patient ID from SharedPreferences
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");

        if (patientId.isEmpty()) {
            Log.e("HistoryFragment", "Patient ID not found in SharedPreferences");
            Toast.makeText(requireContext(), "Patient ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("HistoryFragment", "Patient ID: " + patientId);
        apiUrl = "http://sxm.a58.mytemp.website/get_history.php?patient_id=" + patientId;

        // Updated adapter instantiation: pass doctorProfilePictures list instead of image resource list
        adapter = new DoctorHistoryAdapter(requireContext(), patientId, doctorIds, doctorNames, doctorSpecialties,
                appointmentDates, appointmentPrices, doctorProfilePictures, appointmentIds, appointmentStatuses);
        recyclerView.setAdapter(adapter);

        handler = new Handler(Looper.getMainLooper());
        fetchData();
        handler.postDelayed(autoRefreshRunnable, REFRESH_INTERVAL);
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);

        StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray appointments = jsonObject.getJSONArray("appointments");

                            // Clear existing data
                            doctorIds.clear();
                            doctorNames.clear();
                            doctorSpecialties.clear();
                            appointmentDates.clear();
                            appointmentPrices.clear();
                            doctorProfilePictures.clear();
                            appointmentIds.clear();
                            appointmentStatuses.clear();

                            for (int i = 0; i < appointments.length(); i++) {
                                JSONObject obj = appointments.getJSONObject(i);
                                appointmentIds.add(obj.getInt("appointment_id"));
                                doctorIds.add(obj.getInt("doctor_id"));
                                doctorNames.add(obj.getString("doctor_name"));
                                doctorSpecialties.add(obj.getString("specialty"));
                                appointmentDates.add(obj.getString("appointment_date"));
                                appointmentPrices.add("₹ " + obj.getString("fee") + " /-");

                                // Retrieve the profile_picture URL from the JSON response
                                String profilePicUrl = obj.optString("profile_picture", "");
                                if (profilePicUrl.isEmpty()) {
                                    profilePicUrl = "http://sxm.a58.mytemp.website/doctor_images/default.png";
                                }
                                doctorProfilePictures.add(profilePicUrl);

                                appointmentStatuses.add(obj.getString("status"));
                            }

                            adapter.notifyDataSetChanged();
                            Log.d("HistoryFragment", "Updated adapter with " + doctorNames.size() + " items");
                        } else {
                            Toast.makeText(requireContext(), "No history found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("HistoryFragment", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(requireContext(), "Data parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("HistoryFragment", "Volley error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                    Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(autoRefreshRunnable);
        if (requestQueue != null) {
            requestQueue.cancelAll(request -> true);
        }
    }
}
