package com.example.exmate;

public class TransactionModel {

    private String type;        // "Income" or "Expense"
    private double amount;
    private long time;

    // Optional (depends on type)
    private String source;      // Salary, Business (Income)
    private String category;    // Food, Travel (Expense)

    // 🔹 REQUIRED empty constructor for Firebase
    public TransactionModel() {}

    // 🔹 Income constructor
    public TransactionModel(String type, double amount, long time, String source) {
        this.type = type;
        this.amount = amount;
        this.time = time;
        this.source = source;
    }

    // 🔹 Expense constructor
    public TransactionModel(String type, double amount, long time, String category, boolean isExpense) {
        this.type = type;
        this.amount = amount;
        this.time = time;
        this.category = category;
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

    public String getSource() {
        return source != null ? source : "Income";
    }

    public String getCategory() {
        return category != null ? category : "Expense";
    }
}
