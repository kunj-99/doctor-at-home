package com.infowave.thedoctorathomeuser.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.loaderutil;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.AppointmentStatAdapter;
import com.infowave.thedoctorathomeuser.adapter.ArticleAdapter;
import com.infowave.thedoctorathomeuser.adapter.HealthTipAdapter;
import com.infowave.thedoctorathomeuser.adapter.ServiceAdapter;
import com.infowave.thedoctorathomeuser.adapter.home_slaider;
import com.infowave.thedoctorathomeuser.ArticleItem;
import com.infowave.thedoctorathomeuser.AppointmentStat;
import com.infowave.thedoctorathomeuser.HealthTip;
import com.infowave.thedoctorathomeuser.ServiceItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private home_slaider sliderAdapter;
    private RecyclerView recyclerView, tipRecyclerView, appointmentStatRecyclerView;
    private RecyclerView servicesRecyclerView, articlesRecyclerView;

    private Handler handler;
    private Runnable runnable;
    private int currentPosition = 0;

    // Loader-related fields
    private Handler loaderHandler = new Handler();
    private Runnable loaderRunnable;
    // Counter to track the number of pending network requests
    private int pendingRequestCount = 0;
    // Delay (in ms) after which the loader will be shown if network calls haven't finished.
    private final int LOADER_DELAY = 300;

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
        //        doctorRecyclerView = view.findViewById(R.id.topDoctorsRecyclerView);
        appointmentStatRecyclerView = view.findViewById(R.id.appointmentStatRecyclerView);
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView);
        articlesRecyclerView = view.findViewById(R.id.articlesRecyclerView);

        // If no network is available, show loader immediately
        if (!isNetworkAvailable()) {
            loaderutil.showLoader(getContext());
            attemptHideLoader(); // begin polling for connectivity
        }

        // Initialize loader logic: post a delayed runnable to show the loader if any network calls are pending.
        pendingRequestCount = 0;
        loaderRunnable = new Runnable() {
            @Override
            public void run() {
                // If there are any pending network calls, show the custom loader.
                if (pendingRequestCount > 0) {
                    loaderutil.showLoader(getContext());
                }
            }
        };
        loaderHandler.postDelayed(loaderRunnable, LOADER_DELAY);

        // Start network requests and UI setups.
        setupImageSlider();
        setupHealthTips();
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
        // Increment pending request count.
        pendingRequestCount++;

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        // Replace with your API endpoint URL.
        String url = "http://sxm.a58.mytemp.website/healthtip.php";

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

                        decrementAndDismissLoader();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        decrementAndDismissLoader();
                    }
                }
        );

        // Add the request to the Volley queue.
        requestQueue.add(jsonArrayRequest);
    }

    //    private void setupTopDoctors() {
    //        // Create a Volley request queue using the fragment's context.
    //        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
    //
    //        // Replace with your actual API endpoint URL that returns top doctor data.
    //        String url = "http://sxm.a58.mytemp.website/topdoctor.php";
    //
    //        // Create a JsonArrayRequest assuming the API returns a JSON array.
    //        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
    //                Request.Method.GET,
    //                url,
    //                null,
    //                new Response.Listener<JSONArray>() {
    //                    @Override
    //                    public void onResponse(JSONArray response) {
    //                        List<TopDoctor> doctors = new ArrayList<>();
    //
    //                        try {
    //                            // Loop through the JSON array and parse each object.
    //                            for (int i = 0; i < response.length(); i++) {
    //                                JSONObject jsonObject = response.getJSONObject(i);
    //                                String fullName = jsonObject.getString("full_name");
    //                                String specialty = jsonObject.getString("category_name"); // assuming API returns category_name as specialty
    //
    //                                // Use a static image resource for now.
    //                                int imageResId = R.drawable.doctor_avatar;
    //
    //                                doctors.add(new TopDoctor(fullName, specialty, imageResId));
    //                            }
    //                        } catch (JSONException e) {
    //                            e.printStackTrace();
    //                        }
    //
    //                        // Create and set the adapter with the fetched data.
    //                        TopDoctorAdapter doctorAdapter = new TopDoctorAdapter(getContext(), doctors);
    //                        doctorRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
    //                        doctorRecyclerView.setAdapter(doctorAdapter);
    //                    }
    //                },
    //                new Response.ErrorListener() {
    //                    @Override
    //                    public void onErrorResponse(VolleyError error) {
    //                        error.printStackTrace();
    //                    }
    //                }
    //        );
    //
    //        // Add the request to the Volley request queue.
    //        requestQueue.add(jsonArrayRequest);
    //    }

    private void setupAppointmentStats() {
        // Increment pending request count.
        pendingRequestCount++;

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        // Replace with your API endpoint URL for completed appointment count.
        String url = "http://sxm.a58.mytemp.website/completed_appointment.php";

        // Create a JsonObjectRequest since the API returns a JSON object.
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int count = response.getInt("completed_count");
                            List<AppointmentStat> stats = new ArrayList<>();
                            stats.add(new AppointmentStat("Appointments Completed", count, R.drawable.ic_check_circle));

                            AppointmentStatAdapter statAdapter = new AppointmentStatAdapter(getContext(), stats);
                            appointmentStatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                            appointmentStatRecyclerView.setAdapter(statAdapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        decrementAndDismissLoader();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        decrementAndDismissLoader();
                    }
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void setupServices() {
        List<ServiceItem> services = new ArrayList<>();
//        services.add(new ServiceItem("Doctor Home Visit", "Specialist Doctor at your Doorstep", R.drawable.ic_doctor_home));
        services.add(new ServiceItem("Medicine Delivery", "Order medicines easily", R.drawable.ic_medicine));
        services.add(new ServiceItem("Pathology Lab Test", "Book lab tests from home", R.drawable.ic_lab));

        servicesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        servicesRecyclerView.setAdapter(new ServiceAdapter(getContext(), services));
    }

    private void setupArticles() {
        // Increment pending request count.
        pendingRequestCount++;

        // URL of your PHP endpoint that returns articles in JSON format.
        String url = "http://sxm.a58.mytemp.website/get_articles.php";
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<ArticleItem> articles = new ArrayList<>();
                        try {
                            // Loop through each JSON object in the array.
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject articleObject = response.getJSONObject(i);
                                int id = articleObject.getInt("id");
                                String title = articleObject.getString("title");
                                // If "subtitle" is provided in the JSON, use it; otherwise, default to an empty string.
                                String subtitle = articleObject.optString("title", "");
                                String cover = articleObject.getString("cover"); // full URL provided by PHP
                                String pdf = articleObject.getString("pdf");     // full URL provided by PHP

                                articles.add(new ArticleItem(id, title, subtitle, cover, pdf));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        articlesRecyclerView.setLayoutManager(
                                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
                        );
                        articlesRecyclerView.setAdapter(new ArticleAdapter(getContext(), articles));
                        decrementAndDismissLoader();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getContext(), "Error fetching articles", Toast.LENGTH_SHORT).show();
                        decrementAndDismissLoader();
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
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

    /**
     * Utility method to decrement the counter and dismiss the loader if all network requests are done.
     * If there's no connectivity, keep checking until the network is back before hiding the loader.
     */
    private void decrementAndDismissLoader() {
        pendingRequestCount--;
        if (pendingRequestCount <= 0) {
            // Remove the loader runnable if it hasn't executed yet.
            loaderHandler.removeCallbacks(loaderRunnable);
            attemptHideLoader();
        }
    }

    /**
     * Checks if the network is available.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * Attempts to hide the loader. If the network is still not available, checks again after a delay.
     */
    private void attemptHideLoader() {
        if (isNetworkAvailable()) {
            loaderutil.hideLoader();
        } else {
            // Retry after 1 second until the network is available.
            loaderHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    attemptHideLoader();
                }
            }, 1000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
