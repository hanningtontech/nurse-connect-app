package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import java.io.Serializable;
import java.util.List;

public class StudyMaterial implements Serializable {
    @DocumentId
    private String id = "";
    private String title = "";
    private String description = "";
    private String category = "";
    private List<String> tags;
    private MaterialType materialType = MaterialType.DOCUMENT;
    private String fileUrl = ""; // Changed from fileURL to match repository
    private String fileName = ""; // Added for repository

    private String thumbnailURL;
    private long fileSize = 0;
    private long uploadDate = 0; // Added for repository (timestamp in milliseconds)
    private Integer duration; // for videos in seconds
    private String authorId = "";
    private String authorName = "";
    private String authorPhotoURL;
    private String uploadedBy = ""; // Field for Firestore compatibility
    private transient Timestamp createdAt = Timestamp.now();
    private transient Timestamp updatedAt = Timestamp.now();
    private int likes = 0;
    private int downloads = 0;
    private int shares = 0;
    private int views = 0;
    private int commentCount = 0;
    private float rating = 0f;
    private int reviewCount = 0;
    private String privacy = "public"; // public, private, friends
    private boolean isOfficial = false;
    private boolean isPremium = false;
    private boolean isLikedByUser = false; // Track if current user has liked this material
    private boolean hasRatedByUser = false; // Track if current user has rated this material
    private boolean isDownloadedByUser = false; // Track if current user has downloaded this material
    private long lastModified = 0; // Track when the document was last modified

    // Default constructor for Firestore
    public StudyMaterial() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public MaterialType getMaterialType() { return materialType; }
    public void setMaterialType(MaterialType materialType) { this.materialType = materialType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getType() { return materialType.toString(); }
    public void setType(String type) { 
        try {
            this.materialType = MaterialType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.materialType = MaterialType.DOCUMENT; // Default fallback
        }
    }

    public String getPrice() { return isPremium ? "Premium" : "Free"; }
    public void setPrice(String price) { 
        this.isPremium = "Premium".equalsIgnoreCase(price) || !"Free".equalsIgnoreCase(price);
    }

    public long getUploadDate() { return uploadDate; }
    public void setUploadDate(long uploadDate) { this.uploadDate = uploadDate; }

    public String getThumbnailURL() { return thumbnailURL; }
    public void setThumbnailURL(String thumbnailURL) { this.thumbnailURL = thumbnailURL; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorPhotoURL() { return authorPhotoURL; }
    public void setAuthorPhotoURL(String authorPhotoURL) { this.authorPhotoURL = authorPhotoURL; }
    
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getDownloads() { return downloads; }
    public void setDownloads(int downloads) { this.downloads = downloads; }

    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    
    public boolean isLikedByUser() { return isLikedByUser; }
    public void setLikedByUser(boolean likedByUser) { isLikedByUser = likedByUser; }
    
    public boolean hasRatedByUser() { return hasRatedByUser; }
    public void setHasRatedByUser(boolean hasRatedByUser) { this.hasRatedByUser = hasRatedByUser; }

    public boolean isDownloadedByUser() { return isDownloadedByUser; }
    public void setDownloadedByUser(boolean downloadedByUser) { this.isDownloadedByUser = downloadedByUser; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
} 