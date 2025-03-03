package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.thedoctorathomeuser.Adapter.home_slaider;
import com.example.thedoctorathomeuser.R;

public class HomeFragment extends Fragment {

    private home_slaider adapter;
    private RecyclerView recyclerView;
    private Handler handler;
    private Runnable runnable;
    private int currentPosition = 0;

    // Example image resources for main slider
    private int[] imageList = {
            R.drawable.main1,
            R.drawable.main2,
            R.drawable.main3,
            R.drawable.main4,
            R.drawable.main5
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize the main RecyclerView for image slider
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new home_slaider(imageList);
        recyclerView.setAdapter(adapter);

        // Set up auto-rotation
        setupAutoRotation();

        return view;
    }

    private void setupAutoRotation() {
        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                if (currentPosition == imageList.length) {
                    // Reset position to loop back to the first image
                    currentPosition = 0;
                }
                recyclerView.smoothScrollToPosition(currentPosition++);
                handler.postDelayed(this, 3000); // Delay of 3 seconds
            }
        };

        handler.postDelayed(runnable, 3000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove callbacks to avoid memory leaks
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
