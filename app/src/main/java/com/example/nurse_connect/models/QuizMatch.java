package com.example.nurse_connect.models;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizMatch {
    private String matchId;
    private List<String> playerIds;
    private Map<String, String> playerNames;
    private Map<String, Integer> playerScores;
    private String course;
    private String unit;
    private String career;
    private String currentQuestionId;
    private int currentQuestionIndex;
    private List<String> questionIds;
    private String status; // "waiting", "active", "completed"
    private long startTime;
    private long endTime;
    private String winnerId;
    private int totalQuestions;
    private Map<String, Boolean> playersReady;
    private long questionStartTime;
    private int questionTimeLimit; // seconds
    private int targetPlayerCount; // How many players this match needs
    
    // Add missing fields that Firestore expects
    private boolean matchFull;
    private String leadingPlayer;
    
    // Enhanced quiz gameplay fields
    private Map<String, Boolean> playersAnsweredCurrentQuestion;
    private String currentQuestionAnsweredBy;
    private boolean currentQuestionCompleted;
    private long currentQuestionStartTime;
    
    // New fields for question completion flow
    private long nextQuestionReadyTime;
    private boolean nextQuestionButtonShown;
    
    // Track which players have pressed next question button
    private Map<String, Boolean> playersPressedNext;
    
    public QuizMatch() {
        this.playerIds = new ArrayList<>();
        this.playerNames = new HashMap<>();
        this.playerScores = new HashMap<>();
        this.questionIds = new ArrayList<>();
        this.playersReady = new HashMap<>();
        this.currentQuestionIndex = 0;
        this.totalQuestions = 10; // Default
        this.questionTimeLimit = 30; // 30 seconds per question
        this.status = "waiting";
        this.targetPlayerCount = 2; // Default to 2 players
        this.matchFull = false;
        this.leadingPlayer = null;
        
        // Initialize enhanced quiz gameplay fields
        this.playersAnsweredCurrentQuestion = new HashMap<>();
        this.currentQuestionAnsweredBy = null;
        this.currentQuestionCompleted = false;
        this.currentQuestionStartTime = 0;
        this.playersPressedNext = new HashMap<>();
    }
    
    public QuizMatch(String matchId, String course, String unit, String career) {
        this();
        this.matchId = matchId;
        this.course = course;
        this.unit = unit;
        this.career = career;
    }
    
    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    
    public List<String> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<String> playerIds) { this.playerIds = playerIds; }
    
    public Map<String, String> getPlayerNames() { return playerNames; }
    public void setPlayerNames(Map<String, String> playerNames) { this.playerNames = playerNames; }
    
    public Map<String, Integer> getPlayerScores() { return playerScores; }
    public void setPlayerScores(Map<String, Integer> playerScores) { this.playerScores = playerScores; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    
    public String getCurrentQuestionId() { return currentQuestionId; }
    public void setCurrentQuestionId(String currentQuestionId) { this.currentQuestionId = currentQuestionId; }
    
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    
    public List<String> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<String> questionIds) { this.questionIds = questionIds; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public Map<String, Boolean> getPlayersReady() { return playersReady; }
    public void setPlayersReady(Map<String, Boolean> playersReady) { this.playersReady = playersReady; }
    
    public long getQuestionStartTime() { return questionStartTime; }
    public void setQuestionStartTime(long questionStartTime) { this.questionStartTime = questionStartTime; }
    
    public int getQuestionTimeLimit() { return questionTimeLimit; }
    public void setQuestionTimeLimit(int questionTimeLimit) { this.questionTimeLimit = questionTimeLimit; }
    
    public int getTargetPlayerCount() { return targetPlayerCount; }
    public void setTargetPlayerCount(int targetPlayerCount) { this.targetPlayerCount = targetPlayerCount; }
    
    // Add getters for the new fields
    public boolean isMatchFull() { 
        return playerIds.size() >= targetPlayerCount; 
    }
    public void setMatchFull(boolean matchFull) { this.matchFull = matchFull; }
    
    public String getLeadingPlayer() { return leadingPlayer; }
    public void setLeadingPlayer(String leadingPlayer) { this.leadingPlayer = leadingPlayer; }
    
    // Enhanced quiz gameplay getters and setters
    public Map<String, Boolean> getPlayersAnsweredCurrentQuestion() { return playersAnsweredCurrentQuestion; }
    public void setPlayersAnsweredCurrentQuestion(Map<String, Boolean> playersAnsweredCurrentQuestion) { this.playersAnsweredCurrentQuestion = playersAnsweredCurrentQuestion; }
    
    public String getCurrentQuestionAnsweredBy() { return currentQuestionAnsweredBy; }
    public void setCurrentQuestionAnsweredBy(String currentQuestionAnsweredBy) { this.currentQuestionAnsweredBy = currentQuestionAnsweredBy; }
    
    public boolean isCurrentQuestionCompleted() { return currentQuestionCompleted; }
    public void setCurrentQuestionCompleted(boolean currentQuestionCompleted) { this.currentQuestionCompleted = currentQuestionCompleted; }
    
    public long getCurrentQuestionStartTime() { return currentQuestionStartTime; }
    public void setCurrentQuestionStartTime(long currentQuestionStartTime) { this.currentQuestionStartTime = currentQuestionStartTime; }
    
    // New getters and setters for question completion flow
    public long getNextQuestionReadyTime() { return nextQuestionReadyTime; }
    public void setNextQuestionReadyTime(long nextQuestionReadyTime) { this.nextQuestionReadyTime = nextQuestionReadyTime; }
    
    public boolean isNextQuestionButtonShown() { return nextQuestionButtonShown; }
    public void setNextQuestionButtonShown(boolean nextQuestionButtonShown) { this.nextQuestionButtonShown = nextQuestionButtonShown; }
    
    // Getter and setter for players pressed next
    public Map<String, Boolean> getPlayersPressedNext() { return playersPressedNext; }
    public void setPlayersPressedNext(Map<String, Boolean> playersPressedNext) { this.playersPressedNext = playersPressedNext; }
    
    // Helper methods
    public void addPlayer(String playerId, String playerName) {
        if (!playerIds.contains(playerId)) {
            playerIds.add(playerId);
            playerNames.put(playerId, playerName);
            playerScores.put(playerId, 0);
            playersReady.put(playerId, false);
            playersPressedNext.put(playerId, false);
            
            // Update matchFull based on target player count
            setMatchFull(playerIds.size() >= targetPlayerCount);
            
            Log.d("QuizMatch", "Added player " + playerId + ". Total players: " + playerIds.size() + "/" + targetPlayerCount);
        }
    }
    
    public void removePlayer(String playerId) {
        playerIds.remove(playerId);
        playerNames.remove(playerId);
        playerScores.remove(playerId);
        playersReady.remove(playerId);
        playersPressedNext.remove(playerId);
    }
    
    public boolean isPlayerReady(String playerId) {
        return playersReady.getOrDefault(playerId, false);
    }
    
    public void setPlayerReady(String playerId, boolean ready) {
        playersReady.put(playerId, ready);
    }
    
    public boolean areAllPlayersReady() {
        // Check if we have the target number of players
        if (playerIds.size() < targetPlayerCount) {
            return false;
        }
        
        // Check if all current players are ready
        for (String playerId : playerIds) {
            if (!playersReady.getOrDefault(playerId, false)) {
                return false;
            }
        }
        return true;
    }
    
    public void incrementPlayerScore(String playerId) {
        playerScores.put(playerId, playerScores.getOrDefault(playerId, 0) + 1);
    }
    
    public void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questionIds.size()) {
            currentQuestionId = questionIds.get(currentQuestionIndex);
            questionStartTime = System.currentTimeMillis();
            
            // Reset question state for new question
            resetQuestionState();
        } else {
            // Match completed
            status = "completed";
            endTime = System.currentTimeMillis();
            
            // Find the leading player from scores and store it directly
            String leadingPlayerId = null;
            int highestScore = -1;
            for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                if (entry.getValue() > highestScore) {
                    highestScore = entry.getValue();
                    leadingPlayerId = entry.getKey();
                }
            }
            
            winnerId = leadingPlayerId;
            setLeadingPlayer(leadingPlayerId);
        }
    }
    
    /**
     * Reset the state for a new question
     */
    public void resetQuestionState() {
        playersAnsweredCurrentQuestion.clear();
        currentQuestionAnsweredBy = null;
        currentQuestionCompleted = false;
        currentQuestionStartTime = System.currentTimeMillis();
        
        // Initialize all players as not answered for the new question
        for (String playerId : playerIds) {
            playersAnsweredCurrentQuestion.put(playerId, false);
        }
        
        // Reset next question button state
        playersPressedNext.clear();
        for (String playerId : playerIds) {
            playersPressedNext.put(playerId, false);
        }
    }
    
    /**
     * Mark a player as having answered the current question
     */
    public void markPlayerAnswered(String playerId) {
        playersAnsweredCurrentQuestion.put(playerId, true);
    }
    
    /**
     * Check if all players have answered the current question
     */
    public boolean haveAllPlayersAnswered() {
        for (String playerId : playerIds) {
            if (!playersAnsweredCurrentQuestion.getOrDefault(playerId, false)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if all players have pressed the next question button
     */
    public boolean haveAllPlayersPressedNext() {
        for (String playerId : playerIds) {
            if (!playersPressedNext.getOrDefault(playerId, false)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Mark a player as having pressed the next question button
     */
    public void markPlayerPressedNext(String playerId) {
        playersPressedNext.put(playerId, true);
    }
    
    /**
     * Get current player count
     */
    public int getCurrentPlayerCount() {
        return playerIds != null ? playerIds.size() : 0;
    }
    
    /**
     * Get maximum players for this match
     */
    public int getMaxPlayers() {
        return targetPlayerCount;
    }
    
    /**
     * Check if match is ready to start
     */
    public boolean isReadyToStart() {
        return getCurrentPlayerCount() >= getMaxPlayers() && areAllPlayersReady();
    }
}
