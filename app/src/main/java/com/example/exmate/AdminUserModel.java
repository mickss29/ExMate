package com.example.exmate;

public class AdminUserModel {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private boolean blocked;
    private String createdAt; // 🔥 FULL DATE + TIME (STRING)

    // REQUIRED empty constructor for Firebase
    public AdminUserModel() {}

    // 🔥 OLD CODE COMPATIBILITY (IMPORTANT)
    public AdminUserModel(String uid, String name, String email,
                          String phone, boolean blocked) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.blocked = blocked;
        this.createdAt = "01 Jan 2000, 12:00 AM"; // safe default
    }

    // 🔥 NEW CONSTRUCTOR (FULL DATA)
    public AdminUserModel(String uid, String name, String email,
                          String phone, boolean blocked, String createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.blocked = blocked;
        this.createdAt = createdAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    // ================= SETTERS =================
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
