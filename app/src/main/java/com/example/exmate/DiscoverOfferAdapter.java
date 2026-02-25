package com.example.exmate;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiscoverOfferAdapter
        extends RecyclerView.Adapter<DiscoverOfferAdapter.VH> {

    private final List<DiscoverOfferModel> list;

    public DiscoverOfferAdapter(List<DiscoverOfferModel> list) {
        this.list = list;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvTitle, tvSub, tvDiscount,
                tvCoupon, tvTimer, tvExpiryExact;

        ImageView imgIcon;
        MaterialCardView cardAccent;

        CountDownTimer countDownTimer;

        public VH(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvDiscoverTitle);
            tvSub = itemView.findViewById(R.id.tvDiscoverSub);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvCoupon = itemView.findViewById(R.id.tvCoupon);
            tvTimer = itemView.findViewById(R.id.tvTimer);
            tvExpiryExact = itemView.findViewById(R.id.tvExpiryExact);

            imgIcon = itemView.findViewById(R.id.imgDiscoverIcon);
            cardAccent = itemView.findViewById(R.id.cardDiscoverAccent);
        }
    }

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

        h.tvTitle.setText(m.getTitle());
        h.tvSub.setText(m.getSubtitle());
        h.tvDiscount.setText(m.getDiscountPercent() + "% OFF");
        h.tvCoupon.setText("Use Code: " + m.getCouponCode());

        Glide.with(h.itemView.getContext())
                .load(m.getImageUrl())
                .into(h.imgIcon);

        try {
            h.cardAccent.setCardBackgroundColor(
                    Color.parseColor(m.getAccentColor()));
        } catch (Exception ignored) {}

        h.tvExpiryExact.setText("Expires on: " + m.getExpiryDateTime());

        if (h.countDownTimer != null) {
            h.countDownTimer.cancel();
        }

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault());

            Date expiryDate =
                    sdf.parse(m.getExpiryDateTime());

            if (expiryDate != null) {

                long remaining =
                        expiryDate.getTime() - System.currentTimeMillis();

                if (remaining > 0) {

                    h.countDownTimer =
                            new CountDownTimer(remaining, 1000) {

                                @Override
                                public void onTick(long millisUntilFinished) {

                                    long hours =
                                            millisUntilFinished / (1000 * 60 * 60);

                                    long minutes =
                                            (millisUntilFinished / (1000 * 60)) % 60;

                                    long seconds =
                                            (millisUntilFinished / 1000) % 60;

                                    h.tvTimer.setText(
                                            "Time Left: "
                                                    + hours + "h "
                                                    + minutes + "m "
                                                    + seconds + "s"
                                    );
                                }

                                @Override
                                public void onFinish() {
                                    h.tvTimer.setText("Expired");
                                }
                            }.start();
                }
            }

        } catch (Exception ignored) {}

        // Coupon copy
        h.tvCoupon.setOnClickListener(v -> {

            ClipboardManager clipboard =
                    (ClipboardManager) v.getContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip =
                    ClipData.newPlainText(
                            "Coupon Code",
                            m.getCouponCode());

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(),
                        "Coupon copied!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // 🔥 CARD CLICK → OPEN LINK
        h.cardAccent.setOnClickListener(v -> {

            Toast.makeText(v.getContext(),
                    "Opening offer...",
                    Toast.LENGTH_SHORT).show();

            String url = m.getLink();
            Log.d("LINK_DEBUG", "URL = " + url);

            if (url == null || url.trim().isEmpty()) {
                Toast.makeText(v.getContext(),
                        "No link found",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(v.getContext(),
                        "Cannot open link",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        if (holder.countDownTimer != null) {
            holder.countDownTimer.cancel();
        }
    }
}