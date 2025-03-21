package com.example.thedoctorathomeuser.Adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.complet_bill;
import com.example.thedoctorathomeuser.doctor_details;
import com.example.thedoctorathomeuser.medical_riport;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DoctorHistoryAdapter extends RecyclerView.Adapter<DoctorHistoryAdapter.ViewHolder> {

    private static final String TAG = "DoctorHistoryAdapter";
    private final Context context;
    private final String patientId; // Passed from login or global context
    private final List<Integer> doctorIds;
    private final List<String> doctorNames;
    private final List<String> doctorSpecialties;
    private final List<String> appointmentDates;
    private final List<String> appointmentPrices;
    private final List<Integer> doctorImages;
    private final List<Integer> appointmentIds;
    private final List<String> appointmentStatuses; // e.g., "Completed", "Cancelled", etc.

    // API endpoints for review submission and checking review status
    private static final String REVIEW_API_URL = "http://sxm.a58.mytemp.website/submit_review.php";
    private static final String CHECK_REVIEW_API_URL = "http://sxm.a58.mytemp.website/check_review_status.php";

    // Local flag to prevent multiple pop-ups per doctor during auto-refresh in this session
    private final Set<Integer> reviewPopupShown = new HashSet<>();

    public DoctorHistoryAdapter(Context context, String patientId, List<Integer> doctorIds, List<String> doctorNames,
                                List<String> doctorSpecialties, List<String> appointmentDates,
                                List<String> appointmentPrices, List<Integer> doctorImages,
                                List<Integer> appointmentIds, List<String> appointmentStatuses) {
        this.context = context;
        this.patientId = patientId;
        this.doctorIds = doctorIds;
        this.doctorNames = doctorNames;
        this.doctorSpecialties = doctorSpecialties;
        this.appointmentDates = appointmentDates;
        this.appointmentPrices = appointmentPrices;
        this.doctorImages = doctorImages;
        this.appointmentIds = appointmentIds;
        this.appointmentStatuses = appointmentStatuses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Set doctor and appointment details
        holder.doctorName.setText(doctorNames.get(position));
        holder.doctorSpecialty.setText(doctorSpecialties.get(position));
        holder.appointmentDate.setText(appointmentDates.get(position));
        holder.appointmentPrice.setText(appointmentPrices.get(position));
        holder.doctorImage.setImageResource(doctorImages.get(position));

        String status = appointmentStatuses.get(position);
        if (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("cancelled_by_doctor")) {
            String cancelText;
            if (status.equalsIgnoreCase("cancelled_by_doctor")) {
                cancelText = "Cancelled By Doctor";
            } else {
                cancelText = "Cancelled By User";
            }

            // Disable action buttons and change their text to indicate cancellation
            holder.viewDetailsButton.setEnabled(false);
            holder.viewDetailsButton.setText(cancelText);
            holder.viewDetailsButton.setBackgroundColor(Color.RED);

            holder.btnViewBill.setEnabled(false);
            holder.btnViewBill.setText("Cancelled");

            holder.btnViewReport.setEnabled(false);
            holder.btnViewReport.setText("Cancelled");

            holder.btnViewProfile.setEnabled(false);
            holder.btnViewProfile.setText("Cancelled");

            // Optionally, hide the details layout and any status message view if present
            holder.detailsLayout.setVisibility(View.GONE);
            holder.statusMessage.setVisibility(View.GONE);
        } else {
            // For non-cancelled appointments, ensure buttons are enabled and show default text.
            holder.viewDetailsButton.setEnabled(true);
            holder.viewDetailsButton.setText("View Details");

            holder.btnViewBill.setEnabled(true);
            holder.btnViewBill.setText("View Bill");

            holder.btnViewReport.setEnabled(true);
            holder.btnViewReport.setText("View Report");

            holder.btnViewProfile.setEnabled(true);
            holder.btnViewProfile.setText("View Profile");

            // Make sure details layout is visible if needed
            holder.viewDetailsButton.setVisibility(View.VISIBLE);
        }


        if (appointmentStatuses.get(position).equalsIgnoreCase("Completed")) {
            int docId = doctorIds.get(position);
            int appId = appointmentIds.get(position);
            Log.d(TAG, "Appointment " + appId + " is Completed. Checking review status for doctorId " + docId);
            // Only trigger the check if the popup hasn't been shown yet for this doctor
            if (!reviewPopupShown.contains(docId)) {
                checkAndPromptForReview(docId, appId);
            } else {
                Log.d(TAG, "Review popup already shown for doctorId " + docId + ". Skipping.");
            }
        }

        // Toggle details visibility if not cancelled
        holder.viewDetailsButton.setOnClickListener(v -> {
            if (holder.detailsLayout.getVisibility() == View.GONE) {
                holder.detailsLayout.setVisibility(View.VISIBLE);
                holder.viewDetailsButton.setText("Hide Details");
            } else {
                holder.detailsLayout.setVisibility(View.GONE);
                holder.viewDetailsButton.setText("View Details");
            }
        });


        holder.btnViewBill.setOnClickListener(v -> {
            Intent in = new Intent(context, complet_bill.class);
            context.startActivity(in);
        });

        holder.btnViewReport.setOnClickListener(v -> {
            Intent in = new Intent(context, medical_riport.class);
            in.putExtra("appointment_id", String.valueOf(appointmentIds.get(position)));
            context.startActivity(in);
        });

        holder.btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, doctor_details.class);
            intent.putExtra("doctor_id", String.valueOf(doctorIds.get(position)));
            context.startActivity(intent);
        });
    }

    private void checkAndPromptForReview(int doctorId, int appointmentId) {
        // Make a POST request to check review status from the server.
        StringRequest request = new StringRequest(Request.Method.POST, CHECK_REVIEW_API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean alreadyReviewed = jsonObject.getBoolean("already_reviewed");
                        boolean reviewCanceled = jsonObject.getBoolean("review_canceled");
                        Log.d(TAG, "checkAndPromptForReview API response for doctorId " + doctorId +
                                ": alreadyReviewed=" + alreadyReviewed + ", reviewCanceled=" + reviewCanceled);
                        if (!alreadyReviewed && !reviewCanceled) {
                            // Mark the popup as shown to prevent re-triggering during auto-refresh.                 reviewPopupShown.add(doctorId);
                            showReviewPopup(doctorId, appointmentId);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "checkAndPromptForReview JSON error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "checkAndPromptForReview error: " + error.toString())) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                params.put("doctor_id", String.valueOf(doctorId));
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    private void showReviewPopup(int doctorId, int appointmentId) {
        Log.d(TAG, "Showing review popup for doctorId " + doctorId + ", appointmentId " + appointmentId);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Get references to dialog UI elements
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etReviewComment = dialogView.findViewById(R.id.etReviewComment);
        Button btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);
        Button btnCancelReview = dialogView.findViewById(R.id.btnCancelReview);

        btnSubmitReview.setOnClickListener(v -> {
            int rating = (int) ratingBar.getRating();
            String comment = etReviewComment.getText().toString().trim();
            Log.d(TAG, "Submit review clicked for doctorId " + doctorId + ". Rating: " + rating + ", Comment: " + comment);
            if (rating == 0) {
                Toast.makeText(context, "Please give a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            // Submit review with action "submit"
            submitReview(doctorId, rating, comment, "submit");
            dialog.dismiss();
        });

        btnCancelReview.setOnClickListener(v -> {
            // On cancel, submit review with action "skip"
            submitReview(doctorId, 0, "", "skip");
            Log.d(TAG, "Review canceled for doctorId " + doctorId);
            Toast.makeText(context, "Review canceled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReview(int doctorId, int rating, String comment, String action) {
        Log.d(TAG, "Submitting review: patientId=" + patientId + ", doctorId=" + doctorId + ", rating=" + rating +
                ", comment=" + comment + ", action=" + action);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, REVIEW_API_URL,
                response -> {
                    Log.d(TAG, "submitReview response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            Toast.makeText(context, "Review " + (action.equals("submit") ? "submitted" : "canceled") + " successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to " + action + " review", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "submitReview failed: " + jsonObject.optString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "submitReview JSON exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e(TAG, "submitReview error: " + error.toString());
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", patientId);
                params.put("doctor_id", String.valueOf(doctorId));
                params.put("action", action);
                if (action.equals("submit")) {
                    params.put("rating", String.valueOf(rating));
                    params.put("review_comment", comment);
                }
                Log.d(TAG, "submitReview params: " + params);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(stringRequest);
    }

    @Override
    public int getItemCount() {
        return doctorNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView doctorImage;
        TextView doctorName, doctorSpecialty, appointmentDate, appointmentPrice, statusMessage;
        Button viewDetailsButton, btnViewBill, btnViewReport, btnViewProfile;
        LinearLayout detailsLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            doctorImage = itemView.findViewById(R.id.doctorImage);
            doctorName = itemView.findViewById(R.id.doctorName);
            doctorSpecialty = itemView.findViewById(R.id.doctorSpecialty);
            appointmentDate = itemView.findViewById(R.id.appointmentDate);
            appointmentPrice = itemView.findViewById(R.id.appointmentPrice);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            detailsLayout = itemView.findViewById(R.id.detailsLayout);
            btnViewBill = itemView.findViewById(R.id.btnViewBill);
            btnViewReport = itemView.findViewById(R.id.btnViewReport);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            // New cancellation status message
            statusMessage = itemView.findViewById(R.id.statusMessage);
        }
    }
}
