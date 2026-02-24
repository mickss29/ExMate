package com.example.exmate;

public class DiscoverOfferModel {

    // 🔥 IMPORTANT: Firebase key (not stored inside DB automatically)
    private String id;

    private String title;
    private String subtitle;
    private String category;
    private String couponCode;
    private String expiryDateTime;
    private String createdAt;
    private String imageUrl;

    // Optional (remove if not using anymore)
    private String accentColor;

    private int discountPercent;

    private boolean isActive;

    // 🔥 REQUIRED empty constructor for Firebase
    public DiscoverOfferModel() {}

    // ================= GETTERS =================

    public String getId() {
        return id == null ? "" : id;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public String getSubtitle() {
        return subtitle == null ? "" : subtitle;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public String getCouponCode() {
        return couponCode == null ? "" : couponCode;
    }

    public String getExpiryDateTime() {
        return expiryDateTime == null ? "" : expiryDateTime;
    }

    public String getCreatedAt() {
        return createdAt == null ? "" : createdAt;
    }

    public String getImageUrl() {
        return imageUrl == null ? "" : imageUrl;
    }

    public String getAccentColor() {
        return accentColor == null ? "#FFFFFF" : accentColor;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public boolean isActive() {
        return isActive;
    }

    // ================= SETTERS =================

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public void setExpiryDateTime(String expiryDateTime) {
        this.expiryDateTime = expiryDateTime;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}