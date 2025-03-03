package com.example.thedoctorathomeuser.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;

public class DiseaseAdapter extends RecyclerView.Adapter<DiseaseAdapter.DiseaseViewHolder> {

    private String[] diseases;

    public DiseaseAdapter(String[] diseases) {
        this.diseases = diseases;
    }

    @NonNull
    @Override
    public DiseaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout for each disease
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_disease, parent, false);
        return new DiseaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiseaseViewHolder holder, int position) {
        // Bind the disease name to the TextView
        holder.diseaseText.setText(diseases[position]);
    }

    @Override
    public int getItemCount() {
        // Return the total number of diseases
        return diseases.length;
    }

    // ViewHolder class for the RecyclerView
    public static class DiseaseViewHolder extends RecyclerView.ViewHolder {
        TextView diseaseText;

        public DiseaseViewHolder(@NonNull View itemView) {
            super(itemView);
            diseaseText = itemView.findViewById(R.id.disease_text);
        }
    }
}
