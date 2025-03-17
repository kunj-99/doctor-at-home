package com.example.thedoctorathomeuser.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Map;

public class DoctorHistoryAdapter extends RecyclerView.Adapter<DoctorHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Integer> doctorIds;
    private List<String> doctorNames;
    private List<String> doctorSpecialties;
    private List<String> appointmentDates;
    private List<String> appointmentPrices;
    private List<Integer> doctorImages;
    private List<Integer> appointmentIds;
    private List<String> appointmentStatuses; // Store appointment status

    private static final String REVIEW_API_URL = "http://sxm.a58.mytemp.website/submit_review.php";
    private static final String CHECK_REVIEW_API_URL = "http://sxm.a58.mytemp.website/check_review_status.php";

    public DoctorHistoryAdapter(Context context, List<Integer> doctorIds, List<String> doctorNames,
                                List<String> doctorSpecialties, List<String> appointmentDates,
                                List<String> appointmentPrices, List<Integer> doctorImages,
                                List<Integer> appointmentIds, List<String> appointmentStatuses) {
        this.context = context;
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.doctorName.setText(doctorNames.get(position));
        holder.doctorSpecialty.setText(doctorSpecialties.get(position));
        holder.appointmentDate.setText(appointmentDates.get(position));
        holder.appointmentPrice.setText(appointmentPrices.get(position));
        holder.doctorImage.setImageResource(doctorImages.get(position));

        // Detect completed appointment
        if (appointmentStatuses.get(position).equalsIgnoreCase("Completed")) {
            checkAndPromptForReview(doctorIds.get(position), appointmentIds.get(position));
        }

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
        SharedPreferences sp = context.getSharedPreferences("ReviewPrefs", Context.MODE_PRIVATE);
        boolean isReviewed = sp.getBoolean("reviewed_" + doctorId, false);
        boolean isSkipped = sp.getBoolean("review_skipped_" + doctorId, false); // Check if canceled

        if (!isReviewed && !isSkipped) {
            showReviewPopup(doctorId, appointmentId);
        }
    }


    private void showReviewPopup(int doctorId, int appointmentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etReviewComment = dialogView.findViewById(R.id.etReviewComment);
        Button btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);
        Button btnCancelReview = dialogView.findViewById(R.id.btnCancelReview); // New cancel button

        btnSubmitReview.setOnClickListener(v -> {
            int rating = (int) ratingBar.getRating();
            String comment = etReviewComment.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(context, "Please give a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            submitReview(doctorId, rating, comment);
            dialog.dismiss();
        });

        btnCancelReview.setOnClickListener(v -> {
            // Save in SharedPreferences to prevent the pop-up from appearing again
            SharedPreferences sp = context.getSharedPreferences("ReviewPrefs", Context.MODE_PRIVATE);
            sp.edit().putBoolean("review_skipped_" + doctorId, true).apply();

            Toast.makeText(context, "Review canceled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }


    private void submitReview(int doctorId, int rating, String comment) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, REVIEW_API_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            SharedPreferences sp = context.getSharedPreferences("ReviewPrefs", Context.MODE_PRIVATE);
                            sp.edit().putBoolean("reviewed_" + doctorId, true).apply();
                            Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to submit review", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", String.valueOf(doctorId));
                params.put("rating", String.valueOf(rating));
                params.put("review_comment", comment);
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
        TextView doctorName, doctorSpecialty, appointmentDate, appointmentPrice;
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
        }
    }
}
