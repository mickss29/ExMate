package com.example.exmate;

public class DiscoverCardModel {

    private String title;
    private String subtitle;
    private int iconRes;
    private String accentColor;

    public DiscoverCardModel(String title, String subtitle, int iconRes, String accentColor) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.accentColor = accentColor;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getIconRes() { return iconRes; }
    public String getAccentColor() { return accentColor; }
}
