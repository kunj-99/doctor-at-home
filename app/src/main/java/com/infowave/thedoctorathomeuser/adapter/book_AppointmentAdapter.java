package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.available_doctor;
import com.infowave.thedoctorathomeuser.diseases;

import java.util.List;

public class book_AppointmentAdapter extends RecyclerView.Adapter<book_AppointmentAdapter.AppointmentViewHolder> {

    private final Context context;
    private final List<String> categoryNames;
    private final List<String> prices;
    private final List<String> categoryIds;
    private final List<String> categoryImages; // Add images

    // Updated constructor: accept images too
    public book_AppointmentAdapter(Context context, List<String> categoryNames, List<String> prices, List<String> categoryIds, List<String> categoryImages) {
        this.context = context;
        this.categoryNames = categoryNames;
        this.prices = prices;
        this.categoryIds = categoryIds;
        this.categoryImages = categoryImages;
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

        // Load image with Glide
        String imgUrl = categoryImages.get(position);
        if (imgUrl == null || imgUrl.isEmpty()) {
            holder.imageBackground.setImageResource(R.drawable.plaseholder_error); // fallback image
        } else {
            Glide.with(context)
                    .load(imgUrl)
                    .placeholder(R.drawable.plaseholder_error)
                    .error(R.drawable.plaseholder_error)
                    .centerCrop()
                    .into(holder.imageBackground);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            String categoryId = categoryIds.get(position);
            String categoryName = categoryNames.get(position);

            Intent intent = new Intent(context, available_doctor.class);
            intent.putExtra("category_id", categoryId);
            intent.putExtra("category_name", categoryName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categoryNames.size();
    }

    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        ImageView imageBackground;
        TextView categoryTextView, priceTextView;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBackground = itemView.findViewById(R.id.imageBackground);
            categoryTextView = itemView.findViewById(R.id.degreeTextView);
            priceTextView = itemView.findViewById(R.id.feeTextView);
        }
    }
}
