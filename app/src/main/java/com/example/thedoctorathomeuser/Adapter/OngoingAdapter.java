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
import com.example.thedoctorathomeuser.cancle_appintment;
import com.example.thedoctorathomeuser.track_doctor;

import java.util.List;

public class OngoingAdapter extends RecyclerView.Adapter<OngoingAdapter.DoctorViewHolder> {

    private final List<String> names;
    private final List<String> specialties;
    private final List<String> hospitals;
    private final List<Float> ratings;
    private final List<Integer> imageResIds;
    private final List<Integer> appointmentIds;  // ✅ Added List to store appointment IDs
    private final Context context;

    // Constructor (Now accepts appointmentIds)
    public OngoingAdapter(Context context, List<String> names, List<String> specialties,
                          List<String> hospitals, List<Float> ratings,
                          List<Integer> imageResIds, List<Integer> appointmentIds) {
        this.context = context;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.imageResIds = imageResIds;
        this.appointmentIds = appointmentIds;  // ✅ Initialize appointment IDs
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ongoing, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.setIsRecyclable(false); // Prevents incorrect data binding

        holder.name.setText(names.get(position));
        holder.specialty.setText(specialties.get(position));
        holder.hospital.setText(hospitals.get(position));
        holder.ratingBar.setRating(ratings.get(position));

        // Load image using Glide
        Glide.with(context)
                .load(imageResIds.get(position))
                .placeholder(R.drawable.plasholder) // Default placeholder
                .into(holder.image);

        // Handle Cancel button click (Pass appointment_id)
        holder.cancel.setOnClickListener(v -> {
            Intent intent = new Intent(context, cancle_appintment.class);
            intent.putExtra("appointment_id", appointmentIds.get(position)); // ✅ Pass appointment_id
            context.startActivity(intent);
        });

        // Handle Track button click
        holder.track.setOnClickListener(v -> {
            Intent intent = new Intent(context, track_doctor.class);
            intent.putExtra("doctor_name", names.get(position));
            intent.putExtra("specialty", specialties.get(position));
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
        Button track, cancel;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctor_name);
            specialty = itemView.findViewById(R.id.doctor_specialty);
            hospital = itemView.findViewById(R.id.doctor_availability);
            ratingBar = itemView.findViewById(R.id.doctor_rating);
            image = itemView.findViewById(R.id.civ_profile);
            track = itemView.findViewById(R.id.Track_button);
            cancel = itemView.findViewById(R.id.Cancel_button);
        }
    }
}
