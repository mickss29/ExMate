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

import java.util.ArrayList;
import java.util.List;

public class DummyAdapter extends RecyclerView.Adapter<DummyAdapter.ViewHolder> {

    // =========================================================================
    // TYPES
    // =========================================================================
    public static final int TYPE_TRANSACTION    = 1;
    public static final int TYPE_STAT_CATEGORY  = 2;

    private final int adapterType;

    // =========================================================================
    // DATA
    // =========================================================================
    private List<Transaction>  transactionList;
    private List<String>       categoryList;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /** Default: shows dummy transaction rows. */
    public DummyAdapter() {
        adapterType     = TYPE_TRANSACTION;
        transactionList = new ArrayList<>();
        transactionList.add(new Transaction("Food",             "- ₹450",   false));
        transactionList.add(new Transaction("Groceries",        "- ₹2,000", false));
        transactionList.add(new Transaction("Electricity Bill", "- ₹1,500", false));
        transactionList.add(new Transaction("Salary",           "+ ₹45,000",true));
        transactionList.add(new Transaction("Movie",            "- ₹500",   false));
    }

    /** Statistics mode: shows category breakdown rows. */
    public DummyAdapter(List<String> categories) {
        adapterType  = TYPE_STAT_CATEGORY;
        categoryList = categories;
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (adapterType == TYPE_STAT_CATEGORY) {
            // Stat category has its own layout — unchanged
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stat_category, parent, false);
        } else {
            // BUG FIX: was R.layout.item_transection (old IDs, deleted layout).
            //          Now uses the live layout with correct IDs.
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_transection, parent, false);
        }
        return new ViewHolder(view, adapterType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        if (adapterType == TYPE_STAT_CATEGORY) {
            // ── Stat row (layout unchanged) ──
            if (h.txtIcon     != null) h.txtIcon.setText("🍔");
            if (h.txtCategory != null) h.txtCategory.setText(categoryList.get(position));
            if (h.txtPercent  != null) h.txtPercent.setText("45% of total spend");
            if (h.txtAmount   != null) h.txtAmount.setText("₹4,500");

        } else {
            // ── Transaction row (new IDs from item_user_transection.xml) ──
            Transaction t = transactionList.get(position);

            // Title
            if (h.tvTitle != null) h.tvTitle.setText(t.name);

            // Amount + colour
            if (h.tvAmount != null) {
                h.tvAmount.setText(t.amount);
                h.tvAmount.setTextColor(t.isIncome
                        ? Color.parseColor("#00C896")
                        : Color.parseColor("#FF5A5A"));
            }

            // Date chip
            if (h.tvSub != null) h.tvSub.setText("Today · 8:45 PM");

            // Icon
            if (h.imgType != null) {
                h.imgType.setImageResource(t.isIncome
                        ? R.drawable.ic_income
                        : R.drawable.ic_expense);
                h.imgType.setColorFilter(t.isIncome
                        ? Color.parseColor("#00C896")
                        : Color.parseColor("#FF5A5A"));
            }

            // Arrow badge
            if (h.imgArrow != null) {
                h.imgArrow.setImageResource(t.isIncome
                        ? R.drawable.ic_arrow_up
                        : R.drawable.ic_arrow_down);
                h.imgArrow.setColorFilter(t.isIncome
                        ? Color.parseColor("#00C896")
                        : Color.parseColor("#FF5A5A"));
            }

            // Icon badge bg colours
            if (h.iconOuter  != null) h.iconOuter.setCardBackgroundColor(
                    Color.parseColor("#141929"));
            if (h.iconHolder != null) h.iconHolder.setCardBackgroundColor(
                    Color.parseColor(t.isIncome ? "#0D2E1E" : "#2E0D0D"));

            // Hide divider on last row
            if (h.dividerRow != null) {
                h.dividerRow.setVisibility(
                        position == transactionList.size() - 1
                                ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (adapterType == TYPE_STAT_CATEGORY)
            return categoryList == null ? 0 : categoryList.size();
        return transactionList == null ? 0 : transactionList.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // BUG FIX: transaction branch now finds IDs from item_user_transection.xml:
    //   tvTitle, tvSub, tvAmount, imgType, imgArrow, iconHolder, iconOuter,
    //   chipSub, dividerRow
    // Stat branch unchanged (item_stat_category.xml IDs untouched).
    // =========================================================================
    public static class ViewHolder extends RecyclerView.ViewHolder {

        // ── Stat-only ──
        TextView txtIcon, txtCategory, txtPercent, txtAmount;

        // ── Transaction (new layout) ──
        TextView         tvTitle, tvSub, tvAmount;
        ImageView        imgType, imgArrow;
        MaterialCardView iconHolder, iconOuter, chipSub;
        View             dividerRow;

        public ViewHolder(@NonNull View itemView, int type) {
            super(itemView);

            if (type == TYPE_STAT_CATEGORY) {
                txtIcon     = itemView.findViewById(R.id.txtIcon);
                txtCategory = itemView.findViewById(R.id.txtCategory);
                txtPercent  = itemView.findViewById(R.id.txtPercent);
                txtAmount   = itemView.findViewById(R.id.txtAmount);
            } else {
                // BUG FIX: all IDs match item_user_transection.xml
                tvTitle    = itemView.findViewById(R.id.tvTitle);
                tvSub      = itemView.findViewById(R.id.tvSub);
                tvAmount   = itemView.findViewById(R.id.tvAmount);
                imgType    = itemView.findViewById(R.id.imgType);
                imgArrow   = itemView.findViewById(R.id.imgArrow);
                iconHolder = itemView.findViewById(R.id.iconHolder);
                iconOuter  = itemView.findViewById(R.id.iconOuter);
                chipSub    = itemView.findViewById(R.id.chipSub);
                dividerRow = itemView.findViewById(R.id.dividerRow);
            }
        }
    }

    // =========================================================================
    // INTERNAL MODEL
    // =========================================================================
    static class Transaction {
        String  name;
        String  amount;
        boolean isIncome;

        Transaction(String name, String amount, boolean isIncome) {
            this.name     = name;
            this.amount   = amount;
            this.isIncome = isIncome;
        }
    }
}