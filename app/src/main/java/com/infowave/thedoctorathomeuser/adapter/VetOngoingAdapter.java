package com.infowave.thedoctorathomeuser.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.VetAppointment;

public class VetOngoingAdapter extends RecyclerView.Adapter<VetOngoingAdapter.VetVH> {

    private final List<VetAppointment> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(VetAppointment appt, int position);
    }

    public VetOngoingAdapter(Context context, List<VetAppointment> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VetVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vet_ongoing, parent, false);
        return new VetVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VetVH h, int position) {
        VetAppointment a = items.get(position);
        h.tvPetTitle.setText(a.getPetTitle());
        h.tvReason.setText(a.getReason());
        h.tvWhen.setText(a.getWhen());
        h.tvVet.setText(a.getVetName());
        h.tvAmount.setText(a.getAmount());
        h.tvStatus.setText(a.getStatus());

        // Demo icon
        h.imgPet.setImageResource(R.drawable.ic_pets_24);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(a, position);
        });
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VetVH extends RecyclerView.ViewHolder {
        ImageView imgPet;
        TextView tvPetTitle, tvReason, tvWhen, tvVet, tvAmount, tvStatus;
        VetVH(@NonNull View itemView) {
            super(itemView);
            imgPet = itemView.findViewById(R.id.imgPet);
            tvPetTitle = itemView.findViewById(R.id.tvPetTitle);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvWhen = itemView.findViewById(R.id.tvWhen);
            tvVet = itemView.findViewById(R.id.tvVet);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
