package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
// import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.book_form;
import com.infowave.thedoctorathomeuser.doctor_details;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;

import org.json.JSONException;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private final Context      context;
    private final List<String> doctorIds, names, specialties, hospitals, imageUrls, durations, autoStatuses;
    private final List<Float>  ratings;

    public DoctorAdapter(Context ctx,
                         List<String> doctorIds,
                         List<String> names,
                         List<String> specialties,
                         List<String> hospitals,
                         List<Float> ratings,
                         List<String> imageUrls,
                         List<String> durations,
                         List<String> autoStatuses) {
        this.context      = ctx;
        this.doctorIds    = doctorIds;
        this.names        = names;
        this.specialties  = specialties;
        this.hospitals    = hospitals;
        this.ratings      = ratings;
        this.imageUrls    = imageUrls;
        this.durations    = durations;
        this.autoStatuses = autoStatuses;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int pos) {
        holder.itemView.setVisibility(View.VISIBLE);

        String id        = doctorIds.get(pos);
        String autoStat  = autoStatuses.get(pos);

        holder.autoStatus = autoStat;

        holder.name.setText(names.get(pos));
        holder.specialty.setText(specialties.get(pos));
        holder.hospital.setText(hospitals.get(pos));
        holder.ratingBar.setRating(ratings.get(pos));
        holder.experienceDuration.setText("Experience: " + durations.get(pos));
        Glide.with(context)
                .load(imageUrls.get(pos))
                .placeholder(R.drawable.plasholder)
                .error(R.drawable.plaseholder_error)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, doctor_details.class);
            i.putExtra("doctor_id", id);
            i.putExtra("doctor_image", imageUrls.get(pos));
            context.startActivity(i);
        });

        holder.bookButton.setOnClickListener(v -> {
            Intent i = new Intent(context, book_form.class);
            i.putExtra("doctor_id", id);
            i.putExtra("doctorName", names.get(pos));
            i.putExtra("appointment_status", holder.bookButton.getText().toString());
            context.startActivity(i);
        });

        holder.startAutoRefresh(id);

        if (autoStat.equalsIgnoreCase("inactive")) {
            holder.bookButton.setText("Currently Not Accepting");
            holder.bookButton.setEnabled(false);
            holder.bookButton.setBackgroundColor(
                    context.getResources().getColor(android.R.color.darker_gray)
            );
        }
    }

    @Override public int getItemCount() { return names.size(); }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView  name, specialty, hospital,
                requestCount, pendingCount,
                experienceDuration, tvEta;
        RatingBar ratingBar;
        ImageView image;
        Button    bookButton;
        String    autoStatus;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable     refreshRunnable;

        public DoctorViewHolder(@NonNull View iv) {
            super(iv);
            name               = iv.findViewById(R.id.doctor_name);
            specialty          = iv.findViewById(R.id.doctor_specialty);
            hospital           = iv.findViewById(R.id.doctor_availability);
            ratingBar          = iv.findViewById(R.id.doctor_rating);
            image              = iv.findViewById(R.id.civ_profile);
            bookButton         = iv.findViewById(R.id.schedule_button);
            requestCount       = iv.findViewById(R.id.request_count);
            pendingCount       = iv.findViewById(R.id.pending_count);
            experienceDuration = iv.findViewById(R.id.doctor_experience_duration);
            tvEta              = iv.findViewById(R.id.tv_eta);
        }

        public void startAutoRefresh(String doctorId) {
            stopAutoRefresh();
            refreshRunnable = () -> {
                checkDoctorAppointmentStatus(doctorId);
                handler.postDelayed(refreshRunnable, 5000);
            };
            handler.post(refreshRunnable);
        }

        public void stopAutoRefresh() {
            if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
        }

        private void checkDoctorAppointmentStatus(String doctorId) {
            String url = ApiConfig.endpoint("checkDoctorAppointment.php", "doctor_id", doctorId);


            @SuppressLint("SetTextI18n") JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        itemView.setVisibility(View.VISIBLE);
                        try {
                            boolean hasActive    = response.getBoolean("has_active_appointment");
                            int reqNum           = response.getInt("request_count");
                            int pendNum          = response.getInt("pending_count");
                            int totalEtaMinutes  = response.optInt("total_eta", 0);

                            requestCount.setVisibility(View.VISIBLE);
                            requestCount.setText("Requests: " + reqNum);

                            if (pendNum > 0) {
                                pendingCount.setVisibility(View.VISIBLE);
                                pendingCount.setText("Pending: " + pendNum);
                            } else {
                                pendingCount.setVisibility(View.GONE);
                            }

                            if (totalEtaMinutes > 0) {
                                tvEta.setVisibility(View.VISIBLE);
                                String friendly;
                                if (totalEtaMinutes < 60) {
                                    friendly = "Next slot in ~" + totalEtaMinutes + " min";
                                } else {
                                    int hrs = totalEtaMinutes / 60;
                                    int mins = totalEtaMinutes % 60;
                                    if (mins == 0) {
                                        friendly = "Next slot in ~" + hrs + " hr";
                                    } else {
                                        friendly = "Next slot in ~" + hrs + "h " + mins + "m";
                                    }
                                }
                                tvEta.setText(friendly);
                            } else {
                                tvEta.setVisibility(View.GONE);
                            }

                            if ("inactive".equalsIgnoreCase(autoStatus)) {
                                bookButton.setText("Currently Not Accepting");
                                bookButton.setEnabled(false);
                                bookButton.setBackgroundColor(
                                        itemView.getResources().getColor(android.R.color.darker_gray)
                                );
                                return;
                            }

                            if (hasActive) {
                                bookButton.setText("Request for visit");
                                bookButton.setBackgroundColor(Color.parseColor("#5494DA"));
                            } else {
                                bookButton.setText("Book Appointment");
                            }
                            bookButton.setEnabled(reqNum < 2);

                        } catch (JSONException e) {
                            resetUI();
                            // Log.e("DoctorAdapter", "JSON parsing error: " + e.getMessage());
                        }
                    },
                    error -> {
                        resetUI();
                        // Log.e("DoctorAdapter", "Volley error: " + error.getMessage());
                    }
            );

            RequestQueue q = VolleySingleton.getInstance(itemView.getContext()).getRequestQueue();
            q.add(req);
        }

        @SuppressLint("SetTextI18n")
        private void resetUI() {
            itemView.setVisibility(View.VISIBLE);
            requestCount.setVisibility(View.GONE);
            pendingCount.setVisibility(View.GONE);
            tvEta.setVisibility(View.GONE);
            bookButton.setText("Book Appointment");
            bookButton.setEnabled(true);
        }
    }
}
