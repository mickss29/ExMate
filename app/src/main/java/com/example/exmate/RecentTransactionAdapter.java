package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter
        extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    // ================= DATA =================
    private final List<TransactionModel> list;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");

    public RecentTransactionAdapter(List<TransactionModel> list) {
        this.list = list;
        setHasStableIds(true); // ⭐ performance boost
    }

    // ================= VIEW HOLDER =================
    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgType;
        TextView tvTitle, tvSub, tvAmount;

        // NEW (for premium row)
        View divider;
        MaterialCardView iconHolder;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgType  = itemView.findViewById(R.id.imgType);
            tvTitle  = itemView.findViewById(R.id.tvTitle);
            tvSub    = itemView.findViewById(R.id.tvSub);
            tvAmount = itemView.findViewById(R.id.tvAmount);

            // NEW ids (safe)
            divider = itemView.findViewById(R.id.divider);
            iconHolder = itemView.findViewById(R.id.iconHolder);
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

        boolean isIncome = "Income".equalsIgnoreCase(model.getType());

        // ---------- TITLE (SMART) ----------
        if (isIncome) {
            holder.tvTitle.setText(model.getSource()); // Salary, Business
        } else {
            holder.tvTitle.setText(model.getCategory()); // Food, Travel
        }

        // ---------- DATE / TIME ----------
        holder.tvSub.setText(formatTime(model.getTime()));

        // ---------- AMOUNT (PREMIUM) ----------
        String formattedAmount = moneyFormat.format(model.getAmount());

        if (isIncome) {
            holder.tvAmount.setText("+ ₹" + formattedAmount);
        } else {
            holder.tvAmount.setText("- ₹" + formattedAmount);
        }

        // ---------- UI BASED ON TYPE ----------
        if (isIncome) {

            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.greenIncome)
            );

            holder.imgType.setImageResource(R.drawable.ic_income);

            // NEW premium icon holder color (safe)
            if (holder.iconHolder != null) {
                holder.iconHolder.setCardBackgroundColor(
                        holder.itemView.getContext().getColor(R.color.greenIncomeDark)
                );
            } else {
                // fallback (old)
                holder.imgType.setBackgroundResource(R.drawable.bg_circle_green);
            }

        } else {

            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.redExpense)
            );

            holder.imgType.setImageResource(R.drawable.ic_expense);

            if (holder.iconHolder != null) {
                holder.iconHolder.setCardBackgroundColor(
                        holder.itemView.getContext().getColor(R.color.redExpenseDark)
                );
            } else {
                holder.imgType.setBackgroundResource(R.drawable.bg_circle_red);
            }
        }

        // ---------- DIVIDER (HIDE LAST ITEM) ----------
        if (holder.divider != null) {
            holder.divider.setVisibility(
                    position == getItemCount() - 1 ? View.GONE : View.VISIBLE
            );
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

        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar date = java.util.Calendar.getInstance();
        date.setTimeInMillis(millis);

        boolean sameYear = now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR);

        // Today
        boolean isToday =
                now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
                        now.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR);

        // Yesterday
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);

        boolean isYesterday =
                yesterday.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
                        yesterday.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (isToday) {
            return "Today • " + timeFormat.format(new Date(millis));
        }

        if (isYesterday) {
            return "Yesterday • " + timeFormat.format(new Date(millis));
        }

        // Normal
        SimpleDateFormat fullFormat;

        if (sameYear) {
            fullFormat = new SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault());
        } else {
            fullFormat = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault());
        }

        return fullFormat.format(new Date(millis));
    }

}

