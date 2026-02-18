package com.example.exmate;

public class DiscoverCardModel {

    private String title;
    private String subtitle;
    private String imageUrl;     // from Firebase
    private String accentColor;
    private String category;     // Shopping / Movies / Food / Stocks

    public DiscoverCardModel() {
        // Required empty constructor for Firebase
    }

    public DiscoverCardModel(String title, String subtitle,
                             String imageUrl,
                             String accentColor,
                             String category) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.accentColor = accentColor;
        this.category = category;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getImageUrl() { return imageUrl; }
    public String getAccentColor() { return accentColor; }
    public String getCategory() { return category; }
}
