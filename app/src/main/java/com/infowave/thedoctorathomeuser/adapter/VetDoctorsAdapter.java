package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.VetAppointmentActivity;
import com.infowave.thedoctorathomeuser.network.DoctorAvailabilityBatchPoller;
import com.infowave.thedoctorathomeuser.network.VolleySingleton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VetDoctorsAdapter extends RecyclerView.Adapter<VetDoctorsAdapter.ViewHolder> {

    public interface OnDoctorClickListener {
        default void onDoctorClick(JSONObject doctor) {}
        default void onBookNowClick(JSONObject doctor) {}
    }

    private static final String TAG = "VET_FLOW_ADAPTER";

    private static final int MAX_REQUESTS = 2;
    private static final Object PAYLOAD_AVAILABILITY = new Object();

    private final Context context;
    private final List<JSONObject> doctors;
    private final int animalCategoryId; // selected animal category (from previous screen)
    private final OnDoctorClickListener listener;
    private final Map<String, DoctorAvailabilityBatchPoller.Status> availabilityByDoctor = new HashMap<>();
    private final DoctorAvailabilityBatchPoller availabilityPoller;
    private int attachedHolderCount = 0;

    public VetDoctorsAdapter(@NonNull Context context,
                             @NonNull List<JSONObject> doctors,
                             int animalCategoryId,
                             @NonNull OnDoctorClickListener listener) {
        this.context = context;
        this.doctors = doctors;
        this.animalCategoryId = animalCategoryId;
        this.listener = listener;
        setHasStableIds(true);

        this.availabilityPoller = new DoctorAvailabilityBatchPoller(
                context,
                () -> {
                    List<String> ids = new ArrayList<>();
                    for (JSONObject doctor : this.doctors) {
                        if (doctor == null) continue;
                        int doctorId = doctor.optInt("doctor_id", 0);
                        if (doctorId > 0) ids.add(String.valueOf(doctorId));
                    }
                    return ids;
                },
                statuses -> {
                    availabilityByDoctor.clear();
                    availabilityByDoctor.putAll(statuses);
                    if (!this.doctors.isEmpty()) {
                        notifyItemRangeChanged(0, this.doctors.size(), PAYLOAD_AVAILABILITY);
                    }
                }
        );
    }

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

        if (d == null) return;

        // ----- NEW: handle multiple animal category IDs (CSV) safely -----
        // API may send:
        //   "animal_category_ids": "1,3,5"
        // or legacy:
        //   "animal_category_id": 2
        String animalIdsCsv = d.optString("animal_category_ids", null);
        if (animalIdsCsv == null || animalIdsCsv.isEmpty()) {
            int single = d.optInt("animal_category_id", -1);
            animalIdsCsv = (single > 0) ? String.valueOf(single) : "";
        }

        boolean supportsSelectedAnimal = false;
        if (animalCategoryId > 0 && animalIdsCsv != null && !animalIdsCsv.isEmpty()) {
            String[] parts = animalIdsCsv.split(",");
            for (String part : parts) {
                try {
                    int parsed = Integer.parseInt(part.trim());
                    if (parsed == animalCategoryId) {
                        supportsSelectedAnimal = true;
                        break;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int id = d.optInt("doctor_id", -1);

        Log.d(TAG, "Bind doctor_id=" + id
                + ", full_name=" + d.optString("full_name", "Doctor")
                + ", animal_category_ids=" + animalIdsCsv
                + ", selected_animal=" + animalCategoryId
                + ", supportsSelected=" + supportsSelectedAnimal);

        String name  = d.optString("full_name", "Doctor");
        String spec  = d.optString("specialization", "Veterinarian");
        double rate  = d.optDouble("rating", 0.0);
        int    expYr = d.optInt("experience_years", 0);
        String loc   = d.optString("doctor_location", "");
        String edu   = d.optString("qualification", d.optString("experience_duration", ""));
        double fee   = d.optDouble("consultation_fee", 0.0);

        // NEW: slot_type from API → "day" / "night"
        String slotType = d.optString("slot_type", "day");

        String img   = d.optString("profile_picture", null);
        String auto  = d.optString("auto_status", "Inactive");

        // IMPORTANT: initialize lastHasActive from the card JSON if provided
        h.lastHasActive = d.optBoolean("has_active_appointment", false);

        h.tvDoctorName.setText(name);
        h.tvSpecialization.setText(spec);
        h.tvRatingText.setText(rate > 0 ? String.valueOf(rate) : "—");
        h.ratingBar.setRating((float) rate);
        h.tvExperience.setText(expYr > 0 ? (expYr + " years") : "—");
        h.tvLocation.setText(loc);
        h.tvEducation.setText(edu);

        // ==== NEW: Show "Day ₹X/-" or "Night ₹Y/-" based on slot_type ====
        String prefix;
        if ("night".equalsIgnoreCase(slotType)) {
            prefix = "Night ";
        } else {
            prefix = "Day ";
        }

        String feeFormatted;
        if (fee > 0) {
            if (fee % 1 == 0) {
                feeFormatted = String.valueOf((int) fee);
            } else {
                feeFormatted = String.format(Locale.getDefault(), "%.2f", fee);
            }
        } else {
            feeFormatted = "0";
        }

        String feeLabel = prefix + "₹" + feeFormatted + "/-";
        h.tvConsultationFee.setText(feeLabel);
        // ==== END NEW FEE LABEL ====

        com.infowave.thedoctorathomeuser.network.SlowImageLoader.load(
                h.ivDoctorImage, img, R.drawable.ic_doctor_placeholder, R.drawable.ic_doctor_placeholder);

        // Initial button state from auto_status
        h.autoStatus = auto;
        if ("inactive".equalsIgnoreCase(auto)) {
            h.setDisabledState("Currently Not Accepting");
        } else {
            // Render CTA using current lastHasActive
            if (h.lastHasActive) {
                h.setSecondaryState("Request for visit");
            } else {
                h.setPrimaryState("Book Appointment");
            }
        }

        // Item click → doctor details (if needed by your listener)
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDoctorClick(d);
        });

        // Book / Request → route based on current button state
        // "Book Appointment"  → free doctor → call reserve_doctor.php first
        // "Request for visit" → ongoing doctor → open form directly (no reserve API needed)
        h.btnBookNow.setOnClickListener(v -> {
            if (h.reservationInFlight) return; // debounce double-tap
            if (listener != null) listener.onBookNowClick(d);
            String btnText = h.btnBookNow.getText().toString();
            if ("Request for visit".equals(btnText)) {
                // Request mode: doctor is ongoing, skip reserve, open form directly
                openVetFormAsRequest(h, id, name, animalCategoryId, auto);
            } else {
                // Direct booking mode: reserve first
                reserveAndOpenVetForm(h, d, id, name, animalCategoryId, auto);
            }
        });

        // One adapter-level batch poller supplies status for all visible/listed doctors.
        h.applyAvailability(availabilityByDoctor.get(String.valueOf(id)));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_AVAILABILITY)) {
            JSONObject doctor = doctors.get(position);
            if (doctor == null) return;
            holder.autoStatus = doctor.optString("auto_status", "Inactive");
            holder.applyAvailability(availabilityByDoctor.get(String.valueOf(doctor.optInt("doctor_id", 0))));
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        attachedHolderCount++;
        if (attachedHolderCount == 1) availabilityPoller.start();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
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
     * Opens VetAppointmentActivity in Request mode (no reserve token).
     * Used when doctor already has an ongoing/Confirmed appointment.
     */
    private void openVetFormAsRequest(ViewHolder h, int doctorId, String doctorName,
                                      int animalCategoryId, String autoStatus) {
        SharedPreferences sp = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientId = sp.getString("patient_id", "");
        if (patientId.isEmpty()) {
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, VetAppointmentActivity.class);
        intent.putExtra("doctor_id",          doctorId);
        intent.putExtra("doctor_name",        doctorName);
        intent.putExtra("animal_category_id", animalCategoryId);
        intent.putExtra("auto_status",        autoStatus);
        intent.putExtra("appointment_status", "Request for visit"); // → "Requested" in pending_bill
        intent.putExtra("reservation_token",  "");   // no token for request mode
        intent.putExtra("patient_id",         patientId);
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Calls reserve_doctor.php for vet flow, then opens VetAppointmentActivity with token.
     */
    private void reserveAndOpenVetForm(ViewHolder h, JSONObject d, int doctorId,
                                        String doctorName, int animalCategoryId, String autoStatus) {
        SharedPreferences sp = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientId = sp.getString("patient_id", "");

        if (patientId.isEmpty()) {
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button during API call
        h.btnBookNow.setEnabled(false);
        h.btnBookNow.setText("Checking availability…");
        h.reservationInFlight = true;

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ApiConfig.RESERVE_DOCTOR,
                response -> {
                    h.reservationInFlight = false;
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean success = obj.optBoolean("success", false);

                        if (success) {
                            String token      = obj.optString("reservation_token", "");
                            int    expiresIn  = obj.optInt("expires_in_seconds", 600);

                            // Restore button
                            h.restoreCtaFromLastState();

                            Toast.makeText(context,
                                    "Doctor reserved for " + (expiresIn / 60) + " minutes. Please complete booking.",
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(context, VetAppointmentActivity.class);
                            intent.putExtra("doctor_id",          doctorId);
                            intent.putExtra("doctor_name",        doctorName);
                            intent.putExtra("animal_category_id", animalCategoryId);
                            intent.putExtra("auto_status",        autoStatus);
                            intent.putExtra("appointment_status", "Book Appointment"); // direct booking
                            // ─── lock extras ───────────────────────────────────────
                            intent.putExtra("reservation_token",  token);
                            intent.putExtra("patient_id",         patientId);

                            if (!(context instanceof android.app.Activity)) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            context.startActivity(intent);

                        } else {
                            String code      = obj.optString("code", "");
                            String message   = obj.optString("message", "Doctor is currently unavailable.");
                            boolean allowReq = obj.optBoolean("allow_request", false);

                            if ("DOCTOR_ONGOING_REQUEST_ALLOWED".equals(code) && allowReq) {
                                // Doctor is ongoing — open Request for visit form directly, no token needed
                                h.setSecondaryState("Request for visit");
                                openVetFormAsRequest(h, doctorId, doctorName, animalCategoryId, autoStatus);
                            } else if ("DOCTOR_INACTIVE".equals(code)) {
                                h.setDisabledState("Currently Not Accepting");
                            } else {
                                // DOCTOR_BUSY or any other failure — restore prior state
                                h.restoreCtaFromLastState();
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        h.reservationInFlight = false;
                        h.restoreCtaFromLastState();
                        Toast.makeText(context, "Unexpected error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    h.reservationInFlight = false;
                    h.restoreCtaFromLastState();
                    Toast.makeText(context, "Connection interrupted. Tap Book Appointment again; an existing reservation will be reused safely.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doctor_id",  String.valueOf(doctorId));
                p.put("patient_id", patientId);
                p.put("is_vet_case", "1");
                return p;
            }
        };

        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.5f));
        RequestQueue q = VolleySingleton.getInstance(context).getRequestQueue();
        q.add(req);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.ivDoctorImage);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDoctorImage;
        TextView tvDoctorName, tvSpecialization, tvRatingText, tvExperience;
        TextView tvLocation, tvEducation, tvConsultationFee;

        TextView tvRequests, tvPending, tvEta;
        RatingBar ratingBar;

        AppCompatButton btnBookNow;
        String autoStatus;
        boolean lastHasActive = false; // remembers latest state for CTA
        int lastRequestCount = 0;
        boolean reservationInFlight = false; // prevents double-tap during reserve API call

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDoctorImage     = itemView.findViewById(R.id.ivDoctorImage);
            tvDoctorName      = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialization  = itemView.findViewById(R.id.tvSpecialization);
            tvRatingText      = itemView.findViewById(R.id.tvRating);
            tvExperience      = itemView.findViewById(R.id.tvExperience);
            tvLocation        = itemView.findViewById(R.id.tvLocation);
            tvEducation       = itemView.findViewById(R.id.tvEducation);
            tvConsultationFee = itemView.findViewById(R.id.tvConsultationFee);
            btnBookNow        = itemView.findViewById(R.id.btnBookNow);
            ratingBar         = itemView.findViewById(R.id.ratingBar);
            tvRequests        = itemView.findViewById(R.id.tvRequests);
            tvPending         = itemView.findViewById(R.id.tvPending);
            tvEta             = itemView.findViewById(R.id.tvEta);
        }

        @SuppressLint("SetTextI18n")
        void applyAvailability(DoctorAvailabilityBatchPoller.Status status) {
            if (status == null) {
                tvRequests.setVisibility(View.GONE);
                tvPending.setVisibility(View.GONE);
                tvEta.setVisibility(View.GONE);
                if (!reservationInFlight) restoreCtaFromLastState();
                return;
            }

            lastHasActive = status.hasActiveAppointment;
            lastRequestCount = status.requestCount;

            tvRequests.setVisibility(View.VISIBLE);
            tvRequests.setText("Visit requests: " + status.requestCount + " / 2");

            if (status.pendingCount > 0) {
                tvPending.setVisibility(View.VISIBLE);
                tvPending.setText("Pending: " + status.pendingCount);
            } else {
                tvPending.setVisibility(View.GONE);
            }

            if (status.totalEtaMinutes > 0) {
                tvEta.setVisibility(View.VISIBLE);
                tvEta.setText(formatEta(status.totalEtaMinutes));
            } else {
                tvEta.setVisibility(View.GONE);
            }

            if (!reservationInFlight) {
                restoreCtaFromLastState();
            }
        }

        void restoreCtaFromLastState() {
            if ("inactive".equalsIgnoreCase(autoStatus)) {
                setDisabledState("Currently Not Accepting");
            } else if (lastRequestCount >= MAX_REQUESTS) {
                setDisabledState("Request limit reached");
            } else if (lastHasActive) {
                setSecondaryState("Request for visit");
            } else {
                setPrimaryState("Book Appointment");
            }
        }

        private static String formatEta(int totalEtaMinutes) {
            if (totalEtaMinutes < 60) {
                return "Next slot in ~" + totalEtaMinutes + " min";
            }
            int hr = totalEtaMinutes / 60;
            int min = totalEtaMinutes % 60;
            return min == 0
                    ? ("Next slot in ~" + hr + " hr")
                    : ("Next slot in ~" + hr + "h " + min + "m");
        }

        void setPrimaryState(String text) {
            btnBookNow.setEnabled(true);
            btnBookNow.setText(text);
            btnBookNow.setBackgroundColor(Color.parseColor("#1976D2")); // primary blue
        }

        void setSecondaryState(String text) {
            btnBookNow.setEnabled(true);
            btnBookNow.setText(text);
            btnBookNow.setBackgroundColor(Color.parseColor("#5494DA")); // lighter blue
        }

        void setDisabledState(String text) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText(text);
            btnBookNow.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
        }
    }
}

// Last Updated: 2026-09-18 14:00 IST
