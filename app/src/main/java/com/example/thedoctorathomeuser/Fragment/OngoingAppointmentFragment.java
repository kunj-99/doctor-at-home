package com.example.thedoctorathomeuser.Fragment;

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
import com.android.volley.Response;
import com.android.volley.VolleyError;
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

    private static final String PATIENT_ID = "2"; // Change to dynamic if needed
    private static final String API_URL = "http://sxm.a58.mytemp.website/getOngoingAppointment.php";
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    private List<String> doctorNames = new ArrayList<>();
    private List<String> specialties = new ArrayList<>();
    private List<String> hospitals = new ArrayList<>();
    private List<Float> ratings = new ArrayList<>();
    private List<Integer> imageResIds = new ArrayList<>();
    private List<Integer> appointmentIds = new ArrayList<>();  // ✅ Added list to store appointment IDs

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

        bookAppointment = view.findViewById(R.id.bookButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // ✅ Updated adapter to include appointmentIds
        adapter = new OngoingAdapter(requireContext(), doctorNames, specialties, hospitals, ratings, imageResIds, appointmentIds);
        recyclerView.setAdapter(adapter);

        fetchOngoingAppointments(); // Initial fetch

        bookAppointment.setOnClickListener(v -> vp.setCurrentItem(1));

        // Auto-refresh every 5 seconds
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

                            doctorNames.clear();
                            specialties.clear();
                            hospitals.clear();
                            ratings.clear();
                            imageResIds.clear();
                            appointmentIds.clear();  // ✅ Clear previous appointment IDs

                            for (int i = 0; i < appointmentsArray.length(); i++) {
                                JSONObject appointment = appointmentsArray.getJSONObject(i);
                                Log.d("APPT_DEBUG", "Appointment: " + appointment.toString());

                                doctorNames.add(appointment.getString("doctor_name"));
                                specialties.add(appointment.getString("specialty"));
                                hospitals.add(appointment.getString("hospital_name"));
                                ratings.add((float) appointment.getDouble("experience"));
                                imageResIds.add(R.drawable.main1);
                                appointmentIds.add(appointment.getInt("appointment_id"));  // ✅ Store appointment ID
                            }

                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                Log.d("ADAPTER_UPDATE", "Adapter updated with " + doctorNames.size() + " items");
                            });

                        } else {
                            Toast.makeText(requireContext(), "No ongoing appointments found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing Error: " + e.getMessage());
                        Toast.makeText(requireContext(), "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR", "Error: " + error.getMessage());
                    Toast.makeText(requireContext(), "Volley Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", PATIENT_ID);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    // 🔄 Start auto-refreshing data every 5 seconds
    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchOngoingAppointments();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    // 🛑 Stop auto-refreshing when fragment is destroyed
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
    }
}
