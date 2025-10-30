package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.content.Intent;
// import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private final List<Integer> doctorIds;
    private final Context context;

    public OngoingAdapter(Context context, List<String> names, List<String> specialties,
                          List<String> hospitals, List<Float> ratings,
                          List<String> profilePictures, List<Integer> appointmentIds,
                          List<String> statuses, List<String> durations,
                          List<Integer> doctorIds) {
        this.context = context;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.profilePictures = profilePictures;
        this.appointmentIds = appointmentIds;
        this.statuses = statuses;
        this.durations = durations;
        this.doctorIds = doctorIds;
        setHasStableIds(true); // smoother RecyclerView operations
    }

    @Override
    public int getItemViewType(int position) {
        if (names == null || names.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ongoing, parent, false);
            return new DoctorViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyViewHolder) {
            EmptyViewHolder emptyHolder = (EmptyViewHolder) holder;
            Glide.with(context.getApplicationContext())
                    .load(R.drawable.nodataimg)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(emptyHolder.emptyImage);
            emptyHolder.emptyText.setText("No Ongoing Appointments Found");
        } else {
            DoctorViewHolder viewHolder = (DoctorViewHolder) holder;

            // Bind data
            String name = names.get(position);
            String spec = specialties.get(position);
            String hosp = hospitals.get(position);
            float rating = ratings.get(position) == null ? 0f : ratings.get(position);
            String dur = durations.get(position);
            String picUrl = profilePictures.get(position);
            int apptId = appointmentIds.get(position);
            int docId = doctorIds.get(position);
            String statusRaw = statuses.get(position);
            String status = statusRaw == null ? "" : statusRaw.trim().toLowerCase();

            viewHolder.name.setText(name);
            viewHolder.specialty.setText(spec);
            viewHolder.hospital.setText(hosp);
            viewHolder.ratingBar.setRating(rating);
            viewHolder.experienceDuration.setText("Experience: " + (dur == null ? "" : dur));

            Glide.with(context.getApplicationContext())
                    .load(picUrl)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .thumbnail(0.25f)
                    .centerCrop()
                    .dontAnimate()
                    .placeholder(R.drawable.plasholder)
                    .error(R.drawable.plasholder)
                    .into(viewHolder.image);

            // Buttons default state
            viewHolder.track.setVisibility(View.VISIBLE);
            viewHolder.track.setEnabled(true);
            viewHolder.track.setText("Track");
            viewHolder.track.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.navy_blue));

            viewHolder.cancel.setVisibility(View.VISIBLE);
            viewHolder.cancel.setEnabled(true);
            viewHolder.cancel.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.error));

            // Status handling
            if (status.contains("requested")) {
                viewHolder.track.setText("Requested");
                viewHolder.track.setEnabled(false);
                viewHolder.track.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.gray));
            } else if (status.contains("pending")) {
                viewHolder.track.setText("Pending");
                viewHolder.track.setEnabled(false);
                viewHolder.track.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.gray));
            }

            viewHolder.cancel.setOnClickListener(v -> {
                Intent intent = new Intent(context, cancle_appintment.class);
                intent.putExtra("appointment_id", apptId);
                context.startActivity(intent);
                Toast.makeText(context, "You can cancel your appointment here.", Toast.LENGTH_SHORT).show();
            });

            viewHolder.track.setOnClickListener(v -> {
                if (viewHolder.track.isEnabled()) {
                    Intent intent = new Intent(context, track_doctor.class);
                    intent.putExtra("doctor_name", name);
                    intent.putExtra("appointment_id", apptId);
                    intent.putExtra("specialty", spec);
                    intent.putExtra("doctor_id", docId);
                    context.startActivity(intent);
                    Toast.makeText(context, "Tracking your doctor's location...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (names == null || names.isEmpty()) {
            return 1;
        }
        return names.size();
    }

    @Override
    public long getItemId(int position) {
        // Prefer the real appointment id for true stability
        if (appointmentIds != null && position >= 0 && position < appointmentIds.size()) {
            return appointmentIds.get(position);
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof DoctorViewHolder) {
            Glide.with(context.getApplicationContext()).clear(((DoctorViewHolder) holder).image);
        } else if (holder instanceof EmptyViewHolder) {
            Glide.with(context.getApplicationContext()).clear(((EmptyViewHolder) holder).emptyImage);
        }
        super.onViewRecycled(holder);
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
