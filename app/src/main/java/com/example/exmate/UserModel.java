package com.example.exmate;

/**
 * Firebase-ready UserModel
 * – No-arg constructor required for Firebase deserialization
 * – All setters included
 */
public class UserModel {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String profileImage;
    private String createdAtText;
    private long   createdAtMillis;

    // ── Firebase requires no-arg constructor ──
    public UserModel() {}

    // ── Convenience constructor ──
    public UserModel(String uid, String name, String email, String phone) {
        this.uid   = uid;
        this.name  = name;
        this.email = email;
        this.phone = phone;
        this.role  = "user";
        this.createdAtMillis = System.currentTimeMillis();
    }

    // ── Getters ──
    public String getUid()             { return uid; }
    public String getName()            { return name; }
    public String getEmail()           { return email; }
    public String getPhone()           { return phone; }
    public String getRole()            { return role; }
    public String getProfileImage()    { return profileImage; }
    public String getCreatedAtText()   { return createdAtText; }
    public long   getCreatedAtMillis() { return createdAtMillis; }

    // ── Setters (required by Firebase) ──
    public void setUid(String uid)                     { this.uid = uid; }
    public void setName(String name)                   { this.name = name; }
    public void setEmail(String email)                 { this.email = email; }
    public void setPhone(String phone)                 { this.phone = phone; }
    public void setRole(String role)                   { this.role = role; }
    public void setProfileImage(String profileImage)   { this.profileImage = profileImage; }
    public void setCreatedAtText(String createdAtText) { this.createdAtText = createdAtText; }
    public void setCreatedAtMillis(long createdAtMillis) { this.createdAtMillis = createdAtMillis; }
}