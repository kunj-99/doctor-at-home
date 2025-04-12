package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
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
import com.infowave.thedoctorathomeuser.adapter.OngoingAdapter;
import com.infowave.thedoctorathomeuser.MainActivity;
import com.infowave.thedoctorathomeuser.R;
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

    private final List<String> doctorNames = new ArrayList<>();
    private final List<String> specialties = new ArrayList<>();
    private final List<String> hospitals = new ArrayList<>();
    private final List<Float> ratings = new ArrayList<>();
    // New list to hold profile picture URLs (instead of resource ids)
    private final List<String> profilePictures = new ArrayList<>();
    private final List<Integer> appointmentIds = new ArrayList<>();
    private final List<String> statuses = new ArrayList<>(); // Appointment statuses
    private final List<String> durations = new ArrayList<>(); // Experience duration
    private final List<Integer> doctorIds = new ArrayList<>();


    private final Handler handler = new Handler();
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
        adapter = new OngoingAdapter(requireContext(), doctorNames, specialties, hospitals, ratings, profilePictures, appointmentIds, statuses, durations, doctorIds);

        recyclerView.setAdapter(adapter);

        fetchOngoingAppointments();

        bookAppointment.setOnClickListener(v -> vp.setCurrentItem(1));

        startAutoRefresh();

        return view;
    }

    private void fetchOngoingAppointments() {
        @SuppressLint("NotifyDataSetChanged") StringRequest stringRequest = new StringRequest(Request.Method.POST, API_URL,
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

                            doctorNames.clear();
                            specialties.clear();
                            hospitals.clear();
                            ratings.clear();
                            profilePictures.clear();
                            appointmentIds.clear();
                            statuses.clear();
                            doctorIds.clear();
                            durations.clear();

                            for (int i = 0; i < appointmentsArray.length(); i++) {
                                JSONObject appointment = appointmentsArray.getJSONObject(i);
                                Log.d("APPT_DEBUG", "Appointment: " + appointment.toString());

                                doctorNames.add(appointment.getString("doctor_name"));
                                specialties.add(appointment.getString("specialty"));
                                hospitals.add(appointment.getString("hospital_name"));
                                ratings.add((float) appointment.getDouble("experience"));
                                statuses.add(appointment.getString("status"));
                                appointmentIds.add(appointment.getInt("appointment_id"));
                                durations.add(appointment.getString("experience_duration"));
                                doctorIds.add(appointment.getInt("doctor_id"));


                                // Retrieve the profile_picture URL from the API response
                                String profilePicUrl = appointment.optString("profile_picture", "");
                                profilePictures.add(profilePicUrl);
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
