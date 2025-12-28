package com.example.exmate;

public class CategoryModel {
    public String id;
    public String name;
    public long limit;
    public long spent;
    public String icon;

    public CategoryModel() {} // Firebase required

    public CategoryModel(String id, String name, long limit, long spent, String icon) {
        this.id = id;
        this.name = name;
        this.limit = limit;
        this.spent = spent;
        this.icon = icon;
    }
}
