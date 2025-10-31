package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log; // ✅ Logging
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.cancle_appintment;
import com.infowave.thedoctorathomeuser.doctor_details;
import com.infowave.thedoctorathomeuser.track_doctor;

import java.util.List;

public class OngoingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "OngoingAdapter";

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

        Log.d(TAG, "Adapter constructed. Sizes -> names=" + sz(names)
                + ", specialties=" + sz(specialties)
                + ", hospitals=" + sz(hospitals)
                + ", ratings=" + sz(ratings)
                + ", profilePictures=" + sz(profilePictures)
                + ", appointmentIds=" + sz(appointmentIds)
                + ", statuses=" + sz(statuses)
                + ", durations=" + sz(durations)
                + ", doctorIds=" + sz(doctorIds));
    }

    private int sz(List<?> l) { return l == null ? 0 : l.size(); }

    @Override
    public int getItemViewType(int position) {
        int type = (names == null || names.isEmpty()) ? VIEW_TYPE_EMPTY : VIEW_TYPE_ITEM;
        Log.d(TAG, "getItemViewType(" + position + ") -> " + (type == VIEW_TYPE_EMPTY ? "EMPTY" : "ITEM"));
        return type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder(viewType=" + (viewType == VIEW_TYPE_EMPTY ? "EMPTY" : "ITEM") + ")");
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
        Log.d(TAG, "onBindViewHolder(position=" + position + ", type="
                + (holder instanceof EmptyViewHolder ? "EMPTY" : "ITEM") + ")");

        if (holder instanceof EmptyViewHolder) {
            EmptyViewHolder emptyHolder = (EmptyViewHolder) holder;
            Log.d(TAG, "Binding EMPTY state");
            Glide.with(context.getApplicationContext())
                    .load(R.drawable.nodataimg)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(emptyHolder.emptyImage);
            emptyHolder.emptyText.setText("No Ongoing Appointments Found");
        } else {
            DoctorViewHolder viewHolder = (DoctorViewHolder) holder;

            // Extract + log values
            String name = safeStr(names, position);
            String spec = safeStr(specialties, position);
            String hosp = safeStr(hospitals, position);
            Float ratingObj = safeFloat(ratings, position);
            float rating = ratingObj == null ? 0f : ratingObj;
            String dur = safeStr(durations, position);
            String picUrl = safeStr(profilePictures, position);
            int apptId = safeInt(appointmentIds, position);
            int docId = safeInt(doctorIds, position);
            String statusRaw = safeStr(statuses, position);
            String status = statusRaw.trim().toLowerCase();

            Log.d(TAG, "Bind item -> pos=" + position
                    + ", doctorId=" + docId
                    + ", appointmentId=" + apptId
                    + ", name=" + name
                    + ", spec=" + spec
                    + ", hosp=" + hosp
                    + ", rating=" + rating
                    + ", duration=" + dur
                    + ", status=" + status
                    + ", imageUrl=" + picUrl);

            // Bind UI
            viewHolder.name.setText(name);
            viewHolder.specialty.setText(spec);
            viewHolder.hospital.setText(hosp);
            viewHolder.ratingBar.setRating(rating);
            viewHolder.experienceDuration.setText("Experience: " + (dur == null ? "" : dur));

            // Image
            Log.d(TAG, "Glide.load -> " + picUrl);
            Glide.with(context.getApplicationContext())
                    .load(picUrl)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .thumbnail(0.25f)
                    .centerCrop()
                    .dontAnimate()
                    .placeholder(R.drawable.plasholder)
                    .error(R.drawable.plasholder)
                    .into(viewHolder.image);

            // Click on whole card → doctor_details (SEND doctor_id AS STRING)
            viewHolder.itemView.setOnClickListener(v -> {
                String docIdStr = String.valueOf(docId); // ✅ convert to String
                Log.d(TAG, "Card clicked -> position=" + position
                        + ", doctorId(int)=" + docId + ", doctorId(str)=" + docIdStr
                        + ", imageUrl=" + picUrl);
                Intent intent = new Intent(context, doctor_details.class);
                intent.putExtra("doctor_id", docIdStr);   // ✅ String
                intent.putExtra("doctor_image", picUrl);
                context.startActivity(intent);
            });

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
                Log.d(TAG, "Status REQUESTED -> disabling Track");
                viewHolder.track.setText("Requested");
                viewHolder.track.setEnabled(false);
                viewHolder.track.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.gray));
            } else if (status.contains("pending")) {
                Log.d(TAG, "Status PENDING -> disabling Track");
                viewHolder.track.setText("Pending");
                viewHolder.track.setEnabled(false);
                viewHolder.track.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.gray));
            }

            viewHolder.cancel.setOnClickListener(v -> {
                Log.d(TAG, "Cancel clicked -> appointmentId=" + apptId + ", doctorId=" + docId);
                Intent intent = new Intent(context, cancle_appintment.class);
                intent.putExtra("appointment_id", apptId);
                context.startActivity(intent);
            });

            viewHolder.track.setOnClickListener(v -> {
                Log.d(TAG, "Track clicked -> enabled=" + viewHolder.track.isEnabled()
                        + ", appointmentId=" + apptId + ", doctorId=" + docId);
                if (viewHolder.track.isEnabled()) {
                    Intent intent = new Intent(context, track_doctor.class);
                    intent.putExtra("doctor_name", name);
                    intent.putExtra("appointment_id", apptId);
                    intent.putExtra("specialty", spec);
                    intent.putExtra("doctor_id", docId); // (track_doctor) unchanged: still int
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        int count = (names == null || names.isEmpty()) ? 1 : names.size();
        Log.d(TAG, "getItemCount() -> " + count);
        return count;
    }

    @Override
    public long getItemId(int position) {
        long id = RecyclerView.NO_ID;
        if (appointmentIds != null && position >= 0 && position < appointmentIds.size()) {
            id = appointmentIds.get(position);
        }
        Log.d(TAG, "getItemId(" + position + ") -> " + id);
        return id;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        Log.d(TAG, "onViewRecycled(holder=" + holder.getClass().getSimpleName() + ")");
        if (holder instanceof DoctorViewHolder) {
            Glide.with(context.getApplicationContext()).clear(((DoctorViewHolder) holder).image);
        } else if (holder instanceof EmptyViewHolder) {
            Glide.with(context.getApplicationContext()).clear(((EmptyViewHolder) holder).emptyImage);
        }
        super.onViewRecycled(holder);
    }

    /* ----------------- Helpers ----------------- */

    private String safeStr(List<String> list, int pos) {
        if (list == null || pos < 0 || pos >= list.size()) return "";
        String v = list.get(pos);
        return v == null ? "" : v;
    }

    private Integer safeInt(List<Integer> list, int pos) {
        if (list == null || pos < 0 || pos >= list.size()) return 0;
        Integer v = list.get(pos);
        return v == null ? 0 : v;
    }

    private Float safeFloat(List<Float> list, int pos) {
        if (list == null || pos < 0 || pos >= list.size()) return 0f;
        Float v = list.get(pos);
        return v == null ? 0f : v;
    }

    /* ----------------- ViewHolders ----------------- */

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
            Log.d(TAG, "DoctorViewHolder constructed");
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        ImageView emptyImage;
        TextView emptyText;

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            emptyImage = itemView.findViewById(R.id.empty_state_image);
            emptyText = itemView.findViewById(R.id.empty_state_text);
            Log.d(TAG, "EmptyViewHolder constructed");
        }
    }
}
