package com.infowave.thedoctorathomeuser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

        // Prefer backend URL when available; otherwise use drawable fallback
        String url = a.getImageUrl();
        if (url != null && !url.trim().isEmpty()) {
            Glide.with(h.img.getContext())
                    .load(url)
                    .placeholder(R.drawable.plaseholder_error)   // optional, add in your drawables
                    .error(R.drawable.plaseholder_error)         // optional fallback
                    .into(h.img);
        } else {
            int resId = a.getDrawableRes();
            if (resId != 0) {
                h.img.setImageResource(resId);
            } else {
                // final safety fallback
                h.img.setImageResource(R.drawable.plaseholder_error);
            }
        }

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
