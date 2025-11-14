package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.book_AppointmentAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookAppointmentFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<String> categoryNames = new ArrayList<>();
    private List<String> prices = new ArrayList<>();
    private List<String> categoryIds = new ArrayList<>();
    private List<String> categoryImages = new ArrayList<>(); // Added for images

    private book_AppointmentAdapter adapter;

    private static final String API_URL = ApiConfig.endpoint("bookappointment.php");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_appointment, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        // Use GridLayoutManager for 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);

        adapter = new book_AppointmentAdapter(getContext(), categoryNames, prices, categoryIds, categoryImages);
        recyclerView.setAdapter(adapter);

        fetchDoctorCategories();

        return view;
    }

    private void fetchDoctorCategories() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                API_URL,
                null,
                this::parseDoctorCategories,
                error -> Toast.makeText(
                        getContext(),
                        "Unable to load available categories. Please check your connection.",
                        Toast.LENGTH_LONG
                ).show()
        );

        queue.add(jsonArrayRequest);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void parseDoctorCategories(JSONArray response) {
        categoryNames.clear();
        prices.clear();
        categoryIds.clear();
        categoryImages.clear();

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.getJSONObject(i);

                String id           = obj.getString("id");
                String name         = obj.getString("category_name");
                String price        = obj.getString("price");
                String categoryImg  = obj.optString("category_image", "");

                // NEW: slot_type from backend ("day" / "night") to show before price
                String slotTypeRaw  = obj.optString("slot_type", "");
                String slotLabel;

                if ("night".equalsIgnoreCase(slotTypeRaw)) {
                    slotLabel = "Night ";
                } else if ("day".equalsIgnoreCase(slotTypeRaw)) {
                    slotLabel = "Day ";
                } else {
                    // If older API without slot_type, don’t break UI
                    slotLabel = "";
                }

                categoryIds.add(id);
                categoryNames.add(name);
                // Example: "Day ₹300/-" or "Night ₹400/-"
                prices.add(slotLabel + "₹" + price + "/-");
                categoryImages.add(categoryImg);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(
                    getContext(),
                    "Sorry, something went wrong. Please try again later.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
