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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.MainActivity;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.OngoingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OngoingAppointmentFragment extends Fragment {
    // private static final String TAG = "OngoingAppointment"; // COMMENTED FOR PRODUCTION
    private static final String API_URL = "http://sxm.a58.mytemp.website/getOngoingAppointment.php";
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    private ViewPager vp;
    private RecyclerView recyclerView;
    private Button bookAppointment;
    private OngoingAdapter adapter;

    private String patientId;

    private final List<String> doctorNames     = new ArrayList<>();
    private final List<String> specialties     = new ArrayList<>();
    private final List<String> hospitals       = new ArrayList<>();
    private final List<Float>  ratings         = new ArrayList<>();
    private final List<String> profilePictures = new ArrayList<>();
    private final List<Integer> appointmentIds = new ArrayList<>();
    private final List<String> statuses        = new ArrayList<>();
    private final List<String> durations       = new ArrayList<>();
    private final List<Integer> doctorIds      = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            if (isAdded()) {
                fetchOngoingAppointments();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing_appointment, container, false);

        // ViewPager from parent activity
        if (getActivity() instanceof MainActivity) {
            vp = ((MainActivity) getActivity()).findViewById(R.id.vp);
        }

        // Retrieve patient_id
        SharedPreferences sp = requireActivity()
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            // Log.e(TAG, "Patient ID not found");
            Toast.makeText(getContext(), "Could not load your information. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        bookAppointment = view.findViewById(R.id.bookButton);
        recyclerView    = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OngoingAdapter(
                requireContext(),
                doctorNames,
                specialties,
                hospitals,
                ratings,
                profilePictures,
                appointmentIds,
                statuses,
                durations,
                doctorIds
        );
        recyclerView.setAdapter(adapter);

        fetchOngoingAppointments();
        startAutoRefresh();

        bookAppointment.setOnClickListener(v -> {
            if (vp != null) vp.setCurrentItem(1);
        });

        return view;
    }

    private void fetchOngoingAppointments() {
        @SuppressLint("NotifyDataSetChanged") StringRequest req = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success", false)) {
                            JSONArray arr = json.getJSONArray("appointments");

                            doctorNames.clear();
                            specialties.clear();
                            hospitals.clear();
                            ratings.clear();
                            profilePictures.clear();
                            appointmentIds.clear();
                            statuses.clear();
                            durations.clear();
                            doctorIds.clear();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject appt = arr.getJSONObject(i);
                                doctorNames    .add(appt.getString("doctor_name"));
                                specialties    .add(appt.getString("specialty"));
                                hospitals      .add(appt.getString("hospital_name"));
                                ratings        .add((float) appt.getDouble("experience"));
                                statuses       .add(appt.getString("status"));
                                appointmentIds .add(appt.getInt("appointment_id"));
                                durations      .add(appt.getString("experience_duration"));
                                doctorIds      .add(appt.getInt("doctor_id"));
                                profilePictures.add(appt.optString("profile_picture", ""));
                            }

                            adapter.notifyDataSetChanged();
                            // Log.d(TAG, "Loaded " + doctorNames.size() + " appointments");
                        } else {
                            Toast.makeText(requireContext(),
                                    "You don't have any ongoing appointments right now.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        // Log.e(TAG, "JSON parse error", e);
                        Toast.makeText(requireContext(),
                                "Could not load appointment data. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Log.e(TAG, "Network error", error);
                    Toast.makeText(requireContext(),
                            "Unable to connect. Please check your internet and try again.",
                            Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id", patientId);
                return p;
            }
        };

        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoRefresh();
    }
}
