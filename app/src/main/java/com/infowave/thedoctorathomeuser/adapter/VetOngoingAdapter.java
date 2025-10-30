package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.VetAppointment;

import java.util.List;

public class VetOngoingAdapter extends RecyclerView.Adapter<VetOngoingAdapter.VetVH> {

    private final Context appContext;
    private final LayoutInflater inflater;
    private final List<VetAppointment> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(VetAppointment appt, int position);
    }

    public VetOngoingAdapter(Context context, List<VetAppointment> items) {
        this.appContext = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
        this.items = items;
        setHasStableIds(true); // smoother animations & less layout churn
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    @NonNull
    @Override
    public VetVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_vet_ongoing, parent, false);
        return new VetVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VetVH h, int position) {
        VetAppointment a = items.get(position);

        h.tvPetTitle.setText(s(a.getPetTitle()));
        h.tvReason.setText(s(a.getReason()));
        h.tvWhen.setText(s(a.getWhen()));
        h.tvVet.setText(s(a.getVetName()));
        h.tvAmount.setText(s(a.getAmount()));
        h.tvStatus.setText(s(a.getStatus()));

        // Image
        String img = a.getImageUrl(); // keep your model as-is
        if (!TextUtils.isEmpty(img)) {
            Glide.with(appContext)
                    .load(img)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .thumbnail(0.25f)
                    .centerCrop()
                    .dontAnimate()
                    .placeholder(R.drawable.ic_pets_24)
                    .error(R.drawable.ic_pets_24)
                    .into(h.imgPet);
        } else {
            h.imgPet.setImageResource(R.drawable.ic_pets_24);
        }

        // Status chip
        tintStatus(h.tvStatus, a.getStatus());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(a, position);
        });
    }

    @Override
    public int getItemCount() {
        return (items == null) ? 0 : items.size();
    }

    @Override
    public long getItemId(int position) {
        // Build a stable hash from visible fields (works even if you don't have a numeric ID)
        VetAppointment a = items.get(position);
        String key = s(a.getVetName()) + "|" + s(a.getWhen()) + "|" + s(a.getPetTitle()) + "|" +
                s(a.getAmount()) + "|" + s(a.getStatus());
        return stableHash(key);
    }

    @Override
    public void onViewRecycled(@NonNull VetVH holder) {
        // Prevent image flashes when views are reused
        Glide.with(appContext).clear(holder.imgPet);
        super.onViewRecycled(holder);
    }

    /* ---------- helpers ---------- */

    private String s(String t) { return (t == null) ? "" : t; }

    private long stableHash(String key) {
        // 64-bit multiplicative hash for stability
        long h = 1125899906842597L;
        for (int i = 0; i < key.length(); i++) h = 31L * h + key.charAt(i);
        return h;
    }

    private void tintStatus(TextView tv, String statusRaw) {
        String status = statusRaw == null ? "" : statusRaw.trim().toLowerCase();

        int bg;
        int fg = ContextCompat.getColor(appContext, R.color.white);

        switch (status) {
            case "ongoing":
            case "confirmed":
                bg = R.color.status_green;
                break;
            case "scheduled":
            case "requested":
            case "pending":
                bg = R.color.status_blue;
                break;
            case "cancelled":
            case "cancelled_by_doctor":
            case "canceled":
                bg = R.color.status_red;
                break;
            default:
                bg = R.color.custom_gray;
                fg = ContextCompat.getColor(appContext, R.color.black);
                break;
        }

        tv.setTextColor(fg);

        if (tv.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) tv.getBackground()).setColor(ContextCompat.getColor(appContext, bg));
        } else {
            tv.setBackgroundColor(ContextCompat.getColor(appContext, bg));
        }
    }

    /* ---------- view holder ---------- */

    static class VetVH extends RecyclerView.ViewHolder {
        ImageView imgPet;
        TextView tvPetTitle, tvReason, tvWhen, tvVet, tvAmount, tvStatus;

        VetVH(@NonNull View itemView) {
            super(itemView);
            imgPet     = itemView.findViewById(R.id.imgPet);
            tvPetTitle = itemView.findViewById(R.id.tvPetTitle);
            tvReason   = itemView.findViewById(R.id.tvReason);
            tvWhen     = itemView.findViewById(R.id.tvWhen);
            tvVet      = itemView.findViewById(R.id.tvVet);
            tvAmount   = itemView.findViewById(R.id.tvAmount);
            tvStatus   = itemView.findViewById(R.id.tvStatus);
        }
    }
}
