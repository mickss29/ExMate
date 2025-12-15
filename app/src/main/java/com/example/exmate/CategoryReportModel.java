package com.example.exmate;

public class CategoryReportModel {

    private String category;
    private double amount;

    public CategoryReportModel(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }
}
