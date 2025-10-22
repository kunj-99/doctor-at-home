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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
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

/**
 * Shows Vet (animal) appointment history for the logged-in patient.
 * All identifiers and copy are Vet-specific.
 */
public class VetHistoryFragment extends Fragment {

    private static final String LOG_TAG = "VetHistoryFragment";

    // UI
    private RecyclerView rvVetHistory;
    private ProgressBar pbVetHistory;
    private SwipeRefreshLayout srlVetHistory;

    // Networking
    private RequestQueue volleyQueue;
    private String vetHistoryUrl;
    private String patientId;

    // Data lists for Vet cards
    private final List<Integer>   vetIds              = new ArrayList<>();
    private final List<String>    vetNames            = new ArrayList<>();
    private final List<String>    vetSpecialties      = new ArrayList<>();
    private final List<String>    vetAppointmentDates = new ArrayList<>();
    private final List<String>    vetAppointmentFees  = new ArrayList<>();
    private final List<String>    vetProfilePhotos    = new ArrayList<>();
    private final List<Integer>   vetAppointmentIds   = new ArrayList<>();
    private final List<String>    vetAppointmentStates= new ArrayList<>();

    private VetHistoryAdapter vetHistoryAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vet_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        // Bind views
        rvVetHistory  = root.findViewById(R.id.recyclerView);
        pbVetHistory  = root.findViewById(R.id.progressBar_history);
        srlVetHistory = root.findViewById(R.id.swipeRefreshHistory);

        // Setup RecyclerView
        rvVetHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Volley
        volleyQueue = Volley.newRequestQueue(requireContext());

        // Logged-in patient
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", null);

