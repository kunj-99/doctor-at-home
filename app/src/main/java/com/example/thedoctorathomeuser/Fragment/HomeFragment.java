package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;  // Added missing import
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.Adapter.AppointmentStatAdapter;
import com.example.thedoctorathomeuser.Adapter.ArticleAdapter;
import com.example.thedoctorathomeuser.Adapter.HealthTipAdapter;
import com.example.thedoctorathomeuser.Adapter.ServiceAdapter;
import com.example.thedoctorathomeuser.Adapter.TopDoctorAdapter;
import com.example.thedoctorathomeuser.Adapter.home_slaider;
import com.example.thedoctorathomeuser.ArticleItem;
import com.example.thedoctorathomeuser.AppointmentStat;
import com.example.thedoctorathomeuser.HealthTip;
import com.example.thedoctorathomeuser.ServiceItem;
import com.example.thedoctorathomeuser.TopDoctor;
import com.example.thedoctorathomeuser.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private home_slaider sliderAdapter;
    private RecyclerView recyclerView, tipRecyclerView, doctorRecyclerView, appointmentStatRecyclerView;
    private RecyclerView servicesRecyclerView, articlesRecyclerView;

    private Handler handler;
    private Runnable runnable;
    private int currentPosition = 0;

    private final int[] imageList = {
            R.drawable.main1,
            R.drawable.main2,
            R.drawable.main3,
            R.drawable.main4,
            R.drawable.main5
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind all RecyclerViews
        recyclerView = view.findViewById(R.id.recyclerView);
        tipRecyclerView = view.findViewById(R.id.tipRecyclerView);
        doctorRecyclerView = view.findViewById(R.id.topDoctorsRecyclerView);
        appointmentStatRecyclerView = view.findViewById(R.id.appointmentStatRecyclerView);
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView);
        articlesRecyclerView = view.findViewById(R.id.articlesRecyclerView);

        setupImageSlider();
        setupHealthTips();
        setupTopDoctors();
        setupAppointmentStats();
        setupServices();
        setupArticles();
        setupAutoRotation();

        return view;
    }

    private void setupImageSlider() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        sliderAdapter = new home_slaider(imageList);
        recyclerView.setAdapter(sliderAdapter);
    }

    private void setupHealthTips() {
        // Create a Volley request queue using the fragment's context.
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        // Replace with your API endpoint URL.
        String url = "http://sxm.a58.mytemp.website/healthtip.php";

        // Create a JsonArrayRequest (assuming the API returns a JSON array)
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<HealthTip> tipList = new ArrayList<>();

                        try {
                            // Loop through the JSON array and parse each object.
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String title = jsonObject.getString("title");
                                String description = jsonObject.getString("description");

                                // Use a static image resource for now.
                                int imageResId = R.drawable.food;

                                tipList.add(new HealthTip(title, description, imageResId));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Create and set the adapter with the fetched data.
                        HealthTipAdapter tipAdapter = new HealthTipAdapter(getContext(), tipList);
                        tipRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                        tipRecyclerView.setAdapter(tipAdapter);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        );

        // Add the request to the Volley queue.
        requestQueue.add(jsonArrayRequest);
    }

    private void setupTopDoctors() {
        // Create a Volley request queue using the fragment's context.
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        // Replace with your actual API endpoint URL that returns top doctor data.
        String url = "http://sxm.a58.mytemp.website/topdoctor.php";

        // Create a JsonArrayRequest assuming the API returns a JSON array.
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<TopDoctor> doctors = new ArrayList<>();

                        try {
                            // Loop through the JSON array and parse each object.
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String fullName = jsonObject.getString("full_name");
                                String specialty = jsonObject.getString("category_name"); // assuming API returns category_name as specialty

                                // Use a static image resource for now.
                                int imageResId = R.drawable.doctor_avatar;

                                doctors.add(new TopDoctor(fullName, specialty, imageResId));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Create and set the adapter with the fetched data.
                        TopDoctorAdapter doctorAdapter = new TopDoctorAdapter(getContext(), doctors);
                        doctorRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
                        doctorRecyclerView.setAdapter(doctorAdapter);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        );

        // Add the request to the Volley request queue.
        requestQueue.add(jsonArrayRequest);
    }

    private void setupAppointmentStats() {
        // Create a Volley request queue using the fragment's context.
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        // Replace with your API endpoint URL for completed appointment count.
        String url = "http://sxm.a58.mytemp.website/completed_appointment.php";
        // Log the URL being used
        android.util.Log.d("AppointmentStats", "Requesting completed appointment count from: " + url);

        // Create a JsonObjectRequest since the API returns a JSON object.
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Log the received response
                        android.util.Log.d("AppointmentStats", "Response received: " + response.toString());
                        try {
                            int count = response.getInt("completed_count");
                            // Log the count value
                            android.util.Log.d("AppointmentStats", "Completed count: " + count);

                            List<AppointmentStat> stats = new ArrayList<>();
                            stats.add(new AppointmentStat("Appointments Completed", count, R.drawable.ic_check_circle));

                            AppointmentStatAdapter statAdapter = new AppointmentStatAdapter(getContext(), stats);
                            appointmentStatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                            appointmentStatRecyclerView.setAdapter(statAdapter);
                        } catch (JSONException e) {
                            // Log the JSON parsing error
                            android.util.Log.e("AppointmentStats", "JSON parsing error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Log the Volley error
                        android.util.Log.e("AppointmentStats", "Volley error: " + error.toString());
                        error.printStackTrace();
                    }
                }
        );

        // Add the request to the Volley queue.
        requestQueue.add(jsonObjectRequest);
    }


    private void setupServices() {
        List<ServiceItem> services = new ArrayList<>();
        services.add(new ServiceItem("Doctor Home Visit", "Specialist Doctor at your Doorstep", R.drawable.ic_doctor_home));
        services.add(new ServiceItem("Medicine Delivery", "Order medicines easily", R.drawable.ic_medicine));
        services.add(new ServiceItem("Pathology Lab Test", "Book lab tests from home", R.drawable.ic_lab));

        servicesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        servicesRecyclerView.setAdapter(new ServiceAdapter(getContext(), services));
    }

    private void setupArticles() {
        List<ArticleItem> articles = new ArrayList<>();
        articles.add(new ArticleItem("Iron Supplements for Women", "7 min(s) read", R.drawable.article1));
        articles.add(new ArticleItem("Oil Pulling: Traditional Remedy", "5 min(s) read", R.drawable.article2));

        articlesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        articlesRecyclerView.setAdapter(new ArticleAdapter(getContext(), articles));
    }

    private void setupAutoRotation() {
        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                if (currentPosition == imageList.length) {
                    currentPosition = 0;
                }
                recyclerView.smoothScrollToPosition(currentPosition++);
                handler.postDelayed(this, 3000);
            }
        };

        handler.postDelayed(runnable, 3000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
