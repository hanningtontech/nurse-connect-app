package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;

public class DocumentRating {
    private String id = "";
    private String documentId = "";
    private String userId = "";
    private String userName = "";
    private String userPhotoURL = "";
    private int rating = 0; // 1-5 stars
    private String comment = "";
    private Timestamp createdAt = Timestamp.now();
    private Timestamp updatedAt = Timestamp.now();

    // Default constructor for Firestore
    public DocumentRating() {}

    public DocumentRating(String documentId, String userId, String userName, int rating, String comment) {
        this.documentId = documentId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoURL() { return userPhotoURL; }
    public void setUserPhotoURL(String userPhotoURL) { this.userPhotoURL = userPhotoURL; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
} 