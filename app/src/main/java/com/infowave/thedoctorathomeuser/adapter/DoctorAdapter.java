package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.book_form;
import com.infowave.thedoctorathomeuser.doctor_details;
import com.infowave.thedoctorathomeuser.network.DoctorAvailabilityBatchPoller;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private static final Object PAYLOAD_AVAILABILITY = new Object();

    private final Context      context;
    private final List<String> doctorIds, names, specialties, hospitals, imageUrls, durations, autoStatuses;
    private final List<Float>  ratings;
    private final Map<String, DoctorAvailabilityBatchPoller.Status> availabilityByDoctor = new HashMap<>();
    private final DoctorAvailabilityBatchPoller availabilityPoller;
    private int attachedHolderCount = 0;

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

        this.availabilityPoller = new DoctorAvailabilityBatchPoller(
                ctx,
                () -> new ArrayList<>(this.doctorIds),
                statuses -> {
                    availabilityByDoctor.clear();
                    availabilityByDoctor.putAll(statuses);
                    if (!this.doctorIds.isEmpty()) {
                        notifyItemRangeChanged(0, this.doctorIds.size(), PAYLOAD_AVAILABILITY);
                    }
                }
        );
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
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.load(
                holder.image, imageUrls.get(pos), R.drawable.plasholder, R.drawable.plaseholder_error);

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, doctor_details.class);
            i.putExtra("doctor_id", id);
            i.putExtra("doctor_image", imageUrls.get(pos));
            context.startActivity(i);
        });

        // ── Book button: route based on current button state ─────────────────
        // "Book Appointment" → free doctor → call reserve_doctor.php first
        // "Request for visit" → ongoing doctor → open form directly (no reserve needed)
        holder.bookButton.setOnClickListener(v -> {
            if (holder.reservationInFlight) return; // debounce double-tap
            String btnText = holder.bookButton.getText().toString();
            if ("Request for visit".equals(btnText)) {
                // Request mode: doctor is ongoing, skip reserve, open form directly
                openHumanFormAsRequest(id, names.get(pos));
            } else {
                // Direct booking mode: reserve first
                reserveAndOpenHumanForm(holder, id, names.get(pos));
            }
        });

        holder.applyAvailability(availabilityByDoctor.get(id));
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_AVAILABILITY)) {
            String doctorId = doctorIds.get(position);
            holder.autoStatus = autoStatuses.get(position);
            holder.applyAvailability(availabilityByDoctor.get(doctorId));
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull DoctorViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        attachedHolderCount++;
        if (attachedHolderCount == 1) availabilityPoller.start();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull DoctorViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (attachedHolderCount > 0) attachedHolderCount--;
        if (attachedHolderCount == 0) availabilityPoller.stop();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        attachedHolderCount = 0;
        availabilityPoller.stop();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    /**
     * Opens book_form in Request mode (no reserve token).
     * Used when doctor already has an ongoing/Confirmed appointment.
     */
    private void openHumanFormAsRequest(String doctorId, String doctorName) {
        SharedPreferences sp = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(context, book_form.class);
        i.putExtra("doctor_id", doctorId);
        i.putExtra("doctorName", doctorName);
        i.putExtra("appointment_status", "Request for visit"); // → normalized to "Requested" in pending_bill
        i.putExtra("reservation_token", "");  // no token for request mode
        i.putExtra("patient_id", patientId);
        context.startActivity(i);
    }

    /**
     * Calls reserve_doctor.php, then opens book_form with the reservation_token on success.
     * Shows a DOCTOR_BUSY toast and re-enables the button on failure.
     */
    private void reserveAndOpenHumanForm(DoctorViewHolder holder, String doctorId, String doctorName) {
        // Read patient_id from SharedPreferences
        SharedPreferences sp = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientId = sp.getString("patient_id", "");

        if (patientId.isEmpty()) {
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent double-tap
        holder.bookButton.setEnabled(false);
        holder.bookButton.setText("Checking availability…");
        holder.reservationInFlight = true;

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ApiConfig.RESERVE_DOCTOR,
                response -> {
                    holder.reservationInFlight = false;
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean success = obj.optBoolean("success", false);

                        if (success) {
                            String token = obj.optString("reservation_token", "");
                            int expiresIn = obj.optInt("expires_in_seconds", 600);

                            // Restore button state
                            holder.bookButton.setEnabled(true);
                            holder.bookButton.setText("Book Appointment");

                            Toast.makeText(context,
                                    "Doctor reserved for " + (expiresIn / 60) + " minutes. Please complete booking.",
                                    Toast.LENGTH_SHORT).show();

                            // Open booking form with token
                            Intent i = new Intent(context, book_form.class);
                            i.putExtra("doctor_id", doctorId);
                            i.putExtra("doctorName", doctorName);
                            i.putExtra("appointment_status", "Book Appointment");
                            i.putExtra("reservation_token", token);
                            i.putExtra("patient_id", patientId);
                            context.startActivity(i);

                        } else {
                            String code       = obj.optString("code", "");
                            String message    = obj.optString("message", "Doctor is currently unavailable.");
                            boolean allowReq  = obj.optBoolean("allow_request", false);

                            // Re-enable button so user can interact again
                            holder.bookButton.setEnabled(true);

                            if ("DOCTOR_ONGOING_REQUEST_ALLOWED".equals(code) && allowReq) {
                                // Doctor is ongoing — open Request for visit form directly, no token needed
                                openHumanFormAsRequest(doctorId, doctorName);
                            } else if ("DOCTOR_INACTIVE".equals(code)) {
                                holder.bookButton.setText("Currently Not Accepting");
                                holder.bookButton.setEnabled(false);
                                Toast.makeText(context,
                                        "This doctor is currently not accepting appointments.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                // DOCTOR_BUSY or any other failure — restore latest list-level availability state.
                                holder.restoreCtaFromLastState();
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        holder.bookButton.setEnabled(true);
                        holder.restoreCtaFromLastState();
                        Toast.makeText(context, "Unexpected response. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    holder.reservationInFlight = false;
                    holder.bookButton.setEnabled(true);
                    holder.restoreCtaFromLastState();
                    Toast.makeText(context, "Connection interrupted. Tap Book Appointment again; an existing reservation will be reused safely.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doctor_id",  doctorId);
                p.put("patient_id", patientId);
                p.put("is_vet_case", "0");
                return p;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.5f));
        RequestQueue q = VolleySingleton.getInstance(context).getRequestQueue();
        q.add(req);
    }

    @Override
    public void onViewRecycled(@NonNull DoctorViewHolder holder) {
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.image);
        super.onViewRecycled(holder);
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
        boolean   reservationInFlight = false;
        boolean   lastHasActive = false;
        int       lastRequestCount = 0;

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

        @SuppressLint("SetTextI18n")
        void applyAvailability(DoctorAvailabilityBatchPoller.Status status) {
            itemView.setVisibility(View.VISIBLE);

            if (status == null) {
                requestCount.setVisibility(View.GONE);
                pendingCount.setVisibility(View.GONE);
                tvEta.setVisibility(View.GONE);
                if (!reservationInFlight) restoreCtaFromLastState();
                return;
            }

            lastHasActive = status.hasActiveAppointment;
            lastRequestCount = status.requestCount;

            requestCount.setVisibility(View.VISIBLE);
            requestCount.setText("Visit requests: " + status.requestCount + " / 2");

            if (status.pendingCount > 0) {
                pendingCount.setVisibility(View.VISIBLE);
                pendingCount.setText("Pending: " + status.pendingCount);
            } else {
                pendingCount.setVisibility(View.GONE);
            }

            if (status.totalEtaMinutes > 0) {
                tvEta.setVisibility(View.VISIBLE);
                tvEta.setText(formatEta(status.totalEtaMinutes));
            } else {
                tvEta.setVisibility(View.GONE);
            }

            if (!reservationInFlight) restoreCtaFromLastState();
        }

        void restoreCtaFromLastState() {
            if ("inactive".equalsIgnoreCase(autoStatus)) {
                bookButton.setText("Currently Not Accepting");
                bookButton.setEnabled(false);
                bookButton.setBackgroundColor(
                        itemView.getResources().getColor(android.R.color.darker_gray)
                );
                return;
            }

            if (lastRequestCount >= 2) {
                bookButton.setText("Request limit reached");
                bookButton.setEnabled(false);
                bookButton.setBackgroundColor(
                        itemView.getResources().getColor(android.R.color.darker_gray)
                );
                return;
            }

            if (lastHasActive) {
                bookButton.setText("Request for visit");
                bookButton.setBackgroundColor(Color.parseColor("#5494DA"));
            } else {
                bookButton.setText("Book Appointment");
                bookButton.setBackgroundColor(Color.parseColor("#1976D2"));
            }
            bookButton.setEnabled(true);
        }

        private static String formatEta(int totalEtaMinutes) {
            if (totalEtaMinutes < 60) {
                return "Next slot in ~" + totalEtaMinutes + " min";
            }

            int hrs = totalEtaMinutes / 60;
            int mins = totalEtaMinutes % 60;
            if (mins == 0) {
                return "Next slot in ~" + hrs + " hr";
            }
            return "Next slot in ~" + hrs + "h " + mins + "m";
        }
    }

}

// Last Updated: 2026-09-18 14:00 IST
