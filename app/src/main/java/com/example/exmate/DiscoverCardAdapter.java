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

public class DiscoverCardAdapter extends RecyclerView.Adapter<DiscoverCardAdapter.VH> {

    private final List<DiscoverCardModel> list;

    public DiscoverCardAdapter(List<DiscoverCardModel> list) {
        this.list = list;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvTitle, tvSub;
        ImageView imgIcon;
        MaterialCardView cardAccent;

        VH(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvDiscoverTitle);
            tvSub = itemView.findViewById(R.id.tvDiscoverSub);
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

        DiscoverCardModel m = list.get(position);

        h.tvTitle.setText(m.getTitle());
        h.tvSub.setText(m.getSubtitle());
        h.imgIcon.setImageResource(m.getIconRes());

        try {
            h.cardAccent.setCardBackgroundColor(Color.parseColor(m.getAccentColor()));
        } catch (Exception ignored) {}
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
}
