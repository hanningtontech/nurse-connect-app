package com.example.nurse_connect.models;

import java.io.Serializable;

public class FeaturedContent implements Serializable {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String contentType; // "study_material", "group", "task", "user"
    private String contentId; // ID of the actual content
    private int engagementScore;
    private int likes;
    private int views;
    private int comments;
    private String authorName;
    private String authorPhotoUrl;
    private String category;
    private boolean isRelevantToCourse;
    private long timestamp;

    public FeaturedContent() {}

    public FeaturedContent(String id, String title, String description, String imageUrl, 
                          String contentType, String contentId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.contentType = contentType;
        this.contentId = contentId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public int getEngagementScore() { return engagementScore; }
    public void setEngagementScore(int engagementScore) { this.engagementScore = engagementScore; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorPhotoUrl() { return authorPhotoUrl; }
    public void setAuthorPhotoUrl(String authorPhotoUrl) { this.authorPhotoUrl = authorPhotoUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isRelevantToCourse() { return isRelevantToCourse; }
    public void setRelevantToCourse(boolean relevantToCourse) { isRelevantToCourse = relevantToCourse; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Calculate engagement score based on likes, views, and comments
    public void calculateEngagementScore() {
        this.engagementScore = (likes * 3) + (comments * 5) + (views * 1);
    }
}
