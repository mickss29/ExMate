package com.example.exmate;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class BudgetCategoryAdapter
        extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onEdit(BudgetCategoryModel model);
    }

    private final List<BudgetCategoryModel>  list;
    private final OnCategoryClickListener    listener;

    public BudgetCategoryAdapter(
            List<BudgetCategoryModel> list,
            OnCategoryClickListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.budget_category_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BudgetCategoryModel model = list.get(position);

        // ── Emoji + Name ──
        h.tvEmoji.setText(BudgetFragment.getEmoji(model.getName()));
        h.tvName .setText(model.getName());

        // ── Amounts ──
        h.tvBudget.setText("₹" + formatAmount(model.getBudget()));
        h.tvSpent .setText("₹" + formatAmount(model.getSpent()) + " spent");

        // ── Percent ──
        float percent = model.getBudget() > 0
                ? (model.getSpent() * 100f) / model.getBudget() : 0f;
        int   pct     = (int) Math.min(percent, 100);

        h.tvPercent.setText(pct + "% used");

        // ── Color + status ──
        int    barColor;
        String status;
        if (model.getBudget() == 0) {
            barColor = Color.parseColor("#4B5563");
            status   = "⚪ Not set";
        } else if (percent < 60f) {
            barColor = Color.parseColor("#4ADE80");
            status   = "✅ On track";
        } else if (percent < 85f) {
            barColor = Color.parseColor("#FBBF24");
            status   = "⚠️ Watch out";
        } else if (percent < 100f) {
            barColor = Color.parseColor("#F87171");
            status   = "🔴 Almost full";
        } else {
            barColor = Color.parseColor("#EF4444");
            status   = "🚨 Over budget";
        }

        h.tvStatus.setText(status);
        h.tvStatus.setTextColor(barColor);
        h.tvPercent.setTextColor(barColor);

        // ── Progress fill (animated) ──
        GradientDrawable fillDrawable = new GradientDrawable();
        fillDrawable.setShape(GradientDrawable.RECTANGLE);
        fillDrawable.setCornerRadius(20f);
        fillDrawable.setColor(barColor);
        h.progressFill.setBackground(fillDrawable);

        h.progressFill.post(() -> {
            int totalWidth  = h.progressBarFrame.getWidth();
            int targetWidth = (int) (totalWidth * (pct / 100f));

            ValueAnimator animator = ValueAnimator.ofInt(0, targetWidth);
            animator.setDuration(800);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(anim -> {
                FrameLayout.LayoutParams lp =
                        (FrameLayout.LayoutParams) h.progressFill.getLayoutParams();
                lp.width = (int) anim.getAnimatedValue();
                h.progressFill.setLayoutParams(lp);
            });
            animator.start();
        });

        // ── Edit click ──
        h.ivEdit.setOnClickListener(v -> listener.onEdit(model));
        h.itemView.setOnClickListener(v -> listener.onEdit(model));
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String formatAmount(int value) {
        if (value >= 100_000) return String.format(Locale.getDefault(), "%.1fL", value / 100_000f);
        if (value >= 1_000)   return String.format(Locale.getDefault(), "%.1fK", value / 1_000f);
        return String.valueOf(value);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView     tvEmoji, tvName, tvBudget, tvSpent, tvPercent, tvStatus;
        View         progressFill;
        FrameLayout  progressBarFrame;
        android.widget.ImageView ivEdit;

        ViewHolder(View v) {
            super(v);
            tvEmoji         = v.findViewById(R.id.tvCategoryEmoji);
            tvName          = v.findViewById(R.id.tvCategoryName);
            tvBudget        = v.findViewById(R.id.tvBudgetAmount);
            tvSpent         = v.findViewById(R.id.tvSpentLabel);
            tvPercent       = v.findViewById(R.id.tvPercent);
            tvStatus        = v.findViewById(R.id.tvStatus);
            progressFill    = v.findViewById(R.id.progressFill);
            progressBarFrame= v.findViewById(R.id.progressBarFrame);
            ivEdit          = v.findViewById(R.id.ivEdit);
        }
    }
}