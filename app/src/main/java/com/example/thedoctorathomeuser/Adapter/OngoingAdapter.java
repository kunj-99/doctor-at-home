package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private final List<Integer> appointmentIds;
    private final List<String> statuses; // List for appointment statuses
    private final Context context;

    // Constructor including statuses
    public OngoingAdapter(Context context, List<String> names, List<String> specialties,
                          List<String> hospitals, List<Float> ratings,
                          List<Integer> imageResIds, List<Integer> appointmentIds, List<String> statuses) {
        this.context = context;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.imageResIds = imageResIds;
        this.appointmentIds = appointmentIds;
        this.statuses = statuses;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ongoing, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.setIsRecyclable(false);

        // Set basic info
        holder.name.setText(names.get(position));
        holder.specialty.setText(specialties.get(position));
        holder.hospital.setText(hospitals.get(position));
        holder.ratingBar.setRating(ratings.get(position));

        // Load image using Glide
        Glide.with(context)
                .load(imageResIds.get(position))
                .placeholder(R.drawable.plasholder)
                .into(holder.image);

        // Reset Track button to default state (visible, enabled, and with default blue tint)
        holder.track.setVisibility(View.VISIBLE);
        holder.track.setEnabled(true);
        holder.track.setText("Track");
        holder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));

        // Cancel button remains unchanged (always visible and enabled)
        holder.cancel.setVisibility(View.VISIBLE);
        holder.cancel.setEnabled(true);

        // Get the appointment status, trim and convert to lowercase
        String status = statuses.get(position);
        if (status != null) {
            status = status.trim().toLowerCase();
        } else {
            status = "";
        }

        // Log the status value to check if it's being received correctly
        Log.d("OngoingAdapter", "Appointment ID " + appointmentIds.get(position) + " Status: " + status);

        // Modify the Track button based on the appointment status using contains() for flexible matching
        if (status.contains("requested")) {
            holder.track.setText("riqes is prosesing");
            holder.track.setEnabled(false);
            // Set the disabled color (gray, for example)
            holder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
        } else if (status.contains("pending")) {
            holder.track.setText("panding");
            holder.track.setEnabled(false);
            // Set the disabled color (gray, for example)
            holder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
        }
        // For any other status, the Track button remains "Track", enabled, and tinted blue.

        // Cancel button click listener
        holder.cancel.setOnClickListener(v -> {
            Intent intent = new Intent(context, cancle_appintment.class);
            intent.putExtra("appointment_id", appointmentIds.get(position));
            context.startActivity(intent);
        });

        // Track button click listener (only works if the button is enabled)
        holder.track.setOnClickListener(v -> {
            if (holder.track.isEnabled()) {
                Intent intent = new Intent(context, track_doctor.class);
                intent.putExtra("doctor_name", names.get(position));
                intent.putExtra("appointment_id", appointmentIds.get(position));
                intent.putExtra("specialty", specialties.get(position));
                context.startActivity(intent);
            }
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
