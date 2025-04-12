package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.AppointmentStat;
import com.infowave.thedoctorathomeuser.R;

import java.util.List;

public class AppointmentStatAdapter extends RecyclerView.Adapter<AppointmentStatAdapter.StatViewHolder> {

    private Context context;
    private List<AppointmentStat> statList;

    public AppointmentStatAdapter(Context context, List<AppointmentStat> statList) {
        this.context = context;
        this.statList = statList;
    }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment_stat, parent, false);
        return new StatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        AppointmentStat stat = statList.get(position);
        holder.title.setText(stat.getTitle());
        holder.count.setText(String.valueOf(stat.getCount()));
        holder.icon.setImageResource(stat.getIconResId());
    }

    @Override
    public int getItemCount() {
        return statList.size();
    }

    static class StatViewHolder extends RecyclerView.ViewHolder {
        TextView title, count;
        ImageView icon;

        public StatViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.statTitle);
            count = itemView.findViewById(R.id.statCount);
            icon = itemView.findViewById(R.id.statIcon);
        }
    }
}
