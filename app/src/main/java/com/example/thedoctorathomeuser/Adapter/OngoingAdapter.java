package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.book_form;
import com.example.thedoctorathomeuser.cancle_appintment;
import com.example.thedoctorathomeuser.track_doctor;

public class OngoingAdapter extends RecyclerView.Adapter<OngoingAdapter.DoctorViewHolder> {

    private final String[] names;
    private final String[] specialties;
    private final String[] hospitals;
    private final float[] ratings;
    private final int[] imageResIds;

    private final Context context;




    // Constructor
    public OngoingAdapter(Context context, String[] names, String[] specialties, String[] hospitals, float[] ratings, int[] imageResIds) {
        this.context = context;
        this.names = names;
        this.specialties = specialties;
        this.hospitals = hospitals;
        this.ratings = ratings;
        this.imageResIds = imageResIds;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ongoing, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.name.setText(names[position]);
        holder.specialty.setText(specialties[position]);
        holder.hospital.setText(hospitals[position]);
        holder.ratingBar.setRating(ratings[position]);
        holder.image.setImageResource(imageResIds[position]);

        holder.cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, cancle_appintment.class);
                context.startActivity(intent);
            }
        });
        holder.track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, track_doctor.class);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty, hospital;
        RatingBar ratingBar;
        ImageView image;
        Button track , cancle ;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctor_name);
            specialty = itemView.findViewById(R.id.doctor_specialty);
            hospital = itemView.findViewById(R.id.doctor_availability);
            ratingBar = itemView.findViewById(R.id.doctor_rating);
            image = itemView.findViewById(R.id.civ_profile);
           track = itemView.findViewById(R.id.Track_button);
           cancle = itemView.findViewById(R.id.Cancel_button);
        }
    }
}
