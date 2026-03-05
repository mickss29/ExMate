package com.example.exmate;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class DiscoverCategoryAdapter
        extends RecyclerView.Adapter<DiscoverCategoryAdapter.ViewHolder> {

    // =========================================================================
    // CALLBACK
    // =========================================================================
    public interface OnCategoryClick {
        void onCategorySelected(String categoryName);
    }

    // =========================================================================
    // FIELDS
    // =========================================================================
    private final Context                   context;
    private final List<DiscoverCategoryModel> categoryList;
    private final OnCategoryClick           listener;

    // BUG FIX: start with position 0 selected (shows "All" or first category
    //          highlighted on launch instead of nothing being selected)
    private int selectedPosition = 0;

    // Obsidian palette — matches the dashboard exactly
    private static final int COLOR_GOLD          = Color.parseColor("#D4A853");
    private static final int COLOR_GOLD_BG       = Color.parseColor("#1A1608");
    private static final int COLOR_GOLD_ICON_BG  = Color.parseColor("#2A2210");
    private static final int COLOR_CARD_DEFAULT  = Color.parseColor("#0F1421");
    private static final int COLOR_ICON_BG_DEF   = Color.parseColor("#1A2236");
    private static final int COLOR_STROKE_DEF    = Color.parseColor("#1E2A3E");
    private static final int COLOR_TEXT_MUTED    = Color.parseColor("#8899B4");
    private static final int COLOR_WHITE         = Color.parseColor("#FFFFFF");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public DiscoverCategoryAdapter(Context context,
                                   List<DiscoverCategoryModel> categoryList,
                                   OnCategoryClick listener) {
        this.context      = context;
        this.categoryList = categoryList;
        this.listener     = listener;
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

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

        // ── Text ──
        holder.tvCategoryTitle.setText(model.getCategoryName());

        // ── Icon ──
        holder.imgCategoryIcon.setImageResource(model.getIcon());

        // ── Selected vs default state ──
        boolean isSelected = (position == selectedPosition);
        applyState(holder, isSelected);

        // ── Click ──
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onCategorySelected(model.getCategoryName());
            }
        });

        // ── Press animation (scale bounce) ──
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // =========================================================================
    // STATE HELPERS
    // =========================================================================

    /**
     * Applies selected / default visual state to a ViewHolder.
     * Centralised so both onBindViewHolder and programmatic updates
     * go through a single code path.
     */
    private void applyState(ViewHolder holder, boolean selected) {
        if (selected) {
            // Card: gold tinted bg + gold border
            holder.cardCategory.setCardBackgroundColor(COLOR_GOLD_BG);
            holder.cardCategory.setStrokeColor(COLOR_GOLD);
            holder.cardCategory.setStrokeWidth(dpToPx(1.5f));

            // Icon badge: gold tinted bg
            holder.cardCategoryIconBg.setCardBackgroundColor(COLOR_GOLD_ICON_BG);

            // Icon tint: gold
            holder.imgCategoryIcon.setColorFilter(COLOR_GOLD);

            // Label: white
            holder.tvCategoryTitle.setTextColor(COLOR_WHITE);

        } else {
            // Card: default dark + subtle border
            holder.cardCategory.setCardBackgroundColor(COLOR_CARD_DEFAULT);
            holder.cardCategory.setStrokeColor(COLOR_STROKE_DEF);
            holder.cardCategory.setStrokeWidth(dpToPx(1f));

            // Icon badge: default dark
            holder.cardCategoryIconBg.setCardBackgroundColor(COLOR_ICON_BG_DEF);

            // Icon tint: muted blue-grey
            holder.imgCategoryIcon.setColorFilter(COLOR_TEXT_MUTED);

            // Label: muted
            holder.tvCategoryTitle.setTextColor(COLOR_TEXT_MUTED);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /** Programmatically select a position and trigger the callback. */
    public void selectPosition(int position) {
        if (position < 0 || position >= categoryList.size()) return;
        int prev = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(prev);
        notifyItemChanged(selectedPosition);
        if (listener != null) {
            listener.onCategorySelected(categoryList.get(position).getCategoryName());
        }
    }

    // =========================================================================
    // VIEW HOLDER
    // BUG FIX: IDs now match item_category.xml exactly:
    //   cardCategory      → root MaterialCardView  (R.id.cardCategory)
    //   cardCategoryIconBg→ inner icon badge card   (R.id.cardCategoryIconBg)
    //   imgCategoryIcon   → ImageView               (R.id.imgCategoryIcon)
    //   tvCategoryTitle   → TextView                (R.id.tvCategoryTitle)
    // =========================================================================
    static class ViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView cardCategory;
        final MaterialCardView cardCategoryIconBg;
        final ImageView        imgCategoryIcon;
        final TextView         tvCategoryTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // BUG FIX: was casting itemView directly — now finds by correct IDs
            cardCategory       = itemView.findViewById(R.id.cardCategory);
            cardCategoryIconBg = itemView.findViewById(R.id.cardCategoryIconBg);
            imgCategoryIcon    = itemView.findViewById(R.id.imgCategoryIcon);
            tvCategoryTitle    = itemView.findViewById(R.id.tvCategoryTitle);
        }
    }
}