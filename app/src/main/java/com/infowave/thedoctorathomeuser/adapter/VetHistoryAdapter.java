package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.RefundStatus;
import com.infowave.thedoctorathomeuser.complet_bill;
import com.infowave.thedoctorathomeuser.doctor_details; // For vet details, swap class if you have
import com.infowave.thedoctorathomeuser.medical_riport;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VetHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM  = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

    private final Context context;
    private final String patientId;

    // Data lists (doctor fields reused for vet)
    private final List<Integer> vetIds;            // doctor_id
    private final List<String>  vetNames;          // doctor_name
    private final List<String>  vetSpecialties;    // doctor_specialty
    private final List<String>  appointmentDates;
    private final List<String>  appointmentPrices;
    private final List<String>  vetProfilePictures;// doctor_profile_picture
    private final List<Integer> appointmentIds;
    private final List<String>  appointmentStatuses;

    // Review endpoints
    private static final String REVIEW_API_URL        = ApiConfig.endpoint("submit_vet_review.php");
    private static final String CHECK_REVIEW_API_URL  = ApiConfig.endpoint("check_vet_review_status.php");

    private final Set<Integer> reviewPopupShown = new HashSet<>();

    public VetHistoryAdapter(Context context, String patientId,
                             List<Integer> vetIds, List<String> vetNames, List<String> vetSpecialties,
                             List<String> appointmentDates, List<String> appointmentPrices,
                             List<String> vetProfilePictures, List<Integer> appointmentIds,
                             List<String> appointmentStatuses) {
        this.context = context;
        this.patientId = patientId;
        this.vetIds = vetIds;
        this.vetNames = vetNames;
        this.vetSpecialties = vetSpecialties;
        this.appointmentDates = appointmentDates;
        this.appointmentPrices = appointmentPrices;
        this.vetProfilePictures = vetProfilePictures;
        this.appointmentIds = appointmentIds;
        this.appointmentStatuses = appointmentStatuses;
    }

    @Override
    public int getItemViewType(int position) {
        return (vetNames == null || vetNames.isEmpty()) ? VIEW_TYPE_EMPTY : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_EMPTY) {
            EmptyViewHolder h = (EmptyViewHolder) holder;
            Glide.with(context).load(R.drawable.nodataimg).into(h.emptyImage);
            h.emptyText.setText("No Vet Appointment History Available");
            return;
        }

        ViewHolder h = (ViewHolder) holder;

        // Defensive: Make sure position is valid
        if (position < 0 || position >= vetNames.size()) return;

        h.doctorName.setText(vetNames.get(position));
        h.doctorSpecialty.setText(vetSpecialties.get(position));
        h.appointmentDate.setText(appointmentDates.get(position));
        h.appointmentPrice.setText(appointmentPrices.get(position));

        Glide.with(context)
                .load(safePhotoUrl(vetProfilePictures.get(position)))
                .placeholder(R.drawable.plasholder)
                .into(h.doctorImage);

        String status = safe(vetAppointmentStatus(position));

        // Reset visibility
        h.detailsLayout.setVisibility(View.GONE);
        h.statusMessage.setVisibility(View.GONE);
        h.btnViewBill.setVisibility(View.GONE);
        h.btnViewReport.setVisibility(View.GONE);
        h.btnViewProfile.setVisibility(View.GONE);
        h.btnRefundDetails.setVisibility(View.GONE);

        // Cancelled states
        if (equalsAny(status, "cancelled", "cancelled_by_doctor", "cancelled_by_vet", "Canceled", "Cancelled")) {
            final String cancelText =
                    equalsAny(status, "cancelled_by_doctor", "cancelled_by_vet")
                            ? "Cancelled by Vet"
                            : "Cancelled by You";

            h.viewDetailsButton.setEnabled(true);
            h.viewDetailsButton.setText(cancelText);
            h.viewDetailsButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.error));

            h.viewDetailsButton.setOnClickListener(v -> {
                boolean show = h.detailsLayout.getVisibility() == View.GONE;
                h.detailsLayout.setVisibility(show ? View.VISIBLE : View.GONE);
                h.btnRefundDetails.setVisibility(show ? View.VISIBLE : View.GONE);
                h.viewDetailsButton.setText(show ? "Hide Refund Info" : cancelText);
            });

            h.btnRefundDetails.setOnClickListener(v -> {
                Intent intent = new Intent(context, RefundStatus.class);
                intent.putExtra("appointment_id", appointmentIds.get(position));
                context.startActivity(intent);
            });

        } else {
            // Active/Completed etc.
            h.viewDetailsButton.setEnabled(true);
            h.viewDetailsButton.setText("View Details");
            h.viewDetailsButton.setBackgroundColor(ContextCompat.getColor(context, R.color.navy_blue));

            h.btnViewBill.setVisibility(View.VISIBLE);
            h.btnViewReport.setVisibility(View.VISIBLE);
            h.btnViewProfile.setVisibility(View.VISIBLE);

            h.viewDetailsButton.setOnClickListener(v -> {
                boolean show = h.detailsLayout.getVisibility() == View.GONE;
                h.detailsLayout.setVisibility(show ? View.VISIBLE : View.GONE);
                h.viewDetailsButton.setText(show ? "Hide Details" : "View Details");
            });

            h.btnViewBill.setOnClickListener(v -> {
                Intent in = new Intent(context, complet_bill.class);
                in.putExtra("appointment_id", appointmentIds.get(position));
                context.startActivity(in);
            });

            h.btnViewReport.setOnClickListener(v -> {
                Intent in = new Intent(context, medical_riport.class);
                in.putExtra("appointment_id", String.valueOf(appointmentIds.get(position)));
                context.startActivity(in);
            });

            h.btnViewProfile.setOnClickListener(v -> {
                // If you have vet_details activity, switch class and extra key accordingly.
                Intent intent = new Intent(context, doctor_details.class);
                intent.putExtra("doctor_id", String.valueOf(vetIds.get(position))); // or "vet_id" for vet_details
                context.startActivity(intent);
            });
        }

        // Review prompt for completed vet visits
        if (equalsAny(status, "completed", "Completed", "COMPLETED")) {
            int vId   = vetIds.get(position); // This is doctor_id (your backend returns doctor_id for vet)
            int appId = appointmentIds.get(position);
            if (!reviewPopupShown.contains(appId)) { // Use appId to prevent double popup for same appointment
                reviewPopupShown.add(appId);
                checkAndPromptForReview(vId, appId);
            }
        }
    }

    @Override
    public int getItemCount() {
        return (vetNames == null || vetNames.isEmpty()) ? 1 : vetNames.size();
    }

    // ===== Helpers =====

    private String safePhotoUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return ApiConfig.endpoint("doctor_images/default.png");
        }
        return url;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private String vetAppointmentStatus(int position) {
        return appointmentStatuses.get(position);
    }

    private boolean equalsAny(String value, String... options) {
        if (value == null) return false;
        for (String o : options) {
            if (value.equalsIgnoreCase(o)) return true;
        }
        return false;
    }

    // ===== Review Flow =====

    private void checkAndPromptForReview(int vetId, int appointmentId) {
        StringRequest request = new StringRequest(Request.Method.POST, CHECK_REVIEW_API_URL,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean alreadyReviewed = obj.optBoolean("already_reviewed", false);
                        boolean reviewCanceled  = obj.optBoolean("review_canceled", false);
                        if (!alreadyReviewed && !reviewCanceled) {
                            showReviewPopup(vetId, appointmentId);
                        }
                    } catch (JSONException ignored) { }
                },
                error -> { }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("vet_id", String.valueOf(vetId)); // If backend expects doctor_id, change to doctor_id
                p.put("appointment_id", String.valueOf(appointmentId));
                return p;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    private void showReviewPopup(int vetId, int appointmentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RatingBar ratingBar        = dialogView.findViewById(R.id.ratingBar);
        EditText  etReviewComment  = dialogView.findViewById(R.id.etReviewComment);
        Button    btnSubmitReview  = dialogView.findViewById(R.id.btnSubmitReview);
        Button    btnCancelReview  = dialogView.findViewById(R.id.btnCancelReview);

        btnSubmitReview.setOnClickListener(v -> {
            int rating = (int) ratingBar.getRating();
            String comment = etReviewComment.getText().toString().trim();
            if (rating == 0) {
                Toast.makeText(context, "Please select a rating before submitting.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitReview(vetId, appointmentId, rating, comment, "submit");
            dialog.dismiss();
        });

        btnCancelReview.setOnClickListener(v -> {
            submitReview(vetId, appointmentId, 0, "", "skip");
            Toast.makeText(context, "You chose not to review this appointment.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReview(int vetId, int appointmentId, int rating, String comment, String action) {
        StringRequest req = new StringRequest(Request.Method.POST, REVIEW_API_URL,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean ok = obj.optBoolean("success", false);
                        String msg = ok
                                ? (action.equals("submit") ? "Thank you for your feedback!" : "Review dismissed.")
                                : (action.equals("submit") ? "Failed to submit review. Try again later." : "Failed to dismiss review.");
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    } catch (JSONException ignored) { }
                },
                error -> Toast.makeText(context, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("patient_id", patientId);
                p.put("vet_id", String.valueOf(vetId)); // If backend expects doctor_id, change to doctor_id
                p.put("appointment_id", String.valueOf(appointmentId));
                p.put("action", action);
                if ("submit".equals(action)) {
                    p.put("rating", String.valueOf(rating));
                    p.put("review_comment", comment);
                }
                return p;
            }
        };
        Volley.newRequestQueue(context).add(req);
    }

    // ===== ViewHolders =====

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView doctorImage;
        TextView doctorName, doctorSpecialty, appointmentDate, appointmentPrice, statusMessage;
        Button viewDetailsButton, btnViewBill, btnViewReport, btnViewProfile, btnRefundDetails;
        LinearLayout detailsLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            doctorImage       = itemView.findViewById(R.id.doctorImage);
            doctorName        = itemView.findViewById(R.id.doctorName);
            doctorSpecialty   = itemView.findViewById(R.id.doctorSpecialty);
            appointmentDate   = itemView.findViewById(R.id.appointmentDate);
            appointmentPrice  = itemView.findViewById(R.id.appointmentPrice);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            detailsLayout     = itemView.findViewById(R.id.detailsLayout);
            btnViewBill       = itemView.findViewById(R.id.btnViewBill);
            btnViewReport     = itemView.findViewById(R.id.btnViewReport);
            btnViewProfile    = itemView.findViewById(R.id.btnViewProfile);
            btnRefundDetails  = itemView.findViewById(R.id.btnRefundDetails);
            statusMessage     = itemView.findViewById(R.id.statusMessage);
        }
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        ImageView emptyImage;
        TextView emptyText;

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            emptyImage = itemView.findViewById(R.id.empty_state_image);
            emptyText  = itemView.findViewById(R.id.empty_state_text);
        }
    }
}
