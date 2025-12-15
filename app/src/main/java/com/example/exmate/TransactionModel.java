package com.example.exmate;

public class TransactionModel {

    private String type;   // "Income" or "Expense"
    private double amount;
    private long time;     // timestamp

    // 🔹 Empty constructor (REQUIRED for Firebase)
    public TransactionModel() {
    }

    // 🔹 Constructor used in Dashboard
    public TransactionModel(String type, double amount, long time) {
        this.type = type;
        this.amount = amount;
        this.time = time;
    }

    // ================= GETTERS =================

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public long getTime() {
        return time;
    }
}
