package com.example.nurse_connect.models;

import java.util.HashMap;
import java.util.Map;

public class PlayerStats {
    private String playerId;
    private String playerName;
    private int totalMatches;
    private int wins;
    private int losses;
    private int totalQuestions;
    private int correctAnswers;
    private double winRate;
    private double accuracy;
    private String currentRank;
    private int rankPoints;
    private long totalPlayTime; // milliseconds
    private Map<String, Integer> subjectStats; // course -> questions answered
    private Map<String, Double> subjectAccuracy; // course -> accuracy percentage
    private int streak; // current win streak
    private int bestStreak;
    private long lastPlayedTime;
    
    public PlayerStats() {
        this.subjectStats = new HashMap<>();
        this.subjectAccuracy = new HashMap<>();
        this.currentRank = "Bronze";
        this.rankPoints = 0;
    }
    
    public PlayerStats(String playerId, String playerName) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
    }
    
    // Getters and Setters
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
    
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    
    public String getCurrentRank() { return currentRank; }
    public void setCurrentRank(String currentRank) { this.currentRank = currentRank; }
    
    public int getRankPoints() { return rankPoints; }
    public void setRankPoints(int rankPoints) { this.rankPoints = rankPoints; }
    
    public long getTotalPlayTime() { return totalPlayTime; }
    public void setTotalPlayTime(long totalPlayTime) { this.totalPlayTime = totalPlayTime; }
    
    public Map<String, Integer> getSubjectStats() { return subjectStats; }
    public void setSubjectStats(Map<String, Integer> subjectStats) { this.subjectStats = subjectStats; }
    
    public Map<String, Double> getSubjectAccuracy() { return subjectAccuracy; }
    public void setSubjectAccuracy(Map<String, Double> subjectAccuracy) { this.subjectAccuracy = subjectAccuracy; }
    
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    
    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    
    public long getLastPlayedTime() { return lastPlayedTime; }
    public void setLastPlayedTime(long lastPlayedTime) { this.lastPlayedTime = lastPlayedTime; }
    
    // Helper methods
    public void updateAfterMatch(boolean won, int questionsAnswered, int correctAnswers, String course) {
        totalMatches++;
        if (won) {
            wins++;
            streak++;
            if (streak > bestStreak) {
                bestStreak = streak;
            }
            rankPoints += 25; // Win points
        } else {
            losses++;
            streak = 0;
            rankPoints -= 10; // Loss penalty
        }
        
        totalQuestions += questionsAnswered;
        this.correctAnswers += correctAnswers;
        
        // Update rates
        winRate = totalMatches > 0 ? (double) wins / totalMatches : 0.0;
        accuracy = totalQuestions > 0 ? (double) this.correctAnswers / totalQuestions : 0.0;
        
        // Update subject stats
        subjectStats.put(course, subjectStats.getOrDefault(course, 0) + questionsAnswered);
        
        // Update rank
        updateRank();
        
        lastPlayedTime = System.currentTimeMillis();
    }
    
    private void updateRank() {
        if (rankPoints >= 1000) {
            currentRank = "Diamond";
        } else if (rankPoints >= 750) {
            currentRank = "Platinum";
        } else if (rankPoints >= 500) {
            currentRank = "Gold";
        } else if (rankPoints >= 250) {
            currentRank = "Silver";
        } else {
            currentRank = "Bronze";
        }
        
        // Prevent negative points
        if (rankPoints < 0) {
            rankPoints = 0;
        }
    }
}
