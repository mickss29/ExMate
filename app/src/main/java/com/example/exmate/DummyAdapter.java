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

    private List<Transaction> transactionList;

    public DummyAdapter() {
        // Dummy data
        transactionList = new ArrayList<>();
        transactionList.add(new Transaction("Salary", "₹50,000"));
        transactionList.add(new Transaction("Groceries", "₹2,000"));
        transactionList.add(new Transaction("Electricity Bill", "₹1,500"));
        transactionList.add(new Transaction("Movie", "₹500"));
    }

    @NonNull
    @Override
    public DummyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DummyAdapter.ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvTransactionName.setText(transaction.name);
        holder.tvTransactionAmount.setText(transaction.amount);
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionName, tvTransactionAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionName = itemView.findViewById(R.id.tvTransactionName);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);
        }
    }

    // Inner class for transaction data
    static class Transaction {
        String name;
        String amount;

        Transaction(String name, String amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
