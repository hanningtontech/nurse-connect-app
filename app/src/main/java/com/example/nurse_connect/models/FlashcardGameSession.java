package com.example.nurse_connect.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Flashcard Game Session
 * Tracks a user's game session with specific mode and progress
 */
public class FlashcardGameSession implements Parcelable {
    private String sessionId = "";
    private String userId = "";
    private FlashcardGameMode gameMode;
    private String deckId = "";
    private List<String> flashcardIds = new ArrayList<>();
    
    // Session state
    private int currentCardIndex = 0;
    private int totalCards = 0;
    private int correctAnswers = 0;
    private int incorrectAnswers = 0;
    private int streak = 0;
    private int maxStreak = 0;
    
    // Timed mode specific
    private long timeLimit = 0; // milliseconds
    private long timeRemaining = 0;
    private long startTime = 0;
    
    // Scoring
    private int score = 0;
    private double accuracy = 0.0;
    private int coinsEarned = 0;
    
    // Progress tracking
    private boolean isCompleted = false;
    private Timestamp startedAt = Timestamp.now();
    private Timestamp completedAt = null;
    
    // Game mode specific settings
    private int difficultyLevel = 1; // 1-5
    private boolean showHints = true;
    private boolean immediateFeedback = true;
    
    public FlashcardGameSession() {}
    
    public FlashcardGameSession(String userId, FlashcardGameMode gameMode, String deckId) {
        this.userId = userId;
        this.gameMode = gameMode;
        this.deckId = deckId;
        this.startedAt = Timestamp.now();
    }
    
    // Parcelable implementation
    protected FlashcardGameSession(Parcel in) {
        sessionId = in.readString();
        userId = in.readString();
        gameMode = FlashcardGameMode.valueOf(in.readString());
        deckId = in.readString();
        flashcardIds = in.createStringArrayList();
        currentCardIndex = in.readInt();
        totalCards = in.readInt();
        correctAnswers = in.readInt();
        incorrectAnswers = in.readInt();
        streak = in.readInt();
        maxStreak = in.readInt();
        timeLimit = in.readLong();
        timeRemaining = in.readLong();
        startTime = in.readLong();
        score = in.readInt();
        accuracy = in.readDouble();
        coinsEarned = in.readInt();
        isCompleted = in.readByte() != 0;
        difficultyLevel = in.readInt();
        showHints = in.readByte() != 0;
        immediateFeedback = in.readByte() != 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sessionId);
        dest.writeString(userId);
        dest.writeString(gameMode.name());
        dest.writeString(deckId);
        dest.writeStringList(flashcardIds);
        dest.writeInt(currentCardIndex);
        dest.writeInt(totalCards);
        dest.writeInt(correctAnswers);
        dest.writeInt(incorrectAnswers);
        dest.writeInt(streak);
        dest.writeInt(maxStreak);
        dest.writeLong(timeLimit);
        dest.writeLong(timeRemaining);
        dest.writeLong(startTime);
        dest.writeInt(score);
        dest.writeDouble(accuracy);
        dest.writeInt(coinsEarned);
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeInt(difficultyLevel);
        dest.writeByte((byte) (showHints ? 1 : 0));
        dest.writeByte((byte) (immediateFeedback ? 1 : 0));
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<FlashcardGameSession> CREATOR = new Creator<FlashcardGameSession>() {
        @Override
        public FlashcardGameSession createFromParcel(Parcel in) {
            return new FlashcardGameSession(in);
        }
        
        @Override
        public FlashcardGameSession[] newArray(int size) {
            return new FlashcardGameSession[size];
        }
    };
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public FlashcardGameMode getGameMode() { return gameMode; }
    public void setGameMode(FlashcardGameMode gameMode) { this.gameMode = gameMode; }
    
    public String getDeckId() { return deckId; }
    public void setDeckId(String deckId) { this.deckId = deckId; }
    
    public List<String> getFlashcardIds() { return flashcardIds; }
    public void setFlashcardIds(List<String> flashcardIds) { 
        this.flashcardIds = flashcardIds;
        this.totalCards = flashcardIds.size();
    }
    
    public int getCurrentCardIndex() { return currentCardIndex; }
    public void setCurrentCardIndex(int currentCardIndex) { this.currentCardIndex = currentCardIndex; }
    
    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }
    
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    
    public int getIncorrectAnswers() { return incorrectAnswers; }
    public void setIncorrectAnswers(int incorrectAnswers) { this.incorrectAnswers = incorrectAnswers; }
    
    public int getStreak() { return streak; }
    public void setStreak(int streak) { 
        this.streak = streak;
        if (streak > maxStreak) {
            this.maxStreak = streak;
        }
    }
    
    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }
    
    public long getTimeLimit() { return timeLimit; }
    public void setTimeLimit(long timeLimit) { this.timeLimit = timeLimit; }
    
    public long getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(long timeRemaining) { this.timeRemaining = timeRemaining; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    
    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }
    
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { 
        isCompleted = completed;
        if (completed) {
            completedAt = Timestamp.now();
        }
    }
    
    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }
    
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    
    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public boolean isShowHints() { return showHints; }
    public void setShowHints(boolean showHints) { this.showHints = showHints; }
    
    public boolean isImmediateFeedback() { return immediateFeedback; }
    public void setImmediateFeedback(boolean immediateFeedback) { this.immediateFeedback = immediateFeedback; }
    
    // Utility methods
    public void recordCorrectAnswer() {
        correctAnswers++;
        streak++;
        score += calculatePoints(true);
        updateAccuracy();
    }
    
    public void recordIncorrectAnswer() {
        incorrectAnswers++;
        streak = 0;
        score += calculatePoints(false);
        updateAccuracy();
    }
    
    private void updateAccuracy() {
        int total = correctAnswers + incorrectAnswers;
        if (total > 0) {
            accuracy = (double) correctAnswers / total;
        }
    }
    
    private int calculatePoints(boolean isCorrect) {
        int basePoints = 10;
        int streakBonus = streak * 2;
        int difficultyBonus = difficultyLevel * 5;
        
        if (isCorrect) {
            return basePoints + streakBonus + difficultyBonus;
        } else {
            return Math.max(0, basePoints / 2);
        }
    }
    
    public boolean hasNextCard() {
        return currentCardIndex < totalCards - 1;
    }
    
    public boolean hasPreviousCard() {
        return currentCardIndex > 0;
    }
    
    public void nextCard() {
        if (hasNextCard()) {
            currentCardIndex++;
        }
    }
    
    public void previousCard() {
        if (hasPreviousCard()) {
            currentCardIndex--;
        }
    }
    
    public double getProgress() {
        if (totalCards == 0) return 0.0;
        return (double) currentCardIndex / totalCards;
    }
    
    public long getElapsedTime() {
        if (startTime == 0) return 0;
        return System.currentTimeMillis() - startTime;
    }
    
    public void startTimer() {
        startTime = System.currentTimeMillis();
        timeRemaining = timeLimit;
    }
    
    public void updateTimer() {
        if (startTime > 0 && timeLimit > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            timeRemaining = Math.max(0, timeLimit - elapsed);
            
            if (timeRemaining <= 0) {
                isCompleted = true;
                completedAt = Timestamp.now();
            }
        }
    }
}
