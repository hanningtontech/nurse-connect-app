package com.example.nurse_connect.models;

import java.util.List;
import java.util.Map;

public class QuizQuestion {
    private String questionId;
    private String question;
    private List<String> options;
    private int correctAnswerIndex;
    private String explanation;
    private String course;
    private String unit;
    private String career;
    private String difficulty; // "easy", "medium", "hard"
    private int timeLimit; // seconds
    private Map<String, Object> metadata;
    
    // Additional fields for upload functionality
    private String uploadedBy;
    private long uploadDate;
    
    public QuizQuestion() {}
    
    public QuizQuestion(String questionId, String question, List<String> options, 
                       int correctAnswerIndex, String explanation, 
                       String course, String unit, String career) {
        this.questionId = questionId;
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.explanation = explanation;
        this.course = course;
        this.unit = unit;
        this.career = career;
        this.timeLimit = 30; // Default 30 seconds
        this.difficulty = "medium";
    }
    
    // Getters and Setters
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }
    
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Additional getters and setters for upload functionality
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    
    public long getUploadDate() { return uploadDate; }
    public void setUploadDate(long uploadDate) { this.uploadDate = uploadDate; }
    
    // Helper methods
    public boolean isAnswerCorrect(int selectedIndex) {
        return selectedIndex == correctAnswerIndex;
    }
    
    public String getCorrectAnswer() {
        if (options != null && correctAnswerIndex >= 0 && correctAnswerIndex < options.size()) {
            return options.get(correctAnswerIndex);
        }
        return null;
    }
}
