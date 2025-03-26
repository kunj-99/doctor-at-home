package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.book_form;
import com.example.thedoctorathomeuser.doctor_details;
import com.example.thedoctorathomeuser.network.VolleySingleton;

import org.json.JSONException;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private final Context context;
    private final List<String> doctorIds;
    private final List<String> names;
    private final List<String> specialties;
    private final List<String> hospitals;
    private final List<Float> ratings;
    // List for profile picture URLs
    private final List<String> imageUrls;
    // List for experience duration
    private final List<String> durations;

    public DoctorAdapter(Context context, List<String> doctorIds, List<String> names, List<String> specialties,
                         List<String> hospitals, List<Float> ratings, List<String> imageUrls, List<String> durations) {
        this.context = context;
        this.doctorIds = doctorIds;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.imageUrls = imageUrls;
        this.durations = durations;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        String doctorId = doctorIds.get(position);

        holder.name.setText(names.get(position));
        holder.specialty.setText(specialties.get(position));
        holder.hospital.setText(hospitals.get(position));
        holder.ratingBar.setRating(ratings.get(position));
        holder.experienceDuration.setText("Experience: " + durations.get(position));

        // Load doctor profile image from URL using Glide
        Glide.with(context)
                .load(imageUrls.get(position))
                .placeholder(R.drawable.plasholder)
                .error(R.drawable.plaseholder_error)
                .into(holder.image);

        // Start auto-refresh for appointment status every 5 seconds
        holder.startAutoRefresh(doctorId);

        // Open doctor details when item is clicked
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, doctor_details.class);
            intent.putExtra("doctor_id", doctorId);
            intent.putExtra("doctor_image", imageUrls.get(position));
            context.startActivity(intent);
        });

        // Open book form when button is clicked
        holder.bookButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, book_form.class);
            intent.putExtra("doctor_id", doctorId);
            intent.putExtra("doctorName", names.get(position));
            intent.putExtra("appointment_status", holder.bookButton.getText().toString());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty, hospital, requestCount, pendingCount, experienceDuration;
        RatingBar ratingBar;
        ImageView image;
        Button bookButton;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable refreshRunnable;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctor_name);
            specialty = itemView.findViewById(R.id.doctor_specialty);
            hospital = itemView.findViewById(R.id.doctor_availability);
            ratingBar = itemView.findViewById(R.id.doctor_rating);
            image = itemView.findViewById(R.id.civ_profile);
            bookButton = itemView.findViewById(R.id.schedule_button);
            requestCount = itemView.findViewById(R.id.request_count);
            pendingCount = itemView.findViewById(R.id.pending_count);
            experienceDuration = itemView.findViewById(R.id.doctor_experience_duration);
        }

        public void startAutoRefresh(String doctorId) {
            stopAutoRefresh(); // Remove any existing refresh callbacks

            refreshRunnable = new Runnable() {
                @Override
                public void run() {
                    checkDoctorAppointmentStatus(doctorId);
                    handler.postDelayed(this, 5000);
                }
            };
            handler.post(refreshRunnable);
        }

        public void stopAutoRefresh() {
            if (refreshRunnable != null) {
                handler.removeCallbacks(refreshRunnable);
            }
        }

        private void checkDoctorAppointmentStatus(String doctorId) {
            String url = "http://sxm.a58.mytemp.website/checkDoctorAppointment.php?doctor_id=" + doctorId;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            boolean hasActiveAppointment = response.getBoolean("has_active_appointment");
                            int requestNumber = response.getInt("request_count");
                            int pendingNumber = response.getInt("pending_count");

                            if (requestNumber > 0) {
                                requestCount.setVisibility(View.VISIBLE);
                                requestCount.setText("Requests: " + requestNumber);
                            } else {
                                requestCount.setVisibility(View.GONE);
                            }

                            if (pendingNumber > 0) {
                                pendingCount.setVisibility(View.VISIBLE);
                                pendingCount.setText("Pending: " + pendingNumber);
                            } else {
                                pendingCount.setVisibility(View.GONE);
                            }

                            if (hasActiveAppointment) {
                                bookButton.setText("Request for visit");
                            } else {
                                bookButton.setText("Book Appointment");
                            }
                        } catch (JSONException e) {
                            Log.e("DoctorAdapter", "JSON Parsing error: " + e.getMessage());
                            bookButton.setText("Book Appointment");
                            requestCount.setVisibility(View.GONE);
                            pendingCount.setVisibility(View.GONE);
                        }
                    },
                    error -> {
                        Log.e("DoctorAdapter", "Volley Error: " + error.getMessage());
                        bookButton.setText("Book Appointment");
                        requestCount.setVisibility(View.GONE);
                        pendingCount.setVisibility(View.GONE);
                    });

            RequestQueue queue = VolleySingleton.getInstance(itemView.getContext()).getRequestQueue();
            queue.add(request);
        }
    }

    @Override
    public void onViewRecycled(@NonNull DoctorViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopAutoRefresh();
    }
}
