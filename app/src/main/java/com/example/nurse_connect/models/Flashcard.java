package com.example.nurse_connect.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Flashcard Model
 * Represents a single flashcard with question, answer, and learning metadata
 * Implements Parcelable for Intent passing
 */
public class Flashcard implements Parcelable {
    @DocumentId
    private String flashcardId = "";
    
    // Content
    private String title = ""; // Meaningful title based on unit and content
    private String question = "";
    private String answer = "";
    private String rationale = "";
    private List<String> options = null; // Multiple choice options
    
    // Metadata
    private String career = "";
    private String course = "";
    private String unit = "";
    private String source = ""; // PDF or testbank source
    private String difficulty = "medium"; // easy, medium, hard
    
    // Learning tracking
    private int timesReviewed = 0;
    private int timesCorrect = 0;
    private int timesIncorrect = 0;
    private double confidenceScore = 0.0; // 0.0 to 1.0
    private Timestamp lastReviewed = null;
    private Timestamp nextReview = null; // Spaced repetition scheduling
    
    // Study Progress & Streak Tracking
    private int currentStreakDays = 0; // Current consecutive days studied
    private int maxStreakDays = 0; // Longest streak achieved
    private Timestamp streakStartDate = null; // When current streak started
    private Timestamp firstStudyDate = null; // First time user studied
    private int totalStudySessions = 0; // Total number of study sessions
    private int completedFlashcardSets = 0; // Number of 10-question sets completed
    private int targetFlashcardSets = 40; // Target: 40 sets in 30 days
    private int daysSinceFirstStudy = 0; // Days since first study session
    
    // User progress (per user)
    private String userId = "";
    private boolean isEnrolled = false;
    private int streakDays = 0; // Consecutive days studied
    private Timestamp lastStudied = null;
    
    // System fields
    private Timestamp createdAt = Timestamp.now();
    private Timestamp updatedAt = Timestamp.now();
    private boolean isActive = true;
    
    // Default constructor for Firestore
    public Flashcard() {}
    
    public Flashcard(String question, String answer, String rationale, 
                    String career, String course, String unit) {
        this.question = question;
        this.answer = answer;
        this.rationale = rationale;
        this.career = career;
        this.course = course;
        this.unit = unit;
        // Generate a meaningful title based on the unit
        this.title = generateTitleFromUnit(unit);
    }
    
    public Flashcard(String title, String question, String answer, String rationale, 
                    String career, String course, String unit) {
        this.title = title;
        this.question = question;
        this.answer = answer;
        this.rationale = rationale;
        this.career = career;
        this.course = course;
        this.unit = unit;
    }
    
