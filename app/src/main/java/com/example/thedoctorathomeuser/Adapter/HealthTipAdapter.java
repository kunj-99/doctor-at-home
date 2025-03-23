package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.HealthTip;
import com.example.thedoctorathomeuser.R;

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
        holder.image.setImageResource(tip.getImageResId());
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
