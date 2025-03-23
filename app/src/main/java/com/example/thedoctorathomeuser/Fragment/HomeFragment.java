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
        List<HealthTip> tipList = new ArrayList<>();
        tipList.add(new HealthTip("Stay Hydrated", "Drink 8 glasses of water daily", R.drawable.food));
        tipList.add(new HealthTip("Eat Fruits", "Boost your immune system with vitamin C", R.drawable.food));
        tipList.add(new HealthTip("Take Breaks", "Short walks improve blood circulation", R.drawable.food));

        HealthTipAdapter tipAdapter = new HealthTipAdapter(getContext(), tipList);
        tipRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tipRecyclerView.setAdapter(tipAdapter);
    }

    private void setupTopDoctors() {
        List<TopDoctor> doctors = new ArrayList<>();
        doctors.add(new TopDoctor("Dr. Priya Mehta", "Cardiologist", R.drawable.doctor_avatar));
        doctors.add(new TopDoctor("Dr. Arjun Rao", "Dermatologist", R.drawable.doctor_avatar));
        doctors.add(new TopDoctor("Dr. Aisha Khan", "Neurologist", R.drawable.doctor_avatar));
        doctors.add(new TopDoctor("Dr. Rahul Verma", "Orthopedic", R.drawable.doctor_avatar));
        doctors.add(new TopDoctor("Dr. Neha Singh", "Pediatrician", R.drawable.doctor_avatar));

        TopDoctorAdapter doctorAdapter = new TopDoctorAdapter(getContext(), doctors);
        doctorRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        doctorRecyclerView.setAdapter(doctorAdapter);
    }

    private void setupAppointmentStats() {
        List<AppointmentStat> stats = new ArrayList<>();
        stats.add(new AppointmentStat("Appointments Completed", 12, R.drawable.ic_check_circle));

        AppointmentStatAdapter statAdapter = new AppointmentStatAdapter(getContext(), stats);
        appointmentStatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentStatRecyclerView.setAdapter(statAdapter);
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
