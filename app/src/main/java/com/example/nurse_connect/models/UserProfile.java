package com.example.nurse_connect.models;

public class UserProfile {
    private String studyYear = "";
    private String specialization = "";
    private String institution = "";
    private String bio = "";
    private String course = "";
    private String gpa = "";
    
    // Achievement fields
    private int followersCount = 0;
    private int followingCount = 0;
    private int totalDownloads = 0;
    private float averageRating = 0.0f;
    private int totalMaterials = 0;
    private int totalLikes = 0;

    public UserProfile() {}

    // Getters and Setters
    public String getStudyYear() { return studyYear; }
    public void setStudyYear(String studyYear) { this.studyYear = studyYear; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getGpa() { return gpa; }
    public void setGpa(String gpa) { this.gpa = gpa; }
    
    // Achievement getters and setters
    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public int getTotalDownloads() { return totalDownloads; }
    public void setTotalDownloads(int totalDownloads) { this.totalDownloads = totalDownloads; }

    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    public int getTotalMaterials() { return totalMaterials; }
    public void setTotalMaterials(int totalMaterials) { this.totalMaterials = totalMaterials; }

    public int getTotalLikes() { return totalLikes; }
    public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }
} 