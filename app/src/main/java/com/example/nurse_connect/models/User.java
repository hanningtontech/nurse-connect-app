package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class User {
    @DocumentId
    private String uid = "";
    private String email = "";
    private String username = "";
    private String displayName = "";
    private String handle = ""; // @username format for tagging
    private String phoneNumber;
    private String photoURL;
    private Timestamp createdAt = Timestamp.now();
    private Timestamp lastLoginAt = Timestamp.now();
    private boolean emailVerified = false;
    private boolean isOnline = false;
    private long lastSeen = 0;
    private UserProfile profile = new UserProfile();
    private UserSettings settings = new UserSettings();

    // Default constructor for Firestore
    public User() {}

    public User(String uid, String email, String username, String displayName) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.displayName = displayName;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPhotoURL() { return photoURL; }
    public void setPhotoURL(String photoURL) { this.photoURL = photoURL; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public UserSettings getSettings() { return settings; }
    public void setSettings(UserSettings settings) { this.settings = settings; }
} 