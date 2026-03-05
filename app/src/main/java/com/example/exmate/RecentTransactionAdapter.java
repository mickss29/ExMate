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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter
        extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    // =========================================================================
    // PALETTE
    // =========================================================================
    private static final int COLOR_GREEN         = Color.parseColor("#00C896");
    private static final int COLOR_GREEN_BADGE   = Color.parseColor("#0D2E1E");
    private static final int COLOR_GREEN_PILL    = Color.parseColor("#0A2018");
    private static final int COLOR_RED           = Color.parseColor("#FF5A5A");
    private static final int COLOR_RED_BADGE     = Color.parseColor("#2E0D0D");
    private static final int COLOR_RED_PILL      = Color.parseColor("#200A0A");
    private static final int COLOR_ICON_RING     = Color.parseColor("#141929");

    // =========================================================================
    // DATA
    // =========================================================================
    private final List<TransactionModel> list;
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");

    public RecentTransactionAdapter(List<TransactionModel> list) {
        this.list = list;
        setHasStableIds(true);
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    static class ViewHolder extends RecyclerView.ViewHolder {

        View             accentBar;
        ImageView        imgType, imgArrow;
        TextView         tvTitle, tvSub, tvAmount, tvTypePill;
        MaterialCardView iconHolder, iconOuter, chipSub, cardTypePill, badgeCard;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            accentBar    = itemView.findViewById(R.id.accentBar);
            imgType      = itemView.findViewById(R.id.imgType);
            imgArrow     = itemView.findViewById(R.id.imgArrow);
            tvTitle      = itemView.findViewById(R.id.tvTitle);
            tvSub        = itemView.findViewById(R.id.tvSub);
            tvAmount     = itemView.findViewById(R.id.tvAmount);
            tvTypePill   = itemView.findViewById(R.id.tvTypePill);
            iconHolder   = itemView.findViewById(R.id.iconHolder);
            iconOuter    = itemView.findViewById(R.id.iconOuter);
            chipSub      = itemView.findViewById(R.id.chipSub);
            cardTypePill = itemView.findViewById(R.id.cardTypePill);
            badgeCard    = itemView.findViewById(R.id.badgeCard);
        }
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_transection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        TransactionModel model    = list.get(position);
        boolean          isIncome = "Income".equalsIgnoreCase(model.getType());

        // ── Title ──
        h.tvTitle.setText(isIncome
                ? safeText(model.getSource(),   "Income")
                : safeText(model.getCategory(), "Expense"));

        // ── Date ──
        h.tvSub.setText(formatTime(model.getTime()));

        // ── Amount ──
        String amt = moneyFormat.format(model.getAmount());
        h.tvAmount.setText(isIncome ? "+ ₹" + amt : "- ₹" + amt);

        // ── Colour scheme ──
        applyTheme(h, isIncome);

        // ── Press scale animation ──
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

    @Override
    public long getItemId(int position) {
        return list.get(position).getTime();
    }

    // =========================================================================
    // THEME HELPER — centralised so both income and expense
    //                never diverge across adapters
    // =========================================================================
    private void applyTheme(ViewHolder h, boolean isIncome) {

        int mainColor  = isIncome ? COLOR_GREEN       : COLOR_RED;
        int badgeBg    = isIncome ? COLOR_GREEN_BADGE  : COLOR_RED_BADGE;
        int pillBg     = isIncome ? COLOR_GREEN_PILL   : COLOR_RED_PILL;
        String pillTxt = isIncome ? "INCOME"           : "EXPENSE";
        int arrowRes   = isIncome ? R.drawable.ic_arrow_up   : R.drawable.ic_arrow_down;
        int iconRes    = isIncome ? R.drawable.ic_income      : R.drawable.ic_expense;

        // Left bar
        if (h.accentBar    != null) h.accentBar.setBackgroundColor(mainColor);

        // Amount
        h.tvAmount.setTextColor(mainColor);

        // Arrow badge
        if (h.imgArrow != null) {
            h.imgArrow.setImageResource(arrowRes);
            h.imgArrow.setColorFilter(mainColor);
        }

        // Category icon
        h.imgType.setImageResource(iconRes);
        h.imgType.setColorFilter(mainColor);

        // Icon circles
        if (h.iconOuter  != null) h.iconOuter.setCardBackgroundColor(COLOR_ICON_RING);
        if (h.iconHolder != null) h.iconHolder.setCardBackgroundColor(badgeBg);

        // Type pill
        if (h.cardTypePill != null) h.cardTypePill.setCardBackgroundColor(pillBg);
        if (h.tvTypePill   != null) {
            h.tvTypePill.setText(pillTxt);
            h.tvTypePill.setTextColor(mainColor);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String safeText(String t, String fallback) {
        if (t == null) return fallback;
        String s = t.trim();
        return s.isEmpty() ? fallback : s;
    }

    private String formatTime(long millis) {
        Calendar now       = Calendar.getInstance();
        Calendar date      = Calendar.getInstance();
        date.setTimeInMillis(millis);

        boolean sameYear   = now.get(Calendar.YEAR) == date.get(Calendar.YEAR);
        boolean isToday    = sameYear &&
                now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR);

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday =
                yesterday.get(Calendar.YEAR)        == date.get(Calendar.YEAR) &&
                        yesterday.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR);

        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (isToday)     return "Today · "     + timeFmt.format(new Date(millis));
        if (isYesterday) return "Yesterday · " + timeFmt.format(new Date(millis));

        SimpleDateFormat fullFmt = sameYear
                ? new SimpleDateFormat("EEE, dd MMM · hh:mm a", Locale.getDefault())
                : new SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault());

        return fullFmt.format(new Date(millis));
    }
}