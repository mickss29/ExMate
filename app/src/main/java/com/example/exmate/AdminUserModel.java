package com.example.exmate;

public class AdminUserModel {

    private String name;
    private String email;
    private boolean blocked;

    public AdminUserModel(String name, String email, boolean blocked) {
        this.name = name;
        this.email = email;
        this.blocked = blocked;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
