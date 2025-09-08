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

    private static final String API_URL = "http://sxm.a58.mytemp.website/getOngoingAppointment.php";

    private ViewPager vp;
    private RecyclerView recyclerView;
    private Button bookAppointment;
    private SwipeRefreshLayout swipeRefresh;
    private OngoingAdapter adapter;

    private String patientId;

    private final List<String>  doctorNames      = new ArrayList<>();
    private final List<String>  specialties      = new ArrayList<>();
    private final List<String>  hospitals        = new ArrayList<>();
    private final List<Float>   ratings          = new ArrayList<>();
    private final List<String>  profilePictures  = new ArrayList<>();
    private final List<Integer> appointmentIds   = new ArrayList<>();
    private final List<String>  statuses         = new ArrayList<>();
    private final List<String>  durations        = new ArrayList<>();
    private final List<Integer> doctorIds        = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
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
        swipeRefresh   = view.findViewById(R.id.swipeRefreshOngoing);
        recyclerView   = view.findViewById(R.id.recyclerView);
        bookAppointment= view.findViewById(R.id.bookButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Patient id
        SharedPreferences sp = requireActivity()
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null || patientId.trim().isEmpty()) {
            Toast.makeText(getContext(),
                    "Could not load your information. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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

        // Pull-to-refresh handler
        swipeRefresh.setOnRefreshListener(() -> fetchOngoingAppointments(false));

        // Optional spinner colors
        // swipeRefresh.setColorSchemeResources(R.color.navy_blue, R.color.acqua_green, R.color.purple_500);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // No auto-refresh handlers to clear anymore
    }
}