    // Getters and Setters
    public String getFlashcardId() { return flashcardId; }
    public void setFlashcardId(String flashcardId) { this.flashcardId = flashcardId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public int getTimesReviewed() { return timesReviewed; }
    public void setTimesReviewed(int timesReviewed) { this.timesReviewed = timesReviewed; }
    
    public int getTimesCorrect() { return timesCorrect; }
    public void setTimesCorrect(int timesCorrect) { this.timesCorrect = timesCorrect; }
    
    public int getTimesIncorrect() { return timesIncorrect; }
    public void setTimesIncorrect(int timesIncorrect) { this.timesIncorrect = timesIncorrect; }
    
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public Timestamp getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(Timestamp lastReviewed) { this.lastReviewed = lastReviewed; }
    
    public Timestamp getNextReview() { return nextReview; }
    public void setNextReview(Timestamp nextReview) { this.nextReview = nextReview; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public boolean isEnrolled() { return isEnrolled; }
    public void setEnrolled(boolean enrolled) { isEnrolled = enrolled; }
    
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
    
    public Timestamp getLastStudied() { return lastStudied; }
    public void setLastStudied(Timestamp lastStudied) { this.lastStudied = lastStudied; }
    
    // Study Progress & Streak Tracking Getters/Setters
    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }
    
    public int getMaxStreakDays() { return maxStreakDays; }
    public void setMaxStreakDays(int maxStreakDays) { this.maxStreakDays = maxStreakDays; }
    
    public Timestamp getStreakStartDate() { return streakStartDate; }
    public void setStreakStartDate(Timestamp streakStartDate) { this.streakStartDate = streakStartDate; }
    
    public Timestamp getFirstStudyDate() { return firstStudyDate; }
    public void setFirstStudyDate(Timestamp firstStudyDate) { this.firstStudyDate = firstStudyDate; }
    
    public int getTotalStudySessions() { return totalStudySessions; }
    public void setTotalStudySessions(int totalStudySessions) { this.totalStudySessions = totalStudySessions; }
    
    public int getCompletedFlashcardSets() { return completedFlashcardSets; }
    public void setCompletedFlashcardSets(int completedFlashcardSets) { this.completedFlashcardSets = completedFlashcardSets; }
    
    public int getTargetFlashcardSets() { return targetFlashcardSets; }
    public void setTargetFlashcardSets(int targetFlashcardSets) { this.targetFlashcardSets = targetFlashcardSets; }
    
    public int getDaysSinceFirstStudy() { return daysSinceFirstStudy; }
    public void setDaysSinceFirstStudy(int daysSinceFirstStudy) { this.daysSinceFirstStudy = daysSinceFirstStudy; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    // Utility methods
    public double getAccuracyRate() {
        if (timesReviewed == 0) return 0.0;
        return (double) timesCorrect / timesReviewed;
    }
    
    /**
     * Calculate study progress percentage (completed sets vs target)
     */
    public double getStudyProgressPercentage() {
        if (targetFlashcardSets == 0) return 0.0;
        return Math.min((double) completedFlashcardSets / targetFlashcardSets * 100, 100.0);
    }
    
    /**
     * Calculate days remaining in the 30-day challenge
     */
    public int getDaysRemainingInChallenge() {
        if (firstStudyDate == null) return 30;
        long currentTime = System.currentTimeMillis();
        long firstStudyTime = firstStudyDate.toDate().getTime();
        long daysElapsed = (currentTime - firstStudyTime) / (1000 * 60 * 60 * 24);
        return Math.max(0, 30 - (int) daysElapsed);
    }
    
    /**
     * Check if user is on track for the 30-day challenge
     */
    public boolean isOnTrackForChallenge() {
        if (firstStudyDate == null) return true;
        
        int daysElapsed = 30 - getDaysRemainingInChallenge();
        if (daysElapsed == 0) return true;
        
        // Calculate expected progress: should complete (40 sets / 30 days) * days elapsed
        double expectedProgress = (40.0 / 30.0) * daysElapsed;
        return completedFlashcardSets >= expectedProgress;
    }
    
    /**
     * Get progress status message
     */
    public String getProgressStatusMessage() {
        if (firstStudyDate == null) {
            return "Start your 30-day challenge!";
        }
        
        int daysRemaining = getDaysRemainingInChallenge();
        if (daysRemaining == 0) {
            if (completedFlashcardSets >= targetFlashcardSets) {
                return "🎉 Challenge completed! Great job!";
            } else {
                return "Challenge period ended. You completed " + completedFlashcardSets + "/40 sets.";
            }
        }
        
        if (isOnTrackForChallenge()) {
            return "On track! " + daysRemaining + " days remaining.";
        } else {
            return "Catch up needed! " + daysRemaining + " days remaining.";
        }
    }
    
    /**
     * Get streak status message
     */
    public String getStreakStatusMessage() {
        if (currentStreakDays == 0) {
            return "Start your streak today!";
        } else if (currentStreakDays == 1) {
            return "1 day streak! Keep it going!";
        } else if (currentStreakDays < 7) {
            return currentStreakDays + " day streak! Building momentum!";
        } else if (currentStreakDays < 30) {
            return currentStreakDays + " day streak! You're on fire! 🔥";
        } else {
            return currentStreakDays + " day streak! Unstoppable! 🚀";
        }
    }
    
    public void recordAnswer(boolean isCorrect) {
        timesReviewed++;
        if (isCorrect) {
            timesCorrect++;
        } else {
            timesIncorrect++;
        }
        updateConfidenceScore();
        updateNextReview(isCorrect);
    }
    
    private void updateConfidenceScore() {
        if (timesReviewed == 0) {
            confidenceScore = 0.0;
        } else {
            confidenceScore = (double) timesCorrect / timesReviewed;
        }
    }
    
    private void updateNextReview(boolean isCorrect) {
        // Simple spaced repetition algorithm
        // Correct answers: review later, incorrect answers: review sooner
        long currentTime = System.currentTimeMillis();
        long delay;
        
        if (isCorrect) {
            // Increase interval for correct answers (1 day, 3 days, 7 days, 14 days, 30 days)
            if (timesCorrect <= 1) delay = 24 * 60 * 60 * 1000L; // 1 day
            else if (timesCorrect <= 2) delay = 3 * 24 * 60 * 60 * 1000L; // 3 days
            else if (timesCorrect <= 3) delay = 7 * 24 * 60 * 60 * 1000L; // 7 days
            else if (timesCorrect <= 4) delay = 14 * 24 * 60 * 60 * 1000L; // 14 days
            else delay = 30 * 24 * 60 * 60 * 1000L; // 30 days
        } else {
            // Incorrect answers: review in 1 day
            delay = 24 * 60 * 60 * 1000L;
        }
        
        nextReview = new Timestamp(new Date(currentTime + delay));
    }
    
    public boolean isDueForReview() {
        if (nextReview == null) return true;
        return System.currentTimeMillis() >= nextReview.toDate().getTime();
    }

    /**
     * Generate a meaningful title based on the unit name
     */
    private String generateTitleFromUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            return "Study Question";
        }
        
        // Clean up the unit name
        String cleanUnit = unit.trim();
        
        // Extract the main topic from the unit
        if (cleanUnit.toLowerCase().contains("introduction")) {
            return "Introduction to Healthcare";
        } else if (cleanUnit.toLowerCase().contains("basic") || cleanUnit.toLowerCase().contains("fundamental")) {
            return "Basic Healthcare Concepts";
        } else if (cleanUnit.toLowerCase().contains("patient") || cleanUnit.toLowerCase().contains("care")) {
            return "Patient Care Fundamentals";
        } else if (cleanUnit.toLowerCase().contains("safety") || cleanUnit.toLowerCase().contains("infection")) {
            return "Safety & Infection Control";
        } else if (cleanUnit.toLowerCase().contains("communication")) {
            return "Healthcare Communication";
        } else if (cleanUnit.toLowerCase().contains("documentation")) {
            return "Medical Documentation";
        } else if (cleanUnit.toLowerCase().contains("ethics") || cleanUnit.toLowerCase().contains("legal")) {
            return "Healthcare Ethics & Legal";
        } else if (cleanUnit.toLowerCase().contains("emergency") || cleanUnit.toLowerCase().contains("crisis")) {
            return "Emergency Response";
        } else if (cleanUnit.toLowerCase().contains("medication") || cleanUnit.toLowerCase().contains("pharmacology")) {
            return "Medication Safety";
        } else if (cleanUnit.toLowerCase().contains("assessment") || cleanUnit.toLowerCase().contains("vital")) {
            return "Patient Assessment";
        } else if (cleanUnit.toLowerCase().contains("hygiene") || cleanUnit.toLowerCase().contains("grooming")) {
            return "Personal Hygiene & Grooming";
        } else if (cleanUnit.toLowerCase().contains("mobility") || cleanUnit.toLowerCase().contains("transfer")) {
            return "Patient Mobility & Transfers";
        } else if (cleanUnit.toLowerCase().contains("nutrition") || cleanUnit.toLowerCase().contains("feeding")) {
            return "Nutrition & Feeding";
        } else if (cleanUnit.toLowerCase().contains("elimination") || cleanUnit.toLowerCase().contains("toileting")) {
            return "Elimination Assistance";
        } else if (cleanUnit.toLowerCase().contains("comfort") || cleanUnit.toLowerCase().contains("pain")) {
            return "Patient Comfort & Pain Management";
        } else if (cleanUnit.toLowerCase().contains("rehabilitation") || cleanUnit.toLowerCase().contains("therapy")) {
            return "Rehabilitation & Therapy";
        } else if (cleanUnit.toLowerCase().contains("pediatric") || cleanUnit.toLowerCase().contains("child")) {
            return "Pediatric Care";
        } else if (cleanUnit.toLowerCase().contains("geriatric") || cleanUnit.toLowerCase().contains("elderly")) {
            return "Geriatric Care";
        } else if (cleanUnit.toLowerCase().contains("mental") || cleanUnit.toLowerCase().contains("psychiatric")) {
            return "Mental Health Care";
        } else if (cleanUnit.toLowerCase().contains("obstetric") || cleanUnit.toLowerCase().contains("maternity")) {
            return "Maternal & Newborn Care";
        } else if (cleanUnit.toLowerCase().contains("surgical") || cleanUnit.toLowerCase().contains("operative")) {
            return "Surgical Care";
        } else if (cleanUnit.toLowerCase().contains("cardiac") || cleanUnit.toLowerCase().contains("heart")) {
            return "Cardiac Care";
        } else if (cleanUnit.toLowerCase().contains("respiratory") || cleanUnit.toLowerCase().contains("lung")) {
            return "Respiratory Care";
        } else if (cleanUnit.toLowerCase().contains("diabetes") || cleanUnit.toLowerCase().contains("endocrine")) {
            return "Diabetes & Endocrine Care";
        } else if (cleanUnit.toLowerCase().contains("wound") || cleanUnit.toLowerCase().contains("dressing")) {
            return "Wound Care & Dressing";
        } else if (cleanUnit.toLowerCase().contains("ostomy") || cleanUnit.toLowerCase().contains("colostomy")) {
            return "Ostomy Care";
        } else if (cleanUnit.toLowerCase().contains("catheter") || cleanUnit.toLowerCase().contains("urinary")) {
            return "Catheter Care";
        } else if (cleanUnit.toLowerCase().contains("oxygen") || cleanUnit.toLowerCase().contains("respiratory")) {
            return "Oxygen Therapy";
        } else if (cleanUnit.toLowerCase().contains("iv") || cleanUnit.toLowerCase().contains("intravenous")) {
            return "IV Therapy & Care";
        } else if (cleanUnit.toLowerCase().contains("specimen") || cleanUnit.toLowerCase().contains("lab")) {
            return "Specimen Collection & Lab";
        } else if (cleanUnit.toLowerCase().contains("death") || cleanUnit.toLowerCase().contains("dying")) {
            return "End-of-Life Care";
        } else {
            // For any other units, create a title from the unit name
            String[] words = cleanUnit.split("\\s+");
            if (words.length > 0) {
                // Capitalize first letter of each word and join
                StringBuilder title = new StringBuilder();
                for (String word : words) {
                    if (!word.isEmpty()) {
                        title.append(Character.toUpperCase(word.charAt(0)))
                              .append(word.substring(1).toLowerCase())
                              .append(" ");
                    }
                }
                return title.toString().trim();
            }
            return cleanUnit;
        }
    }

