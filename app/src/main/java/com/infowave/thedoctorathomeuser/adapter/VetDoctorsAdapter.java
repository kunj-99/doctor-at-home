package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.VetAppointmentActivity;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;

import org.json.JSONObject;

import java.util.List;

public class VetDoctorsAdapter extends RecyclerView.Adapter<VetDoctorsAdapter.ViewHolder> {

    public interface OnDoctorClickListener {
        default void onDoctorClick(JSONObject doctor) {}
        default void onBookNowClick(JSONObject doctor) {}
    }

    private final Context context;
    private final List<JSONObject> doctors;
    private final int animalCategoryId;
    private final OnDoctorClickListener listener;

    /**
     * Use this ctor from your Activity:
     * new VetDoctorsAdapter(this, doctorsList, animalCategoryId, this)
     */
    public VetDoctorsAdapter(@NonNull Context context,
                             @NonNull List<JSONObject> doctors,
                             int animalCategoryId,
                             @NonNull OnDoctorClickListener listener) {
        this.context = context;
        this.doctors = doctors;
        this.animalCategoryId = animalCategoryId;
        this.listener = listener;
        setHasStableIds(true); // Enable stable IDs to improve recycling during frequent refreshes
    }

    // Provide stable IDs based on doctor_id
    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= doctors.size()) return RecyclerView.NO_ID;
        JSONObject d = doctors.get(position);
        return d != null ? d.optInt("doctor_id", -position - 1) : RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vet_doctor, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        JSONObject d = doctors.get(position);
        Context ctx = h.itemView.getContext();

        String name  = d.optString("full_name", "Doctor");
        String spec  = d.optString("specialization", "Veterinarian");
        double rate  = d.optDouble("rating", 0.0);
        int    expYr = d.optInt("experience_years", 0);
        String loc   = d.optString("doctor_location", "");
        String edu   = d.optString("qualification", d.optString("experience_duration", ""));
        double fee   = d.optDouble("consultation_fee", 0.0);
        String img   = d.optString("profile_picture", null);
        String auto  = d.optString("auto_status", "Inactive");
        int    id    = d.optInt("doctor_id", -1);

        h.tvDoctorName.setText(name);
        h.tvSpecialization.setText(spec);
        h.tvRatingText.setText(rate > 0 ? String.valueOf(rate) : "—");
        h.ratingBar.setRating((float) rate);
        h.tvExperience.setText(expYr > 0 ? (expYr + " years") : "—");
        h.tvLocation.setText(loc);
        h.tvEducation.setText(edu);
        h.tvConsultationFee.setText(
                fee > 0 ? "₹" + ((fee % 1 == 0) ? ((int) fee) : String.format("%.2f", fee)) : "₹0"
        );

        if (img != null && !img.trim().isEmpty()) {
            Glide.with(ctx)
                    .load(img)
                    .placeholder(R.drawable.ic_doctor_placeholder)
                    .error(R.drawable.ic_doctor_placeholder)
                    .into(h.ivDoctorImage);
        } else {
            h.ivDoctorImage.setImageResource(R.drawable.ic_doctor_placeholder);
        }

        // Initial state from auto_status
        h.autoStatus = auto;
        if ("inactive".equalsIgnoreCase(auto)) {
            h.btnBookNow.setText("Currently Not Accepting");
            h.btnBookNow.setEnabled(false);
            h.btnBookNow.setBackgroundColor(
                    ContextCompat.getColor(ctx, android.R.color.darker_gray)
            );
        } else {
            h.btnBookNow.setEnabled(true);
            h.btnBookNow.setText("Book Appointment");
            h.btnBookNow.setBackgroundColor(Color.parseColor("#1976D2"));
        }

        // Details tap (optional, delegated)
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDoctorClick(d);
        });

        // Book flow
        h.btnBookNow.setOnClickListener(v -> {
            if (listener != null) listener.onBookNowClick(d);

            Intent intent = new Intent(context, VetAppointmentActivity.class);
            intent.putExtra("doctor_id", id);
            intent.putExtra("doctor_name", name);
            intent.putExtra("animal_category_id", animalCategoryId);

            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });

        // Start auto-refresh of status/counters
        h.startAutoRefresh(String.valueOf(id));
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    // Ensure we stop per-row polling when ViewHolders leave the screen/recycle
    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.stopAutoRefresh();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopAutoRefresh();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDoctorImage;
        TextView tvDoctorName, tvSpecialization, tvRatingText, tvExperience;
        TextView tvLocation, tvEducation, tvConsultationFee;

        TextView tvRequests, tvPending, tvEta;
        RatingBar ratingBar;

        AppCompatButton btnBookNow;
        String autoStatus;

        private final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        private Runnable refreshRunnable;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDoctorImage = itemView.findViewById(R.id.ivDoctorImage);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvRatingText = itemView.findViewById(R.id.tvRating);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvEducation = itemView.findViewById(R.id.tvEducation);
            tvConsultationFee = itemView.findViewById(R.id.tvConsultationFee);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);

            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvRequests = itemView.findViewById(R.id.tvRequests);
            tvPending  = itemView.findViewById(R.id.tvPending);
            tvEta      = itemView.findViewById(R.id.tvEta);
        }

        void startAutoRefresh(String doctorId) {
            stopAutoRefresh();
            refreshRunnable = () -> {
                checkDoctorAppointmentStatus(doctorId);
                handler.postDelayed(refreshRunnable, 5000);
            };
            handler.post(refreshRunnable);
        }

        void stopAutoRefresh() {
            if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
        }

        @SuppressLint("SetTextI18n")
        private void checkDoctorAppointmentStatus(String doctorId) {
            String url = ApiConfig.endpoint("checkDoctorAppointment.php", "doctor_id", doctorId);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        int reqNum  = response.optInt("request_count", 0);
                        int pendNum = response.optInt("pending_count", 0);
                        int etaMin  = response.optInt("total_eta", 0);
                        boolean hasActive = response.optBoolean("has_active_appointment", false);

                        tvRequests.setVisibility(View.VISIBLE);
                        tvRequests.setText("Requests: " + reqNum);

                        if (pendNum > 0) {
                            tvPending.setVisibility(View.VISIBLE);
                            tvPending.setText("Pending: " + pendNum);
                        } else {
                            tvPending.setVisibility(View.GONE);
                        }

                        if (etaMin > 0) {
                            tvEta.setVisibility(View.VISIBLE);
                            String friendly;
                            if (etaMin < 60) friendly = "Next slot in ~" + etaMin + " min";
                            else {
                                int hr = etaMin / 60, m = etaMin % 60;
                                friendly = (m == 0)
                                        ? ("Next slot in ~" + hr + " hr")
                                        : ("Next slot in ~" + hr + "h " + m + "m");
                            }
                            tvEta.setText(friendly);
                        } else {
                            tvEta.setVisibility(View.GONE);
                        }

                        if ("inactive".equalsIgnoreCase(autoStatus)) {
                            btnBookNow.setText("Currently Not Accepting");
                            btnBookNow.setEnabled(false);
                            btnBookNow.setBackgroundColor(
                                    ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray)
                            );
                            return;
                        }

                        if (hasActive) {
                            btnBookNow.setText("Request for visit");
                            btnBookNow.setBackgroundColor(Color.parseColor("#5494DA"));
                        } else {
                            btnBookNow.setText("Book Appointment");
                            btnBookNow.setBackgroundColor(Color.parseColor("#1976D2"));
                        }

                        // throttle
                        btnBookNow.setEnabled(reqNum < 2);
                    },
                    error -> {
                        tvRequests.setVisibility(View.GONE);
                        tvPending.setVisibility(View.GONE);
                        tvEta.setVisibility(View.GONE);
                        if (!"inactive".equalsIgnoreCase(autoStatus)) {
                            btnBookNow.setText("Book Appointment");
                            btnBookNow.setEnabled(true);
                            btnBookNow.setBackgroundColor(Color.parseColor("#1976D2"));
                        }
                    }
            );

            RequestQueue q = VolleySingleton.getInstance(itemView.getContext()).getRequestQueue();
            q.add(req);
        }
    }
}
