package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.diseases;

import java.util.List;

public class book_AppointmentAdapter extends RecyclerView.Adapter<book_AppointmentAdapter.AppointmentViewHolder> {

    private final Context context;
    private final List<String> categoryNames;
    private final List<String> prices;
    private final List<String> categoryIds; // Store category IDs

    public book_AppointmentAdapter(Context context, List<String> categoryNames, List<String> prices, List<String> categoryIds) {
        this.context = context;
        this.categoryNames = categoryNames;
        this.prices = prices;
        this.categoryIds = categoryIds;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.categoryTextView.setText(categoryNames.get(position));
        holder.priceTextView.setText(prices.get(position));

        // Set click listener for the RecyclerView item
        holder.itemView.setOnClickListener(v -> {
            String categoryId = categoryIds.get(position); // Fetch category ID
            String categoryName = categoryNames.get(position); // Fetch category Name

            Intent intent = new Intent(context, diseases.class);
            intent.putExtra("category_id", categoryId); // Pass category ID
            intent.putExtra("category_name", categoryName); // Pass category Name
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categoryNames.size();
    }

    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView, priceTextView;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.degreeTextView); // Ensure correct ID
            priceTextView = itemView.findViewById(R.id.feeTextView); // Ensure correct ID
        }
    }
}
