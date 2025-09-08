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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.DoctorHistoryAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private DoctorHistoryAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private RequestQueue requestQueue;

    private final List<String> doctorNames = new ArrayList<>();
    private final List<String> doctorSpecialties = new ArrayList<>();
    private final List<String> appointmentDates = new ArrayList<>();
    private final List<String> appointmentPrices = new ArrayList<>();
    private final List<String> doctorProfilePictures = new ArrayList<>();
    private final List<Integer> doctorIds = new ArrayList<>();
    private final List<Integer> appointmentIds = new ArrayList<>();
    private final List<String> appointmentStatuses = new ArrayList<>();

    private String apiUrl;
    private String patientId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView  = view.findViewById(R.id.recyclerView);
        progressBar   = view.findViewById(R.id.progressBar_history);
        swipeRefresh  = view.findViewById(R.id.swipeRefreshHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        requestQueue = Volley.newRequestQueue(requireContext());

        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");

        if (patientId == null || patientId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "We could not verify your profile. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiUrl = "http://sxm.a58.mytemp.website/get_history.php?patient_id=" + patientId;

        adapter = new DoctorHistoryAdapter(
                requireContext(),
                patientId,
                doctorIds,
                doctorNames,
                doctorSpecialties,
                appointmentDates,
                appointmentPrices,
                doctorProfilePictures,
                appointmentIds,
                appointmentStatuses
        );
        recyclerView.setAdapter(adapter);

        // Pull-to-refresh: fetch without showing the big progress bar
        swipeRefresh.setOnRefreshListener(() -> fetchData(false));

        // Initial load with big progress bar
        fetchData(true);
    }

    private void stopRefreshingUI(boolean hideProgressBarToo) {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
        if (hideProgressBarToo && progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void startInitialLoadingIfNeeded(boolean showLoader) {
        if (showLoader && progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void fetchData(boolean showLoader) {
        startInitialLoadingIfNeeded(showLoader);

        @SuppressLint("NotifyDataSetChanged")
        StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                response -> {
                    stopRefreshingUI(showLoader);

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.optBoolean("success", false)) {
                            JSONArray appointments = jsonObject.getJSONArray("appointments");

                            // New data buffers
                            List<Integer> newAppointmentIds = new ArrayList<>();
                            List<Integer> newDoctorIds = new ArrayList<>();
                            List<String> newDoctorNames = new ArrayList<>();
                            List<String> newDoctorSpecialties = new ArrayList<>();
                            List<String> newAppointmentDates = new ArrayList<>();
                            List<String> newAppointmentPrices = new ArrayList<>();
                            List<String> newDoctorProfilePictures = new ArrayList<>();
                            List<String> newAppointmentStatuses = new ArrayList<>();

                            for (int i = 0; i < appointments.length(); i++) {
                                JSONObject obj = appointments.getJSONObject(i);

                                newAppointmentIds.add(obj.getInt("appointment_id"));
                                newDoctorIds.add(obj.getInt("doctor_id"));
                                newDoctorNames.add(obj.optString("doctor_name", ""));
                                newDoctorSpecialties.add(obj.optString("specialty", ""));
                                newAppointmentDates.add(obj.optString("appointment_date", ""));
                                newAppointmentPrices.add("₹ " + obj.optString("fee", "0") + " /-");

                                String profilePicUrl = obj.optString("profile_picture", "");
                                if (profilePicUrl == null || profilePicUrl.trim().isEmpty()) {
                                    profilePicUrl = "http://sxm.a58.mytemp.website/doctor_images/default.png";
                                }
                                newDoctorProfilePictures.add(profilePicUrl);

                                newAppointmentStatuses.add(obj.optString("status", ""));
                            }

                            // Update only if meaningful differences
                            if (!newAppointmentIds.equals(appointmentIds) ||
                                    !newAppointmentStatuses.equals(appointmentStatuses)) {

                                doctorIds.clear();              doctorIds.addAll(newDoctorIds);
                                doctorNames.clear();            doctorNames.addAll(newDoctorNames);
                                doctorSpecialties.clear();      doctorSpecialties.addAll(newDoctorSpecialties);
                                appointmentDates.clear();       appointmentDates.addAll(newAppointmentDates);
                                appointmentPrices.clear();      appointmentPrices.addAll(newAppointmentPrices);
                                doctorProfilePictures.clear();  doctorProfilePictures.addAll(newDoctorProfilePictures);
                                appointmentIds.clear();         appointmentIds.addAll(newAppointmentIds);
                                appointmentStatuses.clear();    appointmentStatuses.addAll(newAppointmentStatuses);

                                adapter.notifyDataSetChanged();

                                recyclerView.setAlpha(0f);
                                recyclerView.animate().alpha(1f).setDuration(300).start();
                            }
                        } else {
                            Toast.makeText(requireContext(), "No past appointments found yet.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(), "Sorry, we could not process your data. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    stopRefreshingUI(showLoader);
                    Toast.makeText(requireContext(), "Unable to load your appointments. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                });

        // Tagging optional; here we just add
        requestQueue.add(request);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) {
            requestQueue.cancelAll(request -> true);
        }
    }
}
