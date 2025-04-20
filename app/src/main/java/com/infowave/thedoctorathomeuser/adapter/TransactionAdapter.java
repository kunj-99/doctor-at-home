package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.infowave.thedoctorathomeuser.R;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionItem> list;
    private final Context context;
    private static final String TYPE_CREDIT = "credit";
    private static final String DEFAULT_AMOUNT = "₹0.00";
    private static final String DEFAULT_TYPE = "N/A";
    private static final String DEFAULT_REASON = "No description";
    private static final String DEFAULT_TIME = "--:--";

    public TransactionAdapter(Context context) {
        this.context = context;
        this.list = new ArrayList<>();
    }

    public void updateTransactions(List<TransactionItem> newList) {
        this.list = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class TransactionItem {
        public final String amount;
        public final String type;
        public final String reason;
        public final String timestamp;

        public TransactionItem(String amount, String type, String reason, String timestamp) {
            this.amount = amount != null ? amount : "0.00";
            this.type = type != null ? type : "";
            this.reason = reason != null ? reason : "";
            this.timestamp = timestamp != null ? timestamp : "";
        }

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtAmount, txtType, txtReason, txtTime;

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (position < 0 || position >= list.size()) {
                bindDefaultValues(holder);
                return;
            }

            TransactionItem item = list.get(position);
            if (item == null) {
                bindDefaultValues(holder);
                return;
            }

            holder.txtAmount.setText(formatAmount(item.amount));
            holder.txtType.setText(formatType(item.type));
            holder.txtReason.setText(formatReason(item.reason));
            holder.txtTime.setText(formatTimestamp(item.timestamp));

            int colorRes = item.type.equalsIgnoreCase(TYPE_CREDIT) ?
                    R.color.success_green : R.color.error_red;

            holder.txtAmount.setTextColor(ContextCompat.getColor(context, colorRes));
        } catch (Exception e) {
            bindDefaultValues(holder);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void bindDefaultValues(ViewHolder holder) {
        try {
            holder.txtAmount.setText(DEFAULT_AMOUNT);
            holder.txtType.setText(DEFAULT_TYPE);
            holder.txtReason.setText(DEFAULT_REASON);
            holder.txtTime.setText(DEFAULT_TIME);
            holder.txtAmount.setTextColor(Color.BLACK);
        } catch (Exception e) {
            // Fall through
        }
    }

    private String formatAmount(String amount) {
        try {
            double value = Double.parseDouble(amount);
            return String.format("₹%,.2f", value);
        } catch (NumberFormatException e) {
            return DEFAULT_AMOUNT;
        }
    }

    private String formatType(String type) {
        return type != null && !type.isEmpty() ? type.toUpperCase() : DEFAULT_TYPE;
    }

    private String formatReason(String reason) {
        return reason != null && !reason.isEmpty() ? reason : DEFAULT_REASON;
    }

    private String formatTimestamp(String timestamp) {
        return timestamp != null && !timestamp.isEmpty() ? timestamp : DEFAULT_TIME;
    }
}