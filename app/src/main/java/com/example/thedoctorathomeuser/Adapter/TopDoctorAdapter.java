package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.TopDoctor;
import com.example.thedoctorathomeuser.R;

import java.util.List;

public class TopDoctorAdapter extends RecyclerView.Adapter<TopDoctorAdapter.DoctorViewHolder> {

    private Context context;
    private List<TopDoctor> doctorList;

    public TopDoctorAdapter(Context context, List<TopDoctor> doctorList) {
        this.context = context;
        this.doctorList = doctorList;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_top_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        TopDoctor doctor = doctorList.get(position);
        holder.name.setText(doctor.getName());
        holder.specialty.setText(doctor.getSpecialty());

        // Use a static image resource for now.
        holder.image.setImageResource(doctor.getImageResId());

        // If in the future you receive image URLs instead of a static resource,
        // you can load images dynamically using Glide, like so:
        // Glide.with(context)
        //      .load(doctor.getImageUrl())
        //      .placeholder(R.drawable.doctor_avatar)
        //      .error(R.drawable.doctor_avatar)
        //      .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty;
        ImageView image;

        DoctorViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctorName);
            specialty = itemView.findViewById(R.id.doctorSpecialty);
            image = itemView.findViewById(R.id.doctorImage);
        }
    }
}
