package com.example.nurse_connect.models;

/**
 * Flashcard Game Modes
 * Defines different ways users can interact with flashcards
 */
public enum FlashcardGameMode {
    TIMED_MODE("Timed Mode", "Answer as many flashcards as possible within time limit"),
    SPACED_REPETITION("Spaced Repetition", "AI-optimized learning intervals"),
    QUIZ_MODE("Quiz Mode", "Multiple choice questions with immediate feedback"),
    MATCHING_MODE("Matching Mode", "Match terms with definitions"),
    SCENARIO_MODE("Scenario Mode", "Real-world clinical case studies"),
    STUDY_MODE("Study Mode", "Traditional flashcard review"),
    DAILY_CHALLENGE("Daily Challenge", "Daily streak-based challenges"),
    MASTERY_MODE("Mastery Mode", "Focus on difficult concepts");
    
    private final String displayName;
    private final String description;
    
    FlashcardGameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
