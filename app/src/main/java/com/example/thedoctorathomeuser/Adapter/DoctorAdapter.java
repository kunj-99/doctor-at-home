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
import com.example.thedoctorathomeuser.diseases;
import com.example.thedoctorathomeuser.doctor_details;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private final String[] names;
    private final String[] specialties;
    private final String[] hospitals;
    private final float[] ratings;
    private final int[] imageResIds;

    private final Context context;

    public DoctorAdapter( Context context,String[] names, String[] specialties, String[] hospitals, float[] ratings, int[] imageResIds) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.name.setText(names[position]);
        holder.specialty.setText(specialties[position]);
        holder.hospital.setText(hospitals[position]);
        holder.ratingBar.setRating(ratings[position]);
        holder.image.setImageResource(imageResIds[position]);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, doctor_details.class); // Replace with your details activity
            intent.putExtra("doctor_name", names[position]);
            intent.putExtra("doctor_specialty", specialties[position]);
            intent.putExtra("doctor_hospital", hospitals[position]);
            intent.putExtra("doctor_rating", ratings[position]);
            intent.putExtra("doctor_image", imageResIds[position]);
            context.startActivity(intent);
        });

        holder.bookButton.setOnClickListener(v -> {

            Intent intent = new Intent(context, book_form.class);

            context.startActivity(intent);
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
        Button bookButton;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doctor_name);
            specialty = itemView.findViewById(R.id.doctor_specialty);
            hospital = itemView.findViewById(R.id.doctor_availability);
            ratingBar = itemView.findViewById(R.id.doctor_rating);
            image = itemView.findViewById(R.id.civ_profile);
            bookButton = itemView.findViewById(R.id.schedule_button);
        }
    }
}
