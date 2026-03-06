package com.example.exmate;

public class TransactionModel {

    private String type;
    private double amount;
    private long time;
    private String source;
    private String category;
    private String paymentMode;
    private String note;
    private boolean recurring;
    private String recurringType;

    // ✅ REQUIRED: Firebase needs this empty constructor
    public TransactionModel() {}

    // ✅ REQUIRED: Used in HomeFragment → Income transactions
    // new TransactionModel("Income", amount, time, source)
    public TransactionModel(String type, double amount, long time, String source) {
        this.type   = type;
        this.amount = amount;
        this.time   = time;
        this.source = source;
    }

    // ✅ REQUIRED: Used in HomeFragment → Expense transactions
    // new TransactionModel("Expense", amount, time, category, true)
    public TransactionModel(String type, double amount, long time,
                            String category, boolean isExpense) {
        this.type     = type;
        this.amount   = amount;
        this.time     = time;
        this.category = category;
    }

    // ================= GETTERS =================

    public String getType()        { return type        != null ? type        : ""; }
    public double getAmount()      { return amount; }
    public long   getTime()        { return time; }
    public String getSource()      { return source      != null ? source      : ""; }
    public String getCategory()    { return category    != null ? category    : ""; }
    public String getPaymentMode() { return paymentMode != null ? paymentMode : "Cash"; }
    public String getNote()        { return note        != null ? note        : ""; }
    public boolean isRecurring()   { return recurring; }
    public String getRecurringType(){ return recurringType != null ? recurringType : "None"; }

    // ================= SETTERS (Firebase needs these) =================

    public void setType(String type)               { this.type = type; }
    public void setAmount(double amount)           { this.amount = amount; }
    public void setTime(long time)                 { this.time = time; }
    public void setSource(String source)           { this.source = source; }
    public void setCategory(String category)       { this.category = category; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public void setNote(String note)               { this.note = note; }
    public void setRecurring(boolean recurring)    { this.recurring = recurring; }
    public void setRecurringType(String type)      { this.recurringType = type; }
}