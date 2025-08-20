package com.example.nurse_connect.models;

public class MatchmakingQueue {
    private String queueId;
    private String playerId;
    private String playerName;
    private String course;
    private String unit;
    private String career;
    private long joinTime;
    private String status; // "waiting", "matched", "expired"
    private int preferredDifficulty; // 1-3 (easy, medium, hard)
    private String playerRank; // For skill-based matching
    
    public MatchmakingQueue() {}
    
    public MatchmakingQueue(String playerId, String playerName, String course, 
                           String unit, String career) {
        this.queueId = playerId + "_" + System.currentTimeMillis();
        this.playerId = playerId;
        this.playerName = playerName;
        this.course = course;
        this.unit = unit;
        this.career = career;
        this.joinTime = System.currentTimeMillis();
        this.status = "waiting";
        this.preferredDifficulty = 2; // Default to medium
    }
    
    // Getters and Setters
    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }
    
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    
    public long getJoinTime() { return joinTime; }
    public void setJoinTime(long joinTime) { this.joinTime = joinTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getPreferredDifficulty() { return preferredDifficulty; }
    public void setPreferredDifficulty(int preferredDifficulty) { this.preferredDifficulty = preferredDifficulty; }
    
    public String getPlayerRank() { return playerRank; }
    public void setPlayerRank(String playerRank) { this.playerRank = playerRank; }
    
    // Helper methods
    public boolean isCompatibleWith(MatchmakingQueue other) {
        return this.course.equals(other.course) &&
               this.unit.equals(other.unit) &&
               this.career.equals(other.career) &&
               !this.playerId.equals(other.playerId);
    }
    
    public boolean hasExpired(long maxWaitTime) {
        return (System.currentTimeMillis() - joinTime) > maxWaitTime;
    }
}
