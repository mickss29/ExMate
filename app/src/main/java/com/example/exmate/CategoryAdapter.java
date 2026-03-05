package com.example.exmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final Context       context;
    private final List<String>  categories;

    public CategoryAdapter(Context context, List<String> categories) {
        this.context    = context;
        this.categories = categories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String category = categories.get(position);

        holder.categoryName.setText(category);
        holder.categoryIcon.setImageResource(getCategoryIcon(category));

        // Long press → delete
        holder.itemView.setOnLongClickListener(v -> {
            if (context instanceof ManageCategoriesActivity) {
                ((ManageCategoriesActivity) context).deleteCategory(position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    // =========================================================================
    // ICON MAPPING
    // =========================================================================

    private int getCategoryIcon(String category) {
        switch (category.toLowerCase()) {
            case "food":          return R.drawable.ic_food;
            case "transport":     return R.drawable.ic_transport;
            case "shopping":      return R.drawable.ic_shopping;
            case "bills":         return R.drawable.ic_bills;
            case "entertainment": return R.drawable.ic_entertainment;
            case "health":        return R.drawable.ic_health;
            case "education":     return R.drawable.ic_education;
            default:              return R.drawable.ic_category_default;
        }
    }

    // =========================================================================
    // VIEW HOLDER
    // BUG FIX: IDs updated to match item_category.xml
    //   R.id.categoryName  →  R.id.tvCategoryTitle
    //   R.id.categoryIcon  →  R.id.imgCategoryIcon
    // =========================================================================
    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView   categoryName;
        final ImageView  categoryIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.tvCategoryTitle);   // FIXED
            categoryIcon = itemView.findViewById(R.id.imgCategoryIcon);   // FIXED
        }
    }
}