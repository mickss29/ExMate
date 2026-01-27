package com.example.exmate;

public class AdminUserModel {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private boolean blocked;
    private long createdAt; // 🔥 for filter (week / month / date)

    // REQUIRED empty constructor for Firebase
    public AdminUserModel() {}

    public AdminUserModel(String uid, String name, String email, String phone, boolean blocked) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.blocked = blocked;
        this.createdAt = 0L; // safe default (no crash)
    }

    // ================= GETTERS =================
    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // ================= SETTERS =================
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
