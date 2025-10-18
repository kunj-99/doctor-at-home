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

import org.json.JSONObject;

import java.util.List;

public class DegreeAdapter extends RecyclerView.Adapter<DegreeAdapter.DegreeVH> {

    public interface OnDegreeClickListener {
        void onDegreeClicked(JSONObject option);
    }

    private final List<JSONObject> data;
    private final OnDegreeClickListener listener;

    public DegreeAdapter(List<JSONObject> data, OnDegreeClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DegreeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_degree_option, parent, false);
        return new DegreeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DegreeVH h, int pos) {
        JSONObject d = data.get(pos);

        String name   = d.optString("category_name", "Veterinary Category");
        String dis    = d.optString("disease", "General veterinary care.");
        double price  = d.optDouble("price", 0.0);

        h.title.setText(name);
        h.subtitle.setText("Veterinary Category");
        h.desc.setText(dis);
        h.icon.setImageResource(R.drawable.ic_degree_bachelor);
        h.price.setText("₹" + (price % 1 == 0 ? String.valueOf((int) price)
                : String.format("%.2f", price)));

        h.card.setOnClickListener(v -> {
            if (listener != null) listener.onDegreeClicked(d);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class DegreeVH extends RecyclerView.ViewHolder {
        CardView card;
        ImageView icon;
        TextView title, subtitle, desc, price;

        DegreeVH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardRoot);
            icon = itemView.findViewById(R.id.ivIcon);
            title = itemView.findViewById(R.id.tvTitle);
            subtitle = itemView.findViewById(R.id.tvSubtitle);
            desc = itemView.findViewById(R.id.tvDesc);
            price = itemView.findViewById(R.id.tvPrice);
        }
    }
}
