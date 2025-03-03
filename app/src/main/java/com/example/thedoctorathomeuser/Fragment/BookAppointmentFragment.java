package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.Adapter.book_AppointmentAdapter;
import com.example.thedoctorathomeuser.R;

import java.util.Arrays;
import java.util.List;

public class BookAppointmentFragment extends Fragment {

    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_appointment, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);

        // Sample data for degrees and fees
        List<String> degrees = Arrays.asList(
                "BAMS.MS General Physician & Specialist",
                "MBBS General Physician",
                "MBBS MD General Physician & Specialist"
        );

        List<String> fees = Arrays.asList("₹400/-", "₹400/-", "₹400/-");

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new book_AppointmentAdapter(getContext(), degrees, fees));

        return view;
    }
}
