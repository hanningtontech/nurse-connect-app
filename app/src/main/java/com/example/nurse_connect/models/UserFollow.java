package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class UserFollow {
    @DocumentId
    private String id = "";
    private String followerId = "";
    private String followingId = "";
    private Timestamp createdAt = Timestamp.now();

    // Default constructor for Firestore
    public UserFollow() {}

    public UserFollow(String followerId, String followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFollowerId() { return followerId; }
    public void setFollowerId(String followerId) { this.followerId = followerId; }

    public String getFollowingId() { return followingId; }
    public void setFollowingId(String followingId) { this.followingId = followingId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
} 