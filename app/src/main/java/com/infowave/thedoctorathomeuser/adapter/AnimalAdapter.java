package com.infowave.thedoctorathomeuser.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.Animal;
import com.infowave.thedoctorathomeuser.R;

import java.util.List;

public class AnimalAdapter extends RecyclerView.Adapter<AnimalAdapter.VH> {

    public interface OnAnimalClickListener {
        void onAnimalClick(Animal animal);
    }

    private final List<Animal> data;
    private final OnAnimalClickListener listener;

    public AnimalAdapter(List<Animal> data, OnAnimalClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Animal a = data.get(position);
        h.tvName.setText(a.getName());
        h.img.setImageResource(a.getDrawableRes());
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAnimalClick(a);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;
        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgAnimal);
            tvName = itemView.findViewById(R.id.tvAnimalName);
        }
    }
}
