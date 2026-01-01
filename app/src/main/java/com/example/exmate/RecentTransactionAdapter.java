package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class RecentTransactionAdapter
        extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    // ================= DATA =================
    private final List<TransactionModel> list;

    public RecentTransactionAdapter(List<TransactionModel> list) {
        this.list = list;
        setHasStableIds(true); // ⭐ performance boost
    }

    // ================= VIEW HOLDER =================
    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgType;
        TextView tvTitle, tvSub, tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgType  = itemView.findViewById(R.id.imgType);
            tvTitle  = itemView.findViewById(R.id.tvTitle);
            tvSub    = itemView.findViewById(R.id.tvSub);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }

    // ================= ADAPTER =================
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_transection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        TransactionModel model = list.get(position);

        // ---------- TITLE (SMART) ----------
        if ("Income".equalsIgnoreCase(model.getType())) {
            holder.tvTitle.setText(model.getSource()); // Salary, Business
        } else {
            holder.tvTitle.setText(model.getCategory()); // Food, Travel
        }

        // ---------- DATE / TIME ----------
        holder.tvSub.setText(formatTime(model.getTime()));

        // ---------- AMOUNT ----------
        holder.tvAmount.setText("₹ " + model.getAmount());

        // ---------- UI BASED ON TYPE ----------
        if ("Income".equalsIgnoreCase(model.getType())) {

            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.greenIncome)
            );
            holder.imgType.setImageResource(R.drawable.ic_income);
            holder.imgType.setBackgroundResource(R.drawable.bg_circle_green);

        } else {

            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.redExpense)
            );
            holder.imgType.setImageResource(R.drawable.ic_expense);
            holder.imgType.setBackgroundResource(R.drawable.bg_circle_red);
        }

        // ---------- CLICK (FUTURE READY) ----------
        holder.itemView.setOnClickListener(v -> {
            // Later: open bottom sheet / details
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).getTime(); // stable unique
    }

    // ================= HELPERS =================
    private String formatTime(long millis) {
        return new SimpleDateFormat(
                "dd MMM • hh:mm a",
                Locale.getDefault()
        ).format(new Date(millis));
    }
}
