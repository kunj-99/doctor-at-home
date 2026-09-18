package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.HealthTip;
import com.infowave.thedoctorathomeuser.R;

import java.util.List;

public class HealthTipAdapter extends RecyclerView.Adapter<HealthTipAdapter.TipViewHolder> {

    private Context context;
    private List<HealthTip> tips;

    public HealthTipAdapter(Context context, List<HealthTip> tips) {
        this.context = context;
        this.tips = tips;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_health_tip, parent, false);
        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        HealthTip tip = tips.get(position);
        holder.tvTitle.setText(tip.getTitle());
        holder.tvDesc.setText(tip.getDescription());

        // If URL present → load with Glide
        if (tip.hasImageUrl()) {
            com.infowave.thedoctorathomeuser.network.SlowImageLoader.load(
                    holder.image, tip.getImageUrl(), R.drawable.food, R.drawable.food);
        } else if (tip.getImageResId() != 0) {
            com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.image);
            holder.image.setImageResource(tip.getImageResId());
        } else {
            com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.image);
            holder.image.setImageResource(R.drawable.food);
        }
    }

    @Override
    public void onViewRecycled(@NonNull TipViewHolder holder) {
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.image);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return tips.size();
    }

    static class TipViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        ImageView image;

        TipViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTipTitle);
            tvDesc = itemView.findViewById(R.id.tvTipDesc);
            image = itemView.findViewById(R.id.imageTip);
        }
    }
}

// Last Updated: 2026-09-18 14:00 IST
