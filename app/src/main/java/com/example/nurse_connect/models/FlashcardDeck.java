package com.example.nurse_connect.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

/**
 * Flashcard Deck Model
 * Represents a collection of flashcards for a specific career, course, and unit
 */
public class FlashcardDeck implements Parcelable {
    @DocumentId
    private String deckId = "";
    
    // Basic info
    private String name = "";
    private String description = "";
    private String career = "";
    private String course = "";
    private String unit = "";
    
    // Content
    private List<String> flashcardIds = null;
    private int totalFlashcards = 0;
    private int activeFlashcards = 0;
    
    // Learning settings
    private int dailyGoal = 20; // Default daily goal
    private int weeklyGoal = 100; // Default weekly goal
    private boolean isSpacedRepetition = true;
    private boolean isAdaptive = true; // Adjusts difficulty based on performance
    
    // User enrollment
    private String userId = "";
    private boolean isEnrolled = false;
    private Timestamp enrollmentDate = null;
    private int currentStreak = 0;
    private int longestStreak = 0;
    private Timestamp lastStudied = null;
    
    // Progress tracking
    private int totalStudied = 0;
    private int totalCorrect = 0;
    private double averageScore = 0.0;
    private int daysStudied = 0;
    private int weeklyProgress = 0;
    private int dailyProgress = 0;
    
    // System fields
    private Timestamp createdAt = Timestamp.now();
    private Timestamp updatedAt = Timestamp.now();
    private boolean isActive = true;
    private String source = ""; // PDF or testbank source
    
    // Default constructor for Firestore
    public FlashcardDeck() {}
    
    public FlashcardDeck(String name, String description, String career, String course, String unit) {
        this.name = name;
        this.description = description;
        this.career = career;
        this.course = course;
        this.unit = unit;
    }
    
