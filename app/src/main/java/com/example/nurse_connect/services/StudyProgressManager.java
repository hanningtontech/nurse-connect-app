package com.example.nurse_connect.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Study Progress Manager
 * Handles 30-day streak counting and study progress tracking
 * Manages the 400 unique questions challenge over 30 days
 * 
 * IMPORTANT: This system now tracks UNIQUE flashcards only.
 * - Each flashcard ID is recorded once when completed
 * - Progress is based on unique questions, not total attempts
 * - Prevents double-counting of repeated questions
 * - Ensures accurate progress tracking
 */
public class StudyProgressManager {
    private static final String TAG = "StudyProgressManager";
    private static final String PREFS_NAME = "StudyProgressPrefs";
    
    // Challenge constants
    private static final int CHALLENGE_DAYS = 30;
    private static final int TARGET_QUESTIONS = 400; // Total questions (40 sets * 10 questions)
    private static final int QUESTIONS_PER_SET = 10;
    private static final long HOURS_BETWEEN_STREAKS = 8; // Count next streak every 8 hours
    
    // SharedPreferences keys
    private static final String KEY_FIRST_STUDY_DATE = "first_study_date";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_MAX_STREAK = "max_streak";
    private static final String KEY_STREAK_START_DATE = "streak_start_date";
    private static final String KEY_LAST_STUDY_DATE = "last_study_date";
    private static final String KEY_COMPLETED_SETS = "completed_sets";
    private static final String KEY_TOTAL_STUDY_SESSIONS = "total_study_sessions";
    private static final String KEY_CURRENT_SESSION_QUESTIONS = "current_session_questions";
    private static final String KEY_TOTAL_QUESTIONS_STUDIED = "total_questions_studied";
    private static final String KEY_COMPLETED_FLASHCARD_IDS = "completed_flashcard_ids";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    public StudyProgressManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Start a new study session (resets current session counter)
     */
    public void startNewStudySession() {
        // Reset current session questions counter
        prefs.edit().putInt(KEY_CURRENT_SESSION_QUESTIONS, 0).apply();
        Log.d(TAG, "New study session started - counter reset");
    }
    
    /**
     * Record a completed flashcard study session
     * Only counts NEW questions, not previously completed ones
     */
    public void recordStudySession(int questionsCompleted) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        
        long currentTime = System.currentTimeMillis();
        Date currentDate = new Date(currentTime);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        
        // Get or set first study date
        long firstStudyTime = prefs.getLong(KEY_FIRST_STUDY_DATE, currentTime);
        if (firstStudyTime == currentTime) {
            // First time studying
            prefs.edit().putLong(KEY_FIRST_STUDY_DATE, currentTime).apply();
            Log.d(TAG, "First study session recorded");
        }
        
        // Update last study date
        prefs.edit().putLong(KEY_LAST_STUDY_DATE, currentTime).apply();
        
        // Track current session questions (resets each session)
        prefs.edit().putInt(KEY_CURRENT_SESSION_QUESTIONS, questionsCompleted).apply();
        
        // Track total questions studied across all sessions
        int totalQuestions = prefs.getInt(KEY_TOTAL_QUESTIONS_STUDIED, 0);
        prefs.edit().putInt(KEY_TOTAL_QUESTIONS_STUDIED, totalQuestions + questionsCompleted).apply();
        
        // Only count NEW questions from this session for progress tracking
        // This ensures we don't double-count or include old questions
        int currentCompletedQuestions = prefs.getInt(KEY_COMPLETED_SETS, 0);
        int newTotal = currentCompletedQuestions + questionsCompleted; // Add only new questions
        
        // Ensure we don't exceed the target
        if (newTotal > TARGET_QUESTIONS) {
            newTotal = TARGET_QUESTIONS;
        }
        
        prefs.edit().putInt(KEY_COMPLETED_SETS, newTotal).apply();
        Log.d(TAG, "Completed " + questionsCompleted + " NEW questions this session. Total questions: " + newTotal + "/" + TARGET_QUESTIONS);
        
        // Update total study sessions
        int currentSessions = prefs.getInt(KEY_TOTAL_STUDY_SESSIONS, 0);
        prefs.edit().putInt(KEY_TOTAL_STUDY_SESSIONS, currentSessions + 1).apply();
        
        // Update streak
        updateStreak(currentTime);
        
        // Ensure required documents exist
        ensureRequiredDocumentsExist(userId);
        
