package com.example.thedoctorathomeuser.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;

import java.util.List;

public class DiseaseAdapter extends RecyclerView.Adapter<DiseaseAdapter.DiseaseViewHolder> {

    private List<String> diseaseList;

    public DiseaseAdapter(List<String> diseaseList) {
        this.diseaseList = diseaseList != null ? diseaseList : List.of(); // Avoid null list
    }

    @NonNull
    @Override
    public DiseaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_disease, parent, false);
        return new DiseaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiseaseViewHolder holder, int position) {
        holder.diseaseText.setText(diseaseList.get(position));
    }

    @Override
    public int getItemCount() {
        return diseaseList == null ? 0 : diseaseList.size(); // Prevent crashes
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
