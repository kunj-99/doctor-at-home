package com.infowave.thedoctorathomeuser.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
// import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.RefundStatus;
import com.infowave.thedoctorathomeuser.complet_bill;
import com.infowave.thedoctorathomeuser.doctor_details;
import com.infowave.thedoctorathomeuser.medical_riport;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DoctorHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // private static final String TAG = "DoctorHistoryAdapter";
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

    private final Context context;
    private final String patientId; // Passed from login or global context
    private final List<Integer> doctorIds;
    private final List<String> doctorNames;
    private final List<String> doctorSpecialties;
    private final List<String> appointmentDates;
    private final List<String> appointmentPrices;
    private final List<String> doctorProfilePictures;
    private final List<Integer> appointmentIds;
    private final List<String> appointmentStatuses;

    private static final String REVIEW_API_URL = "http://sxm.a58.mytemp.website/submit_review.php";
    private static final String CHECK_REVIEW_API_URL = "http://sxm.a58.mytemp.website/check_review_status.php";

    private final Set<Integer> reviewPopupShown = new HashSet<>();

    public DoctorHistoryAdapter(Context context, String patientId, List<Integer> doctorIds, List<String> doctorNames,
                                List<String> doctorSpecialties, List<String> appointmentDates,
                                List<String> appointmentPrices, List<String> doctorProfilePictures,
                                List<Integer> appointmentIds, List<String> appointmentStatuses) {
        this.context = context;
        this.patientId = patientId;
        this.doctorIds = doctorIds;
        this.doctorNames = doctorNames;
        this.doctorSpecialties = doctorSpecialties;
        this.appointmentDates = appointmentDates;
        this.appointmentPrices = appointmentPrices;
        this.doctorProfilePictures = doctorProfilePictures;
        this.appointmentIds = appointmentIds;
        this.appointmentStatuses = appointmentStatuses;
    }

    @Override
    public int getItemViewType(int position) {
        if (doctorNames == null || doctorNames.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        }
        return VIEW_TYPE_ITEM;
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
        if (holder instanceof EmptyViewHolder) {
            EmptyViewHolder emptyHolder = (EmptyViewHolder) holder;
            Glide.with(context)
                    .load(R.drawable.nodataimg)
                    .into(emptyHolder.emptyImage);
            emptyHolder.emptyText.setText("No Appointment History Available");
        } else {
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.doctorName.setText(doctorNames.get(position));
            viewHolder.doctorSpecialty.setText(doctorSpecialties.get(position));
            viewHolder.appointmentDate.setText(appointmentDates.get(position));
            viewHolder.appointmentPrice.setText(appointmentPrices.get(position));

            Glide.with(context)
                    .load(doctorProfilePictures.get(position))
                    .placeholder(R.drawable.plasholder)
                    .into(viewHolder.doctorImage);

            String status = appointmentStatuses.get(position);

            viewHolder.detailsLayout.setVisibility(View.GONE);
            viewHolder.statusMessage.setVisibility(View.GONE);
            viewHolder.btnViewBill.setVisibility(View.GONE);
            viewHolder.btnViewReport.setVisibility(View.GONE);
            viewHolder.btnViewProfile.setVisibility(View.GONE);
            viewHolder.btnRefundDetails.setVisibility(View.GONE);

            if (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("cancelled_by_doctor")) {
                String cancelText = status.equalsIgnoreCase("cancelled_by_doctor") ? "Cancelled By Doctor" : "Cancelled By User";

                viewHolder.viewDetailsButton.setEnabled(true);
                viewHolder.viewDetailsButton.setText(cancelText);
                // viewHolder.viewDetailsButton.setBackgroundColor(Color.RED);
                viewHolder.viewDetailsButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.error));

                viewHolder.viewDetailsButton.setOnClickListener(v -> {
                    if (viewHolder.detailsLayout.getVisibility() == View.GONE) {
                        viewHolder.detailsLayout.setVisibility(View.VISIBLE);
                        viewHolder.btnRefundDetails.setVisibility(View.VISIBLE);
                        viewHolder.viewDetailsButton.setText("Hide Refund Info");
                    } else {
                        viewHolder.detailsLayout.setVisibility(View.GONE);
                        viewHolder.btnRefundDetails.setVisibility(View.GONE);
                        viewHolder.viewDetailsButton.setText(cancelText);
                    }
                });

                viewHolder.btnRefundDetails.setOnClickListener(v -> {
                    Intent intent = new Intent(context, RefundStatus.class);
                    intent.putExtra("appointment_id", appointmentIds.get(position));
                    context.startActivity(intent);
                });

            } else {
                viewHolder.viewDetailsButton.setEnabled(true);
                viewHolder.viewDetailsButton.setText("View Details");
                viewHolder.viewDetailsButton.setBackgroundColor(ContextCompat.getColor(context, R.color.navy_blue));

                viewHolder.btnViewBill.setVisibility(View.VISIBLE);
                viewHolder.btnViewReport.setVisibility(View.VISIBLE);
                viewHolder.btnViewProfile.setVisibility(View.VISIBLE);

                viewHolder.viewDetailsButton.setOnClickListener(v -> {
                    if (viewHolder.detailsLayout.getVisibility() == View.GONE) {
                        viewHolder.detailsLayout.setVisibility(View.VISIBLE);
                        viewHolder.viewDetailsButton.setText("Hide Details");
                    } else {
                        viewHolder.detailsLayout.setVisibility(View.GONE);
                        viewHolder.viewDetailsButton.setText("View Details");
                    }
                });

                viewHolder.btnViewBill.setOnClickListener(v -> {
                    Intent in = new Intent(context, complet_bill.class);
                    in.putExtra("appointment_id", appointmentIds.get(position));
                    context.startActivity(in);
                });

                viewHolder.btnViewReport.setOnClickListener(v -> {
                    Intent in = new Intent(context, medical_riport.class);
                    in.putExtra("appointment_id", String.valueOf(appointmentIds.get(position)));
                    context.startActivity(in);
                });

                viewHolder.btnViewProfile.setOnClickListener(v -> {
                    Intent intent = new Intent(context, doctor_details.class);
                    intent.putExtra("doctor_id", String.valueOf(doctorIds.get(position)));
                    context.startActivity(intent);
                });
            }

            if (status.equalsIgnoreCase("Completed")) {
                int docId = doctorIds.get(position);
                int appId = appointmentIds.get(position);
                if (!reviewPopupShown.contains(docId)) {
                    checkAndPromptForReview(docId, appId);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        if (doctorNames == null || doctorNames.isEmpty()) {
            return 1;
        }
        return doctorNames.size();
    }

    private void checkAndPromptForReview(int doctorId, int appointmentId) {
        StringRequest request = new StringRequest(Request.Method.POST, CHECK_REVIEW_API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean alreadyReviewed = jsonObject.getBoolean("already_reviewed");
                        boolean reviewCanceled = jsonObject.getBoolean("review_canceled");
                        // Log.d(TAG, "checkAndPromptForReview API response for doctorId " + doctorId +
                        //         ": alreadyReviewed=" + alreadyReviewed + ", reviewCanceled=" + reviewCanceled);
                        if (!alreadyReviewed && !reviewCanceled) {
                            reviewPopupShown.add(doctorId);
                            showReviewPopup(doctorId, appointmentId);
                        }
                    } catch (JSONException e) {
                        // Log.e(TAG, "checkAndPromptForReview JSON error: " + e.getMessage());
                    }
                },
                error -> {
                    // Log.e(TAG, "checkAndPromptForReview error: " + error.toString());
                }) {
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
        // Log.d(TAG, "Showing review popup for doctorId " + doctorId + ", appointmentId " + appointmentId);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etReviewComment = dialogView.findViewById(R.id.etReviewComment);
        Button btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);
        Button btnCancelReview = dialogView.findViewById(R.id.btnCancelReview);

        btnSubmitReview.setOnClickListener(v -> {
            int rating = (int) ratingBar.getRating();
            String comment = etReviewComment.getText().toString().trim();
            // Log.d(TAG, "Submit review clicked for doctorId " + doctorId + ". Rating: " + rating + ", Comment: " + comment);
            if (rating == 0) {
                Toast.makeText(context, "Please select a rating before submitting.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitReview(doctorId, rating, comment, "submit");
            dialog.dismiss();
        });

        btnCancelReview.setOnClickListener(v -> {
            submitReview(doctorId, 0, "", "skip");
            // Log.d(TAG, "Review canceled for doctorId " + doctorId);
            Toast.makeText(context, "You chose not to review this appointment.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReview(int doctorId, int rating, String comment, String action) {
        // Log.d(TAG, "Submitting review: patientId=" + patientId + ", doctorId=" + doctorId + ", rating=" + rating +
        //         ", comment=" + comment + ", action=" + action);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, REVIEW_API_URL,
                response -> {
                    // Log.d(TAG, "submitReview response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            String msg = (action.equals("submit"))
                                    ? "Thank you for your feedback!"
                                    : "Review canceled successfully.";
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        } else {
                            String failMsg = (action.equals("submit"))
                                    ? "Failed to submit review. Please try again later."
                                    : "Failed to cancel review. Please try again later.";
                            Toast.makeText(context, failMsg, Toast.LENGTH_SHORT).show();
                            // Log.e(TAG, "submitReview failed: " + jsonObject.optString("error"));
                        }
                    } catch (JSONException e) {
                        // Log.e(TAG, "submitReview JSON exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Log.e(TAG, "submitReview error: " + error.toString());
                    Toast.makeText(context, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
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
                // Log.d(TAG, "submitReview params: " + params);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(stringRequest);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView doctorImage;
        TextView doctorName, doctorSpecialty, appointmentDate, appointmentPrice, statusMessage;
        Button viewDetailsButton, btnViewBill, btnViewReport, btnViewProfile;
        Button btnRefundDetails;
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
            btnRefundDetails = itemView.findViewById(R.id.btnRefundDetails);
            statusMessage = itemView.findViewById(R.id.statusMessage);
        }
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        ImageView emptyImage;
        TextView emptyText;

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            emptyImage = itemView.findViewById(R.id.empty_state_image);
            emptyText = itemView.findViewById(R.id.empty_state_text);
        }
    }
}