        // Save to Firestore
        saveProgressToFirestore(userId);
    }
    
    /**
     * Record individual flashcard completion to prevent double-counting
     * This method should be called for each individual flashcard completed
     */
    public void recordIndividualFlashcard(String flashcardId) {
        if (flashcardId == null || flashcardId.trim().isEmpty()) {
            Log.w(TAG, "Cannot record flashcard with null or empty ID");
            return;
        }
        
        // Get the set of completed flashcard IDs
        java.util.Set<String> completedIds = prefs.getStringSet(KEY_COMPLETED_FLASHCARD_IDS, new java.util.HashSet<>());
        
        // Check if this flashcard was already completed
        if (completedIds.contains(flashcardId)) {
            Log.d(TAG, "Flashcard " + flashcardId + " already completed, skipping");
            return;
        }
        
        // Add this flashcard ID to completed set
        java.util.Set<String> newCompletedIds = new java.util.HashSet<>(completedIds);
        newCompletedIds.add(flashcardId);
        prefs.edit().putStringSet(KEY_COMPLETED_FLASHCARD_IDS, newCompletedIds).apply();
        
        Log.d(TAG, "New flashcard completed: " + flashcardId + ". Total unique flashcards: " + newCompletedIds.size());
    }
    
    /**
     * Get count of unique flashcards completed
     */
    public int getUniqueFlashcardsCompleted() {
        java.util.Set<String> completedIds = prefs.getStringSet(KEY_COMPLETED_FLASHCARD_IDS, new java.util.HashSet<>());
        return completedIds.size();
    }
    
    /**
     * Update the current streak based on study activity
     * Counts every 8 hours since day 1, resets to 1 after day 30
     */
    private void updateStreak(long currentTime) {
        long firstStudyTime = prefs.getLong(KEY_FIRST_STUDY_DATE, 0);
        long lastStudyTime = prefs.getLong(KEY_LAST_STUDY_DATE, 0);
        long streakStartTime = prefs.getLong(KEY_STREAK_START_DATE, 0);
        
        if (firstStudyTime == 0) {
            // First study session
            prefs.edit()
                .putLong(KEY_STREAK_START_DATE, currentTime)
                .putInt(KEY_CURRENT_STREAK, 1)
                .putInt(KEY_MAX_STREAK, 1)
                .apply();
            return;
        }
        
        // Calculate days since first study
        long daysSinceFirst = (currentTime - firstStudyTime) / (1000 * 60 * 60 * 24);
        
        // Reset streak to 1 after day 30
        if (daysSinceFirst >= CHALLENGE_DAYS) {
            prefs.edit()
                .putLong(KEY_STREAK_START_DATE, currentTime)
                .putInt(KEY_CURRENT_STREAK, 1)
                .apply();
            Log.d(TAG, "30-day challenge completed, streak reset to 1");
            return;
        }
        
        if (lastStudyTime == 0) {
            // First study session in this streak
            prefs.edit()
                .putLong(KEY_STREAK_START_DATE, currentTime)
                .putInt(KEY_CURRENT_STREAK, 1)
                .apply();
            return;
        }
        
        // Calculate hours between last study and current study
        long hoursDiff = (currentTime - lastStudyTime) / (1000 * 60 * 60);
        
        if (hoursDiff >= HOURS_BETWEEN_STREAKS) {
            // 8+ hours have passed - increment streak
            int currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0);
            int newStreak = currentStreak + 1;
            prefs.edit().putInt(KEY_CURRENT_STREAK, newStreak).apply();
            
            // Update max streak if needed
            int maxStreak = prefs.getInt(KEY_MAX_STREAK, 0);
            if (newStreak > maxStreak) {
                prefs.edit().putInt(KEY_MAX_STREAK, newStreak).apply();
                Log.d(TAG, "New max streak: " + newStreak + " (every 8 hours)!");
            }
            
            Log.d(TAG, "8+ hours passed - streak: " + newStreak + " (every 8 hours)");
        } else {
            // Less than 8 hours - streak continues
            Log.d(TAG, "Less than 8 hours - streak continues");
        }
    }
    
    /**
     * Get current session progress (questions completed in this session)
     */
    public int getCurrentSessionProgress() {
        return prefs.getInt(KEY_CURRENT_SESSION_QUESTIONS, 0);
    }
    
    /**
     * Get total questions studied across all sessions
     */
    public int getTotalQuestionsStudied() {
        return prefs.getInt(KEY_TOTAL_QUESTIONS_STUDIED, 0);
    }
    
    /**
     * Get current study progress information
     */
    public StudyProgress getStudyProgress() {
        long firstStudyTime = prefs.getLong(KEY_FIRST_STUDY_DATE, 0);
        long currentTime = System.currentTimeMillis();
        
        int currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0);
        int maxStreak = prefs.getInt(KEY_MAX_STREAK, 0);
        
        // Use unique flashcard count instead of potentially inflated count
        int uniqueFlashcardsCompleted = getUniqueFlashcardsCompleted();
        int totalSessions = prefs.getInt(KEY_TOTAL_STUDY_SESSIONS, 0);
        
        // Calculate days since first study
        int daysSinceFirst = 0;
        int daysRemaining = CHALLENGE_DAYS;
        if (firstStudyTime > 0) {
            daysSinceFirst = (int) ((currentTime - firstStudyTime) / (1000 * 60 * 60 * 24));
            daysRemaining = Math.max(0, CHALLENGE_DAYS - daysSinceFirst);
        }
        
        // Calculate progress percentage based on unique questions (no double-counting)
        double progressPercentage = (double) uniqueFlashcardsCompleted / TARGET_QUESTIONS * 100;
        
        // Check if on track
        boolean isOnTrack = isOnTrackForChallenge(daysSinceFirst, uniqueFlashcardsCompleted);
        
        // Get current session and total questions
        int currentSessionQuestions = prefs.getInt(KEY_CURRENT_SESSION_QUESTIONS, 0);
        int totalQuestionsStudied = prefs.getInt(KEY_TOTAL_QUESTIONS_STUDIED, 0);
        
        return new StudyProgress(
            currentStreak,
            maxStreak,
            uniqueFlashcardsCompleted, // Use unique count
            TARGET_QUESTIONS,
            totalSessions,
            daysSinceFirst,
            daysRemaining,
            progressPercentage,
            isOnTrack,
            firstStudyTime > 0,
            currentSessionQuestions,
            totalQuestionsStudied
        );
    }
    
    /**
     * Check if user is on track for the 30-day challenge
     */
    private boolean isOnTrackForChallenge(int daysElapsed, int completedSets) {
        if (daysElapsed == 0) return true;
        
        // Calculate expected progress: should complete (400 questions / 30 days) * days elapsed
        double expectedProgress = (double) TARGET_QUESTIONS / CHALLENGE_DAYS * daysElapsed;
        return completedSets >= expectedProgress;
    }
    
    /**
     * Save progress to Firestore
     */
    private void saveProgressToFirestore(String userId) {
        StudyProgress progress = getStudyProgress();
        
        // Create progress document
        ProgressDocument progressDoc = new ProgressDocument(
            userId,
            progress.getCurrentStreak(),
            progress.getMaxStreak(),
            progress.getCompletedSets(),
            progress.getTargetSets(),
            progress.getTotalSessions(),
            progress.getDaysSinceFirst(),
            progress.getDaysRemaining(),
            progress.getProgressPercentage(),
            progress.isOnTrack(),
            Timestamp.now()
        );
        
        db.collection("users").document(userId)
            .collection("study_progress")
            .document("current_progress")
            .set(progressDoc)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Progress saved to Firestore"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to save progress to Firestore", e));
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
    
    /**
     * Ensure required Firestore documents exist
     */
    private void ensureRequiredDocumentsExist(String userId) {
        // Create study_stats document if it doesn't exist
        db.collection("users").document(userId)
            .collection("study_stats")
            .document("flashcards")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    // Create the document with initial values
                    java.util.Map<String, Object> initialStats = new java.util.HashMap<>();
                    initialStats.put("totalCardsStudied", 0);
                    initialStats.put("totalScore", 0);
                    initialStats.put("lastStudied", com.google.firebase.Timestamp.now());
                    initialStats.put("createdAt", com.google.firebase.Timestamp.now());
                    initialStats.put("updatedAt", com.google.firebase.Timestamp.now());
                    
                    db.collection("users").document(userId)
                        .collection("study_stats")
                        .document("flashcards")
                        .set(initialStats)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Study stats document created"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to create study stats document", e));
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to check study stats document", e));
            
        // Create flashcard_memory collection structure if needed
        // This will be created automatically when first flashcard is saved
        Log.d(TAG, "Ensuring flashcard_memory collection structure exists");
    }
    
    /**
     * Reset all study progress and start fresh
     */
    public void resetProgress() {
        String userId = getCurrentUserId();
        if (userId == null) return;
        
        // Clear all local data
        prefs.edit()
            .remove(KEY_FIRST_STUDY_DATE)
            .remove(KEY_CURRENT_STREAK)
            .remove(KEY_MAX_STREAK)
            .remove(KEY_STREAK_START_DATE)
            .remove(KEY_LAST_STUDY_DATE)
            .remove(KEY_COMPLETED_SETS)
            .remove(KEY_TOTAL_STUDY_SESSIONS)
            .remove(KEY_COMPLETED_FLASHCARD_IDS)
            .apply();
        
        // Clear other potential flashcard-related SharedPreferences
        clearAllFlashcardPreferences();
        
        // Clear Firestore data
        if (db != null) {
            // Clear study progress
            db.collection("users").document(userId)
                .collection("study_progress")
                .document("current_progress")
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Study progress Firestore data cleared"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to clear study progress Firestore data", e));
            
            // Clear flashcard deck progress (streaks, etc.)
            db.collection("users").document(userId)
                .collection("flashcard_decks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot deckDoc : querySnapshot.getDocuments()) {
                        // Reset streak data for each deck
                        deckDoc.getReference().update(
                            "currentStreak", 0,
                            "longestStreak", 0,
                            "lastStudied", null,
                            "totalStudied", 0,
                            "totalCorrect", 0,
                            "averageScore", 0.0,
                            "daysStudied", 0,
                            "weeklyProgress", new java.util.HashMap<>(),
                            "dailyProgress", new java.util.HashMap<>(),
                            "updatedAt", com.google.firebase.Timestamp.now()
                        ).addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "Deck streak reset: " + deckDoc.getId())
                        ).addOnFailureListener(e -> 
                            Log.e(TAG, "Failed to reset deck streak: " + deckDoc.getId(), e)
                        );
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get flashcard decks for reset", e));
            
            // Clear flashcard memory (individual card progress)
            db.collection("users").document(userId)
                .collection("flashcard_memory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot memoryDoc : querySnapshot.getDocuments()) {
                        memoryDoc.getReference().delete()
                            .addOnSuccessListener(aVoid -> 
                                Log.d(TAG, "Flashcard memory cleared: " + memoryDoc.getId())
                            )
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "Failed to clear flashcard memory: " + memoryDoc.getId(), e)
                            );
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get flashcard memory for reset", e));
        }
        
        Log.d(TAG, "Study progress reset successfully - all streak data cleared");
    }
    
    /**
     * Clear all flashcard-related SharedPreferences data
     */
    private void clearAllFlashcardPreferences() {
        // Clear any other SharedPreferences that might contain flashcard data
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        // Clear all keys that might contain flashcard progress
        for (String key : prefs.getAll().keySet()) {
            if (key.toLowerCase().contains("flashcard") || 
                key.toLowerCase().contains("streak") || 
                key.toLowerCase().contains("study") ||
                key.toLowerCase().contains("deck") ||
                key.toLowerCase().contains("progress")) {
                editor.remove(key);
                Log.d(TAG, "Cleared preference key: " + key);
            }
        }
        
        editor.apply();
        
        // Also clear other potential SharedPreferences files
        try {
            // Clear FlashcardDeck preferences
            android.content.SharedPreferences deckPrefs = context.getSharedPreferences("FlashcardDeckPrefs", Context.MODE_PRIVATE);
            deckPrefs.edit().clear().apply();
            Log.d(TAG, "Cleared FlashcardDeck preferences");
            
            // Clear any other potential preference files
            android.content.SharedPreferences generalPrefs = context.getSharedPreferences("GeneralPrefs", Context.MODE_PRIVATE);
            generalPrefs.edit().clear().apply();
            Log.d(TAG, "Cleared general preferences");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing additional preferences", e);
        }
    }
    
    /**
     * Force reset the first study date to start counting from today
     */
    public void resetFirstStudyDate() {
        // Set first study date to current time (today)
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(KEY_FIRST_STUDY_DATE, currentTime).apply();
        
        // Reset streak to 1 (since we're starting today)
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, 1)
            .putInt(KEY_MAX_STREAK, 1)
            .putLong(KEY_STREAK_START_DATE, currentTime)
            .putLong(KEY_LAST_STUDY_DATE, currentTime)
            .putInt(KEY_COMPLETED_SETS, 0)
            .putInt(KEY_TOTAL_STUDY_SESSIONS, 0)
            .putStringSet(KEY_COMPLETED_FLASHCARD_IDS, new java.util.HashSet<>())
            .apply();
        
        Log.d(TAG, "First study date reset to today - streak will start from day 1");
    }
    
    /**
     * Quick reset - just reset the streak counter and first study date
     * This is useful if you just want to start the streak over without clearing all progress
     */
    public void quickResetStreak() {
        resetFirstStudyDate();
        Log.d(TAG, "Quick streak reset completed - starting from day 1");
    }
    
    /**
     * Get formatted date string
     */
    public String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Study Progress Data Class
     */
    public static class StudyProgress {
        private final int currentStreak;
        private final int maxStreak;
        private final int completedSets;
        private final int targetSets;
        private final int totalSessions;
        private final int daysSinceFirst;
        private final int daysRemaining;
        private final double progressPercentage;
        private final boolean isOnTrack;
        private final boolean hasStarted;
        private final int currentSessionQuestions;
        private final int totalQuestionsStudied;
        
        public StudyProgress(int currentStreak, int maxStreak, int completedSets, int targetSets,
                           int totalSessions, int daysSinceFirst, int daysRemaining,
                           double progressPercentage, boolean isOnTrack, boolean hasStarted,
                           int currentSessionQuestions, int totalQuestionsStudied) {
            this.currentStreak = currentStreak;
            this.maxStreak = maxStreak;
            this.completedSets = completedSets;
            this.targetSets = targetSets;
            this.totalSessions = totalSessions;
            this.daysSinceFirst = daysSinceFirst;
            this.daysRemaining = daysRemaining;
            this.progressPercentage = progressPercentage;
            this.isOnTrack = isOnTrack;
            this.hasStarted = hasStarted;
            this.currentSessionQuestions = currentSessionQuestions;
            this.totalQuestionsStudied = totalQuestionsStudied;
        }
        
        // Getters
        public int getCurrentStreak() { return currentStreak; }
        public int getMaxStreak() { return maxStreak; }
        public int getCompletedSets() { return completedSets; }
        public int getTargetSets() { return targetSets; }
        public int getTotalSessions() { return totalSessions; }
        public int getDaysSinceFirst() { return daysSinceFirst; }
        public int getDaysRemaining() { return daysRemaining; }
        public double getProgressPercentage() { return progressPercentage; }
        public boolean isOnTrack() { return isOnTrack; }
        public boolean hasStarted() { return hasStarted; }
        public int getCurrentSessionQuestions() { return currentSessionQuestions; }
        public int getTotalQuestionsStudied() { return totalQuestionsStudied; }
        
        /**
         * Get progress status message
         */
        public String getProgressStatusMessage() {
            if (!hasStarted) {
                return "Start your 30-day challenge!";
            }
            
            if (daysRemaining == 0) {
                if (completedSets >= targetSets) {
                    return "🎉 Challenge completed! Great job!";
                } else {
                    return "Challenge period ended. You completed " + completedSets + "/" + targetSets + " unique questions.";
                }
            }
            
            if (isOnTrack) {
                return "On track! " + daysRemaining + " days remaining.";
            } else {
                return "Catch up needed! " + daysRemaining + " days remaining.";
            }
        }
        
        /**
         * Get streak status message
         */
        public String getStreakStatusMessage() {
            if (currentStreak == 0) {
                return "Start your streak today!";
            } else if (currentStreak == 1) {
                return "1 streak (8 hours)! Keep it going!";
            } else if (currentStreak < 7) {
                return currentStreak + " streaks (every 8 hours)! Building momentum!";
            } else if (currentStreak < 30) {
                return currentStreak + " streaks (every 8 hours)! You're on fire! 🔥";
            } else {
                return currentStreak + " streaks (every 8 hours)! Unstoppable! 🚀";
            }
        }
    }
    
    /**
     * Firestore Document Class
     */
    private static class ProgressDocument {
        public String userId;
        public int currentStreak;
        public int maxStreak;
        public int completedSets;
        public int targetSets;
        public int totalSessions;
        public int daysSinceFirst;
        public int daysRemaining;
        public double progressPercentage;
        public boolean isOnTrack;
        public Timestamp lastUpdated;
        
        public ProgressDocument() {} // Required for Firestore
        
        public ProgressDocument(String userId, int currentStreak, int maxStreak, int completedSets,
                              int targetSets, int totalSessions, int daysSinceFirst, int daysRemaining,
                              double progressPercentage, boolean isOnTrack, Timestamp lastUpdated) {
            this.userId = userId;
            this.currentStreak = currentStreak;
            this.maxStreak = maxStreak;
            this.completedSets = completedSets;
            this.targetSets = targetSets;
            this.totalSessions = totalSessions;
            this.daysSinceFirst = daysSinceFirst;
            this.daysRemaining = daysRemaining;
            this.progressPercentage = progressPercentage;
            this.isOnTrack = isOnTrack;
            this.lastUpdated = lastUpdated;
        }
    }
}
