package com.example.nurse_connect.models;

import com.google.firebase.firestore.DocumentId;

public class Comment {
    @DocumentId
    private String id;
    private String userId;
    private String username;
    private String text;
    private Long createdAt;
    private int likes;
    private int replyCount;
    private String parentCommentId;
    private String replyingToUsername;

    // Default constructor for Firestore
    public Comment() {}

    public Comment(String userId, String username, String text) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.createdAt = Long.valueOf(System.currentTimeMillis());
        this.likes = 0;
        this.replyCount = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getReplyingToUsername() {
        return replyingToUsername;
    }

    public void setReplyingToUsername(String replyingToUsername) {
        this.replyingToUsername = replyingToUsername;
    }

    public boolean isReply() {
        return parentCommentId != null && !parentCommentId.isEmpty();
    }
} 