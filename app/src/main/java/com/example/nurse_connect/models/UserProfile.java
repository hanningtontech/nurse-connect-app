package com.example.nurse_connect.models;

public class UserProfile {
    // Academic/Educational Information
    private String studyYear = "";
    private String specialization = "";
    private String institution = "";
    private String course = "";
    private String gpa = "";
    
    // Professional Information (moved to main User model)
    // private String phoneNumber = ""; // Now in User model
    // private String nursingCareer = ""; // Now in User model
    // private String yearsExperience = ""; // Now in User model
    // private String currentInstitution = ""; // Now in User model
    
    // Personal Information
    private String bio = "";
    private String location = "";
    private String dateOfBirth = "";
    private String gender = "";
    
    // Social/Professional Links
    private String linkedinUrl = "";
    private String websiteUrl = "";
    private String twitterHandle = "";
    
    // Achievement fields
    private int followersCount = 0;
    private int followingCount = 0;
    private int totalDownloads = 0;
    private float averageRating = 0.0f;
    private int totalMaterials = 0;
    private int totalLikes = 0;
    
    // Study/Progress tracking
    private int totalStudySessions = 0;
    private long totalStudyTime = 0; // in minutes
    private String lastStudyDate = "";

    public UserProfile() {}

    // Getters and Setters for Academic/Educational Information
    public String getStudyYear() { return studyYear; }
    public void setStudyYear(String studyYear) { this.studyYear = studyYear; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getGpa() { return gpa; }
    public void setGpa(String gpa) { this.gpa = gpa; }
    
    // Getters and Setters for Personal Information
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    // Getters and Setters for Social/Professional Links
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    
    public String getTwitterHandle() { return twitterHandle; }
    public void setTwitterHandle(String twitterHandle) { this.twitterHandle = twitterHandle; }
    
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
    
    // Study/Progress tracking getters and setters
    public int getTotalStudySessions() { return totalStudySessions; }
    public void setTotalStudySessions(int totalStudySessions) { this.totalStudySessions = totalStudySessions; }
    
    public long getTotalStudyTime() { return totalStudyTime; }
    public void setTotalStudyTime(long totalStudyTime) { this.totalStudyTime = totalStudyTime; }
    
    public String getLastStudyDate() { return lastStudyDate; }
    public void setLastStudyDate(String lastStudyDate) { this.lastStudyDate = lastStudyDate; }
    
    // Helper method to check if profile has basic information
    public boolean hasBasicInfo() {
        return (studyYear != null && !studyYear.trim().isEmpty()) ||
               (specialization != null && !specialization.trim().isEmpty()) ||
               (institution != null && !institution.trim().isEmpty()) ||
               (course != null && !course.trim().isEmpty()) ||
               (bio != null && !bio.trim().isEmpty());
    }
} 