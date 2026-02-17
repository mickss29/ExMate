package com.example.exmate;

public class ExpenseModel {

    public String rawText;

    public double amount;
    public String currency;

    public long timestamp;
    public String dateLabel;

    public String note;
    public String category;

    public String paymentMode;

    public boolean isRecurring;
    public String recurringType;

    public double confidence;
    public String source;

    public ExpenseModel() {
        currency = "INR";
        paymentMode = "Unknown";
        category = "Other";
        source = "voice";
        confidence = 0.5;
        isRecurring = false;
        recurringType = null;
        dateLabel = "today";
        timestamp = System.currentTimeMillis();
        note = "Expense";
        amount = 0;
    }
}
