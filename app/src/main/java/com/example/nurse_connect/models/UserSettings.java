package com.example.nurse_connect.models;

public class UserSettings {
    private boolean notifications = true;
    private String privacy = "public"; // public, private, friends

    public UserSettings() {}

    // Getters and Setters
    public boolean isNotifications() { return notifications; }
    public void setNotifications(boolean notifications) { this.notifications = notifications; }

    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
} 