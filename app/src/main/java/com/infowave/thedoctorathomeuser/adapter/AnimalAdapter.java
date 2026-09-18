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

        // Price chip
        double price = a.getPrice();
        if (price > 0) {
            String p = (price % 1 == 0) ? "₹" + (int) price : "₹" + String.format("%.2f", price);
            h.tvPriceChip.setText(p);
            h.tvPriceChip.setVisibility(View.VISIBLE);
        } else {
            h.tvPriceChip.setVisibility(View.GONE);
        }

        // Category static label (you can map per item if backend sends type)
        h.tvCategory.setText("ANIMAL");

        // Load image
        String url = a.getImageUrl();
        if (url != null && !url.trim().isEmpty()) {
            com.infowave.thedoctorathomeuser.network.SlowImageLoader.load(
                    h.img, url, R.drawable.plaseholder_error, R.drawable.plaseholder_error);
        } else {
            com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(h.img);
            int resId = a.getDrawableRes();
            h.img.setImageResource(resId != 0 ? resId : R.drawable.plaseholder_error);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAnimalClick(a);
        });
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.img);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName, tvCategory, tvPriceChip;
        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgAnimal);
            tvName = itemView.findViewById(R.id.tvAnimalName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPriceChip = itemView.findViewById(R.id.tvPriceChip);
        }
    }
}

// Last Updated: 2026-09-18 14:00 IST