        if (patientId == null || patientId.trim().isEmpty()) {
            Log.e(LOG_TAG, "Patient ID is missing in SharedPreferences!");
            Toast.makeText(requireContext(), "Unable to verify your profile. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vet history endpoint
        vetHistoryUrl = ApiConfig.endpoint("get_vet_history.php", "patient_id", patientId);
        Log.d(LOG_TAG, "Calling vet history endpoint: " + vetHistoryUrl);

        // Adapter
        vetHistoryAdapter = new VetHistoryAdapter(
                requireContext(),
                patientId,
                vetIds,
                vetNames,
                vetSpecialties,
                vetAppointmentDates,
                vetAppointmentFees,
                vetProfilePhotos,
                vetAppointmentIds,
                vetAppointmentStates
        );
        rvVetHistory.setAdapter(vetHistoryAdapter);

        // Pull-to-refresh
        srlVetHistory.setOnRefreshListener(() -> loadVetHistory(false));

        // Initial load
        loadVetHistory(true);
    }

    private void setLoading(boolean loading) {
        if (loading) {
            if (pbVetHistory != null) pbVetHistory.setVisibility(View.VISIBLE);
        } else {
            if (pbVetHistory != null) pbVetHistory.setVisibility(View.GONE);
            if (srlVetHistory != null && srlVetHistory.isRefreshing()) srlVetHistory.setRefreshing(false);
        }
    }

    private void applyDataWithDiff(
            List<Integer> newAppointmentIds,
            List<Integer> newVetIds,
            List<String>  newVetNames,
            List<String>  newVetSpecialties,
            List<String>  newAppointmentDates,
            List<String>  newAppointmentFees,
            List<String>  newVetPhotos,
            List<String>  newAppointmentStates
    ) {
        boolean changed =
                !newAppointmentIds.equals(vetAppointmentIds) ||
                        !newAppointmentStates.equals(vetAppointmentStates);

        if (!changed) {
            Log.i(LOG_TAG, "No changes in vet appointment data; skipping UI update.");
            return;
        }

        vetIds.clear();                 vetIds.addAll(newVetIds);
        vetNames.clear();               vetNames.addAll(newVetNames);
        vetSpecialties.clear();         vetSpecialties.addAll(newVetSpecialties);
        vetAppointmentDates.clear();    vetAppointmentDates.addAll(newAppointmentDates);
        vetAppointmentFees.clear();     vetAppointmentFees.addAll(newAppointmentFees);
        vetProfilePhotos.clear();       vetProfilePhotos.addAll(newVetPhotos);
        vetAppointmentIds.clear();      vetAppointmentIds.addAll(newAppointmentIds);
        vetAppointmentStates.clear();   vetAppointmentStates.addAll(newAppointmentStates);

        Log.d(LOG_TAG, "UI updated with " + newAppointmentIds.size() + " vet appointments.");
        vetHistoryAdapter.notifyDataSetChanged();
        rvVetHistory.setAlpha(0f);
        rvVetHistory.animate().alpha(1f).setDuration(250).start();
    }

    private String safePhotoUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return ApiConfig.endpoint("doctor_images/default.png");
        }
        return raw;
    }

    private String formatFee(String rawFee) {
        String fee = (rawFee == null || rawFee.trim().isEmpty()) ? "0" : rawFee.trim();
        return "₹ " + fee + " /-";
    }

    private void handleEmptyOrNoSuccess(boolean showToast, String rawResp) {
        Log.w(LOG_TAG, "No vet history found or API returned success=false. Response: " + rawResp);
        if (showToast) {
            Toast.makeText(requireContext(), "No vet appointment history yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleParseError(Exception e, String resp) {
        Log.e(LOG_TAG, "Parse error: " + e.getMessage() + " | Response: " + resp, e);
        Toast.makeText(requireContext(), "We couldn’t process your vet history. Please try again later.", Toast.LENGTH_SHORT).show();
    }

    private void handleNetworkError(VolleyError e) {
        Log.e(LOG_TAG, "Network error during vet history API call: " + e.getMessage(), e);
        Toast.makeText(requireContext(), "Unable to load vet history. Please check your internet connection.", Toast.LENGTH_SHORT).show();
    }

    private void loadVetHistory(boolean showLoader) {
        setLoading(showLoader);

        @SuppressLint("NotifyDataSetChanged")
        StringRequest req = new StringRequest(
                Request.Method.GET,
                vetHistoryUrl,
                resp -> {
                    setLoading(false);
                    Log.d(LOG_TAG, "API response: " + resp);

                    try {
                        JSONObject root = new JSONObject(resp);
                        if (!root.optBoolean("success", false)) {
                            handleEmptyOrNoSuccess(true, resp);
                            return;
                        }

                        JSONArray arr = root.optJSONArray("appointments");
                        if (arr == null || arr.length() == 0) {
                            handleEmptyOrNoSuccess(true, resp);
                            return;
                        }

                        List<Integer> newAppointmentIds    = new ArrayList<>();
                        List<Integer> newVetIds            = new ArrayList<>();
                        List<String>  newVetNames          = new ArrayList<>();
                        List<String>  newVetSpecialties    = new ArrayList<>();
                        List<String>  newAppointmentDates  = new ArrayList<>();
                        List<String>  newAppointmentFees   = new ArrayList<>();
                        List<String>  newVetPhotos         = new ArrayList<>();
                        List<String>  newAppointmentStates = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);

                            // Log each appointment object (for debug)
                            Log.v(LOG_TAG, "Vet appointment JSON: " + obj.toString());

                            // For vet endpoints, always expect doctor_id / doctor_name etc.
                            newAppointmentIds.add(obj.optInt("appointment_id"));
                            newVetIds.add(obj.optInt("doctor_id")); // Vet ID == doctor_id in your PHP
                            newVetNames.add(obj.optString("doctor_name", ""));
                            newVetSpecialties.add(obj.optString("doctor_specialty", ""));
                            newAppointmentDates.add(obj.optString("appointment_date", ""));
                            newAppointmentFees.add(formatFee(obj.optString("fee", "0")));
                            newVetPhotos.add(safePhotoUrl(obj.optString("doctor_profile_picture", "")));
                            newAppointmentStates.add(obj.optString("status", ""));
                        }

                        applyDataWithDiff(
                                newAppointmentIds,
                                newVetIds,
                                newVetNames,
                                newVetSpecialties,
                                newAppointmentDates,
                                newAppointmentFees,
                                newVetPhotos,
                                newAppointmentStates
                        );
                    } catch (JSONException e) {
                        handleParseError(e, resp);
                    }
                },
                err -> {
                    setLoading(false);
                    handleNetworkError(err);
                }
        );

        volleyQueue.add(req);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (volleyQueue != null) {
            volleyQueue.cancelAll(request -> true);
            Log.i(LOG_TAG, "Cancelled all pending Volley requests in VetHistoryFragment.");
        }
    }
}
