package com.example.exmate;

public class AdminTransactionModel {

    private double amount;
    private String title;   // source / category
    private String mode;    // paymentMode
    private String date;    // date string
    private boolean income; // true = income, false = expense

    public AdminTransactionModel() {}

    public AdminTransactionModel(double amount, String title,
                                 String mode, String date, boolean income) {
        this.amount = amount;
        this.title = title;
        this.mode = mode;
        this.date = date;
        this.income = income;
    }

    public double getAmount() {
        return amount;
    }

    public String getTitle() {
        return title;
    }

    public String getMode() {
        return mode;
    }

    public String getDate() {
        return date;
    }

    public boolean isIncome() {
        return income;
    }
}
