package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.thedoctorathomeuser.Adapter.DoctorHistoryAdapter;
import com.example.thedoctorathomeuser.R;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private DoctorHistoryAdapter adapter;

    private String[] doctorNames = {
            "Dr. Tranquilli",
            "Dr. Hetu",
            "Dr. Patel",
            "Dr. Mehta",
            "Dr. Sharma"};

    private String[] doctorSpecialties = {
            "Specialist Medicine", "Specialist Medicine", "Cardiologist", "Neurologist", "Orthopedic"
    };

    private String[] appointmentDates = {
            "21 April, 2023", "29 June, 2024", "10 May, 2023", "12 July, 2023", "15 August, 2023"
    };

    private String[] appointmentPrices = {
            "₹ 540 /-", "₹ 570 /-", "₹ 800 /-", "₹ 1200 /-", "₹ 950 /-"
    };

    private int[] doctorImages = {
            R.drawable.main3, R.drawable.main1, R.drawable.main4, R.drawable.main2, R.drawable.main5
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DoctorHistoryAdapter(requireContext(), doctorNames, doctorSpecialties, appointmentDates, appointmentPrices, doctorImages  );
        recyclerView.setAdapter(adapter);
    }


}
