package com.example.exmate;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminTransactionAdapter
        extends RecyclerView.Adapter<AdminTransactionAdapter.VH> {

    private final List<AdminTransactionModel> list;

    public AdminTransactionAdapter(List<AdminTransactionModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_admin_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        AdminTransactionModel t = list.get(p);

        h.tvTitle.setText(t.getTitle());
        h.tvAmount.setText("₹" + t.getAmount());
        h.tvDate.setText(t.getDate());
        h.tvMode.setText(t.getMode());

        if (t.isIncome()) {
            h.tvAmount.setTextColor(Color.parseColor("#1B8E3E")); // green
        } else {
            h.tvAmount.setTextColor(Color.parseColor("#B00020")); // red
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate, tvMode;

        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvDate = v.findViewById(R.id.tvDate);
            tvMode = v.findViewById(R.id.tvMode);
        }
    }
}
