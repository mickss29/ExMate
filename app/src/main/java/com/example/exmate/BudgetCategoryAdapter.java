package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BudgetCategoryAdapter
        extends RecyclerView.Adapter<BudgetCategoryAdapter.CategoryViewHolder> {

    private final List<BudgetCategoryModel> categoryList;

    public BudgetCategoryAdapter(List<BudgetCategoryModel> categoryList) {
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);

        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CategoryViewHolder holder,
            int position) {

        BudgetCategoryModel model = categoryList.get(position);

        holder.txtCategoryName.setText(model.getName());
        holder.imgCategory.setImageResource(model.getIconRes());

        // ✅ Selection UI (simple + safe)
        holder.itemView.setAlpha(model.isSelected() ? 1f : 0.6f);

        holder.itemView.setOnClickListener(v -> {
            model.setSelected(!model.isSelected());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // ================= VIEW HOLDER =================
    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        ImageView imgCategory;
        TextView txtCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCategory = itemView.findViewById(R.id.imgCategory);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
        }
    }

    // ================= HELPER =================
    public List<BudgetCategoryModel> getSelectedCategories() {
        List<BudgetCategoryModel> selected = new ArrayList<>();
        for (BudgetCategoryModel m : categoryList) {
            if (m.isSelected()) selected.add(m);
        }
        return selected;
    }
}
