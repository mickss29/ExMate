package com.example.exmate;

public class DiscoverCategoryModel {

    private String title;
    private int icon;

    // Required empty constructor for RecyclerView / Firebase
    public DiscoverCategoryModel() {
    }

    public DiscoverCategoryModel(String title, int icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public int getIcon() {
        return icon;
    }

    // Optional setters (good practice)
    public void setTitle(String title) {
        this.title = title;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
