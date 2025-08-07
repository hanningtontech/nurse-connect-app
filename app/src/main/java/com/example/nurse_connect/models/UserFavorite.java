package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class UserFavorite {
    @DocumentId
    private String id = "";
    private String userId = "";
    private String materialId = "";
    private String materialTitle = "";
    private String materialCategory = "";
    private String authorName = "";
    private String authorId = "";
    private Timestamp addedAt = Timestamp.now();
    
    // Default constructor for Firestore
    public UserFavorite() {}
    
    // Constructor for creating new favorites
    public UserFavorite(String userId, String materialId, String materialTitle, 
                       String materialCategory, String authorName, String authorId) {
        this.userId = userId;
        this.materialId = materialId;
        this.materialTitle = materialTitle;
        this.materialCategory = materialCategory;
        this.authorName = authorName;
        this.authorId = authorId;
        this.addedAt = Timestamp.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    
    public String getMaterialTitle() { return materialTitle; }
    public void setMaterialTitle(String materialTitle) { this.materialTitle = materialTitle; }
    
    public String getMaterialCategory() { return materialCategory; }
    public void setMaterialCategory(String materialCategory) { this.materialCategory = materialCategory; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }
} 