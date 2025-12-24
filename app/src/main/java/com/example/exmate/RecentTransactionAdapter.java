package com.example.exmate;

import android.graphics.Color;
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

        // ---------- TITLE ----------
        holder.tvTitle.setText(model.getType());

        // ---------- DATE / TIME ----------
        holder.tvSub.setText(formatTime(model.getTime()));

        // ---------- AMOUNT ----------
        holder.tvAmount.setText("₹ " + model.getAmount());

        // ---------- INCOME / EXPENSE UI ----------
        if ("Income".equalsIgnoreCase(model.getType())) {

            holder.tvAmount.setTextColor(Color.parseColor("#22C55E")); // green
            holder.imgType.setImageResource(R.drawable.ic_income);
            holder.imgType.setBackgroundResource(R.drawable.bg_circle_green);

        } else {

            holder.tvAmount.setTextColor(Color.parseColor("#F87171")); // red
            holder.imgType.setImageResource(R.drawable.ic_expense);
            holder.imgType.setBackgroundResource(R.drawable.bg_circle_red);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= HELPERS =================
    private String formatTime(long millis) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
