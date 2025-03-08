package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.book_form;
import com.example.thedoctorathomeuser.doctor_details;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private final Context context;
    private final List<String> names;
    private final List<String> specialties;
    private final List<String> hospitals;
    private final List<Float> ratings;
    private final List<String> imageUrls;

    public DoctorAdapter(Context context, List<String> names, List<String> specialties, List<String> hospitals, List<Float> ratings, List<String> imageUrls) {
        this.context = context;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.name.setText(names.get(position));
        holder.specialty.setText(specialties.get(position));
        holder.hospital.setText(hospitals.get(position));
        holder.ratingBar.setRating(ratings.get(position));

        // Load image using Glide with a placeholder
        Glide.with(context)
                .load(imageUrls.get(position)) // Load from URL
                .placeholder(R.drawable.plasholder) // Ensure `placeholder.png` exists in `res/drawable`
                .error(R.drawable.plaseholder_error) // Show if load fails
                .into(holder.image);

        // Click listener for Doctor Details page
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, doctor_details.class);
            intent.putExtra("doctor_name", names.get(position));
            intent.putExtra("doctor_specialty", specialties.get(position));
            intent.putExtra("doctor_hospital", hospitals.get(position));
            intent.putExtra("doctor_rating", ratings.get(position));
            intent.putExtra("doctor_image", imageUrls.get(position)); // Pass URL
            context.startActivity(intent);
        });

        // Click listener for Book Appointment
        holder.bookButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, book_form.class);
            intent.putExtra("doctor_name", names.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty, hospital;
        RatingBar ratingBar;
        ImageView image;
        Button bookButton;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctor_name);
            specialty = itemView.findViewById(R.id.doctor_specialty);
            hospital = itemView.findViewById(R.id.doctor_availability);
            ratingBar = itemView.findViewById(R.id.doctor_rating);
            image = itemView.findViewById(R.id.civ_profile);
            bookButton = itemView.findViewById(R.id.schedule_button);
        }
    }
}
