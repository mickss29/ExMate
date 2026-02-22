package com.example.exmate;

public class DiscoverCategoryModel {

    private String title;
    private int icon;

    // Required empty constructor
    public DiscoverCategoryModel() {
    }

    public DiscoverCategoryModel(String title, int icon) {
        this.title = title;
        this.icon = icon;
    }

    // 🔥 Existing Getter
    public String getTitle() {
        return title;
    }

    // 🔥 NEW Getter (Adapter Compatible)
    public String getCategoryName() {
        return title;
    }

    public int getIcon() {
        return icon;
    }

    // Optional setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}