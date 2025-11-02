package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.bumptech.glide.signature.ObjectKey;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.VetAppointment;

import java.util.List;

public class VetOngoingAdapter extends RecyclerView.Adapter<VetOngoingAdapter.VetVH> {

    private static final String DEFAULT_DOCTOR_IMAGE_URL =
            "https://thedoctorathome.in/doctor_images/default.png";

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
        setHasStableIds(true);
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

        // --- Doctor image URL: normalize + fallback, then load with Glide ---
        String rawUrl = a.getImageUrl(); // profile_picture from API
        String imgUrl = cleanUrlOrDefault(rawUrl);

        if (!TextUtils.isEmpty(imgUrl)) {
            Glide.with(appContext)
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .signature(new ObjectKey(imgUrl))   // cache refresh when URL changes
                    .thumbnail(0.25f)
                    .circleCrop()
                    .dontAnimate()
                    .placeholder(R.drawable.ic_pets_24)
                    .error(R.drawable.ic_pets_24)
                    .into(h.imgPet);
        } else {
            h.imgPet.setImageResource(R.drawable.ic_pets_24);
        }

        // Status chip
        tintStatus(h.tvStatus, a.getStatus());

        // Row click → external listener
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(a, position);
        });

        // Track Doctor button → open Track activity with doctor_id & appointment_id
        if (h.btnTrackDoctor != null) {
            h.btnTrackDoctor.setOnClickListener(v -> {
                String doctorIdStr = s(a.getDoctorId());      // requires VetAppointment#getDoctorId()
                String apptIdStr   = s(a.getAppointmentId()); // requires VetAppointment#getAppointmentId()

                if (!TextUtils.isEmpty(doctorIdStr) && !TextUtils.isEmpty(apptIdStr)) {
                    Intent i = new Intent(appContext, com.infowave.thedoctorathomeuser.track_doctor.class);
                    i.putExtra("doctor_id", doctorIdStr);          // track_doctor reads String safely
                    i.putExtra("appointment_id", apptIdStr);       // track_doctor reads String safely
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(i);
                }
            });
        }

        // Cancel button → launch cancle_appintment with integer appointment_id
        if (h.btnCancelAppointment != null) {
            h.btnCancelAppointment.setOnClickListener(v -> {
                String apptIdStr = s(a.getAppointmentId()); // String in model
                int apptId = parseIntSafe(apptIdStr, -1);    // activity expects int extra
                if (apptId > 0) {
                    Intent i = new Intent(appContext, com.infowave.thedoctorathomeuser.cancle_appintment.class);
                    i.putExtra("appointment_id", apptId);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(i);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return (items == null) ? 0 : items.size();
    }

    @Override
    public long getItemId(int position) {
        VetAppointment a = items.get(position);
        String key = s(a.getVetName()) + "|" + s(a.getWhen()) + "|" + s(a.getPetTitle()) + "|" +
                s(a.getAmount()) + "|" + s(a.getStatus()) + "|" + s(a.getImageUrl());
        return stableHash(key);
    }

    @Override
    public void onViewRecycled(@NonNull VetVH holder) {
        Glide.with(appContext).clear(holder.imgPet);
        super.onViewRecycled(holder);
    }

    /* ---------- helpers ---------- */

    private String s(String t) { return (t == null) ? "" : t; }

    private long stableHash(String key) {
        long h = 1125899906842597L;
        for (int i = 0; i < key.length(); i++) h = 31L * h + key.charAt(i);
        return h;
    }

    private static int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
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

    /** Normalize possibly-buggy URLs and return a safe default when missing. */
    private String cleanUrlOrDefault(String raw) {
        if (raw == null) return DEFAULT_DOCTOR_IMAGE_URL;
        String u = raw.trim();

        // strip accidental quotes
        if ((u.startsWith("\"") && u.endsWith("\"")) || (u.startsWith("'") && u.endsWith("'"))) {
            u = u.substring(1, u.length() - 1).trim();
        }

        if (u.isEmpty() || "null".equalsIgnoreCase(u)) return DEFAULT_DOCTOR_IMAGE_URL;

        // absolute?
        if (u.startsWith("http://") || u.startsWith("https://")) {
            int secondHttps = u.indexOf("https://", 8);
            int secondHttp  = u.indexOf("http://", 7);
            int idx = -1;
            if (secondHttps >= 0) idx = secondHttps;
            else if (secondHttp >= 0) idx = secondHttp;
            if (idx > 0) return u.substring(idx); // fix double-prefix
            return u;
        }

        // keep default to avoid broken relative paths
        return DEFAULT_DOCTOR_IMAGE_URL;
    }

    /* ---------- view holder ---------- */

    static class VetVH extends RecyclerView.ViewHolder {
        ImageView imgPet;
        TextView tvPetTitle, tvReason, tvWhen, tvVet, tvAmount, tvStatus;
        View btnTrackDoctor;        // existing: @id/Track_button
        View btnCancelAppointment;  // new:   @id/btnCancelAppointment

        VetVH(@NonNull View itemView) {
            super(itemView);
            imgPet               = itemView.findViewById(R.id.imgPet);
            tvPetTitle           = itemView.findViewById(R.id.tvPetTitle);
            tvReason             = itemView.findViewById(R.id.tvReason);
            tvWhen               = itemView.findViewById(R.id.tvWhen);
            tvVet                = itemView.findViewById(R.id.tvVet);
            tvAmount             = itemView.findViewById(R.id.tvAmount);
            tvStatus             = itemView.findViewById(R.id.tvStatus);
            btnTrackDoctor       = itemView.findViewById(R.id.Track_button);          // must exist
            btnCancelAppointment = itemView.findViewById(R.id.Cancel_button);  // add in XML
        }
    }
}
