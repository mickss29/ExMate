package com.example.exmate;

import android.content.Context;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class DiscoverCategoryAdapter
        extends RecyclerView.Adapter<DiscoverCategoryAdapter.ViewHolder> {

    public interface OnCategoryClick {
        void onCategorySelected(String categoryName);
    }

    private Context context;
    private List<DiscoverCategoryModel> categoryList;
    private OnCategoryClick listener;
    private int selectedPosition = -1;

    public DiscoverCategoryAdapter(Context context,
                                   List<DiscoverCategoryModel> categoryList,
                                   OnCategoryClick listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
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

        DiscoverCategoryModel model = categoryList.get(position);

        holder.categoryName.setText(model.getCategoryName());
        holder.categoryIcon.setImageResource(model.getIcon());

        // 🔥 Selected Highlight Effect
        if (position == selectedPosition) {
            holder.cardView.setStrokeWidth(3);
            holder.cardView.setStrokeColor(0xFF6366F1); // Indigo highlight
        } else {
            holder.cardView.setStrokeWidth(1);
            holder.cardView.setStrokeColor(0xFFE6E6E6);
        }

        // 🔥 Click
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();

            if (listener != null) {
                listener.onCategorySelected(model.getCategoryName());
            }
        });

        // 🔥 Premium Press Animation
        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView categoryName;
        ImageView categoryIcon;
        MaterialCardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            cardView = (MaterialCardView) itemView;
        }
    }
}