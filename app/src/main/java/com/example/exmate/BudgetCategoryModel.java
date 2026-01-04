package com.example.exmate;

public class BudgetCategoryModel {

    private String name;
    private int budget;
    private int spent;

    // ✅ selection state (for bottom sheet)
    private boolean selected;

    public BudgetCategoryModel() {}

    public BudgetCategoryModel(String name) {
        this.name = name;
        this.budget = 0;
        this.spent = 0;
        this.selected = false;
    }

    public BudgetCategoryModel(String name, int budget) {
        this.name = name;
        this.budget = budget;
        this.spent = 0;
        this.selected = false;
    }

    // ===== GETTERS =====

    public String getName() {
        return name;
    }

    public int getBudget() {
        return budget;
    }

    public int getSpent() {
        return spent;
    }

    public int getRemaining() {
        return Math.max(budget - spent, 0);
    }

    // ===== SETTERS =====

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public void setSpent(int spent) {
        this.spent = spent;
    }

    // ===== SELECTION (IMPORTANT FIX) =====

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
