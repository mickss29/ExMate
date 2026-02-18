package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DiscoverCategoryAdapter
        extends RecyclerView.Adapter<DiscoverCategoryAdapter.ViewHolder> {

    private final List<DiscoverCategoryModel> list;
    private final OnCategoryClick listener;

    public interface OnCategoryClick {
        void onClick(String category);
    }

    public DiscoverCategoryAdapter(List<DiscoverCategoryModel> list,
                                   OnCategoryClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discover_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        DiscoverCategoryModel model = list.get(position);

        holder.tvTitle.setText(model.getTitle());
        holder.icon.setImageResource(model.getIcon());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(model.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        ImageView icon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCategoryTitle);
            icon = itemView.findViewById(R.id.imgCategoryIcon);
        }
    }
}
