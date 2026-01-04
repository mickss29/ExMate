package com.example.exmate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BudgetCategoryAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ===== MODES =====
    public static final int MODE_EDIT = 1;
    public static final int MODE_ANALYSIS = 2;

    private final List<BudgetCategoryModel> list;
    private final int mode;
    private final Runnable onChange;

    // ===== EDIT MODE =====
    public BudgetCategoryAdapter(
            List<BudgetCategoryModel> list,
            Runnable onChange
    ) {
        this.list = list;
        this.onChange = onChange;
        this.mode = MODE_EDIT;
    }

    // ===== ANALYSIS MODE =====
    public BudgetCategoryAdapter(List<BudgetCategoryModel> list) {
        this.list = list;
        this.onChange = null;
        this.mode = MODE_ANALYSIS;
    }

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == MODE_EDIT) {
            View v = inflater.inflate(
                    R.layout.item_budget_category_analysis,
                    parent,
                    false
            );
            return new EditHolder(v);
        } else {
            View v = inflater.inflate(
                    R.layout.item_budget_category_analysis,
                    parent,
                    false
            );
            return new AnalysisHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        BudgetCategoryModel model = list.get(position);

        if (holder instanceof EditHolder) {
            bindEdit((EditHolder) holder, model);
        } else {
            bindAnalysis((AnalysisHolder) holder, model);
        }
    }

    // ================= EDIT MODE =================
    private void bindEdit(EditHolder h, BudgetCategoryModel model) {

        // NAME
        h.tvName.setText(model.getName());

        // REMOVE OLD WATCHER (IMPORTANT)
        if (h.etAmount.getTag() instanceof TextWatcher) {
            h.etAmount.removeTextChangedListener(
                    (TextWatcher) h.etAmount.getTag()
            );
        }

        // SET VALUE
        if (model.getAmount() > 0) {
            h.etAmount.setText(String.valueOf(model.getAmount()));
        } else {
            h.etAmount.setText("");
        }

        // CREATE NEW WATCHER
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                int val = 0;
                if (!s.toString().isEmpty()) {
                    try {
                        val = Integer.parseInt(s.toString());
                    } catch (NumberFormatException ignored) {}
                }

                model.setAmount(val);

                if (onChange != null) {
                    onChange.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        // ADD & TAG WATCHER
        h.etAmount.addTextChangedListener(watcher);
        h.etAmount.setTag(watcher);
    }

    // ================= ANALYSIS MODE =================

    private void bindAnalysis(
            AnalysisHolder h,
            BudgetCategoryModel model
    ) {

        int budget = model.getAmount();
        int spent = model.getSpent();

        h.tvCategoryName.setText(model.getName());
        h.tvCategoryBudget.setText("Budget: ₹" + budget);

        int percent =
                budget == 0 ? 0 : (spent * 100) / budget;

        h.progressCategory.setProgress(
                Math.min(percent, 100)
        );

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
            h.tvCategoryStatus.setText(
                    "Warning: Near your limit"
            );
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
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDERS =================

    static class EditHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        EditText etAmount;

        EditHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            etAmount = itemView.findViewById(R.id.etCategoryAmount);
        }
    }

    static class AnalysisHolder extends RecyclerView.ViewHolder {

        TextView tvCategoryName;
        TextView tvCategoryBudget;
        TextView tvCategoryStatus;
        ProgressBar progressCategory;

        AnalysisHolder(@NonNull View itemView) {
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

    // ===== SIMPLE TEXT WATCHER =====
    abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