    // Parcelable implementation
    protected Flashcard(Parcel in) {
        flashcardId = in.readString();
        title = in.readString();
        question = in.readString();
        answer = in.readString();
        rationale = in.readString();
        if (in.readByte() == 0) {
            options = null;
        } else {
            options = new ArrayList<>();
            in.readList(options, String.class.getClassLoader());
        }
        career = in.readString();
        course = in.readString();
        unit = in.readString();
        source = in.readString();
        difficulty = in.readString();
        timesReviewed = in.readInt();
        timesCorrect = in.readInt();
        timesIncorrect = in.readInt();
        confidenceScore = in.readDouble();
        
        // Handle nullable Timestamps - convert milliseconds to seconds
        long lastReviewedTime = in.readLong();
        lastReviewed = lastReviewedTime > 0 ? new Timestamp(lastReviewedTime / 1000, 0) : null;
        
        long nextReviewTime = in.readLong();
        nextReview = nextReviewTime > 0 ? new Timestamp(nextReviewTime / 1000, 0) : null;
        
        userId = in.readString();
        isEnrolled = in.readByte() != 0;
        streakDays = in.readInt();
        
        long lastStudiedTime = in.readLong();
        lastStudied = lastStudiedTime > 0 ? new Timestamp(lastStudiedTime / 1000, 0) : null;
        
        long createdAtTime = in.readLong();
        createdAt = createdAtTime > 0 ? new Timestamp(createdAtTime / 1000, 0) : Timestamp.now();
        
        long updatedAtTime = in.readLong();
        updatedAt = updatedAtTime > 0 ? new Timestamp(updatedAtTime / 1000, 0) : Timestamp.now();
        
        isActive = in.readByte() != 0;
    }

