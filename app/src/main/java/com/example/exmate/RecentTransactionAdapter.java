package com.example.exmate;

import android.graphics.Color;
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

        ImageView imgType, imgArrow;
        TextView tvTitle, tvSub, tvAmount;

        MaterialCardView iconHolder;
        MaterialCardView iconOuter;
        MaterialCardView chipSub;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgType  = itemView.findViewById(R.id.imgType);
            imgArrow = itemView.findViewById(R.id.imgArrow);

            tvTitle  = itemView.findViewById(R.id.tvTitle);
            tvSub    = itemView.findViewById(R.id.tvSub);
            tvAmount = itemView.findViewById(R.id.tvAmount);

            iconHolder = itemView.findViewById(R.id.iconHolder);
            iconOuter  = itemView.findViewById(R.id.iconOuter);
            chipSub    = itemView.findViewById(R.id.chipSub);
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

        // ---------- TITLE ----------
        if (isIncome) {
            holder.tvTitle.setText(safeText(model.getSource(), "Income"));
        } else {
            holder.tvTitle.setText(safeText(model.getCategory(), "Expense"));
        }

        // ---------- DATE / TIME ----------
        holder.tvSub.setText(formatTime(model.getTime()));

        // ---------- AMOUNT ----------
        String formattedAmount = moneyFormat.format(model.getAmount());

        if (isIncome) {
            holder.tvAmount.setText("+ ₹" + formattedAmount);
        } else {
            holder.tvAmount.setText("- ₹" + formattedAmount);
        }

        // ---------- UI BASED ON TYPE ----------
        if (isIncome) {

            // Amount color
            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.greenIncome)
            );

            // Arrow
            if (holder.imgArrow != null) {
                holder.imgArrow.setImageResource(R.drawable.ic_arrow_up);
                holder.imgArrow.setColorFilter(
                        holder.itemView.getContext().getColor(R.color.greenIncome)
                );
            }

            // Main icon
            holder.imgType.setImageResource(R.drawable.ic_income);

            // Glass badge colors
            if (holder.iconOuter != null) {
                holder.iconOuter.setCardBackgroundColor(Color.parseColor("#0F172A"));
            }

            if (holder.iconHolder != null) {
                holder.iconHolder.setCardBackgroundColor(
                        holder.itemView.getContext().getColor(R.color.greenIncomeDark)
                );
            }

        } else {

            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.redExpense)
            );

            if (holder.imgArrow != null) {
                holder.imgArrow.setImageResource(R.drawable.ic_arrow_down);
                holder.imgArrow.setColorFilter(
                        holder.itemView.getContext().getColor(R.color.redExpense)
                );
            }

            holder.imgType.setImageResource(R.drawable.ic_expense);

            if (holder.iconOuter != null) {
                holder.iconOuter.setCardBackgroundColor(Color.parseColor("#0F172A"));
            }

            if (holder.iconHolder != null) {
                holder.iconHolder.setCardBackgroundColor(
                        holder.itemView.getContext().getColor(R.color.redExpenseDark)
                );
            }
        }

        // ---------- CHIP POLISH ----------
        // (optional future: show paymentMode also)
        if (holder.chipSub != null) {
            holder.chipSub.setStrokeWidth(1);
            holder.chipSub.setStrokeColor(Color.parseColor("#1E293B"));
        }

        // ---------- CLICK (SMOOTH PREMIUM FEEL) ----------
        holder.itemView.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(90)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start())
                    .start();

            // Later: open details bottom sheet
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public long getItemId(int position) {
        // stable unique
        return list.get(position).getTime();
    }

    // ================= HELPERS =================
    private String safeText(String t, String fallback) {
        if (t == null) return fallback;
        String s = t.trim();
        return s.isEmpty() ? fallback : s;
    }

    private String formatTime(long millis) {

        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar date = java.util.Calendar.getInstance();
        date.setTimeInMillis(millis);

        boolean sameYear = now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR);

        boolean isToday =
                now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
                        now.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR);

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

        SimpleDateFormat fullFormat;

        if (sameYear) {
            fullFormat = new SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault());
        } else {
            fullFormat = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault());
        }

        return fullFormat.format(new Date(millis));
    }
}
