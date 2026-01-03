package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BudgetCategoryAdapter
        extends RecyclerView.Adapter<BudgetCategoryAdapter.Holder> {

    private final List<BudgetCategoryModel> list;

    public BudgetCategoryAdapter(List<BudgetCategoryModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder, int position) {

        BudgetCategoryModel model = list.get(position);

        holder.tvName.setText(model.getName());

        // 🔥 VERY IMPORTANT: remove old listener before setChecked
        holder.cb.setOnCheckedChangeListener(null);
        holder.cb.setChecked(model.isSelected());

        holder.cb.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    model.setSelected(isChecked);
                }
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvName;
        CheckBox cb;
        ImageView iv;

        Holder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvCategoryName);
            cb = v.findViewById(R.id.cbCategory);
            iv = v.findViewById(R.id.ivCategoryIcon);
        }
    }
}
