package com.example.exmate;

public class TransactionModel {

    private String title;
    private String category;
    private String date;
    private String amount;
    private boolean isIncome;

    public TransactionModel(String title, String category, String date, String amount, boolean isIncome) {
        this.title = title;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.isIncome = isIncome;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getAmount() {
        return amount;
    }

    public boolean isIncome() {
        return isIncome;
    }
}
