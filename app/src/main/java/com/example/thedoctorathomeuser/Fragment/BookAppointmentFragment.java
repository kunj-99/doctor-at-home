package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.Adapter.book_AppointmentAdapter;
import com.example.thedoctorathomeuser.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookAppointmentFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<String> degrees = new ArrayList<>();
    private List<String> fees = new ArrayList<>();
    private book_AppointmentAdapter adapter;

    private static final String API_URL = "http://yourdomain.com/api/doctor-categories";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_appointment, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new book_AppointmentAdapter(getContext(), degrees, fees);
        recyclerView.setAdapter(adapter);

        fetchDoctorCategories();

        return view;
    }

    private void fetchDoctorCategories() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, API_URL, null,
                response -> {
                    parseDoctorCategories(response);
                },
                error -> Toast.makeText(getContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show());

        queue.add(jsonArrayRequest);
    }

    private void parseDoctorCategories(JSONArray response) {
        degrees.clear();
        fees.clear();

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.getJSONObject(i);
                degrees.add(obj.getString("degree"));
                fees.add("₹" + obj.getString("fee") + "/-");
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
