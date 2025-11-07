package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
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
    private final List<String> durations;            // e.g., "6 Years"
    private final List<Integer> doctorIds;
    private final List<Integer> experienceYears;     // numeric (kept for completeness)
    private final List<String> appointmentTimeDisplays; // e.g., "Thu, 09 Oct · 02:15 PM"
    private final List<Float> amounts;               // e.g., 650.0f

    private final Context context;

    public OngoingAdapter(Context context,
                          List<String> names,
                          List<String> specialties,
                          List<String> hospitals,
                          List<Float> ratings,
                          List<String> profilePictures,
                          List<Integer> appointmentIds,
                          List<String> statuses,
                          List<String> durations,
                          List<Integer> doctorIds,
                          List<Integer> experienceYears,
                          List<String> appointmentTimeDisplays,
                          List<Float> amounts) {

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
        this.experienceYears = experienceYears;
        this.appointmentTimeDisplays = appointmentTimeDisplays;
        this.amounts = amounts;

        setHasStableIds(true);

        Log.d(TAG, "Adapter constructed. sizes: names=" + sz(names)
                + " specs=" + sz(specialties)
                + " hosp=" + sz(hospitals)
                + " ratings=" + sz(ratings)
                + " pics=" + sz(profilePictures)
                + " apptIds=" + sz(appointmentIds)
                + " statuses=" + sz(statuses)
                + " durations=" + sz(durations)
                + " doctorIds=" + sz(doctorIds)
                + " expYears=" + sz(experienceYears)
                + " timeDisp=" + sz(appointmentTimeDisplays)
                + " amounts=" + sz(amounts));
    }

    private int sz(List<?> l) { return l == null ? 0 : l.size(); }

    @Override
    public int getItemViewType(int position) {
        return (names == null || names.isEmpty()) ? VIEW_TYPE_EMPTY : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ongoing, parent, false);
            return new DoctorViewHolder(v);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (h instanceof EmptyViewHolder) {
            EmptyViewHolder eh = (EmptyViewHolder) h;
            Glide.with(context.getApplicationContext())
                    .load(R.drawable.nodataimg)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(eh.emptyImage);
            eh.emptyText.setText("No Ongoing Appointments Found");
            return;
        }

        DoctorViewHolder v = (DoctorViewHolder) h;

        String name = safeStr(names, position);
        String spec = safeStr(specialties, position);
        String hosp = safeStr(hospitals, position);
        float rating = clamp0to5(safeFloat(ratings, position));
        String durationText = safeStr(durations, position);   // already "6 Years"
        int years = safeInt(experienceYears, position);       // numeric (not displayed directly)
        String timeDisp = safeStr(appointmentTimeDisplays, position);
        float amount = safeFloat(amounts, position);

        String picUrl = safeStr(profilePictures, position);
        int apptId = safeInt(appointmentIds, position);
        int docId = safeInt(doctorIds, position);
        String status = safeStr(statuses, position).trim().toLowerCase();

        // Bind top info
        v.name.setText(name);
        v.specialty.setText(spec);
        v.hospital.setText(hosp);

        // Experience
        v.experienceDuration.setText(durationText.isEmpty() ? "" : ("Experience: " + durationText));

        // Rating
        if (rating <= 0f) {
            v.ratingBar.setVisibility(View.GONE);
        } else {
            v.ratingBar.setVisibility(View.VISIBLE);
            v.ratingBar.setIsIndicator(true);
            v.ratingBar.setStepSize(0.5f);
            v.ratingBar.setRating(rating);
        }

        // time and amount
        v.tvAppointmentTime.setText(timeDisp);
        v.tvConsultationFee.setText("₹" + trimTrailingZeros(amount));

        // Image
        Glide.with(context.getApplicationContext())
                .load(picUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .thumbnail(0.25f)
                .centerCrop()
                .dontAnimate()
                .placeholder(R.drawable.plasholder)
                .error(R.drawable.plasholder)
                .into(v.image);

        // Card click → details
        v.itemView.setOnClickListener(view -> {
            Intent i = new Intent(context, doctor_details.class);
            i.putExtra("doctor_id", String.valueOf(docId)); // keep behavior consistent with details screen
            i.putExtra("doctor_image", picUrl);
            context.startActivity(i);
        });

        // Buttons default state
        v.track.setVisibility(View.VISIBLE);
        v.track.setEnabled(true);
        v.track.setText("Track");
        v.track.setBackgroundTintList(
                ContextCompat.getColorStateList(context, R.color.navy_blue));

        v.cancel.setVisibility(View.VISIBLE);
        v.cancel.setEnabled(true);
        v.cancel.setBackgroundTintList(
                ContextCompat.getColorStateList(context, R.color.error));

        if (status.contains("requested")) {
            v.track.setText("Requested");
            v.track.setEnabled(false);
            v.track.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.gray));
        } else if (status.contains("pending")) {
            v.track.setText("Pending");
            v.track.setEnabled(false);
            v.track.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.gray));
        }

        v.cancel.setOnClickListener(view -> {
            Intent i = new Intent(context, cancle_appintment.class);
            i.putExtra("appointment_id", apptId);
            context.startActivity(i);
        });

        // ✅ TRACK INTENT — send all essentials consistently as String extras
        v.track.setOnClickListener(view -> {
            if (!v.track.isEnabled()) return;
            Intent i = new Intent(context, track_doctor.class);
            i.putExtra("appointment_id", String.valueOf(apptId));
            i.putExtra("doctor_id", String.valueOf(docId));
            i.putExtra("doctor_name", name);
            i.putExtra("specialty", spec);
            i.putExtra("doctor_image", picUrl);
            // If `track_doctor` expects anything else (e.g., hospital or time), uncomment:
            // i.putExtra("hospital", hosp);
            // i.putExtra("appointment_time_display", timeDisp);
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return (names == null || names.isEmpty()) ? 1 : names.size();
    }

    @Override
    public long getItemId(int position) {
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

    /* ============== Helpers ============== */

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

    private float clamp0to5(float r) {
        if (r < 0f) return 0f;
        if (r > 5f) return 5f;
        return r;
    }

    private String trimTrailingZeros(float value) {
        String s = String.valueOf(value);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.isEmpty() ? "0" : s;
    }

    /* ============== ViewHolders ============== */

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty, hospital, experienceDuration;
        TextView tvAppointmentTime, tvConsultationFee;
        RatingBar ratingBar;
        ImageView image;
        Button track, cancel;

        DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctor_name);
            specialty = itemView.findViewById(R.id.doctor_specialty);
            hospital = itemView.findViewById(R.id.doctor_availability);
            ratingBar = itemView.findViewById(R.id.doctor_rating);
            image = itemView.findViewById(R.id.civ_profile);
            track = itemView.findViewById(R.id.Track_button);
            cancel = itemView.findViewById(R.id.Cancel_button);
            experienceDuration = itemView.findViewById(R.id.doctor_experience_duration);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvConsultationFee = itemView.findViewById(R.id.tvConsultationFee);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        ImageView emptyImage;
        TextView emptyText;

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            emptyImage = itemView.findViewById(R.id.empty_state_image);
            emptyText = itemView.findViewById(R.id.empty_state_text);
        }
    }
}
