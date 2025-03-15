package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.complet_bill;
import com.example.thedoctorathomeuser.doctor_details;

import java.util.List;

public class DoctorHistoryAdapter extends RecyclerView.Adapter<DoctorHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Integer> doctorIds;
    private List<String> doctorNames;
    private List<String> doctorSpecialties;
    private List<String> appointmentDates;
    private List<String> appointmentPrices;
    private List<Integer> doctorImages;

    public DoctorHistoryAdapter(Context context, List<Integer> doctorIds, List<String> doctorNames,
                                List<String> doctorSpecialties, List<String> appointmentDates,
                                List<String> appointmentPrices, List<Integer> doctorImages) {
        this.context = context;
        this.doctorIds = doctorIds;
        this.doctorNames = doctorNames;
        this.doctorSpecialties = doctorSpecialties;
        this.appointmentDates = appointmentDates;
        this.appointmentPrices = appointmentPrices;
        this.doctorImages = doctorImages;
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

        int doctorId = doctorIds.get(position);  // ✅ Get doctor_id for future use

        // Set click listener to toggle the visibility of additional buttons
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
            // Handle View Medical Report Click (Add action here)
        });

        holder.btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, doctor_details.class);
            intent.putExtra("doctor_id", String.valueOf(doctorIds.get(position))); // ✅ Convert Integer to String
            context.startActivity(intent);
        });

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
