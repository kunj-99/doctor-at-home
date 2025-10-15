package com.infowave.thedoctorathomeuser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.VetDoctor;

import java.util.List;

public class VetDoctorsAdapter extends RecyclerView.Adapter<VetDoctorsAdapter.ViewHolder> {

    private final List<VetDoctor> doctors;
    private final OnDoctorClickListener listener;

    public interface OnDoctorClickListener {
        void onDoctorClick(VetDoctor doctor);
        void onBookNowClick(VetDoctor doctor);
    }

    public VetDoctorsAdapter(List<VetDoctor> doctors, OnDoctorClickListener listener) {
        this.doctors = doctors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vet_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VetDoctor doctor = doctors.get(position);

        // For now, use a placeholder - you can add Glide later
        holder.ivDoctorImage.setImageResource(R.drawable.ic_doctor_placeholder);

        holder.tvDoctorName.setText(doctor.getName());
        holder.tvSpecialization.setText(doctor.getSpecialization());
        holder.tvRating.setText(String.valueOf(doctor.getRating()));
        holder.tvExperience.setText(doctor.getExperience() + " years");
        holder.tvLocation.setText(doctor.getLocation());
        holder.tvEducation.setText(doctor.getEducation());
        holder.tvConsultationFee.setText("$" + doctor.getConsultationFee());

        // Set click listeners
        holder.itemView.setOnClickListener(v -> listener.onDoctorClick(doctor));
        holder.btnBookNow.setOnClickListener(v -> listener.onBookNowClick(doctor));
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDoctorImage;
        TextView tvDoctorName, tvSpecialization, tvRating, tvExperience;
        TextView tvLocation, tvEducation, tvConsultationFee;
        androidx.appcompat.widget.AppCompatButton btnBookNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDoctorImage = itemView.findViewById(R.id.ivDoctorImage);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvEducation = itemView.findViewById(R.id.tvEducation);
            tvConsultationFee = itemView.findViewById(R.id.tvConsultationFee);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }
    }
}