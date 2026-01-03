package com.example.exmate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BudgetCategoryAdapter
        extends RecyclerView.Adapter<BudgetCategoryAdapter.Holder> {

    private final List<BudgetCategoryModel> list;
    private final Runnable onChange; // nullable

    // ✅ Constructor 1 (USED in BudgetAnalysisFragment)
    public BudgetCategoryAdapter(List<BudgetCategoryModel> list) {
        this.list = list;
        this.onChange = null;
    }

    // ✅ Constructor 2 (USED in BudgetFragment – add/edit)
    public BudgetCategoryAdapter(
            List<BudgetCategoryModel> list,
            Runnable onChange
    ) {
        this.list = list;
        this.onChange = onChange;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_budget_category_analysis,
                        parent,
                        false
                );
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder h,
            int position
    ) {

        BudgetCategoryModel model = list.get(position);

        String name = model.getName();
        int budget = model.getAmount();
        int spent = model.getSpent();

        h.tvCategoryName.setText(name);
        h.tvCategoryBudget.setText("Budget: ₹" + budget);

        int percent = 0;
        if (budget > 0) {
            percent = (spent * 100) / budget;
        }

        h.progressCategory.setProgress(Math.min(percent, 100));

        if (percent < 80) {
            h.tvCategoryStatus.setText(
                    "You are ₹" + (budget - spent) + " under your limit"
            );
            h.tvCategoryStatus.setTextColor(
                    Color.parseColor("#2E7D32")
            );
            h.progressCategory.setProgressTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#4CAF50")
                    )
            );
        } else if (percent < 100) {
            h.tvCategoryStatus.setText("Warning: Near your limit");
            h.tvCategoryStatus.setTextColor(
                    Color.parseColor("#EF6C00")
            );
            h.progressCategory.setProgressTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#FB8C00")
                    )
            );
        } else {
            h.tvCategoryStatus.setText(
                    "Over budget by ₹" + (spent - budget)
            );
            h.tvCategoryStatus.setTextColor(
                    Color.parseColor("#C62828")
            );
            h.progressCategory.setProgressTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#E53935")
                    )
            );
        }

        // 🔔 Notify parent if needed (BudgetFragment)
        if (onChange != null) {
            onChange.run();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDER =================
    static class Holder extends RecyclerView.ViewHolder {

        TextView tvCategoryName;
        TextView tvCategoryBudget;
        TextView tvCategoryStatus;
        ProgressBar progressCategory;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName =
                    itemView.findViewById(R.id.tvCategoryName);
            tvCategoryBudget =
                    itemView.findViewById(R.id.tvCategoryBudget);
            tvCategoryStatus =
                    itemView.findViewById(R.id.tvCategoryStatus);
            progressCategory =
                    itemView.findViewById(R.id.progressCategory);
        }
    }
}
