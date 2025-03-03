package com.example.thedoctorathomeuser.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.diseases;

import java.util.List;

public class book_AppointmentAdapter extends RecyclerView.Adapter<book_AppointmentAdapter.AppointmentViewHolder> {

    private final Context context;
    private final List<String> degrees;
    private final List<String> fees;

    public book_AppointmentAdapter(Context context, List<String> degrees, List<String> fees) {
        this.context = context;
        this.degrees = degrees;
        this.fees = fees;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.degreeTextView.setText(degrees.get(position));
        holder.feeTextView.setText(fees.get(position));

        // Set click listener for the RecyclerView item
        holder.itemView.setOnClickListener(v -> {
            // Navigate to Disease activity
            Intent intent = new Intent(context, diseases.class);
            intent.putExtra("degree", degrees.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return degrees.size();
    }

    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView degreeTextView, feeTextView;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            degreeTextView = itemView.findViewById(R.id.degreeTextView);
            feeTextView = itemView.findViewById(R.id.feeTextView);
        }
    }
}
