package com.example.exmate;

public class BudgetCategoryModel {

    private String name;
    private int icon;
    private boolean selected;

    public BudgetCategoryModel(String name, int icon) {
        this.name = name;
        this.icon = icon;
        this.selected = false;
    }

    public String getName() { return name; }
    public int getIcon() { return icon; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
