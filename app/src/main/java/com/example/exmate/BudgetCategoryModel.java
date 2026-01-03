package com.example.exmate;

public class BudgetCategoryModel {

    private String name;
    private int amount;   // budget
    private int spent;    // spent amount
    private boolean selected;

    // Constructor 1
    public BudgetCategoryModel(String name) {
        this.name = name;
        this.amount = 0;
        this.spent = 0;
        this.selected = false;
    }

    // Constructor 2
    public BudgetCategoryModel(String name, int amount) {
        this.name = name;
        this.amount = amount;
        this.spent = 0;
        this.selected = false;
    }

    // ===== GETTERS / SETTERS =====
    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    // ✅ THIS FIXES YOUR ERROR
    public int getSpent() {
        return spent;
    }

    public void setSpent(int spent) {
        this.spent = spent;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
