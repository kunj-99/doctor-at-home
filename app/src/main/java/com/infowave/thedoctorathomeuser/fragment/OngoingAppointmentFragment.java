package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout; // tabs
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.MainActivity;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.OngoingAdapter;
import com.infowave.thedoctorathomeuser.adapter.VetOngoingAdapter;   // NEW
import com.infowave.thedoctorathomeuser.VetAppointment;              // NEW (as per your import path)

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OngoingAppointmentFragment extends Fragment {

    private static final String API_URL = ApiConfig.endpoint("getOngoingAppointment.php");

    private ViewPager vp;
    private RecyclerView recyclerView;
    private Button bookAppointment;
    private SwipeRefreshLayout swipeRefresh;
    private OngoingAdapter adapter; // patient adapter

    // Tabs
    private TabLayout appointmentTabs;

    private String patientId;

    // ----- Patient data (AS-IS) -----
    private final List<String>  doctorNames      = new ArrayList<>();
    private final List<String>  specialties      = new ArrayList<>();
    private final List<String>  hospitals        = new ArrayList<>();
    private final List<Float>   ratings          = new ArrayList<>();
    private final List<String>  profilePictures  = new ArrayList<>();
    private final List<Integer> appointmentIds   = new ArrayList<>();
    private final List<String>  statuses         = new ArrayList<>();
    private final List<String>  durations        = new ArrayList<>();
    private final List<Integer> doctorIds        = new ArrayList<>();

    // ----- Vet (demo) -----
    private VetOngoingAdapter vetAdapter;
    private final List<VetAppointment> vetDemo = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // IMPORTANT: make sure the XML file name matches this
        return inflater.inflate(R.layout.fragment_ongoing_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        // ViewPager from parent activity (to switch tabs when booking)
        if (getActivity() instanceof MainActivity) {
            vp = ((MainActivity) getActivity()).findViewById(R.id.vp);
        }

        // UI
        appointmentTabs = view.findViewById(R.id.appointmentTabs);
        swipeRefresh    = view.findViewById(R.id.swipeRefreshOngoing);
        recyclerView    = view.findViewById(R.id.recyclerView);
        bookAppointment = view.findViewById(R.id.bookButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Patient id
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null || patientId.trim().isEmpty()) {
            Toast.makeText(getContext(),
                    "Could not load your information. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Patient adapter (AS-IS)
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

        // Vet adapter (static demo)
        seedVetDemo();
        vetAdapter = new VetOngoingAdapter(requireContext(), vetDemo);

        // Default: show Patient Ongoing (original behavior)
        recyclerView.setAdapter(adapter);

        // Tabs switching logic (no change to patient flow)
        if (appointmentTabs != null) {
            appointmentTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        // Patient ongoing (original)
                        recyclerView.setAdapter(adapter);
                        // Optionally refresh patient silently so UI stays snappy
                        fetchOngoingAppointments(true);
                    } else {
                        // Vet ongoing (demo data)
                        recyclerView.setAdapter(vetAdapter);
                        // Stop spinner if any
                        stopRefreshingUI();
                    }
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
            // Ensure Patient tab is selected by default
            TabLayout.Tab patientTab = appointmentTabs.getTabAt(0);
            if (patientTab != null) patientTab.select();
        }

        // Pull-to-refresh handler
        swipeRefresh.setOnRefreshListener(() -> {
            int pos = (appointmentTabs != null) ? appointmentTabs.getSelectedTabPosition() : 0;
            if (pos == 0) {
                // Patient refresh (original)
                fetchOngoingAppointments(false);
            } else {
                // Vet refresh (demo reseed)
                seedVetDemo();
                vetAdapter.notifyDataSetChanged();
                stopRefreshingUI();
            }
        });

        // Initial load (show the swipe spinner programmatically)
        swipeRefresh.setRefreshing(true);
        fetchOngoingAppointments(false);

        // Book button → switch to booking tab/page
        bookAppointment.setOnClickListener(v -> {
            if (vp != null) vp.setCurrentItem(1);
        });
    }

    private void stopRefreshingUI() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void fetchOngoingAppointments(boolean silent) {
        @SuppressLint("NotifyDataSetChanged")
        StringRequest req = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    stopRefreshingUI();
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
                                doctorNames    .add(appt.optString("doctor_name", ""));
                                specialties    .add(appt.optString("specialty", ""));
                                hospitals      .add(appt.optString("hospital_name", ""));
                                ratings        .add((float) appt.optDouble("experience", 0));
                                statuses       .add(appt.optString("status", ""));
                                appointmentIds .add(appt.optInt("appointment_id", 0));
                                durations      .add(appt.optString("experience_duration", ""));
                                doctorIds      .add(appt.optInt("doctor_id", 0));
                                profilePictures.add(appt.optString("profile_picture", ""));
                            }

                            adapter.notifyDataSetChanged();
                        } else if (!silent) {
                            Toast.makeText(requireContext(),
                                    "You don't have any ongoing appointments right now.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        if (!silent) {
                            Toast.makeText(requireContext(),
                                    "Could not load appointment data. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> {
                    stopRefreshingUI();
                    if (!silent) {
                        Toast.makeText(requireContext(),
                                "Unable to connect. Please check your internet and try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id", patientId);
                return p;
            }
        };

        Volley.newRequestQueue(requireContext()).add(req);
    }

    // Static demo data for Vet ongoing (cards)
    private void seedVetDemo() {
        vetDemo.clear();
        vetDemo.add(new VetAppointment(
                "Bruno (Dog)", "Skin allergy consultation", "Dr. K. Desai",
                "Thu, 09 Oct • 02:15 PM", "₹650", "Ongoing", "https://i.imgur.com/7kQ7K.png"
        ));
        vetDemo.add(new VetAppointment(
                "Misty (Cat)", "Deworming", "Dr. A. Shah",
                "Thu, 09 Oct • 04:00 PM", "₹350", "Scheduled", "https://i.imgur.com/xp8Zp.png"
        ));
        vetDemo.add(new VetAppointment(
                "Sheru (Dog)", "Vaccination", "Dr. P. Rana",
                "Fri, 10 Oct • 11:20 AM", "₹500", "Ongoing", "https://i.imgur.com/3yQpL.png"
        ));
    }
}
