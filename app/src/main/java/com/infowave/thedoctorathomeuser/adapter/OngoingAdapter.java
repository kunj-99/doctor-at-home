package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.cancle_appintment;
import com.infowave.thedoctorathomeuser.track_doctor;

import java.util.List;

public class OngoingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

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

    @Override
    public int getItemViewType(int position) {
        // If there is no data in the list, return the empty view type.
        if (names == null || names.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY) {
            // Inflate the empty state layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(view);
        } else {
            // Inflate the regular item layout for ongoing appointments
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ongoing, parent, false);
            return new DoctorViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyViewHolder) {
            // Bind the empty view holder: set image and text if needed
            EmptyViewHolder emptyHolder = (EmptyViewHolder) holder;
            Glide.with(context)
                    .load(R.drawable.nodataimg)  // Your empty state image resource
                    .into(emptyHolder.emptyImage);
//            emptyHolder.emptyText.setText("No Appointments Available");
        } else {
            // Bind the regular item data
            DoctorViewHolder viewHolder = (DoctorViewHolder) holder;
            viewHolder.setIsRecyclable(false);

            viewHolder.name.setText(names.get(position));
            viewHolder.specialty.setText(specialties.get(position));
            viewHolder.hospital.setText(hospitals.get(position));
            viewHolder.ratingBar.setRating(ratings.get(position));
            viewHolder.experienceDuration.setText("Experience: " + durations.get(position));

            Glide.with(context)
                    .load(profilePictures.get(position))
                    .placeholder(R.drawable.plasholder)
                    .into(viewHolder.image);

            viewHolder.track.setVisibility(View.VISIBLE);
            viewHolder.track.setEnabled(true);
            viewHolder.track.setText("Track");
            viewHolder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.navy_blue));

            viewHolder.cancel.setVisibility(View.VISIBLE);
            viewHolder.cancel.setEnabled(true);
            viewHolder.cancel.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.error));

            String status = statuses.get(position);
            if (status != null) {
                status = status.trim().toLowerCase();
            } else {
                status = "";
            }

            Log.d("OngoingAdapter", "Appointment ID " + appointmentIds.get(position) + " Status: " + status);

            if (status.contains("requested")) {
                viewHolder.track.setText("Requested");
                viewHolder.track.setEnabled(false);
                viewHolder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
            } else if (status.contains("pending")) {
                viewHolder.track.setText("pending");
                viewHolder.track.setEnabled(false);
                viewHolder.track.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
            }

            viewHolder.cancel.setOnClickListener(v -> {
                Intent intent = new Intent(context, cancle_appintment.class);
                intent.putExtra("appointment_id", appointmentIds.get(position));
                context.startActivity(intent);
            });

            viewHolder.track.setOnClickListener(v -> {
                if (viewHolder.track.isEnabled()) {
                    Intent intent = new Intent(context, track_doctor.class);
                    intent.putExtra("doctor_name", names.get(position));
                    intent.putExtra("appointment_id", appointmentIds.get(position));
                    intent.putExtra("specialty", specialties.get(position));
                    intent.putExtra("doctor_id", doctorIds.get(position)); // Pass doctor_id
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // If the names list is empty, return 1 to display the empty state view.
        if (names == null || names.isEmpty()) {
            return 1;
        }
        return names.size();
    }

    // ViewHolder for the regular ongoing appointment item
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

    // ViewHolder for the empty state view when there is no data
    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        ImageView emptyImage;
        TextView emptyText;

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            emptyImage = itemView.findViewById(R.id.empty_state_image);
            emptyText = itemView.findViewById(R.id.empty_state_text);
        }
    }
}
