// app/src/main/java/com/infowave/thedoctorathomeuser/adapter/DegreeAdapter.java
package com.infowave.thedoctorathomeuser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.DegreeOption;

import java.util.List;

public class DegreeAdapter extends RecyclerView.Adapter<DegreeAdapter.DegreeVH> {

    public interface OnDegreeClickListener {
        void onDegreeClicked(DegreeOption option);
    }

    private final List<DegreeOption> data;
    private final OnDegreeClickListener listener;

    public DegreeAdapter(List<DegreeOption> data, OnDegreeClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DegreeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_degree_option, parent, false);
        return new DegreeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DegreeVH h, int pos) {
        DegreeOption d = data.get(pos);
        h.title.setText(d.getPrimaryTitle());
        h.subtitle.setText(d.getSubtitle());
        h.desc.setText(d.getDescription());
        h.icon.setImageResource(d.getIconRes());
        h.card.setOnClickListener(v -> {
            if (listener != null) listener.onDegreeClicked(d);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class DegreeVH extends RecyclerView.ViewHolder {
        CardView card;
        ImageView icon;
        TextView title, subtitle, desc;

        DegreeVH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardRoot);
            icon = itemView.findViewById(R.id.ivIcon);
            title = itemView.findViewById(R.id.tvTitle);
            subtitle = itemView.findViewById(R.id.tvSubtitle);
            desc = itemView.findViewById(R.id.tvDesc);
        }
    }
}
