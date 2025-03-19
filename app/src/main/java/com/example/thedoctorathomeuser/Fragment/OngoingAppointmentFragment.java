package com.example.thedoctorathomeuser.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.Adapter.OngoingAdapter;
import com.example.thedoctorathomeuser.MainActivity;
import com.example.thedoctorathomeuser.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OngoingAppointmentFragment extends Fragment {
    private ViewPager vp;
    private RecyclerView recyclerView;
    private Button bookAppointment;
    private OngoingAdapter adapter;

    private String patientId;
    private static final String API_URL = "http://sxm.a58.mytemp.website/getOngoingAppointment.php";
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    private List<String> doctorNames = new ArrayList<>();
    private List<String> specialties = new ArrayList<>();
    private List<String> hospitals = new ArrayList<>();
    private List<Float> ratings = new ArrayList<>();
    private List<Integer> imageResIds = new ArrayList<>();
    private List<Integer> appointmentIds = new ArrayList<>();
    private List<String> statuses = new ArrayList<>(); // Appointment statuses
    // New list for experience duration
    private List<String> durations = new ArrayList<>();

    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing_appointment, container, false);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            vp = mainActivity.findViewById(R.id.vp);
        }

        // Retrieve patient_id from SharedPreferences
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Log.e("OngoingAppointmentFragment", "Patient ID not found in SharedPreferences");
            Toast.makeText(getContext(), "Patient ID not available", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("OngoingAppointmentFragment", "Patient ID retrieved: " + patientId);
        }

        bookAppointment = view.findViewById(R.id.bookButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Pass all lists including statuses and durations to the adapter
        adapter = new OngoingAdapter(requireContext(), doctorNames, specialties, hospitals, ratings, imageResIds, appointmentIds, statuses, durations);
        recyclerView.setAdapter(adapter);

        fetchOngoingAppointments();

        bookAppointment.setOnClickListener(v -> vp.setCurrentItem(1));

        startAutoRefresh();

        return view;
    }

    private void fetchOngoingAppointments() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("API_RESPONSE", "Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        if (jsonObject.has("error")) {
                            Toast.makeText(requireContext(), jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (jsonObject.getBoolean("success")) {
                            JSONArray appointmentsArray = jsonObject.getJSONArray("appointments");

                            // Clear existing lists
                            doctorNames.clear();
                            specialties.clear();
                            hospitals.clear();
                            ratings.clear();
                            imageResIds.clear();
                            appointmentIds.clear();
                            statuses.clear();
                            durations.clear();

                            for (int i = 0; i < appointmentsArray.length(); i++) {
                                JSONObject appointment = appointmentsArray.getJSONObject(i);
                                Log.d("APPT_DEBUG", "Appointment: " + appointment.toString());

                                doctorNames.add(appointment.getString("doctor_name"));
                                specialties.add(appointment.getString("specialty"));
                                hospitals.add(appointment.getString("hospital_name"));
                                // Note: For ratings, adjust as needed; here using "experience" field,
                                // but you might want a separate rating field.
                                ratings.add((float) appointment.getDouble("experience"));
                                statuses.add(appointment.getString("status")); // Capture status
                                imageResIds.add(R.drawable.main1);
                                appointmentIds.add(appointment.getInt("appointment_id"));
                                // New: add experience duration from JSON response
                                durations.add(appointment.getString("experience_duration"));
                            }

                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    adapter.notifyDataSetChanged();
                                    Log.d("ADAPTER_UPDATE", "Adapter updated with " + doctorNames.size() + " items");
                                });
                            }
                        } else {
                            Toast.makeText(requireContext(), "No ongoing appointments found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing Error: " + e.getMessage());
                        Toast.makeText(requireContext(), "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMsg = (error.getMessage() != null) ? error.getMessage() : "Unknown Error";
                    Log.e("VOLLEY_ERROR", "Error: " + errorMsg);
                    Toast.makeText(requireContext(), "Volley Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    fetchOngoingAppointments();
                    handler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoRefresh();
    }
}
