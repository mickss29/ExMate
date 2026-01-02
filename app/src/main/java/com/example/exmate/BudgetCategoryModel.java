package com.example.exmate;

public class BudgetCategoryModel {

    private String name;
    private int iconRes;
    private boolean selected;

    public BudgetCategoryModel(String name, int iconRes) {
        this.name = name;
        this.iconRes = iconRes;
        this.selected = false;
    }

    public String getName() {
        return name;
    }

    public int getIconRes() {
        return iconRes;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
