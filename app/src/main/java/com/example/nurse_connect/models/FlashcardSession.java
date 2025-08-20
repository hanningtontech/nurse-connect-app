package com.example.nurse_connect.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.List;
import java.util.Map;

/**
 * Model class for Flashcard Session
 * Represents a saved flashcard study session
 */
public class FlashcardSession {
    
    @PropertyName("sessionId")
    private String sessionId;
    
    @PropertyName("userId")
    private String userId;
    
    @PropertyName("heading")
    private String heading;
    
    @PropertyName("totalAnswered")
    private int totalAnswered;
    
    @PropertyName("correctAnswers")
    private int correctAnswers;
    
    @PropertyName("accuracy")
    private double accuracy;
    
    @PropertyName("isStudyMode")
    private boolean isStudyMode;
    
    @PropertyName("timestamp")
    private Timestamp timestamp;
    
    @PropertyName("date")
    private String date;
    
    @PropertyName("time")
    private String time;
    
    @PropertyName("career")
    private String career;
    
    @PropertyName("course")
    private String course;
    
    @PropertyName("unit")
    private String unit;
    
    @PropertyName("deckName")
    private String deckName;
    
    @PropertyName("deckId")
    private String deckId;
    
    @PropertyName("flashcards")
    private List<Map<String, Object>> flashcards;
    
    // Default constructor for Firestore
    public FlashcardSession() {}
    
    // Constructor with main parameters
    public FlashcardSession(String userId, String heading, int totalAnswered, int correctAnswers, 
                           double accuracy, boolean isStudyMode) {
        this.userId = userId;
        this.heading = heading;
        this.totalAnswered = totalAnswered;
        this.correctAnswers = correctAnswers;
        this.accuracy = accuracy;
        this.isStudyMode = isStudyMode;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getHeading() {
        return heading;
    }
    
    public void setHeading(String heading) {
        this.heading = heading;
    }
    
    public int getTotalAnswered() {
        return totalAnswered;
    }
    
    public void setTotalAnswered(int totalAnswered) {
        this.totalAnswered = totalAnswered;
    }
    
    public int getCorrectAnswers() {
        return correctAnswers;
    }
    
    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
    
    public double getAccuracy() {
        return accuracy;
    }
    
    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }
    
    public boolean isStudyMode() {
        return isStudyMode;
    }
    
    public void setStudyMode(boolean studyMode) {
        isStudyMode = studyMode;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getCareer() {
        return career;
    }
    
    public void setCareer(String career) {
        this.career = career;
    }
    
    public String getCourse() {
        return course;
    }
    
    public void setCourse(String course) {
        this.course = course;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getDeckName() {
        return deckName;
    }
    
    public void setDeckName(String deckName) {
        this.deckName = deckName;
    }
    
    public String getDeckId() {
        return deckId;
    }
    
    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }
    
    public List<Map<String, Object>> getFlashcards() {
        return flashcards;
    }
    
    public void setFlashcards(List<Map<String, Object>> flashcards) {
        this.flashcards = flashcards;
    }
    
    // Helper methods
    public String getFormattedAccuracy() {
        return String.format("%.1f%%", accuracy);
    }
    
    public String getFormattedScore() {
        return String.format("%d/%d", correctAnswers, totalAnswered);
    }
    
    public String getFormattedDateTime() {
        if (date != null && time != null) {
            return date + " at " + time;
        }
        return "Unknown";
    }
    
    public boolean isPassed() {
        return accuracy >= 60;
    }
    
    public String getPerformanceLevel() {
        if (accuracy >= 80) return "Excellent";
        if (accuracy >= 60) return "Good";
        return "Practice";
    }
}
