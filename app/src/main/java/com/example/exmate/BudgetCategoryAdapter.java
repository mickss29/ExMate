package com.example.exmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

    private final Context context;
    private ArrayList<CategoryModel> list;

    public BudgetCategoryAdapter(Context context, ArrayList<CategoryModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.budget_category_item, parent, false);   // <-- FIXED LAYOUT
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel model = list.get(position);

        // --- Basic values ---
        holder.tvName.setText(model.name);
        holder.tvLimit.setText("₹" + model.limit);
        holder.tvSpent.setText("₹" + model.spent);
        holder.tvRemaining.setText("₹" + (model.limit - model.spent));

        int percent = model.limit > 0 ? (int) ((model.spent * 100) / model.limit) : 0;
        holder.tvPercent.setText(percent + "% used");
        holder.progressBar.setProgress(percent);

        // --- Icon ---
        int iconId = context.getResources().getIdentifier(
                model.icon, "drawable", context.getPackageName()
        );
        holder.ivIcon.setImageResource(
                iconId != 0 ? iconId : R.drawable.ic_category_default
        );
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    // --- smooth list update ---
    public void updateList(ArrayList<CategoryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();   // we can upgrade to DiffUtil later
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIcon;
        TextView tvName, tvLimit, tvSpent, tvRemaining, tvPercent;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivIcon      = itemView.findViewById(R.id.ivIcon);
            tvName      = itemView.findViewById(R.id.tvCategoryName);
            tvLimit     = itemView.findViewById(R.id.tvLimit);
            tvSpent     = itemView.findViewById(R.id.tvSpent);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvPercent   = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
