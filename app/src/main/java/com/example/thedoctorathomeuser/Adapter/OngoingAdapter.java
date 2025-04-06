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
    private final List<String> profilePictures;
    private final List<Integer> appointmentIds;
    private final List<String> statuses;
    private final List<String> durations;
    private final List<Integer> doctorIds; // Added list for doctor IDs
    private final Context context;

    public OngoingAdapter(Context context, List<String> names, List<String> specialties,
                          List<String> hospitals, List<Float> ratings,
                          List<String> profilePictures, List<Integer> appointmentIds,
                          List<String> statuses, List<String> durations,
                          List<Integer> doctorIds) { // Include doctorIds in constructor
        this.context = context;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.profilePictures = profilePictures;
        this.appointmentIds = appointmentIds;
        this.statuses = statuses;
        this.durations = durations;
        this.doctorIds = doctorIds; // Assign doctorIds
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

        holder.name.setText(names.get(position));
        holder.specialty.setText(specialties.get(position));
        holder.hospital.setText(hospitals.get(position));
        holder.ratingBar.setRating(ratings.get(position));
        holder.experienceDuration.setText("Experience: " + durations.get(position));

        Glide.with(context)
                .load(profilePictures.get(position))
                .placeholder(R.drawable.plasholder)
                .into(holder.image);

        holder.track.setVisibility(View.VISIBLE);
        holder.track.setEnabled(true);
        holder.track.setText("Track");
        holder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.dgreen));

        holder.cancel.setVisibility(View.VISIBLE);
        holder.cancel.setEnabled(true);

        String status = statuses.get(position);
        if (status != null) {
            status = status.trim().toLowerCase();
        } else {
            status = "";
        }

        Log.d("OngoingAdapter", "Appointment ID " + appointmentIds.get(position) + " Status: " + status);

        if (status.contains("requested")) {
            holder.track.setText("Requested");
            holder.track.setEnabled(false);
            holder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
        } else if (status.contains("pending")) {
            holder.track.setText("pending");
            holder.track.setEnabled(false);
            holder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
        }

        holder.cancel.setOnClickListener(v -> {
            Intent intent = new Intent(context, cancle_appintment.class);
            intent.putExtra("appointment_id", appointmentIds.get(position));
            context.startActivity(intent);
        });

        holder.track.setOnClickListener(v -> {
            if (holder.track.isEnabled()) {
                Intent intent = new Intent(context, track_doctor.class);
                intent.putExtra("doctor_name", names.get(position));
                intent.putExtra("appointment_id", appointmentIds.get(position));
                intent.putExtra("specialty", specialties.get(position));
                intent.putExtra("doctor_id", doctorIds.get(position)); // ✅ Pass doctor_id
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty, hospital, experienceDuration;
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
            experienceDuration = itemView.findViewById(R.id.doctor_experience_duration);
        }
    }
}
