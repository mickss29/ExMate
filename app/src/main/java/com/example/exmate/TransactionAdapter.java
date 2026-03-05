package com.example.exmate;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TransactionAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // =========================================================================
    // PALETTE
    // =========================================================================
    private static final int COLOR_GREEN       = Color.parseColor("#00C896");
    private static final int COLOR_GREEN_BADGE = Color.parseColor("#0D2E1E");
    private static final int COLOR_GREEN_PILL  = Color.parseColor("#0A2018");
    private static final int COLOR_RED         = Color.parseColor("#FF5A5A");
    private static final int COLOR_RED_BADGE   = Color.parseColor("#2E0D0D");
    private static final int COLOR_RED_PILL    = Color.parseColor("#200A0A");
    private static final int COLOR_ICON_RING   = Color.parseColor("#141929");

    // =========================================================================
    // DATA
    // =========================================================================
    private final List<TransactionListItem> list;

    public TransactionAdapter(List<TransactionListItem> list) {
        this.list = list;
    }

    // =========================================================================
    // VIEW TYPES
    // =========================================================================
    @Override
    public int getItemViewType(int position) {
        return list.get(position).getType();
    }

    // =========================================================================
    // CREATE
    // =========================================================================
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LayoutInflater inf = LayoutInflater.from(parent.getContext());

        if (viewType == TransactionListItem.TYPE_DATE) {
            return new DateViewHolder(
                    inf.inflate(R.layout.item_date_header, parent, false));
        } else {
            return new TxnViewHolder(
                    inf.inflate(R.layout.item_user_transection, parent, false));
        }
    }

    // =========================================================================
    // BIND
    // =========================================================================
    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        TransactionListItem item = list.get(position);

        // ── Date section header ──
        if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).txtDateHeader.setText(item.getDateTitle());
            return;
        }

        // ── Transaction card ──
        TxnViewHolder h       = (TxnViewHolder) holder;
        boolean       income  = item.isIncome();

        int   mainColor  = income ? COLOR_GREEN       : COLOR_RED;
        int   badgeBg    = income ? COLOR_GREEN_BADGE  : COLOR_RED_BADGE;
        int   pillBg     = income ? COLOR_GREEN_PILL   : COLOR_RED_PILL;
        String pillTxt   = income ? "INCOME"           : "EXPENSE";
        int   arrowRes   = income ? R.drawable.ic_arrow_up   : R.drawable.ic_arrow_down;
        int   iconRes    = income ? R.drawable.ic_income      : R.drawable.ic_expense;

        // Title
        h.tvTitle.setText(item.getCategory());

        // Date chip
        h.tvSub.setText(item.getMeta());

        // Amount
        h.tvAmount.setText(item.getAmount());
        h.tvAmount.setTextColor(mainColor);

        // Left accent bar
        if (h.accentBar    != null) h.accentBar.setBackgroundColor(mainColor);

        // Icon
        h.imgType.setImageResource(iconRes);
        h.imgType.setColorFilter(mainColor);

        // Arrow badge
        if (h.imgArrow != null) {
            h.imgArrow.setImageResource(arrowRes);
            h.imgArrow.setColorFilter(mainColor);
        }

        // Icon circles
        if (h.iconOuter  != null) h.iconOuter.setCardBackgroundColor(COLOR_ICON_RING);
        if (h.iconHolder != null) h.iconHolder.setCardBackgroundColor(badgeBg);

        // Type pill
        if (h.cardTypePill != null) h.cardTypePill.setCardBackgroundColor(pillBg);
        if (h.tvTypePill   != null) {
            h.tvTypePill.setText(pillTxt);
            h.tvTypePill.setTextColor(mainColor);
        }

        // HIGH SPEND badge
        if (h.badgeCard != null) {
            h.badgeCard.setVisibility(item.isHighSpend() ? View.VISIBLE : View.GONE);
        }

        // Fade-in entrance
        h.itemView.setAlpha(0f);
        h.itemView.animate().alpha(1f).setDuration(220).start();

        // Press animation
        h.itemView.setOnClickListener(v ->
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                        .withEndAction(() ->
                                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start())
                        .start());
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    // =========================================================================
    // VIEW HOLDERS
    // =========================================================================

    /** Date section header */
    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView txtDateHeader;
        DateViewHolder(@NonNull View v) {
            super(v);
            txtDateHeader = v.findViewById(R.id.txtDateHeader);
        }
    }

    /** Transaction card */
    static class TxnViewHolder extends RecyclerView.ViewHolder {
        View             accentBar;
        ImageView        imgType, imgArrow;
        TextView         tvTitle, tvSub, tvAmount, tvTypePill;
        MaterialCardView iconHolder, iconOuter, chipSub, cardTypePill, badgeCard;

        TxnViewHolder(@NonNull View v) {
            super(v);
            accentBar    = v.findViewById(R.id.accentBar);
            imgType      = v.findViewById(R.id.imgType);
            imgArrow     = v.findViewById(R.id.imgArrow);
            tvTitle      = v.findViewById(R.id.tvTitle);
            tvSub        = v.findViewById(R.id.tvSub);
            tvAmount     = v.findViewById(R.id.tvAmount);
            tvTypePill   = v.findViewById(R.id.tvTypePill);
            iconHolder   = v.findViewById(R.id.iconHolder);
            iconOuter    = v.findViewById(R.id.iconOuter);
            chipSub      = v.findViewById(R.id.chipSub);
            cardTypePill = v.findViewById(R.id.cardTypePill);
            badgeCard    = v.findViewById(R.id.badgeCard);
        }
    }
}