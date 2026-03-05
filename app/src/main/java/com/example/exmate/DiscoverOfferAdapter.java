package com.example.exmate;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiscoverOfferAdapter
        extends RecyclerView.Adapter<DiscoverOfferAdapter.VH> {

    // =========================================================================
    // OBSIDIAN PALETTE  (matches dashboard + discover activity)
    // =========================================================================
    private static final int COLOR_CARD_DEFAULT   = Color.parseColor("#0F1421");
    private static final int COLOR_STROKE_DEFAULT = Color.parseColor("#1E2A3E");
    private static final int COLOR_GOLD           = Color.parseColor("#D4A853");
    private static final String COLOR_FALLBACK    = "#0F1421";

    // =========================================================================
    // DATA
    // =========================================================================
    private final List<DiscoverOfferModel> list;

    public DiscoverOfferAdapter(List<DiscoverOfferModel> list) {
        this.list = list;
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    static class VH extends RecyclerView.ViewHolder {

        TextView         tvTitle, tvSub, tvDiscount, tvCoupon, tvTimer, tvExpiryExact;
        ImageView        imgIcon;
        MaterialCardView cardAccent;
        CountDownTimer   countDownTimer;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle      = itemView.findViewById(R.id.tvDiscoverTitle);
            tvSub        = itemView.findViewById(R.id.tvDiscoverSub);
            tvDiscount   = itemView.findViewById(R.id.tvDiscount);
            tvCoupon     = itemView.findViewById(R.id.tvCoupon);
            tvTimer      = itemView.findViewById(R.id.tvTimer);
            tvExpiryExact= itemView.findViewById(R.id.tvExpiryExact);
            imgIcon      = itemView.findViewById(R.id.imgDiscoverIcon);
            cardAccent   = itemView.findViewById(R.id.cardDiscoverAccent);
        }
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discover_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        DiscoverOfferModel m = list.get(position);

        // ── Text fields ──
        h.tvTitle.setText(m.getTitle());
        h.tvSub.setText(m.getSubtitle());
        h.tvDiscount.setText(m.getDiscountPercent() + "% OFF");
        // BUG FIX: show only the code in the chip (label was already baked into XML)
        h.tvCoupon.setText(m.getCouponCode());
        h.tvExpiryExact.setText("Expires " + m.getExpiryDateTime());

        // ── Icon: load image URL with Glide, fallback to ic_offer ──
        if (m.getImageUrl() != null && !m.getImageUrl().isEmpty()) {
            Glide.with(h.imgIcon.getContext())
                    .load(m.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .placeholder(R.drawable.ic_offer)
                    .error(R.drawable.ic_offer)
                    .into(h.imgIcon);
        } else {
            h.imgIcon.setImageResource(R.drawable.ic_offer);
            h.imgIcon.setColorFilter(Color.parseColor("#8899B4"));
        }

        // ── Card accent color from Firebase (fallback to dark default) ──
        try {
            String hex = m.getAccentColor();
            if (hex != null && hex.startsWith("#") && (hex.length() == 7 || hex.length() == 9)) {
                // Apply a very dark tint so text stays legible on Obsidian bg
                h.cardAccent.setStrokeColor(Color.parseColor(hex));
                h.cardAccent.setCardBackgroundColor(COLOR_CARD_DEFAULT);
            } else {
                h.cardAccent.setCardBackgroundColor(COLOR_CARD_DEFAULT);
                h.cardAccent.setStrokeColor(COLOR_STROKE_DEFAULT);
            }
        } catch (Exception ignored) {
            h.cardAccent.setCardBackgroundColor(COLOR_CARD_DEFAULT);
            h.cardAccent.setStrokeColor(COLOR_STROKE_DEFAULT);
        }

        // ── Countdown timer ──
        if (h.countDownTimer != null) {
            h.countDownTimer.cancel();
            h.countDownTimer = null;
        }

        startCountdown(h, m.getExpiryDateTime());

        // ── Coupon chip → copy to clipboard ──
        h.tvCoupon.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(
                        ClipData.newPlainText("Coupon Code", m.getCouponCode()));
                Toast.makeText(v.getContext(),
                        "Code copied: " + m.getCouponCode(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // ── Card click → open offer link ──
        h.cardAccent.setOnClickListener(v -> openLink(v.getContext(), m.getLink()));

        // ── Press animation ──
        h.cardAccent.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
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
        return list == null ? 0 : list.size();
    }

    // BUG FIX: cancel timer when view is recycled to prevent
    //          ghost updates on the wrong item after scroll
    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        if (holder.countDownTimer != null) {
            holder.countDownTimer.cancel();
            holder.countDownTimer = null;
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void startCountdown(@NonNull VH h, String expiryDateTime) {
        if (expiryDateTime == null || expiryDateTime.isEmpty()) {
            h.tvTimer.setText("—");
            return;
        }

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Date expiryDate = sdf.parse(expiryDateTime);
            if (expiryDate == null) { h.tvTimer.setText("—"); return; }

            long remaining = expiryDate.getTime() - System.currentTimeMillis();

            if (remaining <= 0) {
                h.tvTimer.setText("Expired");
                h.tvTimer.setTextColor(Color.parseColor("#FF5A5A"));
                return;
            }

            // Reset to gold for active offers
            h.tvTimer.setTextColor(COLOR_GOLD);

            h.countDownTimer = new CountDownTimer(remaining, 1000) {
                @Override
                public void onTick(long ms) {
                    long h2 = ms / (1000 * 60 * 60);
                    long m2 = (ms / (1000 * 60)) % 60;
                    long s2 = (ms / 1000) % 60;
                    h.tvTimer.setText(h2 + "h " + m2 + "m " + s2 + "s");
                }

                @Override
                public void onFinish() {
                    h.tvTimer.setText("Expired");
                    h.tvTimer.setTextColor(Color.parseColor("#FF5A5A"));
                }
            }.start();

        } catch (Exception ignored) {
            h.tvTimer.setText("—");
        }
    }

    private void openLink(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, "No link available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show();
        }
    }
}