    public static final Creator<Flashcard> CREATOR = new Creator<Flashcard>() {
        @Override
        public Flashcard createFromParcel(Parcel in) {
            return new Flashcard(in);
        }

        @Override
        public Flashcard[] newArray(int size) {
            return new Flashcard[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(flashcardId);
        dest.writeString(title);
        dest.writeString(question);
        dest.writeString(answer);
        dest.writeString(rationale);
        if (options == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeList(options);
        }
        dest.writeString(career);
        dest.writeString(course);
        dest.writeString(unit);
        dest.writeString(source);
        dest.writeString(difficulty);
        dest.writeInt(timesReviewed);
        dest.writeInt(timesCorrect);
        dest.writeInt(timesIncorrect);
        dest.writeDouble(confidenceScore);
        
        // Handle nullable Timestamps
        dest.writeLong(lastReviewed != null ? lastReviewed.toDate().getTime() : -1);
        dest.writeLong(nextReview != null ? nextReview.toDate().getTime() : -1);
        
        dest.writeString(userId);
        dest.writeByte((byte) (isEnrolled ? 1 : 0));
        dest.writeInt(streakDays);
        
        dest.writeLong(lastStudied != null ? lastStudied.toDate().getTime() : -1);
        dest.writeLong(createdAt != null ? createdAt.toDate().getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.toDate().getTime() : -1);
        
        dest.writeByte((byte) (isActive ? 1 : 0));
    }
}
