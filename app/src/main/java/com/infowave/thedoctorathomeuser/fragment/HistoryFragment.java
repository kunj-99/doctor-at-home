package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
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
// import android.util.Log; // For debugging only, commented in production.
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.DoctorHistoryAdapter;
import com.infowave.thedoctorathomeuser.R;
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
    private List<String> doctorProfilePictures = new ArrayList<>();
    private List<Integer> doctorIds = new ArrayList<>();
    private List<Integer> appointmentIds = new ArrayList<>();
    private List<String> appointmentStatuses = new ArrayList<>();

    private String apiUrl;
    private String patientId;
    private Handler handler;
    private final int REFRESH_INTERVAL = 5000; // 5 seconds

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchData(false); // false = no progress bar
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

        requestQueue = Volley.newRequestQueue(requireContext());

        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");

        if (patientId.isEmpty()) {
            // Log.e("HistoryFragment", "Patient ID not found in SharedPreferences");
            Toast.makeText(requireContext(), "We could not verify your profile. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log.d("HistoryFragment", "Patient ID: " + patientId);
        apiUrl = "http://sxm.a58.mytemp.website/get_history.php?patient_id=" + patientId;

        adapter = new DoctorHistoryAdapter(requireContext(), patientId, doctorIds, doctorNames, doctorSpecialties,
                appointmentDates, appointmentPrices, doctorProfilePictures, appointmentIds, appointmentStatuses);
        recyclerView.setAdapter(adapter);

        handler = new Handler(Looper.getMainLooper());
        fetchData(true); // true = show progress bar initially
        handler.postDelayed(autoRefreshRunnable, REFRESH_INTERVAL);
    }

    private void fetchData(boolean showLoader) {
        if (showLoader) progressBar.setVisibility(View.VISIBLE);

        @SuppressLint("NotifyDataSetChanged") StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                response -> {
                    if (showLoader) progressBar.setVisibility(View.GONE);

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            JSONArray appointments = jsonObject.getJSONArray("appointments");

                            // Temp new list
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
                                newDoctorNames.add(obj.getString("doctor_name"));
                                newDoctorSpecialties.add(obj.getString("specialty"));
                                newAppointmentDates.add(obj.getString("appointment_date"));
                                newAppointmentPrices.add("₹ " + obj.getString("fee") + " /-");

                                String profilePicUrl = obj.optString("profile_picture", "");
                                if (profilePicUrl.isEmpty()) {
                                    profilePicUrl = "http://sxm.a58.mytemp.website/doctor_images/default.png";
                                }
                                newDoctorProfilePictures.add(profilePicUrl);

                                newAppointmentStatuses.add(obj.getString("status"));
                            }

                            // Compare with existing
                            if (!newAppointmentIds.equals(appointmentIds) ||
                                    !newAppointmentStatuses.equals(appointmentStatuses)) {

                                doctorIds.clear(); doctorIds.addAll(newDoctorIds);
                                doctorNames.clear(); doctorNames.addAll(newDoctorNames);
                                doctorSpecialties.clear(); doctorSpecialties.addAll(newDoctorSpecialties);
                                appointmentDates.clear(); appointmentDates.addAll(newAppointmentDates);
                                appointmentPrices.clear(); appointmentPrices.addAll(newAppointmentPrices);
                                doctorProfilePictures.clear(); doctorProfilePictures.addAll(newDoctorProfilePictures);
                                appointmentIds.clear(); appointmentIds.addAll(newAppointmentIds);
                                appointmentStatuses.clear(); appointmentStatuses.addAll(newAppointmentStatuses);

                                adapter.notifyDataSetChanged();

                                recyclerView.setAlpha(0f);
                                recyclerView.animate().alpha(1f).setDuration(400).start();
                                // Log.d("HistoryFragment", "History Updated: " + doctorNames.size() + " items");
                            }
                        } else {
                            Toast.makeText(requireContext(), "No past appointments found yet.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        // Log.e("HistoryFragment", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(requireContext(), "Sorry, we could not process your data. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (showLoader) progressBar.setVisibility(View.GONE);
                    // Log.e("HistoryFragment", "Volley error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                    Toast.makeText(requireContext(), "Unable to load your appointments. Please check your internet connection.", Toast.LENGTH_SHORT).show();
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
