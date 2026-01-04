package com.example.exmate;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BudgetCategoryAdapter
        extends RecyclerView.Adapter<BudgetCategoryAdapter.Holder> {

    public interface OnBudgetClick {
        void onClick(BudgetCategoryModel model);
    }

    private final List<BudgetCategoryModel> list;
    private final OnBudgetClick listener;

    public BudgetCategoryAdapter(
            List<BudgetCategoryModel> list,
            OnBudgetClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder h, int pos) {

        BudgetCategoryModel m = list.get(pos);

        int budget = m.getBudget();
        int spent = m.getSpent();

        h.tvName.setText(m.getName());
        h.tvBudget.setText("Budget: ₹" + budget);

        // ===== SAFE % CALCULATION (FLOAT BASED) =====
        int percent;
        if (budget <= 0) {
            percent = 0;
        } else {
            percent = Math.round((spent * 100f) / budget);
            percent = Math.min(percent, 100);
        }

        h.progressBar.setProgress(percent);

        // ===== STATUS + COLOR =====
        if (spent < budget) {

            int left = budget - spent;
            h.tvStatus.setText(
                    "You are ₹" + left + " under your limit");
            h.tvStatus.setTextColor(
                    Color.parseColor("#16A34A"));

            h.progressBar.getProgressDrawable()
                    .setColorFilter(
                            Color.parseColor("#16A34A"),
                            PorterDuff.Mode.SRC_IN
                    );

        } else if (spent == budget) {

            h.tvStatus.setText("You have used full budget");
            h.tvStatus.setTextColor(
                    Color.parseColor("#F59E0B"));

            h.progressBar.getProgressDrawable()
                    .setColorFilter(
                            Color.parseColor("#F59E0B"),
                            PorterDuff.Mode.SRC_IN
                    );

        } else {

            int over = spent - budget;
            h.tvStatus.setText(
                    "Over budget by ₹" + over);
            h.tvStatus.setTextColor(
                    Color.parseColor("#DC2626"));

            h.progressBar.getProgressDrawable()
                    .setColorFilter(
                            Color.parseColor("#DC2626"),
                            PorterDuff.Mode.SRC_IN
                    );
        }

        h.btnAction.setOnClickListener(v ->
                listener.onClick(m));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDER =================
    static class Holder extends RecyclerView.ViewHolder {

        TextView tvName, tvBudget, tvStatus;
        ProgressBar progressBar;
        Button btnAction;

        Holder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvBudget = v.findViewById(R.id.tvBudget);
            tvStatus = v.findViewById(R.id.tvStatus);
            progressBar = v.findViewById(R.id.progressBar);
            btnAction = v.findViewById(R.id.btnAction);
        }
    }
}
