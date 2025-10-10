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
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.VetHistoryAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VetHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private VetHistoryAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private RequestQueue requestQueue;

    private final List<String> vetNames = new ArrayList<>();
    private final List<String> vetSpecialties = new ArrayList<>();
    private final List<String> appointmentDates = new ArrayList<>();
    private final List<String> appointmentPrices = new ArrayList<>();
    private final List<String> vetProfilePictures = new ArrayList<>();
    private final List<Integer> vetIds = new ArrayList<>();
    private final List<Integer> appointmentIds = new ArrayList<>();
    private final List<String> appointmentStatuses = new ArrayList<>();

    private String apiUrl;
    private String patientId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vet_history, container, false);
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

        // ANIMAL endpoint
        apiUrl = ApiConfig.endpoint("get_vet_history.php", "patient_id", patientId);

        adapter = new VetHistoryAdapter(
                requireContext(),
                patientId,
                vetIds,
                vetNames,
                vetSpecialties,
                appointmentDates,
                appointmentPrices,
                vetProfilePictures,
                appointmentIds,
                appointmentStatuses
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> fetchData(false));
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

                            List<Integer> newAppointmentIds = new ArrayList<>();
                            List<Integer> newVetIds = new ArrayList<>();
                            List<String> newVetNames = new ArrayList<>();
                            List<String> newVetSpecialties = new ArrayList<>();
                            List<String> newAppointmentDates = new ArrayList<>();
                            List<String> newAppointmentPrices = new ArrayList<>();
                            List<String> newVetProfilePictures = new ArrayList<>();
                            List<String> newAppointmentStatuses = new ArrayList<>();

                            for (int i = 0; i < appointments.length(); i++) {
                                JSONObject obj = appointments.getJSONObject(i);

                                newAppointmentIds.add(obj.getInt("appointment_id"));
                                newVetIds.add(obj.getInt("vet_id")); // server uses vet_id
                                newVetNames.add(obj.optString("vet_name", ""));
                                newVetSpecialties.add(obj.optString("specialty", ""));
                                newAppointmentDates.add(obj.optString("appointment_date", ""));
                                newAppointmentPrices.add("₹ " + obj.optString("fee", "0") + " /-");

                                String profilePicUrl = obj.optString("profile_picture", "");
                                if (profilePicUrl == null || profilePicUrl.trim().isEmpty()) {
                                    profilePicUrl = ApiConfig.endpoint("doctor_images/default.png");
                                }
                                newVetProfilePictures.add(profilePicUrl);

                                newAppointmentStatuses.add(obj.optString("status", ""));
                            }

                            if (!newAppointmentIds.equals(appointmentIds) ||
                                    !newAppointmentStatuses.equals(appointmentStatuses)) {

                                vetIds.clear();              vetIds.addAll(newVetIds);
                                vetNames.clear();            vetNames.addAll(newVetNames);
                                vetSpecialties.clear();      vetSpecialties.addAll(newVetSpecialties);
                                appointmentDates.clear();    appointmentDates.addAll(newAppointmentDates);
                                appointmentPrices.clear();   appointmentPrices.addAll(newAppointmentPrices);
                                vetProfilePictures.clear();  vetProfilePictures.addAll(newVetProfilePictures);
                                appointmentIds.clear();      appointmentIds.addAll(newAppointmentIds);
                                appointmentStatuses.clear(); appointmentStatuses.addAll(newAppointmentStatuses);

                                adapter.notifyDataSetChanged();
                                recyclerView.setAlpha(0f);
                                recyclerView.animate().alpha(1f).setDuration(300).start();
                            }
                        } else {
                            Toast.makeText(requireContext(), "No animal appointment history yet.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(), "Sorry, we could not process your data. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    stopRefreshingUI(showLoader);
                    Toast.makeText(requireContext(), "Unable to load animal history. Please check your internet.", Toast.LENGTH_SHORT).show();
                });

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
