package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DummyAdapter extends RecyclerView.Adapter<DummyAdapter.ViewHolder> {

    // 🔹 TYPES
    public static final int TYPE_TRANSACTION = 1;
    public static final int TYPE_STAT_CATEGORY = 2;

    private int adapterType = TYPE_TRANSACTION;

    // 🔹 TRANSACTION DATA
    private List<Transaction> transactionList;

    // 🔹 STATISTICS DATA
    private List<String> categoryList;

    // ================= TRANSACTION CONSTRUCTOR =================
    public DummyAdapter() {
        adapterType = TYPE_TRANSACTION;

        transactionList = new ArrayList<>();
        transactionList.add(new Transaction("Food", "- ₹450"));
        transactionList.add(new Transaction("Groceries", "- ₹2,000"));
        transactionList.add(new Transaction("Electricity Bill", "- ₹1,500"));
        transactionList.add(new Transaction("Movie", "- ₹500"));
    }

    // ================= STATISTICS CONSTRUCTOR =================
    public DummyAdapter(List<String> categories) {
        adapterType = TYPE_STAT_CATEGORY;
        categoryList = categories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;

        if (adapterType == TYPE_STAT_CATEGORY) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stat_category, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transection, parent, false);
        }

        return new ViewHolder(view, adapterType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (adapterType == TYPE_STAT_CATEGORY) {
            // 🔹 STATISTICS (DUMMY UI)
            holder.txtIcon.setText("🍔");
            holder.txtCategory.setText(categoryList.get(position));
            holder.txtPercent.setText("45% of total spend");
            holder.txtAmount.setText("₹4,500");
        } else {
            // 🔹 TRANSACTIONS (MATCHES YOUR XML)
            Transaction transaction = transactionList.get(position);

            holder.txtIcon.setText("🍔");
            holder.txtCategory.setText(transaction.name);
            holder.txtAmount.setText(transaction.amount);

            holder.txtNote.setText("Dinner with friends");
            holder.txtMeta.setText("12 Sep 2025 • 8:45 PM • UPI");
            holder.txtBadge.setText("HIGH SPEND");
        }
    }

    @Override
    public int getItemCount() {
        if (adapterType == TYPE_STAT_CATEGORY) {
            return categoryList == null ? 0 : categoryList.size();
        } else {
            return transactionList == null ? 0 : transactionList.size();
        }
    }

    // ================= VIEW HOLDER =================
    public static class ViewHolder extends RecyclerView.ViewHolder {

        // 🔹 SHARED / TRANSACTION VIEWS
        TextView txtIcon, txtCategory, txtAmount, txtNote, txtMeta, txtBadge;

        // 🔹 STATISTICS EXTRA
        TextView txtPercent;

        public ViewHolder(@NonNull View itemView, int type) {
            super(itemView);

            if (type == TYPE_STAT_CATEGORY) {
                txtIcon = itemView.findViewById(R.id.txtIcon);
                txtCategory = itemView.findViewById(R.id.txtCategory);
                txtPercent = itemView.findViewById(R.id.txtPercent);
                txtAmount = itemView.findViewById(R.id.txtAmount);
            } else {
                txtIcon = itemView.findViewById(R.id.txtIcon);
                txtCategory = itemView.findViewById(R.id.txtCategory);
                txtAmount = itemView.findViewById(R.id.txtAmount);
                txtNote = itemView.findViewById(R.id.txtNote);
                txtMeta = itemView.findViewById(R.id.txtMeta);
                txtBadge = itemView.findViewById(R.id.txtBadge);
            }
        }
    }

    // ================= TRANSACTION MODEL =================
    static class Transaction {
        String name;
        String amount;

        Transaction(String name, String amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
