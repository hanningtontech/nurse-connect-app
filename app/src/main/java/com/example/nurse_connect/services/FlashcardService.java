package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardDeck;
import com.example.nurse_connect.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

/**
 * Flashcard Service
 * Handles flashcard operations, learning algorithms, and user progress tracking
 * Now integrated with Gemini AI for real-time flashcard generation
 */
public class FlashcardService {
    private static final String TAG = "FlashcardService";
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private GeminiFlashcardService geminiService;
    
    public interface FlashcardCallback {
        void onFlashcardsLoaded(List<Flashcard> flashcards);
        void onError(String error);
    }
    
    public interface DeckCallback {
        void onDeckLoaded(FlashcardDeck deck);
        void onError(String error);
    }
    
    public interface ProgressCallback {
        void onProgressUpdated(boolean success);
        void onError(String error);
    }
    
    public FlashcardService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        geminiService = new GeminiFlashcardService();
        Log.d(TAG, "FlashcardService initialized with Gemini AI integration");
    }
    
    /**
     * Create a new flashcard deck
     */
    private void createNewDeck(String deckId, FlashcardCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        Log.d(TAG, "Creating new deck with ID: " + deckId);

        // Create deck name and description
        String deckName = String.format("Sample Deck - %s", deckId);
        String description = String.format("Sample flashcard deck for testing");

        FlashcardDeck deck = new FlashcardDeck(deckName, description, "CNA", "Basic Skills", "Unit 1");
        deck.setDeckId(deckId);
        deck.setUserId(currentUserId);

        // Save to Firestore
        db.collection("flashcard_decks")
                .document(deckId)
                .set(deck)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New deck created successfully, fetching flashcards from quiz_questions");
                    // Fetch flashcards from existing quiz_questions collection
                    fetchFlashcardsFromQuizQuestions(deck, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating flashcard deck", e);
                    callback.onError("Failed to create deck: " + e.getMessage());
                });
    }

    /**
     * Create a new flashcard deck with career/course/unit parameters
     */
    private void createNewDeckWithParams(String career, String course, String unit, DeckCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        Log.d(TAG, "Creating new deck with params - career: " + career + ", course: " + course + ", unit: " + unit);

        // Create deck name and description
        String deckName = String.format("%s - %s - %s", career, course, unit);
        String description = String.format("Flashcards for %s in %s, %s", unit, course, career);

        FlashcardDeck deck = new FlashcardDeck(deckName, description, career, course, unit);
        deck.setUserId(currentUserId);

        // Save to Firestore
        db.collection("flashcard_decks")
                .add(deck)
                .addOnSuccessListener(documentReference -> {
                    deck.setDeckId(documentReference.getId());
                    Log.d(TAG, "New deck created successfully with ID: " + documentReference.getId());
                    callback.onDeckLoaded(deck);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating flashcard deck", e);
                    callback.onError("Failed to create deck: " + e.getMessage());
                });
    }
    
    /**
     * Get available flashcard decks for a user
     */
    public void getAvailableDecks(String career, String course, String unit, DeckCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }
        
        Query query = db.collection("flashcard_decks")
                .whereEqualTo("career", career)
                .whereEqualTo("course", course)
                .whereEqualTo("unit", unit)
                .whereEqualTo("isActive", true);
        
        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Get the first available deck
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        FlashcardDeck deck = doc.toObject(FlashcardDeck.class);
                        if (deck != null) {
                            deck.setDeckId(doc.getId());
                            callback.onDeckLoaded(deck);
                        } else {
                            callback.onError("Failed to load deck");
                        }
                    } else {
                        // Create a new deck if none exists
                        createNewDeckWithParams(career, course, unit, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting flashcard decks", e);
                    callback.onError("Failed to load decks: " + e.getMessage());
                });
    }
    
    /**
     * Get daily flashcards for study
     */
    public void getDailyFlashcards(String deckId, FlashcardCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        Log.d(TAG, "Getting daily flashcards for deck: " + deckId);

        // First get the deck to see available flashcards
        db.collection("flashcard_decks")
                .document(deckId)
                .get()
                .addOnSuccessListener(deckDoc -> {
                    if (deckDoc.exists()) {
                        Log.d(TAG, "Deck found in Firestore");
                        FlashcardDeck deck = deckDoc.toObject(FlashcardDeck.class);
                        if (deck != null && deck.getFlashcardIds() != null && !deck.getFlashcardIds().isEmpty()) {
                            Log.d(TAG, "Deck has " + deck.getFlashcardIds().size() + " existing flashcards");
                            // Get flashcards from the deck
                            getFlashcardsByIds(deck.getFlashcardIds(), callback);
                        } else {
                            Log.d(TAG, "Deck has no existing flashcards, fetching from quiz_questions");
                            // Fetch flashcards from existing quiz_questions collection
                            fetchFlashcardsFromQuizQuestions(deck, callback);
                        }
                    } else {
                        Log.d(TAG, "Deck not found in Firestore, creating new deck");
                        // Create a new deck if none exists
                        createNewDeck(deckId, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting deck", e);
                    callback.onError("Failed to load deck: " + e.getMessage());
                });
    }
    
    /**
     * Get flashcards by IDs
     */
    public void getFlashcardsByIds(List<String> flashcardIds, FlashcardCallback callback) {
        if (flashcardIds == null || flashcardIds.isEmpty()) {
            callback.onFlashcardsLoaded(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Getting flashcards by IDs: " + flashcardIds.size() + " IDs");

        // Get up to 20 flashcards for daily study
        List<String> dailyIds = flashcardIds.subList(0, Math.min(20, flashcardIds.size()));
        Log.d(TAG, "Getting " + dailyIds.size() + " flashcards for daily study");

        db.collection("flashcards")
                .whereIn("flashcardId", dailyIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Flashcard> flashcards = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Flashcard flashcard = doc.toObject(Flashcard.class);
                        if (flashcard != null) {
                            flashcard.setFlashcardId(doc.getId());
                            Log.d(TAG, "Loaded flashcard with ID: " + doc.getId() + ", options: " + (flashcard.getOptions() != null ? flashcard.getOptions().size() : "null"));
                            if (flashcard.getOptions() != null) {
                                for (int i = 0; i < flashcard.getOptions().size(); i++) {
                                    Log.d(TAG, "  Option " + i + ": " + flashcard.getOptions().get(i));
                                }
                            }
                            flashcards.add(flashcard);
                        }
                    }

                    Log.d(TAG, "Successfully loaded " + flashcards.size() + " flashcards from Firestore");

                    // Shuffle flashcards for variety
                    Collections.shuffle(flashcards);
                    callback.onFlashcardsLoaded(flashcards);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting flashcards", e);
                    callback.onError("Failed to load flashcards: " + e.getMessage());
                });
    }
    
    /**
     * Generate flashcards from PDFs/testbanks
     */
    private void generateFlashcardsFromSource(FlashcardDeck deck, FlashcardCallback callback) {
        Log.d(TAG, "Generating sample flashcards for deck: " + deck.getDeckId());
        
        // For now, create sample flashcards
        // In the future, this will extract from PDFs/testbanks
        List<Flashcard> sampleFlashcards = createSampleFlashcards(deck);
        
        Log.d(TAG, "Created " + sampleFlashcards.size() + " sample flashcards, saving to Firestore");

        // Save flashcards to Firestore
        saveFlashcards(sampleFlashcards, deck, callback);
    }
    
    /**
     * Create sample flashcards for testing
     */
    private List<Flashcard> createSampleFlashcards(FlashcardDeck deck) {
        Log.d(TAG, "Creating sample flashcards for career: " + deck.getCareer() + ", course: " + deck.getCourse() + ", unit: " + deck.getUnit());
        
        List<Flashcard> flashcards = new ArrayList<>();
        
        // Sample questions based on career/course/unit
        String[] questions = {
            "What is the primary role of a " + deck.getCareer() + "?",
            "Which of the following is a key skill in " + deck.getCourse() + "?",
            "What is the main focus of " + deck.getUnit() + "?",
            "How does " + deck.getUnit() + " relate to patient care?",
            "What are the safety protocols in " + deck.getUnit() + "?",
            "Which assessment is most important in " + deck.getUnit() + "?",
            "How do you document " + deck.getUnit() + " procedures?",
            "What are the legal considerations in " + deck.getUnit() + "?",
            "How do you communicate with patients about " + deck.getUnit() + "?",
            "What equipment is needed for " + deck.getUnit() + "?",
            "What are the infection control measures for " + deck.getUnit() + "?",
            "How do you evaluate patient response to " + deck.getUnit() + "?",
            "What are the emergency procedures in " + deck.getUnit() + "?",
            "How do you teach patients about " + deck.getUnit() + "?",
            "What are the quality indicators for " + deck.getUnit() + "?",
            "How do you collaborate with other healthcare providers on " + deck.getUnit() + "?",
            "What are the cultural considerations in " + deck.getUnit() + "?",
            "How do you maintain patient privacy during " + deck.getUnit() + "?",
            "What are the documentation requirements for " + deck.getUnit() + "?",
            "How do you ensure patient comfort during " + deck.getUnit() + "?"
        };
        
        String[] answers = {
            "To provide safe and effective patient care",
            "Patient assessment and communication",
            "Patient safety and well-being",
            "It directly impacts patient outcomes",
            "Infection control is critical in healthcare settings",
            "Assessment provides the foundation for care planning",
            "Documentation is a legal and professional requirement",
            "Patient rights must always be protected",
            "Effective communication improves patient understanding",
            "Proper equipment ensures safe and effective care",
            "Infection control protects both patients and staff",
            "Ongoing assessment ensures care effectiveness",
            "Emergency situations require immediate action",
            "Patient education promotes self-care and compliance",
            "Quality measures ensure care standards are met",
            "Healthcare is a team effort requiring coordination",
            "Cultural sensitivity improves patient care",
            "Patient dignity and privacy must be maintained",
            "Complete documentation supports quality care",
            "Patient comfort improves care experience"
        };
        
        String[] rationales = {
            "This is the fundamental purpose of nursing care",
            "These skills are essential for effective patient care",
            "Patient safety is always the primary concern",
            "All nursing activities should improve patient outcomes",
            "Infection control is critical in healthcare settings",
            "Assessment provides the foundation for care planning",
            "Documentation is a legal and professional requirement",
            "Patient rights must always be protected",
            "Effective communication improves patient understanding",
            "Proper equipment ensures safe and effective care",
            "Infection control protects both patients and staff",
            "Ongoing assessment ensures care effectiveness",
            "Emergency situations require immediate action",
            "Patient education promotes self-care and compliance",
            "Quality measures ensure care standards are met",
            "Healthcare is a team effort requiring coordination",
            "Cultural sensitivity improves patient care",
            "Patient dignity and privacy must be maintained",
            "Complete documentation supports quality care",
            "Patient comfort improves care experience"
        };
        
        for (int i = 0; i < questions.length; i++) {
            Flashcard flashcard = new Flashcard(questions[i], answers[i], rationales[i],
                    deck.getCareer(), deck.getCourse(), deck.getUnit());
            flashcard.setSource("Sample Generated");
            flashcard.setDifficulty("medium");
            
            // Create multiple choice options with realistic content
            List<String> options = new ArrayList<>();
            options.add(answers[i]); // Correct answer
            
            // Create realistic incorrect options based on the question type
            String[] incorrectOptions = generateIncorrectOptions(questions[i], answers[i], deck.getCareer(), deck.getCourse(), deck.getUnit());
            options.add(incorrectOptions[0]);
            options.add(incorrectOptions[1]);
            options.add(incorrectOptions[2]);
            
            Collections.shuffle(options); // Randomize order
            flashcard.setOptions(options);
            
            // Verify options were set correctly
            Log.d(TAG, "Created flashcard " + i + " with " + options.size() + " options: " + options);
            Log.d(TAG, "Flashcard options after setOptions: " + (flashcard.getOptions() != null ? flashcard.getOptions().size() : "null"));
            if (flashcard.getOptions() != null) {
                for (int j = 0; j < flashcard.getOptions().size(); j++) {
                    Log.d(TAG, "  Verified option " + j + ": " + flashcard.getOptions().get(j));
                }
            }
            
            flashcards.add(flashcard);
        }
        
        Log.d(TAG, "Created " + flashcards.size() + " sample flashcards");
        
        return flashcards;
    }
    
    /**
     * Save flashcards to Firestore
     */
    private void saveFlashcards(List<Flashcard> flashcards, FlashcardDeck deck, FlashcardCallback callback) {
        List<String> flashcardIds = new ArrayList<>();
        final int[] savedCount = {0};
        
        Log.d(TAG, "Saving " + flashcards.size() + " flashcards to Firestore");
        
        for (int i = 0; i < flashcards.size(); i++) {
            Flashcard flashcard = flashcards.get(i);
            Log.d(TAG, "Saving flashcard " + i + " with options: " + (flashcard.getOptions() != null ? flashcard.getOptions().size() : "null"));
            db.collection("flashcards")
                    .add(flashcard)
                    .addOnSuccessListener(documentReference -> {
                        String flashcardId = documentReference.getId();
                        flashcardIds.add(flashcardId);
                        
                        // Update the local flashcard object with its ID
                        flashcard.setFlashcardId(flashcardId);
                        Log.d(TAG, "Saved flashcard with ID: " + flashcardId + ", options: " + (flashcard.getOptions() != null ? flashcard.getOptions().size() : "null"));
                        
                        savedCount[0]++;
                        
                        if (savedCount[0] == flashcards.size()) {
                            // All flashcards saved, update deck
                            deck.setFlashcardIds(flashcardIds);
                            deck.setTotalFlashcards(flashcardIds.size());
                            
                            Log.d(TAG, "All flashcards saved. Updating deck with " + flashcardIds.size() + " flashcard IDs");
                            
                            db.collection("flashcard_decks")
                                    .document(deck.getDeckId())
                                    .update("flashcardIds", flashcardIds, "totalFlashcards", flashcardIds.size())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Deck updated successfully. Calling callback with " + flashcards.size() + " flashcards");
                                        callback.onFlashcardsLoaded(flashcards);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating deck", e);
                                        callback.onError("Failed to update deck");
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving flashcard", e);
                        callback.onError("Failed to save flashcard");
                    });
        }
    }
    
    /**
     * Record user's answer to a flashcard
     */
    public void recordAnswer(String flashcardId, boolean isCorrect, ProgressCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Recording answer for flashcard ID: '" + flashcardId + "', isCorrect: " + isCorrect);
        
        // Safety check: ensure flashcardId is not empty
        if (flashcardId == null || flashcardId.trim().isEmpty()) {
            Log.e(TAG, "Invalid flashcard ID: '" + flashcardId + "'");
            callback.onError("Invalid flashcard ID");
            return;
        }
        
        // Check if this is an AI-generated flashcard (starts with "gemini_" or "fallback_")
        if (flashcardId.startsWith("gemini_") || flashcardId.startsWith("fallback_")) {
            Log.d(TAG, "AI-generated flashcard detected, recording answer locally without Firestore lookup");
            
            // For AI-generated flashcards, we don't need to look them up in Firestore
            // Just record the answer locally and call success
            callback.onProgressUpdated(true);
            return;
        }
        
        // For regular flashcards, update progress in Firestore
        db.collection("flashcards")
                .document(flashcardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Flashcard flashcard = documentSnapshot.toObject(Flashcard.class);
                    if (flashcard != null) {
                        Log.d(TAG, "Found flashcard, updating progress");
                        flashcard.recordAnswer(isCorrect);
                        flashcard.setUpdatedAt(com.google.firebase.Timestamp.now());
                        
                        // Create a map with only the allowed progress fields
                        Map<String, Object> progressUpdates = new HashMap<>();
                        progressUpdates.put("timesReviewed", flashcard.getTimesReviewed());
                        progressUpdates.put("timesCorrect", flashcard.getTimesCorrect());
                        progressUpdates.put("timesIncorrect", flashcard.getTimesIncorrect());
                        progressUpdates.put("confidenceScore", flashcard.getConfidenceScore());
                        progressUpdates.put("lastReviewed", flashcard.getLastReviewed());
                        progressUpdates.put("nextReview", flashcard.getNextReview());
                        progressUpdates.put("streakDays", flashcard.getStreakDays());
                        progressUpdates.put("lastStudied", flashcard.getLastStudied());
                        progressUpdates.put("updatedAt", flashcard.getUpdatedAt());
                        
                        // Save updated flashcard using update() with only allowed fields
                        db.collection("flashcards")
                                .document(flashcardId)
                                .update(progressUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Flashcard progress updated successfully");
                                    callback.onProgressUpdated(true);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating flashcard", e);
                                    callback.onError("Failed to update progress");
                                });
                    } else {
                        Log.e(TAG, "Flashcard not found in document snapshot");
                        callback.onError("Flashcard not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting flashcard", e);
                    callback.onError("Failed to load flashcard");
                });
    }
    
    /**
     * Enroll user in a flashcard deck
     */
    public void enrollInDeck(String deckId, ProgressCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }
        
        db.collection("flashcard_decks")
                .document(deckId)
                .update("isEnrolled", true, "enrollmentDate", com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    callback.onProgressUpdated(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error enrolling in deck", e);
                    callback.onError("Failed to enroll: " + e.getMessage());
                });
    }
    
    /**
     * Get user's learning statistics
     */
    public void getUserStats(String userId, DeckCallback callback) {
        // This would aggregate stats from all user's flashcard activities
        // For now, return a placeholder
        callback.onError("Stats feature coming soon");
    }

    /**
     * Generates realistic incorrect options for a flashcard based on the question content.
     */
    private String[] generateIncorrectOptions(String question, String correctAnswer, String career, String course, String unit) {
        List<String> incorrectOptions = new ArrayList<>();
        
        // Create contextually relevant incorrect options based on the question
        if (question.contains("role")) {
            incorrectOptions.add("To manage hospital finances");
            incorrectOptions.add("To conduct medical research");
            incorrectOptions.add("To provide administrative support");
        } else if (question.contains("skill")) {
            incorrectOptions.add("Advanced surgical techniques");
            incorrectOptions.add("Pharmaceutical calculations");
            incorrectOptions.add("Radiology interpretation");
        } else if (question.contains("focus")) {
            incorrectOptions.add("Financial profitability");
            incorrectOptions.add("Administrative efficiency");
            incorrectOptions.add("Research advancement");
        } else if (question.contains("relation")) {
            incorrectOptions.add("It increases hospital costs");
            incorrectOptions.add("It improves staff satisfaction");
            incorrectOptions.add("It reduces paperwork");
        } else if (question.contains("safety")) {
            incorrectOptions.add("Cost reduction measures");
            incorrectOptions.add("Staff scheduling protocols");
            incorrectOptions.add("Inventory management");
        } else if (question.contains("assessment")) {
            incorrectOptions.add("Financial documentation");
            incorrectOptions.add("Staff performance reviews");
            incorrectOptions.add("Equipment maintenance logs");
        } else if (question.contains("documentation")) {
            incorrectOptions.add("Personal opinions");
            incorrectOptions.add("Financial records");
            incorrectOptions.add("Staff schedules");
        } else if (question.contains("legal")) {
            incorrectOptions.add("Hospital policies");
            incorrectOptions.add("Financial regulations");
            incorrectOptions.add("Staff guidelines");
        } else if (question.contains("communication")) {
            incorrectOptions.add("Technical jargon");
            incorrectOptions.add("Financial reports");
            incorrectOptions.add("Administrative memos");
        } else if (question.contains("equipment")) {
            incorrectOptions.add("Office supplies");
            incorrectOptions.add("Administrative tools");
            incorrectOptions.add("Financial software");
        } else if (question.contains("infection control")) {
            incorrectOptions.add("Cost management");
            incorrectOptions.add("Staff training");
            incorrectOptions.add("Quality assurance");
        } else if (question.contains("evaluation")) {
            incorrectOptions.add("Financial performance");
            incorrectOptions.add("Administrative efficiency");
            incorrectOptions.add("Staff productivity");
        } else if (question.contains("emergency")) {
            incorrectOptions.add("Routine procedures");
            incorrectOptions.add("Scheduled appointments");
            incorrectOptions.add("Administrative tasks");
        } else if (question.contains("teaching")) {
            incorrectOptions.add("Administrative procedures");
            incorrectOptions.add("Financial management");
            incorrectOptions.add("Staff supervision");
        } else if (question.contains("quality")) {
            incorrectOptions.add("Cost effectiveness");
            incorrectOptions.add("Administrative efficiency");
            incorrectOptions.add("Staff satisfaction");
        } else if (question.contains("collaboration")) {
            incorrectOptions.add("Independent work");
            incorrectOptions.add("Solo decision making");
            incorrectOptions.add("Individual responsibility");
        } else if (question.contains("cultural")) {
            incorrectOptions.add("Financial considerations");
            incorrectOptions.add("Administrative policies");
            incorrectOptions.add("Staff guidelines");
        } else if (question.contains("privacy")) {
            incorrectOptions.add("Public information sharing");
            incorrectOptions.add("Open communication");
            incorrectOptions.add("Transparent processes");
        } else if (question.contains("comfort")) {
            incorrectOptions.add("Staff convenience");
            incorrectOptions.add("Administrative efficiency");
            incorrectOptions.add("Financial savings");
        } else {
            // Generic but still relevant options for other question types
            incorrectOptions.add("This is not a primary concern");
            incorrectOptions.add("This is a secondary priority");
            incorrectOptions.add("This is not relevant to the situation");
        }
        
        // Ensure we have exactly 3 incorrect options
        while (incorrectOptions.size() > 3) {
            incorrectOptions.remove(incorrectOptions.size() - 1);
        }
        while (incorrectOptions.size() < 3) {
            incorrectOptions.add("This option is not correct");
        }
        
        // Shuffle to randomize order
        Collections.shuffle(incorrectOptions);
        return incorrectOptions.toArray(new String[0]);
    }

    /**
     * Try to get more questions with relaxed criteria if we don't have enough
     */
    private void fetchMoreQuestionsWithRelaxedCriteria(FlashcardDeck deck, List<Flashcard> existingFlashcards, FlashcardCallback callback) {
        Log.d(TAG, "Trying to get more questions with relaxed criteria");
        
        // Try to get questions with just career and course (ignore unit)
        db.collection("quiz_questions")
                .whereEqualTo("career", deck.getCareer())
                .whereEqualTo("course", deck.getCourse())
                .limit(15) // Get more questions
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Flashcard> additionalFlashcards = new ArrayList<>();
                    List<String> additionalIds = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Skip if we already have this question
                        if (existingFlashcards.stream().anyMatch(f -> f.getFlashcardId().equals(doc.getId()))) {
                            continue;
                        }
                        
                        Flashcard flashcard = convertQuizQuestionToFlashcard(doc);
                        if (flashcard != null) {
                            additionalFlashcards.add(flashcard);
                            additionalIds.add(flashcard.getFlashcardId());
                        }
                    }
                    
                    // Combine existing and additional flashcards
                    List<Flashcard> allFlashcards = new ArrayList<>(existingFlashcards);
                    allFlashcards.addAll(additionalFlashcards);
                    
                    if (allFlashcards.size() >= 4) {
                        Log.d(TAG, "Successfully got " + allFlashcards.size() + " questions with relaxed criteria");
                        
                        // Update deck with all flashcard IDs
                        List<String> allIds = new ArrayList<>();
                        allIds.addAll(existingFlashcards.stream().map(Flashcard::getFlashcardId).collect(java.util.stream.Collectors.toList()));
                        allIds.addAll(additionalIds);
                        
                        deck.setFlashcardIds(allIds);
                        deck.setTotalFlashcards(allIds.size());
                        
                        // Save the updated deck
                        saveDeck(deck, new DeckCallback() {
                            @Override
                            public void onDeckLoaded(FlashcardDeck savedDeck) {
                                Log.d(TAG, "Deck updated with all flashcard IDs, calling callback with " + allFlashcards.size() + " questions");
                                callback.onFlashcardsLoaded(allFlashcards);
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error saving deck: " + error);
                                callback.onFlashcardsLoaded(allFlashcards);
                            }
                        });
                    } else {
                        Log.w(TAG, "Still only " + allFlashcards.size() + " questions available even with relaxed criteria");
                        callback.onError("Not enough questions available. Need at least 4 questions for " + deck.getCareer() + " - " + deck.getCourse() + " - " + deck.getUnit() + ". Only " + allFlashcards.size() + " found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching additional questions", e);
                    callback.onError("Failed to fetch additional questions: " + e.getMessage());
                });
    }

    /**
     * Save a flashcard deck to Firestore
     */
    private void saveDeck(FlashcardDeck deck, DeckCallback callback) {
        if (deck.getDeckId() == null) {
            callback.onError("Deck ID is null");
            return;
        }
        
        Log.d(TAG, "Saving deck: " + deck.getDeckId());
        
        db.collection("flashcard_decks")
                .document(deck.getDeckId())
                .set(deck)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deck saved successfully");
                    callback.onDeckLoaded(deck);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving deck", e);
                    callback.onError("Failed to save deck: " + e.getMessage());
                });
    }

    /**
     * Fetch flashcards from existing quiz_questions collection
     */
    private void fetchFlashcardsFromQuizQuestions(FlashcardDeck deck, FlashcardCallback callback) {
        Log.d(TAG, "Fetching flashcards from quiz_questions for career: " + deck.getCareer() + ", course: " + deck.getCourse() + ", unit: " + deck.getUnit());
        
        // Query quiz_questions collection for matching career/course/unit
        db.collection("quiz_questions")
                .whereEqualTo("career", deck.getCareer())
                .whereEqualTo("course", deck.getCourse())
                .whereEqualTo("unit", deck.getUnit())
                .limit(10) // Get up to 10 questions to ensure we have at least 5 for study
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Flashcard> flashcards = new ArrayList<>();
                    List<String> flashcardIds = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Convert quiz question to flashcard format
                        Flashcard flashcard = convertQuizQuestionToFlashcard(doc);
                        if (flashcard != null) {
                            flashcards.add(flashcard);
                            flashcardIds.add(flashcard.getFlashcardId());
                            Log.d(TAG, "Converted quiz question to flashcard: " + flashcard.getQuestion());
                        }
                    }
                    
                    if (!flashcards.isEmpty()) {
                        Log.d(TAG, "Successfully converted " + flashcards.size() + " quiz questions to flashcards");
                        
                        // Ensure we have at least 4 questions for study (lowered from 5)
                        if (flashcards.size() < 4) {
                            Log.w(TAG, "Only " + flashcards.size() + " questions available, trying to get more questions");
                            
                            // Try to get more questions by relaxing the criteria
                            fetchMoreQuestionsWithRelaxedCriteria(deck, flashcards, callback);
                            return;
                        }
                        
                        // Update the deck with the flashcard IDs
                        deck.setFlashcardIds(flashcardIds);
                        deck.setTotalFlashcards(flashcardIds.size());
                        
                        // Save the updated deck
                        saveDeck(deck, new DeckCallback() {
                            @Override
                            public void onDeckLoaded(FlashcardDeck savedDeck) {
                                Log.d(TAG, "Deck updated with flashcard IDs, calling callback with " + flashcards.size() + " questions");
                                callback.onFlashcardsLoaded(flashcards);
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error saving deck: " + error);
                                // Still return the flashcards even if deck save fails
                                callback.onFlashcardsLoaded(flashcards);
                            }
                        });
                    } else {
                        Log.w(TAG, "No quiz questions found for the specified criteria");
                        callback.onError("No questions available for " + deck.getCareer() + " - " + deck.getCourse() + " - " + deck.getUnit());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching quiz questions", e);
                    callback.onError("Failed to fetch questions: " + e.getMessage());
                });
    }
    
    /**
     * Convert a quiz question document to a Flashcard object
     */
    private Flashcard convertQuizQuestionToFlashcard(QueryDocumentSnapshot doc) {
        try {
            Flashcard flashcard = new Flashcard();
            
            // Set basic fields
            flashcard.setFlashcardId(doc.getId());
            flashcard.setQuestion(doc.getString("question"));
            flashcard.setAnswer(doc.getString("correctAnswer"));
            flashcard.setRationale(doc.getString("explanation"));
            flashcard.setCareer(doc.getString("career"));
            flashcard.setCourse(doc.getString("course"));
            flashcard.setUnit(doc.getString("unit"));
            flashcard.setSource("Quiz Questions Database");
            flashcard.setDifficulty(doc.getString("difficulty") != null ? doc.getString("difficulty") : "medium");
            
            // Debug: Log the raw options data
            Log.d(TAG, "Raw options data for question: " + doc.getString("question"));
            Log.d(TAG, "Options field exists: " + doc.contains("options"));
            if (doc.contains("options")) {
                Object optionsObj = doc.get("options");
                Log.d(TAG, "Options object type: " + (optionsObj != null ? optionsObj.getClass().getSimpleName() : "null"));
                Log.d(TAG, "Options object: " + optionsObj);
            }
            
            // Convert options array to List<String> - handle different data structures
            List<String> options = new ArrayList<>();
            if (doc.contains("options")) {
                Object optionsObj = doc.get("options");
                
                if (optionsObj instanceof Map) {
                    // Options are stored as "0", "1", "2", "3" keys (your current structure)
                    Map<String, Object> optionsMap = (Map<String, Object>) optionsObj;
                    Log.d(TAG, "Options map keys: " + optionsMap.keySet());
                    
                    // Try to get options by string keys first
                    for (int i = 0; i < 4; i++) {
                        String option = (String) optionsMap.get(String.valueOf(i));
                        if (option != null && !option.trim().isEmpty()) {
                            options.add(option.trim());
                            Log.d(TAG, "Added option " + i + ": " + option);
                        } else {
                            Log.w(TAG, "Option " + i + " is null or empty: " + option);
                        }
                    }
                    
                    // If we still don't have 4 options, try alternative key formats
                    if (options.size() < 4) {
                        Log.d(TAG, "Trying alternative key formats, current options: " + options.size());
                        
                        // Try integer keys
                        for (int i = 0; i < 4; i++) {
                            if (options.size() >= 4) break;
                            String option = (String) optionsMap.get(i);
                            if (option != null && !option.trim().isEmpty() && !options.contains(option.trim())) {
                                options.add(option.trim());
                                Log.d(TAG, "Added option with int key " + i + ": " + option);
                            }
                        }
                    }
                } else if (optionsObj instanceof List) {
                    // Options are stored as a List
                    List<Object> optionsList = (List<Object>) optionsObj;
                    Log.d(TAG, "Options list size: " + optionsList.size());
                    
                    for (int i = 0; i < Math.min(4, optionsList.size()); i++) {
                        Object optionObj = optionsList.get(i);
                        if (optionObj != null) {
                            String option = optionObj.toString().trim();
                            if (!option.isEmpty()) {
                                options.add(option);
                                Log.d(TAG, "Added option " + i + ": " + option);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Unexpected options data type: " + (optionsObj != null ? optionsObj.getClass().getSimpleName() : "null"));
                }
            } else {
                Log.w(TAG, "No options field found in document");
            }
            
            Log.d(TAG, "Final options count: " + options.size());
            if (options.size() == 4) {
                flashcard.setOptions(options);
                Log.d(TAG, "Successfully converted options: " + options);
            } else {
                Log.w(TAG, "Invalid options count: " + options.size() + " for question: " + doc.getString("question"));
                Log.w(TAG, "Available options: " + options);
                
                // If we don't have 4 options, try to create fallback options
                if (options.size() > 0) {
                    Log.d(TAG, "Creating fallback options to reach 4 total");
                    while (options.size() < 4) {
                        options.add("Option " + (options.size() + 1));
                    }
                    flashcard.setOptions(options);
                    Log.d(TAG, "Using fallback options: " + options);
                } else {
                    return null;
                }
            }
            
            return flashcard;
        } catch (Exception e) {
            Log.e(TAG, "Error converting quiz question to flashcard", e);
            return null;
        }
    }

    /**
     * Generate flashcards using Gemini AI for a specific topic
     */
    public void generateFlashcardsWithAI(String career, String course, String unit, int count, FlashcardCallback callback) {
        Log.d(TAG, "Generating " + count + " flashcards with Gemini AI for " + career + " - " + course + " - " + unit);
        
        geminiService.generateFlashcards(career, course, unit, count, new GeminiFlashcardService.FlashcardGenerationCallback() {
            @Override
            public void onFlashcardsGenerated(List<Flashcard> flashcards) {
                Log.d(TAG, "Gemini AI generated " + flashcards.size() + " flashcards successfully");
                
                // Save the generated flashcards to Firestore
                saveGeneratedFlashcards(flashcards, career, course, unit, callback);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Gemini AI generation failed: " + error);
                callback.onError("AI generation failed: " + error);
            }
        });
    }
    
    /**
     * Save AI-generated flashcards to Firestore
     */
    private void saveGeneratedFlashcards(List<Flashcard> flashcards, String career, String course, String unit, FlashcardCallback callback) {
        if (flashcards == null || flashcards.isEmpty()) {
            callback.onError("No flashcards to save");
            return;
        }
        
        Log.d(TAG, "Saving " + flashcards.size() + " AI-generated flashcards to Firestore");
        
        // Create a new deck for these flashcards
        FlashcardDeck deck = new FlashcardDeck(
            career + " - " + course + " - " + unit,
            "AI-generated flashcards for " + unit + " in " + course + ", " + career,
            career, course, unit
        );
        
        // Save deck first
        db.collection("flashcard_decks")
            .add(deck)
            .addOnSuccessListener(documentReference -> {
                deck.setDeckId(documentReference.getId());
                Log.d(TAG, "Deck created with ID: " + documentReference.getId());
                
                // Now save all flashcards
                saveFlashcardsToFirestore(flashcards, deck, callback);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating deck", e);
                callback.onError("Failed to create deck: " + e.getMessage());
            });
    }
    
    /**
     * Save flashcards to Firestore with proper IDs
     */
    private void saveFlashcardsToFirestore(List<Flashcard> flashcards, FlashcardDeck deck, FlashcardCallback callback) {
        List<String> flashcardIds = new ArrayList<>();
        final int[] savedCount = {0};
        
        for (int i = 0; i < flashcards.size(); i++) {
            Flashcard flashcard = flashcards.get(i);
            
            db.collection("flashcards")
                .add(flashcard)
                .addOnSuccessListener(documentReference -> {
                    String flashcardId = documentReference.getId();
                    flashcardIds.add(flashcardId);
                    flashcard.setFlashcardId(flashcardId);
                    
                    savedCount[0]++;
                    Log.d(TAG, "Saved flashcard " + savedCount[0] + "/" + flashcards.size() + " with ID: " + flashcardId);
                    
                    if (savedCount[0] == flashcards.size()) {
                        // All flashcards saved, update deck
                        deck.setFlashcardIds(flashcardIds);
                        deck.setTotalFlashcards(flashcardIds.size());
                        
                        // Update deck in Firestore
                        db.collection("flashcard_decks")
                            .document(deck.getDeckId())
                            .update("flashcardIds", flashcardIds, "totalFlashcards", flashcardIds.size())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Deck updated successfully with " + flashcardIds.size() + " flashcard IDs");
                                callback.onFlashcardsLoaded(flashcards);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating deck", e);
                                // Still return flashcards even if deck update fails
                                callback.onFlashcardsLoaded(flashcards);
                            });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving flashcard", e);
                    callback.onError("Failed to save flashcard: " + e.getMessage());
                });
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (geminiService != null) {
            geminiService.cleanup();
        }
    }
}
