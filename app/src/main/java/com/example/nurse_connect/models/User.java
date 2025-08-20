package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class User {
    @DocumentId
    private String uid = "";
    private String email = "";
    private String username = "";
    private String displayName = "";
    private String handle = ""; // @username format for tagging
    private String phoneNumber = "";
    private String photoURL = "";
    
    // New required fields from enhanced sign-up
    private String nursingCareer = "";
    
    // Optional fields from enhanced sign-up
    private String yearsExperience = "";
    private String currentInstitution = "";
    
    @ServerTimestamp
    private Timestamp createdAt;
    @ServerTimestamp
    private Timestamp lastLoginAt;
    @ServerTimestamp
    private Timestamp updatedAt;
    
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
        this.createdAt = Timestamp.now();
        this.lastLoginAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Enhanced constructor with all fields
    public User(String uid, String email, String username, String displayName, String phoneNumber, 
                String nursingCareer, String yearsExperience, String currentInstitution) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.nursingCareer = nursingCareer;
        this.yearsExperience = yearsExperience;
        this.currentInstitution = currentInstitution;
        this.createdAt = Timestamp.now();
        this.lastLoginAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
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

    public String getNursingCareer() { return nursingCareer; }
    public void setNursingCareer(String nursingCareer) { this.nursingCareer = nursingCareer; }

    public String getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(String yearsExperience) { this.yearsExperience = yearsExperience; }

    public String getCurrentInstitution() { return currentInstitution; }
    public void setCurrentInstitution(String currentInstitution) { this.currentInstitution = currentInstitution; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

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

    // Helper method to check if profile is complete
    public boolean isProfileComplete() {
        return email != null && !email.isEmpty() &&
               username != null && !username.isEmpty() &&
               displayName != null && !displayName.isEmpty() &&
               phoneNumber != null && !phoneNumber.isEmpty() &&
               nursingCareer != null && !nursingCareer.isEmpty();
    }

    // Helper method to get display name or username as fallback
    public String getDisplayNameOrUsername() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        return username != null ? username : "Unknown User";
    }

    // Helper method to get profile photo URL or default
    public String getProfilePhotoUrl() {
        if (photoURL != null && !photoURL.trim().isEmpty()) {
            return photoURL;
        }
        return null; // Will use default placeholder in UI
    }
} 