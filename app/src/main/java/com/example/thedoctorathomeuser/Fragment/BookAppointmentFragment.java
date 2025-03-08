package com.example.thedoctorathomeuser.Fragment;

import android.os.Bundle;
import android.util.Log;
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
    private List<String> categoryNames = new ArrayList<>();
    private List<String> prices = new ArrayList<>();
    private book_AppointmentAdapter adapter;

    private static final String API_URL = "http://sxm.a58.mytemp.website/bookappointment.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_appointment, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new book_AppointmentAdapter(getContext(), categoryNames, prices);
        recyclerView.setAdapter(adapter);

        fetchDoctorCategories();

        return view;
    }

    private void fetchDoctorCategories() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, API_URL, null,
                response -> {
                    Log.d("API_RESPONSE", "Response: " + response.toString()); // Debugging log
                    parseDoctorCategories(response);
                },
                error -> {
                    Log.e("API_ERROR", "Volley Error: " + error.toString());
                    if (error.networkResponse != null) {
                        String errorData = new String(error.networkResponse.data);
                        Log.e("API_ERROR", "Error Response: " + errorData);
                    }
                    Toast.makeText(getContext(), "Failed to fetch data. Check Logcat.", Toast.LENGTH_LONG).show();
                });

        queue.add(jsonArrayRequest);
    }

    private void parseDoctorCategories(JSONArray response) {
        categoryNames.clear();
        prices.clear();

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.getJSONObject(i);
                categoryNames.add(obj.getString("category_name"));  // Corrected JSON key
                prices.add("₹" + obj.getString("price") + "/-");   // Corrected JSON key
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e("JSON_ERROR", "Parsing Error: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Invalid data format from server", Toast.LENGTH_SHORT).show();
        }
    }
}
