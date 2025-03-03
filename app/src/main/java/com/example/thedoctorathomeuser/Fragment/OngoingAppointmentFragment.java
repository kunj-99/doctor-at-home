package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.thedoctorathomeuser.MainActivity;
import com.example.thedoctorathomeuser.Adapter.OngoingAdapter;
import com.example.thedoctorathomeuser.R;

public class OngoingAppointmentFragment extends Fragment {
    private ViewPager vp;
    RecyclerView recyclerView;
    Button bookAppointment;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ongoing_appointment, container, false);

        // Access ViewPager from MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            vp = mainActivity.findViewById(R.id.vp);
        }

        bookAppointment = view.findViewById(R.id.bookButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Dummy data
        String[] names = {"Dr. Tranquilli", "Dr. Smith", "Dr. Patel"};
        String[] specialties = {"Specialist Medicine", "Dermatologist", "Cardiologist"};
        String[] hospitals = {"Patel Hospital", "City Hospital", "Metro Hospital"};
        float[] ratings = {4.0f, 4.5f, 4.2f};
        int[] imageResIds = {R.drawable.main1, R.drawable.main2, R.drawable.main3};

        // Set up the adapter
        OngoingAdapter adapter = new OngoingAdapter(requireContext(), names, specialties, hospitals, ratings, imageResIds);
        recyclerView.setAdapter(adapter);

        bookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vp.setCurrentItem(1);
            }
        });

        return view;
    }
}
