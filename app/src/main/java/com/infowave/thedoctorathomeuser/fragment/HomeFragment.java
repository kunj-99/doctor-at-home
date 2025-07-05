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
import androidx.recyclerview.widget.*;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.*;
import com.infowave.thedoctorathomeuser.*;

import org.json.*;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private home_slaider sliderAdapter;
    private RecyclerView recyclerView, tipRecyclerView, appointmentStatRecyclerView;
    private RecyclerView servicesRecyclerView, articlesRecyclerView;

    private Handler handler;
    private Runnable runnable;
    private int currentPosition = 0;

    // Dynamic slider image URLs
    private List<String> imageUrls = new ArrayList<>();

    // Loader-related fields
    private Handler loaderHandler = new Handler();
    private Runnable loaderRunnable;
    private int pendingRequestCount = 0;
    private final int LOADER_DELAY = 300;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind all RecyclerViews
        recyclerView = view.findViewById(R.id.recyclerView);
        tipRecyclerView = view.findViewById(R.id.tipRecyclerView);
        appointmentStatRecyclerView = view.findViewById(R.id.appointmentStatRecyclerView);
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView);
        articlesRecyclerView = view.findViewById(R.id.articlesRecyclerView);

        // If no network is available, show loader immediately
        if (!isNetworkAvailable()) {
            loaderutil.showLoader(getContext());
            attemptHideLoader();
        }

        pendingRequestCount = 0;
        loaderRunnable = new Runnable() {
            @Override
            public void run() {
                if (pendingRequestCount > 0) {
                    loaderutil.showLoader(getContext());
                }
            }
        };
        loaderHandler.postDelayed(loaderRunnable, LOADER_DELAY);

        setupImageSlider();
        setupHealthTips();
        setupAppointmentStats();
        setupServices();
        setupArticles();

        return view;
    }

    private void setupImageSlider() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.setPadding(8, 0, 8, 0);
        recyclerView.setClipToPadding(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        String url = "http://sxm.a58.mytemp.website/get_slider_images.php";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString("status").equals("success")) {
                                JSONArray sliderImages = response.getJSONArray("slider_images");
                                imageUrls.clear();
                                for (int i = 0; i < sliderImages.length(); i++) {
                                    JSONObject obj = sliderImages.getJSONObject(i);
                                    imageUrls.add(obj.getString("image_url"));
                                }
                                sliderAdapter = new home_slaider(getContext(), imageUrls);
                                recyclerView.setAdapter(sliderAdapter);
                                setupAutoRotation(imageUrls.size());
                            }
                        } catch (JSONException e) {
                            // e.printStackTrace();
                            Toast.makeText(getContext(), "Unable to load banner images. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error.printStackTrace();
                        Toast.makeText(getContext(), "Could not load banner images.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void setupHealthTips() {
        pendingRequestCount++;

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
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
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                String title = jsonObject.getString("title");
                                String description = jsonObject.getString("description");
                                int imageResId = R.drawable.food;
                                tipList.add(new HealthTip(title, description, imageResId));
                            }
                        } catch (JSONException e) {
                            // e.printStackTrace();
                            Toast.makeText(getContext(), "Unable to load health tips.", Toast.LENGTH_SHORT).show();
                        }
                        HealthTipAdapter tipAdapter = new HealthTipAdapter(getContext(), tipList);
                        tipRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                        tipRecyclerView.setAdapter(tipAdapter);
                        decrementAndDismissLoader();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error.printStackTrace();
                        Toast.makeText(getContext(), "Unable to load health tips. Please try again.", Toast.LENGTH_SHORT).show();
                        decrementAndDismissLoader();
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
    }

    private void setupAppointmentStats() {
        pendingRequestCount++;

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        String url = "http://sxm.a58.mytemp.website/completed_appointment.php";

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
                            // e.printStackTrace();
                            Toast.makeText(getContext(), "Could not load appointment stats.", Toast.LENGTH_SHORT).show();
                        }
                        decrementAndDismissLoader();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error.printStackTrace();
                        Toast.makeText(getContext(), "Could not fetch appointment stats.", Toast.LENGTH_SHORT).show();
                        decrementAndDismissLoader();
                    }
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void setupServices() {
        List<ServiceItem> services = new ArrayList<>();
        services.add(new ServiceItem("Medicine Delivery", "Order medicines easily", R.drawable.ic_medicine));
        services.add(new ServiceItem("Pathology Lab Test", "Book lab tests from home", R.drawable.ic_lab));
        servicesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        servicesRecyclerView.setAdapter(new ServiceAdapter(getContext(), services));
    }

    private void setupArticles() {
        pendingRequestCount++;
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
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject articleObject = response.getJSONObject(i);
                                int id = articleObject.getInt("id");
                                String title = articleObject.getString("title");
                                String subtitle = articleObject.optString("subtitle", "");
                                String cover = articleObject.getString("cover");
                                String pdf = articleObject.getString("pdf");
                                articles.add(new ArticleItem(id, title, subtitle, cover, pdf));
                            }
                        } catch (JSONException e) {
                            // e.printStackTrace();
                            Toast.makeText(getContext(), "Unable to load articles.", Toast.LENGTH_SHORT).show();
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
                        // error.printStackTrace();
                        Toast.makeText(getContext(), "Could not fetch articles. Please try again.", Toast.LENGTH_SHORT).show();
                        decrementAndDismissLoader();
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);
    }

    private void setupAutoRotation(int imageCount) {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        handler = new Handler();
        currentPosition = 0;
        runnable = new Runnable() {
            @Override
            public void run() {
                if (imageCount == 0) return;
                if (currentPosition == imageCount) {
                    currentPosition = 0;
                }
                recyclerView.smoothScrollToPosition(currentPosition++);
                handler.postDelayed(this, 3000);
            }
        };
        handler.postDelayed(runnable, 3000);
    }

    private void decrementAndDismissLoader() {
        pendingRequestCount--;
        if (pendingRequestCount <= 0) {
            loaderHandler.removeCallbacks(loaderRunnable);
            attemptHideLoader();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void attemptHideLoader() {
        if (isNetworkAvailable()) {
            loaderutil.hideLoader();
        } else {
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
