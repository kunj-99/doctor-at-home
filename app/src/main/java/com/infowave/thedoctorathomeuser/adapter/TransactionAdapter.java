package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.R;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionItem> list;
    private Context context;

    public TransactionAdapter(List<TransactionItem> list, Context context) {
        this.list = list;
        this.context = context;
    }

    // ✅ Embedded Model Class
    public static class TransactionItem {
        public String amount, type, reason, timestamp;

        public TransactionItem(String amount, String type, String reason, String timestamp) {
            this.amount = amount;
            this.type = type;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }

    // ✅ ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAmount, txtType, txtReason, txtTime;

        public ViewHolder(View view) {
            super(view);
            txtAmount = view.findViewById(R.id.txtAmount);
            txtType = view.findViewById(R.id.txtType);
            txtReason = view.findViewById(R.id.txtReason);
            txtTime = view.findViewById(R.id.txtTime);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {
        TransactionItem item = list.get(i);
        holder.txtAmount.setText("₹" + item.amount);
        holder.txtType.setText(item.type.toUpperCase());
        holder.txtReason.setText(item.reason);
        holder.txtTime.setText(item.timestamp);

        // Color based on type
        holder.txtAmount.setTextColor(item.type.equalsIgnoreCase("credit") ?
                context.getResources().getColor(android.R.color.holo_green_dark) :
                context.getResources().getColor(android.R.color.holo_red_dark));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