    // Getters and Setters
    public String getDeckId() { return deckId; }
    public void setDeckId(String deckId) { this.deckId = deckId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public List<String> getFlashcardIds() { return flashcardIds; }
    public void setFlashcardIds(List<String> flashcardIds) { 
        this.flashcardIds = flashcardIds; 
        this.totalFlashcards = flashcardIds != null ? flashcardIds.size() : 0;
    }
    
    public int getTotalFlashcards() { return totalFlashcards; }
    public void setTotalFlashcards(int totalFlashcards) { this.totalFlashcards = totalFlashcards; }
    
    public int getActiveFlashcards() { return activeFlashcards; }
    public void setActiveFlashcards(int activeFlashcards) { this.activeFlashcards = activeFlashcards; }
    
    public int getDailyGoal() { return dailyGoal; }
    public void setDailyGoal(int dailyGoal) { this.dailyGoal = dailyGoal; }
    
    public int getWeeklyGoal() { return weeklyGoal; }
    public void setWeeklyGoal(int weeklyGoal) { this.weeklyGoal = weeklyGoal; }
    
    public boolean isSpacedRepetition() { return isSpacedRepetition; }
    public void setSpacedRepetition(boolean spacedRepetition) { isSpacedRepetition = spacedRepetition; }
    
    public boolean isAdaptive() { return isAdaptive; }
    public void setAdaptive(boolean adaptive) { isAdaptive = adaptive; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public boolean isEnrolled() { return isEnrolled; }
    public void setEnrolled(boolean enrolled) { isEnrolled = enrolled; }
    
    public Timestamp getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(Timestamp enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    
    public Timestamp getLastStudied() { return lastStudied; }
    public void setLastStudied(Timestamp lastStudied) { this.lastStudied = lastStudied; }
    
    public int getTotalStudied() { return totalStudied; }
    public void setTotalStudied(int totalStudied) { this.totalStudied = totalStudied; }
    
    public int getTotalCorrect() { return totalCorrect; }
    public void setTotalCorrect(int totalCorrect) { this.totalCorrect = totalCorrect; }
    
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
    
    public int getDaysStudied() { return daysStudied; }
    public void setDaysStudied(int daysStudied) { this.daysStudied = daysStudied; }
    
    public int getWeeklyProgress() { return weeklyProgress; }
    public void setWeeklyProgress(int weeklyProgress) { this.weeklyProgress = weeklyProgress; }
    
    public int getDailyProgress() { return dailyProgress; }
    public void setDailyProgress(int dailyProgress) { this.dailyProgress = dailyProgress; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    // Utility methods
    public double getCompletionRate() {
        if (totalFlashcards == 0) return 0.0;
        return (double) totalStudied / totalFlashcards;
    }
    
    public double getAccuracyRate() {
        if (totalStudied == 0) return 0.0;
        return (double) totalCorrect / totalStudied;
    }
    
    public boolean isDailyGoalMet() {
        return dailyProgress >= dailyGoal;
    }
    
    public boolean isWeeklyGoalMet() {
        return weeklyProgress >= weeklyGoal;
    }
    
    public void recordStudySession(int cardsStudied, int correctAnswers) {
        totalStudied += cardsStudied;
        totalCorrect += correctAnswers;
        dailyProgress += cardsStudied;
        weeklyProgress += cardsStudied;
        
        // Update average score
        if (totalStudied > 0) {
            averageScore = (double) totalCorrect / totalStudied;
        }
        
        // Update streak
        updateStreak();
        
        // Update last studied
        lastStudied = Timestamp.now();
        updatedAt = Timestamp.now();
    }
    
    private void updateStreak() {
        // Simple streak logic - can be enhanced
        if (lastStudied != null) {
            long timeDiff = System.currentTimeMillis() - lastStudied.toDate().getTime();
            long oneDay = 24 * 60 * 60 * 1000L;
            
            if (timeDiff <= oneDay) {
                currentStreak++;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
            } else if (timeDiff > oneDay && timeDiff <= 2 * oneDay) {
                // Within 2 days, maintain streak
                // Do nothing
            } else {
                // Break streak
                currentStreak = 0;
            }
        } else {
            currentStreak = 1;
        }
    }
    
    public void resetDailyProgress() {
        dailyProgress = 0;
    }
    
    public void resetWeeklyProgress() {
        weeklyProgress = 0;
    }
    
    public String getDisplayName() {
        return String.format("%s - %s - %s", career, course, unit);
    }
    
    // Parcelable implementation
    protected FlashcardDeck(Parcel in) {
        deckId = in.readString();
        name = in.readString();
        description = in.readString();
        career = in.readString();
        course = in.readString();
        unit = in.readString();
        flashcardIds = in.createStringArrayList();
        totalFlashcards = in.readInt();
        activeFlashcards = in.readInt();
        dailyGoal = in.readInt();
        weeklyGoal = in.readInt();
        isSpacedRepetition = in.readByte() != 0;
        isAdaptive = in.readByte() != 0;
        userId = in.readString();
        isEnrolled = in.readByte() != 0;
        currentStreak = in.readInt();
        longestStreak = in.readInt();
        totalStudied = in.readInt();
        totalCorrect = in.readInt();
        averageScore = in.readDouble();
        daysStudied = in.readInt();
        weeklyProgress = in.readInt();
        dailyProgress = in.readInt();
        isActive = in.readByte() != 0;
        source = in.readString();
        
        // Handle nullable Timestamps
        long enrollmentTime = in.readLong();
        enrollmentDate = enrollmentTime > 0 ? new Timestamp(enrollmentTime / 1000, 0) : null;
        
        long lastStudiedTime = in.readLong();
        lastStudied = lastStudiedTime > 0 ? new Timestamp(lastStudiedTime / 1000, 0) : null;
        
        long createdAtTime = in.readLong();
        createdAt = createdAtTime > 0 ? new Timestamp(createdAtTime / 1000, 0) : Timestamp.now();
        
        long updatedAtTime = in.readLong();
        updatedAt = updatedAtTime > 0 ? new Timestamp(updatedAtTime / 1000, 0) : Timestamp.now();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deckId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(career);
        dest.writeString(course);
        dest.writeString(unit);
        dest.writeStringList(flashcardIds != null ? flashcardIds : new ArrayList<>());
        dest.writeInt(totalFlashcards);
        dest.writeInt(activeFlashcards);
        dest.writeInt(dailyGoal);
        dest.writeInt(weeklyGoal);
        dest.writeByte((byte) (isSpacedRepetition ? 1 : 0));
        dest.writeByte((byte) (isAdaptive ? 1 : 0));
        dest.writeString(userId);
        dest.writeByte((byte) (isEnrolled ? 1 : 0));
        dest.writeInt(currentStreak);
        dest.writeInt(longestStreak);
        dest.writeInt(totalStudied);
        dest.writeInt(totalCorrect);
        dest.writeDouble(averageScore);
        dest.writeInt(daysStudied);
        dest.writeInt(weeklyProgress);
        dest.writeInt(dailyProgress);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeString(source);
        
        // Handle nullable Timestamps
        dest.writeLong(enrollmentDate != null ? enrollmentDate.toDate().getTime() : 0);
        dest.writeLong(lastStudied != null ? lastStudied.toDate().getTime() : 0);
        dest.writeLong(createdAt != null ? createdAt.toDate().getTime() : 0);
        dest.writeLong(updatedAt != null ? updatedAt.toDate().getTime() : 0);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<FlashcardDeck> CREATOR = new Creator<FlashcardDeck>() {
        @Override
        public FlashcardDeck createFromParcel(Parcel in) {
            return new FlashcardDeck(in);
        }
        
        @Override
        public FlashcardDeck[] newArray(int size) {
            return new FlashcardDeck[size];
        }
    };
}
