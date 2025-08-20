package com.example.nurse_connect.ui.flashcards;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityFlashcardBinding;
import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardDeck;
import com.example.nurse_connect.models.FlashcardGameMode;
import com.example.nurse_connect.services.FlashcardService;
import com.example.nurse_connect.services.StudyProgressManager;
import com.example.nurse_connect.services.StudyProgressManager.StudyProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flashcard Activity
 * Provides a Quizlet-like interface for studying flashcards
 */
public class FlashcardActivity extends AppCompatActivity {
    
    private ActivityFlashcardBinding binding;
    private FlashcardService flashcardService;
    private FlashcardDeck currentDeck;
    private List<Flashcard> currentFlashcards;
    private int currentCardIndex = 0;
    private boolean isAnswerRevealed = false;
    private int correctAnswers = 0;
    private int totalAnswered = 0;
    private boolean isStudyMode = false;
    
    // Study Progress Manager
    private StudyProgressManager studyProgressManager;
    
    // Sound effects
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;
    
    // Firebase
    private FirebaseFirestore db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardBinding.inflate(getLayoutInflater());
        if (binding == null) {
            Log.e("FlashcardActivity", "Failed to initialize binding");
            finish();
            return;
        }
        setContentView(binding.getRoot());
        
        // Initialize Study Progress Manager
        studyProgressManager = new StudyProgressManager(this);
        
        flashcardService = new FlashcardService();
        db = FirebaseFirestore.getInstance();
        
        // Get deck info from intent
        String deckId = getIntent().getStringExtra("deck_id");
        String career = getIntent().getStringExtra("career");
        String course = getIntent().getStringExtra("course");
        String unit = getIntent().getStringExtra("unit");
        
        // Check if this is Study Mode
        isStudyMode = getIntent().getBooleanExtra("is_study_mode", false);
        FlashcardGameMode gameMode = (FlashcardGameMode) getIntent().getSerializableExtra("selected_game_mode");

        // If flashcards were passed directly (AI-generated path), use them immediately
        java.util.ArrayList<Flashcard> passedFlashcards = getIntent().getParcelableArrayListExtra("flashcards");
        if (passedFlashcards != null && !passedFlashcards.isEmpty()) {
            Log.d("FlashcardActivity", "Using flashcards passed via Intent: " + passedFlashcards.size());
            
            // Debug: Log ALL flashcards to see what was received
            for (int i = 0; i < passedFlashcards.size(); i++) {
                Flashcard card = passedFlashcards.get(i);
                Log.d("FlashcardActivity", "Flashcard " + i + " - Question: " + card.getQuestion());
                Log.d("FlashcardActivity", "Flashcard " + i + " - Source: " + card.getSource());
                if (card.getOptions() != null) {
                    Log.d("FlashcardActivity", "Flashcard " + i + " - Options count: " + card.getOptions().size());
                    for (int j = 0; j < card.getOptions().size(); j++) {
                        Log.d("FlashcardActivity", "Flashcard " + i + " - Option " + j + ": '" + card.getOptions().get(j) + "'");
                    }
                    Log.d("FlashcardActivity", "Flashcard " + i + " - Correct Answer: '" + card.getAnswer() + "'");
                } else {
                    Log.w("FlashcardActivity", "Flashcard " + i + " has no options!");
                }
            }
            
            currentFlashcards = passedFlashcards;
            // Create a lightweight deck placeholder for labeling (deckId may be null)
            currentDeck = new FlashcardDeck(
                    (career != null ? career : "Nursing"),
                    "AI Generated Study Session",
                    (career != null ? career : ""),
                    (course != null ? course : ""),
                    (unit != null ? unit : "")
            );
            if (deckId != null) currentDeck.setDeckId(deckId);
            setupUI();
            initializeSounds();
            showCurrentCard();
            updateProgress();
            return;
        }

        if (deckId != null) {
            // Load existing deck
            loadDeck(deckId);
        } else if (career != null && course != null && unit != null) {
            // Create or get deck for career/course/unit
            flashcardService.getAvailableDecks(career, course, unit, new FlashcardService.DeckCallback() {
                @Override
                public void onDeckLoaded(FlashcardDeck deck) {
                    currentDeck = deck;
                    loadDailyFlashcards();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(FlashcardActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "Invalid parameters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupUI();

        // Initialize sound effects
        initializeSounds();
    }

    /**
     * Initialize sound effects
     */
    private void initializeSounds() {
        try {
            // Try to create sound players, but don't crash if files don't exist
            correctSoundPlayer = MediaPlayer.create(this, R.raw.correct_sound);
            incorrectSoundPlayer = MediaPlayer.create(this, R.raw.incorrect_sound);
            
            // Set up sound players only if they were created successfully
            if (correctSoundPlayer != null) {
                correctSoundPlayer.setLooping(false);
                Log.d("FlashcardActivity", "Correct sound initialized successfully");
            } else {
                Log.w("FlashcardActivity", "Correct sound file not found - sound effects disabled");
            }
            
            if (incorrectSoundPlayer != null) {
                incorrectSoundPlayer.setLooping(false);
                Log.d("FlashcardActivity", "Incorrect sound initialized successfully");
            } else {
                Log.w("FlashcardActivity", "Incorrect sound file not found - sound effects disabled");
            }
        } catch (Exception e) {
            Log.w("FlashcardActivity", "Sound initialization failed - sound effects disabled", e);
            // Don't crash the app, just disable sound effects
            correctSoundPlayer = null;
            incorrectSoundPlayer = null;
        }
    }

    private void setupUI() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "setupUI() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            // Back button
            binding.btnBack.setOnClickListener(v -> finish());
            
            // Results button
            binding.btnSeeResults.setOnClickListener(v -> {
            Log.d("FlashcardActivity", "See Results button clicked");
            completeSession();
        });
            
            // Answer buttons
            binding.btnShowAnswer.setOnClickListener(v -> revealAnswer());
            
            // Initially hide answer-related buttons
            binding.layoutAnswer.setVisibility(View.GONE);
            
            // Setup swipe gestures for navigation
            setupSwipeGestures();
            
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in setupUI", e);
        }
    }
    

    
    private void updateSwipeHint() {
        if (binding != null && binding.textSwipeHint != null) {
            if (currentFlashcards != null && currentFlashcards.size() > 1) {
                // Show swipe hints only when there are multiple cards
                binding.textSwipeHint.setVisibility(View.VISIBLE);
                
                // Customize hint based on current position
                if (currentCardIndex == 0) {
                    binding.textSwipeHint.setText("👈 Swipe left for next card");
                } else if (currentCardIndex == currentFlashcards.size() - 1) {
                    binding.textSwipeHint.setText("👈 Swipe right for previous card");
                } else {
                    binding.textSwipeHint.setText("👈 Swipe left for next • Swipe right for previous 👉");
                }
            } else {
                // Hide hint if only one card
                binding.textSwipeHint.setVisibility(View.GONE);
            }
        }
    }
    
    private void showSwipeFeedback(String message) {
        if (binding != null) {
            // Show a quick toast message for swipe feedback
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    // Track answered cards to prevent re-answering
    private Set<Integer> answeredCards = new HashSet<>();
    
    // Modern swipe animations
    private void animateCardTransition(boolean isNext) {
        if (binding == null) return;
        
        View currentCard = binding.layoutQuestion;
        if (currentCard == null) return;
        
        // Create slide animation - use only available Android animations
        Animation slideOut = AnimationUtils.loadAnimation(this, 
            isNext ? android.R.anim.slide_out_right : android.R.anim.slide_in_left);
        slideOut.setDuration(300);
        
        // Create slide in animation - use only available Android animations
        Animation slideIn = AnimationUtils.loadAnimation(this, 
            isNext ? android.R.anim.slide_in_left : android.R.anim.slide_out_right);
        slideIn.setDuration(300);
        
        // Set animation listener
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                // After slide out, show new card and slide in
                showCurrentCard();
                currentCard.startAnimation(slideIn);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        
        // Start slide out animation
        currentCard.startAnimation(slideOut);
    }
    
    // Enhanced swipe gesture with visual feedback
    private void setupSwipeGestures() {
        // Create a gesture detector for swipe gestures
        View mainContent = binding.scrollView;
        
        // Add touch listener for swipe detection
        mainContent.setOnTouchListener(new View.OnTouchListener() {
            private float startX = 0;
            private float startY = 0;
            private static final float SWIPE_THRESHOLD = 100; // Minimum distance for swipe
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;
                        
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        
                        float deltaX = endX - startX;
                        float deltaY = endY - startY;
                        
                        // Check if it's a horizontal swipe (more horizontal than vertical)
                        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                            if (deltaX > 0) {
                                // Swipe right - go to previous card
                                if (canGoToPreviousCard()) {
                                    showSwipeFeedback("👈 Previous");
                                    animateCardTransition(false);
                                    showPreviousCard();
                                }
                            } else {
                                // Swipe left - go to next card
                                if (canGoToNextCard()) {
                                    showSwipeFeedback("Next 👉");
                                    animateCardTransition(true);
                                    showNextCard();
                                }
                            }
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }
    
    private boolean canGoToPreviousCard() {
        return currentCardIndex > 0;
    }
    
    private boolean canGoToNextCard() {
        return currentFlashcards != null && currentCardIndex < currentFlashcards.size() - 1;
    }
    
    private void loadDeck(String deckId) {
        try {
            // For now, we'll create a sample deck
            // In the future, this would load from Firestore
            currentDeck = new FlashcardDeck("Sample Deck", "Sample description", "CNA", "Basic Skills", "Unit 1");
            currentDeck.setDeckId(deckId);
            loadDailyFlashcards();
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in loadDeck", e);
        }
    }
    
    private void loadDailyFlashcards() {
        if (currentDeck == null) return;

        try {
            Log.d("FlashcardActivity", "Loading daily flashcards for deck: " + currentDeck.getDeckId());

            flashcardService.getDailyFlashcards(currentDeck.getDeckId(), new FlashcardService.FlashcardCallback() {
                @Override
                public void onFlashcardsLoaded(List<Flashcard> flashcards) {
                    try {
                        Log.d("FlashcardActivity", "Loaded " + flashcards.size() + " flashcards");
                        // Log the first few flashcard IDs to verify they're set
                        for (int i = 0; i < Math.min(3, flashcards.size()); i++) {
                            Flashcard card = flashcards.get(i);
                            Log.d("FlashcardActivity", "Card " + i + " ID: '" + card.getFlashcardId() + "'");
                            Log.d("FlashcardActivity", "Card " + i + " Options: " + (card.getOptions() != null ? card.getOptions().size() : "null"));
                            if (card.getOptions() != null) {
                                for (int j = 0; j < card.getOptions().size(); j++) {
                                    Log.d("FlashcardActivity", "  Option " + j + ": " + card.getOptions().get(j));
                                }
                            }
                        }
                        currentFlashcards = flashcards;
                        if (!flashcards.isEmpty()) {
                            // Log the first few flashcard IDs to verify they're set
                            for (int i = 0; i < Math.min(3, flashcards.size()); i++) {
                                Flashcard card = flashcards.get(i);
                                Log.d("FlashcardActivity", "Card " + i + " ID: '" + card.getFlashcardId() + "'");
                            }
                            
                            showCurrentCard();
                            updateProgress();
                        } else {
                            Toast.makeText(FlashcardActivity.this, "No flashcards available", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e("FlashcardActivity", "Error in onFlashcardsLoaded callback", e);
                    }
                }

                @Override
                public void onError(String error) {
                    try {
                        Log.e("FlashcardActivity", "Error loading flashcards: " + error);
                        Toast.makeText(FlashcardActivity.this, error, Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e) {
                        Log.e("FlashcardActivity", "Error in onError callback", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in loadDailyFlashcards", e);
        }
    }
    
    private void showCurrentCard() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "showCurrentCard() called but binding is null, activity may be destroyed");
            return;
        }
        
        if (currentFlashcards == null || currentFlashcards.isEmpty() || 
            currentCardIndex < 0 || currentCardIndex >= currentFlashcards.size()) {
            return;
        }
        
        try {
            Flashcard currentCard = currentFlashcards.get(currentCardIndex);
            
            Log.d("FlashcardActivity", "Showing card " + currentCardIndex + " with ID: '" + currentCard.getFlashcardId() + "'");
            
            // Show question with better formatting
            binding.textQuestion.setText(currentCard.getQuestion());
            binding.textQuestion.setTextColor(getResources().getColor(R.color.text_primary));
            binding.textQuestion.setTypeface(null, Typeface.NORMAL);
            
            // Check if this is a fallback question and change background accordingly
            if (currentCard.getSource() != null && currentCard.getSource().toLowerCase().contains("fallback")) {
                Log.d("FlashcardActivity", "Fallback question detected: " + currentCard.getSource());
                binding.layoutQuestion.setBackgroundResource(R.drawable.fallback_question_background);
                
                // Show fallback indicator in progress section
                if (binding.textFallbackIndicator != null) {
                    binding.textFallbackIndicator.setVisibility(View.VISIBLE);
                }
                
                // Add a small indicator text to show it's a fallback question
                if (binding.textQuestion.getText() != null) {
                    String questionText = binding.textQuestion.getText().toString();
                    String fallbackIndicator = "📚 Fallback Question\n\n" + questionText;
                    binding.textQuestion.setText(fallbackIndicator);
                }
            } else {
                // Use normal background for AI-generated questions
                binding.layoutQuestion.setBackgroundResource(R.drawable.flashcard_question_background);
                
                // Hide fallback indicator in progress section
                if (binding.textFallbackIndicator != null) {
                    binding.textFallbackIndicator.setVisibility(View.GONE);
                }
                
                // Remove any fallback indicator if present
                if (binding.textQuestion.getText() != null) {
                    String questionText = binding.textQuestion.getText().toString();
                    if (questionText.startsWith("📚 Fallback Question\n\n")) {
                        questionText = questionText.replace("📚 Fallback Question\n\n", "");
                        binding.textQuestion.setText(questionText);
                    }
                }
            }
            
            // Show multiple choice options if available
            if (currentCard.getOptions() != null && !currentCard.getOptions().isEmpty()) {
                Log.d("FlashcardActivity", "Showing options for card " + currentCardIndex + ": " + currentCard.getOptions().size() + " options");
                
                // Debug: Log each option to see what's actually stored
                for (int i = 0; i < currentCard.getOptions().size(); i++) {
                    Log.d("FlashcardActivity", "Option " + i + ": '" + currentCard.getOptions().get(i) + "'");
                }
                
                binding.layoutOptions.setVisibility(View.VISIBLE);
                
                // Set option text with proper formatting
                binding.btnOption1.setText("A. " + currentCard.getOptions().get(0));
                binding.btnOption2.setText("B. " + currentCard.getOptions().get(1));
                binding.btnOption3.setText("C. " + currentCard.getOptions().get(2));
                binding.btnOption4.setText("D. " + currentCard.getOptions().get(3));
                
                // Debug: Log what's actually being set on the buttons
                Log.d("FlashcardActivity", "Button 1 text: '" + binding.btnOption1.getText() + "'");
                Log.d("FlashcardActivity", "Button 2 text: '" + binding.btnOption2.getText() + "'");
                Log.d("FlashcardActivity", "Button 3 text: '" + binding.btnOption3.getText() + "'");
                Log.d("FlashcardActivity", "Button 4 text: '" + binding.btnOption4.getText() + "'");
                
                // Reset button states and enable all options
                resetOptionButtons();
                
                // Set click listeners for options
                binding.btnOption1.setOnClickListener(v -> selectOption(0));
                binding.btnOption2.setOnClickListener(v -> selectOption(1));
                binding.btnOption3.setOnClickListener(v -> selectOption(2));
                binding.btnOption4.setOnClickListener(v -> selectOption(3));
            } else {
                Log.e("FlashcardActivity", "No options found for card " + currentCardIndex + ". Options: " + currentCard.getOptions());
                binding.layoutOptions.setVisibility(View.GONE);
            }
            
            // Check if this card has already been answered
            boolean isCardAnswered = answeredCards.contains(currentCardIndex);
            
            if (isCardAnswered) {
                // Show the answer if card was already answered
                isAnswerRevealed = true;
                binding.layoutAnswer.setVisibility(View.VISIBLE);
                binding.btnShowAnswer.setVisibility(View.GONE);
                
                // Disable option buttons since card is already answered
                binding.btnOption1.setEnabled(false);
                binding.btnOption2.setEnabled(false);
                binding.btnOption3.setEnabled(false);
                binding.btnOption4.setEnabled(false);
            } else {
                // Reset answer state and hide answer section for new cards
                isAnswerRevealed = false;
                binding.layoutAnswer.setVisibility(View.GONE);
                binding.btnShowAnswer.setVisibility(View.VISIBLE);
                
                // Enable option buttons for new cards
                binding.btnOption1.setEnabled(true);
                binding.btnOption2.setEnabled(true);
                binding.btnOption3.setEnabled(true);
                binding.btnOption4.setEnabled(true);
            }
            
            // Hide feedback
            binding.layoutFeedback.setVisibility(View.GONE);
            
            // Update navigation buttons
            updateNavigationButtons();
            
            // Update progress
            updateProgress();
            
            // Update swipe hint
            updateSwipeHint();
            
            // Special handling for the last card - ensure See Results button is visible if answer was revealed
            if (currentCardIndex == currentFlashcards.size() - 1 && isAnswerRevealed) {
                Log.d("FlashcardActivity", "Last card with revealed answer - ensuring See Results button is visible");
                binding.btnSeeResults.setVisibility(View.VISIBLE);
            }
            
            // Load user memory for this card
            loadUserMemoryForCard(currentCard.getFlashcardId());
            
            // Check if any cards in the set are fallback questions and show general indicator
            checkForFallbackQuestionsInSet();
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in showCurrentCard", e);
        }
    }
    
    private void resetOptionButtons() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "resetOptionButtons() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            // Reset all option buttons to normal state
            binding.btnOption1.setEnabled(true);
            binding.btnOption2.setEnabled(true);
            binding.btnOption3.setEnabled(true);
            binding.btnOption4.setEnabled(true);
            
            binding.btnOption1.setBackgroundResource(R.drawable.option_button_normal);
            binding.btnOption2.setBackgroundResource(R.drawable.option_button_normal);
            binding.btnOption3.setBackgroundResource(R.drawable.option_button_normal);
            binding.btnOption4.setBackgroundResource(R.drawable.option_button_normal);
            
            binding.btnOption1.setTextColor(getResources().getColor(R.color.text_primary));
            binding.btnOption2.setTextColor(getResources().getColor(R.color.text_primary));
            binding.btnOption3.setTextColor(getResources().getColor(R.color.text_primary));
            binding.btnOption4.setTextColor(getResources().getColor(R.color.text_primary));
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in resetOptionButtons", e);
        }
    }
    
    private void checkForFallbackQuestionsInSet() {
        if (binding == null || currentFlashcards == null) return;
        
        try {
            boolean hasFallbackQuestions = false;
            int fallbackCount = 0;
            
            for (Flashcard card : currentFlashcards) {
                if (card.getSource() != null && card.getSource().toLowerCase().contains("fallback")) {
                    hasFallbackQuestions = true;
                    fallbackCount++;
                }
            }
            
            if (hasFallbackQuestions) {
                Log.d("FlashcardActivity", "Flashcard set contains " + fallbackCount + " fallback questions");
                if (binding.textFallbackIndicator != null) {
                    binding.textFallbackIndicator.setText("📚 " + fallbackCount + " Fallback Questions Used");
                    binding.textFallbackIndicator.setVisibility(View.VISIBLE);
                }
            } else {
                Log.d("FlashcardActivity", "Flashcard set contains only AI-generated questions");
                if (binding.textFallbackIndicator != null) {
                    binding.textFallbackIndicator.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error checking for fallback questions", e);
        }
    }
    
    private void selectOption(int optionIndex) {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "selectOption() called but binding is null, activity may be destroyed");
            return;
        }
        
        Log.d("FlashcardActivity", "selectOption() called for option " + optionIndex + " on card " + currentCardIndex + " of " + (currentFlashcards != null ? currentFlashcards.size() : "null"));
        
        // Check if this card has already been answered
        if (answeredCards.contains(currentCardIndex)) {
            Log.d("FlashcardActivity", "Card " + currentCardIndex + " has already been answered, ignoring selection");
            return;
        }
        
        if (currentFlashcards == null || currentCardIndex < 0 || currentCardIndex >= currentFlashcards.size()) {
            Log.e("FlashcardActivity", "Invalid card index or flashcards null");
            return;
        }
        
        try {
            Flashcard currentCard = currentFlashcards.get(currentCardIndex);
            
            // Safety check: ensure the flashcard has a valid ID
            if (currentCard.getFlashcardId() == null || currentCard.getFlashcardId().trim().isEmpty()) {
                Log.e("FlashcardActivity", "Invalid flashcard ID in selectOption: '" + currentCard.getFlashcardId() + "'");
                Toast.makeText(this, "Error: Invalid flashcard data", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String selectedAnswer = currentCard.getOptions().get(optionIndex);
            boolean isCorrect = selectedAnswer.equals(currentCard.getAnswer());
            
            Log.d("FlashcardActivity", "Option selected: " + selectedAnswer + ", Correct answer: " + currentCard.getAnswer() + ", IsCorrect: " + isCorrect);
            
            // Mark this card as answered to prevent re-answering
            answeredCards.add(currentCardIndex);
            
            // Disable all option buttons to prevent multiple selections
            disableAllOptionButtons();
            
            // Play sound effect
            playAnswerSound(isCorrect);
            
            // Highlight correct/incorrect answer
            highlightAnswer(optionIndex, isCorrect);
            
            // Show feedback message
            showAnswerFeedback(isCorrect);
            
            // Flip card to reveal answer after a delay
            new android.os.Handler().postDelayed(() -> {
                if (binding != null) { // Check if activity is still alive
                    flipCardToAnswer();
                }
            }, 1500);
            
            // Record answer after card flip
            Log.d("FlashcardActivity", "Scheduling recordAnswer() in 2.5 seconds...");
            new android.os.Handler().postDelayed(() -> {
                if (binding != null) { // Check if activity is still alive
                    Log.d("FlashcardActivity", "Calling recordAnswer() now...");
                    recordAnswer(isCorrect);
                }
            }, 2500);
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in selectOption", e);
        }
    }
    
    private void disableAllOptionButtons() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "disableAllOptionButtons() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            binding.btnOption1.setEnabled(false);
            binding.btnOption2.setEnabled(false);
            binding.btnOption3.setEnabled(false);
            binding.btnOption4.setEnabled(false);
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in disableAllOptionButtons", e);
        }
    }

    private void playAnswerSound(boolean isCorrect) {
        if (isCorrect) {
            if (correctSoundPlayer != null && !correctSoundPlayer.isPlaying()) {
                correctSoundPlayer.start();
            }
        } else {
            if (incorrectSoundPlayer != null && !incorrectSoundPlayer.isPlaying()) {
                incorrectSoundPlayer.start();
            }
        }
    }

    private void highlightAnswer(int selectedOption, boolean isCorrect) {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "highlightAnswer() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            // Reset all button backgrounds
            binding.btnOption1.setBackgroundResource(R.drawable.option_button_normal);
            binding.btnOption2.setBackgroundResource(R.drawable.option_button_normal);
            binding.btnOption3.setBackgroundResource(R.drawable.option_button_normal);
            binding.btnOption4.setBackgroundResource(R.drawable.option_button_normal);
            
            // Highlight selected option
            View selectedButton = getOptionButton(selectedOption);
            if (selectedButton != null) {
                if (isCorrect) {
                    selectedButton.setBackgroundResource(R.drawable.option_button_correct);
                } else {
                    selectedButton.setBackgroundResource(R.drawable.option_button_incorrect);
                    // Also highlight correct answer
                    int correctIndex = getCorrectAnswerIndex();
                    if (correctIndex >= 0) {
                        View correctButton = getOptionButton(correctIndex);
                        if (correctButton != null) {
                            correctButton.setBackgroundResource(R.drawable.option_button_correct);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in highlightAnswer", e);
        }
    }
    
    private View getOptionButton(int index) {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "getOptionButton() called but binding is null, activity may be destroyed");
            return null;
        }
        
        try {
            switch (index) {
                case 0: return binding.btnOption1;
                case 1: return binding.btnOption2;
                case 2: return binding.btnOption3;
                case 3: return binding.btnOption4;
                default: return binding.btnOption1;
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in getOptionButton", e);
            return null;
        }
    }
    
    private int getCorrectAnswerIndex() {
        if (currentFlashcards == null || currentCardIndex < 0 || currentCardIndex >= currentFlashcards.size()) {
            return -1;
        }
        
        Flashcard currentCard = currentFlashcards.get(currentCardIndex);
        if (currentCard.getOptions() == null) return -1;
        
        for (int i = 0; i < currentCard.getOptions().size(); i++) {
            if (currentCard.getOptions().get(i).equals(currentCard.getAnswer())) {
                return i;
            }
        }
        return -1;
    }
    
    private void revealAnswer() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "revealAnswer() called but binding is null, activity may be destroyed");
            return;
        }
        
        if (currentFlashcards == null || currentCardIndex < 0 || currentCardIndex >= currentFlashcards.size()) {
            return;
        }
        
        try {
            Flashcard currentCard = currentFlashcards.get(currentCardIndex);
            
            // Show answer
            binding.textAnswer.setText(currentCard.getAnswer());
            binding.textRationale.setText(currentCard.getRationale());
            
            // Animate answer reveal
            Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
            binding.layoutAnswer.startAnimation(slideUp);
            binding.layoutAnswer.setVisibility(View.VISIBLE);
            
            // Hide the show answer button since answer is now revealed
            binding.btnShowAnswer.setVisibility(View.GONE);
            
            isAnswerRevealed = true;
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in revealAnswer", e);
        }
    }

    private void flipCardToAnswer() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "flipCardToAnswer() called but binding is null, activity may be destroyed");
            return;
        }
        
        if (currentFlashcards == null || currentCardIndex < 0 || currentCardIndex >= currentFlashcards.size()) {
            return;
        }

        Flashcard currentCard = currentFlashcards.get(currentCardIndex);

        try {
            // Show answer with enhanced animation
            binding.textAnswer.setText(currentCard.getAnswer());
            binding.textRationale.setText(currentCard.getRationale());
            
            // Create smooth flip animation
            Animation flipAnimation = AnimationUtils.loadAnimation(this, R.anim.card_flip);
            binding.layoutAnswer.startAnimation(flipAnimation);
            binding.layoutAnswer.setVisibility(View.VISIBLE);
            
            // Hide the show answer button since answer is now revealed
            binding.btnShowAnswer.setVisibility(View.GONE);
            

            isAnswerRevealed = true;
            
            // Auto-scroll to show the answer and rationale
            new Handler().postDelayed(() -> {
                if (binding != null) {
                    // Scroll to the answer section using the ScrollView
                    binding.layoutAnswer.requestFocus();
                    binding.layoutAnswer.post(() -> {
                        // Find the ScrollView in the layout and scroll to the answer
                        View scrollView = binding.getRoot().findViewById(R.id.scroll_view);
                        if (scrollView instanceof android.widget.ScrollView) {
                            android.widget.ScrollView sv = (android.widget.ScrollView) scrollView;
                            sv.smoothScrollTo(0, binding.layoutAnswer.getTop());
                        }
                    });
                }
            }, 500); // Small delay to ensure animation completes
            
            // Log user interaction for memory
            logCardInteraction(currentCard.getFlashcardId(), "answer_revealed");
            
            // If this is the last card and answer is revealed, show "See Results" button
            if (currentCardIndex == currentFlashcards.size() - 1) {
                Log.d("FlashcardActivity", "Last card answered, showing See Results button");
                binding.btnSeeResults.setVisibility(View.VISIBLE);
                
                // Also show the swipe hint for the last card
                if (binding.textSwipeHint != null) {
                    binding.textSwipeHint.setText("👈 Swipe right for previous card • Click See Results to finish");
                }
                
                // Force the button to be visible and log it
                Log.d("FlashcardActivity", "See Results button visibility set to VISIBLE");
                Log.d("FlashcardActivity", "Button current visibility: " + binding.btnSeeResults.getVisibility());
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in flipCardToAnswer", e);
        }
    }
    
    private void loadUserMemoryForCard(String flashcardId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
        if (currentUserId != null && flashcardId != null && !flashcardId.trim().isEmpty()) {
            db.collection("users").document(currentUserId)
                .collection("flashcard_memory").document(flashcardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Show user's previous performance on this card
                        showUserMemoryIndicator(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> Log.d("FlashcardActivity", "No previous memory for card: " + flashcardId));
        } else {
            Log.d("FlashcardActivity", "Skipping user memory load - invalid flashcard ID: '" + flashcardId + "'");
        }
    }
    
    private void showUserMemoryIndicator(DocumentSnapshot memoryDoc) {
        // Show visual indicator of user's previous performance
        // This could be a small icon or color change
        Log.d("FlashcardActivity", "User has previous memory for this card");
    }
    
    private void logCardInteraction(String flashcardId, String interaction) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
        if (currentUserId != null && flashcardId != null && !flashcardId.trim().isEmpty()) {
            // Log the interaction for AI learning
            db.collection("users").document(currentUserId)
                .collection("flashcard_memory").document(flashcardId)
                .update(interaction + "_count", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d("FlashcardActivity", "Logged interaction: " + interaction))
                .addOnFailureListener(e -> {
                    // Create document if it doesn't exist
                    db.collection("users").document(currentUserId)
                        .collection("flashcard_memory").document(flashcardId)
                        .set(new java.util.HashMap<String, Object>() {{
                            put(interaction + "_count", 1);
                            put("last_interaction", FieldValue.serverTimestamp());
                            put("flashcard_id", flashcardId);
                        }});
                });
        } else {
            Log.d("FlashcardActivity", "Skipping interaction logging - invalid flashcard ID: '" + flashcardId + "'");
        }
    }

    private void showAnswerFeedback(boolean isCorrect) {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "showAnswerFeedback() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            if (isCorrect) {
                binding.textFeedback.setText("🎉 Correct! +10 coins earned!");
                binding.textFeedback.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                binding.textFeedback.setText("💪 Keep going! Learning takes time.");
                binding.textFeedback.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
            binding.layoutFeedback.setVisibility(View.VISIBLE);

            // Hide feedback after delay
            new android.os.Handler().postDelayed(() -> {
                if (binding != null) {
                    binding.layoutFeedback.setVisibility(View.GONE);
                }
            }, 2000);
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in showAnswerFeedback", e);
        }
    }
    
    private void recordAnswer(boolean isCorrect) {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "recordAnswer() called but binding is null, activity may be destroyed");
            return;
        }
        
        Log.d("FlashcardActivity", "recordAnswer() called with isCorrect: " + isCorrect + " for card index: " + currentCardIndex);
        
        if (currentFlashcards == null || currentCardIndex < 0 || currentCardIndex >= currentFlashcards.size()) {
            Log.e("FlashcardActivity", "recordAnswer() - Invalid state, returning early");
            return;
        }
        
        try {
            Flashcard currentCard = currentFlashcards.get(currentCardIndex);
            
            Log.d("FlashcardActivity", "Recording answer for card " + currentCardIndex + ", flashcardId: '" + currentCard.getFlashcardId() + "', isCorrect: " + isCorrect);
            
            // Safety check: ensure the flashcard has a valid ID
            if (currentCard.getFlashcardId() == null || currentCard.getFlashcardId().trim().isEmpty()) {
                Log.e("FlashcardActivity", "Invalid flashcard ID: '" + currentCard.getFlashcardId() + "'");
                Toast.makeText(this, "Error: Invalid flashcard data", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Record answer in service
            flashcardService.recordAnswer(currentCard.getFlashcardId(), isCorrect, new FlashcardService.ProgressCallback() {
                @Override
                public void onProgressUpdated(boolean success) {
                    if (success) {
                        // Update local progress
                        if (isCorrect) {
                            correctAnswers++;
                            // Only award points for correct answers
                            // No points for incorrect answers
                        }
                        totalAnswered++;
                        
                        // Update progress display
                        updateProgress();
                        
                        // If this is Study Mode, track additional progress
                        if (isStudyMode) {
                            trackStudyModeProgress(isCorrect);
                        }
                        
                        // Log answer for AI memory
                        logCardInteraction(currentCard.getFlashcardId(), isCorrect ? "correct_answer" : "incorrect_answer");
                        
                        // Check if this was the last card and show "See Results" button
                        if (currentCardIndex == currentFlashcards.size() - 1) {
                            Log.d("FlashcardActivity", "Last card answered! Current index: " + currentCardIndex + ", Total cards: " + currentFlashcards.size());
                            // This was the last card, show "See Results" button instead of next button
                            // But only show it after the answer is revealed
                            runOnUiThread(() -> {
                                if (binding != null) { // Check if activity is still alive
                                    Log.d("FlashcardActivity", "Showing 'See Results' button for last question...");
                                    // Always show See Results button for the last card when answered
                                    binding.btnSeeResults.setVisibility(View.VISIBLE);
                                    
                                    // Force the button to be visible and log it
                                    Log.d("FlashcardActivity", "See Results button visibility set to VISIBLE in recordAnswer");
                                    Log.d("FlashcardActivity", "Button current visibility: " + binding.btnSeeResults.getVisibility());
                                    
                                    // Update swipe hint for last card
                                    if (binding.textSwipeHint != null) {
                                        binding.textSwipeHint.setText("👈 Swipe right for previous card • Click See Results to finish");
                                    }
                                }
                            });
                        } else {
                            Log.d("FlashcardActivity", "Not the last card. Current index: " + currentCardIndex + ", Total cards: " + currentFlashcards.size());
                        }
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e("FlashcardActivity", "Error recording answer: " + error);
                    if (binding != null) { // Check if activity is still alive
                        Toast.makeText(FlashcardActivity.this, "Error recording answer: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in recordAnswer", e);
        }
    }
    
    private void showPreviousCard() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "showPreviousCard() called but binding is null, activity may be destroyed");
            return;
        }
        
        if (currentCardIndex > 0) {
            currentCardIndex--;
            showCurrentCard();
        }
    }
    
    private void showNextCard() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "showNextCard() called but binding is null, activity may be destroyed");
            return;
        }
        
        Log.d("FlashcardActivity", "showNextCard() called. Current index: " + currentCardIndex + ", Total cards: " + (currentFlashcards != null ? currentFlashcards.size() : "null"));
        if (currentFlashcards != null && currentCardIndex < currentFlashcards.size() - 1) {
            currentCardIndex++;
            showCurrentCard();
        } else {
            Log.d("FlashcardActivity", "At last card, cannot go next...");
            // Don't auto-complete, let user answer the last question
            Toast.makeText(this, "This is the last question", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetCardState() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "resetCardState() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            // Reset the card to question state
            if (currentFlashcards != null && currentCardIndex >= 0 && currentCardIndex < currentFlashcards.size()) {
                Flashcard currentCard = currentFlashcards.get(currentCardIndex);
                
                // Reset question text
                binding.textQuestion.setText(currentCard.getQuestion());
                binding.textQuestion.setTextColor(getResources().getColor(R.color.text_primary));
                binding.textQuestion.setTypeface(null, Typeface.NORMAL);
                
                // Show options again
                binding.layoutOptions.setVisibility(View.VISIBLE);
                
                // Hide answer section
                binding.layoutAnswer.setVisibility(View.GONE);
                
                // Reset answer state
                isAnswerRevealed = false;
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in resetCardState", e);
        }
    }
    
    private void updateNavigationButtons() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "updateNavigationButtons() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            boolean isLastCard = currentFlashcards != null && currentCardIndex == currentFlashcards.size() - 1;
            
            Log.d("FlashcardActivity", "updateNavigationButtons() - Is Last Card: " + isLastCard + " (Index: " + currentCardIndex + ", Total: " + (currentFlashcards != null ? currentFlashcards.size() : "null") + ")");
            
            // Only handle the See Results button visibility
            // The button will be shown when the answer is revealed on the last card
            if (isLastCard) {
                // Don't hide the button here - let the answer reveal logic handle it
                Log.d("FlashcardActivity", "Last card detected - See Results button will be shown when answer is revealed");
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in updateNavigationButtons", e);
        }
    }
    
    private void updateProgress() {
        // Check if binding is still valid
        if (binding == null) {
            Log.w("FlashcardActivity", "updateProgress() called but binding is null, activity may be destroyed");
            return;
        }
        
        try {
            if (currentFlashcards != null) {
                binding.textProgress.setText(String.format("Card %d of %d", currentCardIndex + 1, currentFlashcards.size()));
                binding.progressBar.setMax(currentFlashcards.size());
                binding.progressBar.setProgress(currentCardIndex + 1);
            }
            
            if (totalAnswered > 0) {
                double accuracy = (double) correctAnswers / totalAnswered * 100;
                binding.textAccuracy.setText(String.format("Accuracy: %.1f%%", accuracy));
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in updateProgress", e);
        }
    }
    
    private void completeSession() {
        // Record study progress
        if (studyProgressManager != null && currentFlashcards != null) {
            int totalQuestions = currentFlashcards.size();
            studyProgressManager.recordStudySession(totalQuestions);
            Log.d("FlashcardActivity", "Study progress recorded: " + totalQuestions + " questions completed");
        }
        
        // Calculate final results
        int totalAnswered = this.totalAnswered;
        int correctAnswers = this.correctAnswers;
        double accuracy = totalAnswered > 0 ? (double) correctAnswers / totalAnswered * 100 : 0;
        
        Log.d("FlashcardActivity", "Session completed! Total: " + totalAnswered + ", Correct: " + correctAnswers + ", Accuracy: " + accuracy + "%");
        
        // Always show results activity, but pass study mode flag
        Intent intent = new Intent(this, FlashcardResultsActivity.class);
        intent.putExtra("total_answered", totalAnswered);
        intent.putExtra("correct_answers", correctAnswers);
        intent.putExtra("is_study_mode", isStudyMode);
        intent.putExtra("questions_completed", currentFlashcards != null ? currentFlashcards.size() : 0);
        intent.putExtra("accuracy", accuracy);
        
        // Log the intent data being sent
        Log.d("FlashcardActivity", "Creating intent for FlashcardResultsActivity with data:");
        Log.d("FlashcardActivity", "  - Total answered: " + totalAnswered);
        Log.d("FlashcardActivity", "  - Correct answers: " + correctAnswers);
        Log.d("FlashcardActivity", "  - Study mode: " + isStudyMode);
        Log.d("FlashcardActivity", "  - Questions completed: " + (currentFlashcards != null ? currentFlashcards.size() : 0));
        Log.d("FlashcardActivity", "  - Accuracy: " + accuracy);
        
        // Add flashcard data for review
        if (currentFlashcards != null) {
            intent.putExtra("flashcards", new ArrayList<>(currentFlashcards));
        }
        
        // Add deck information if available
        if (getIntent().hasExtra("career")) {
            intent.putExtra("career", getIntent().getStringExtra("career"));
            intent.putExtra("course", getIntent().getStringExtra("course"));
            intent.putExtra("unit", getIntent().getStringExtra("unit"));
        }
        
        // Add deck ID if available
        if (getIntent().hasExtra("deck_id")) {
            intent.putExtra("deck_id", getIntent().getStringExtra("deck_id"));
        }
        
        // Start the results activity
        Log.d("FlashcardActivity", "Starting FlashcardResultsActivity...");
        startActivity(intent);
        Log.d("FlashcardActivity", "FlashcardResultsActivity started successfully");
        
        // Finish this activity to prevent going back
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up MediaPlayer resources
        if (correctSoundPlayer != null) {
            correctSoundPlayer.release();
            correctSoundPlayer = null;
        }
        if (incorrectSoundPlayer != null) {
            incorrectSoundPlayer.release();
            incorrectSoundPlayer = null;
        }
        
        if (binding != null) {
            binding = null;
        }
    }
    
    /**
     * Track Study Mode specific progress for AI recommendations
     */
    private void trackStudyModeProgress(boolean isCorrect) {
        if (!isStudyMode) return;
        
        try {
            // Update study statistics in Firestore
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
            if (currentUserId != null) {
                // Update study stats for AI recommendations
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("study_stats")
                    .document("flashcards")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Document exists, update it
                            documentSnapshot.getReference().update(
                                "totalCardsStudied", com.google.firebase.firestore.FieldValue.increment(1),
                                "totalScore", com.google.firebase.firestore.FieldValue.increment(isCorrect ? 1 : 0),
                                "lastStudied", com.google.firebase.Timestamp.now(),
                                "updatedAt", com.google.firebase.Timestamp.now()
                            ).addOnSuccessListener(aVoid -> {
                                Log.d("FlashcardActivity", "Study mode progress updated successfully");
                            }).addOnFailureListener(e -> {
                                Log.e("FlashcardActivity", "Failed to update study mode progress", e);
                            });
                        } else {
                            // Document doesn't exist, create it
                            java.util.Map<String, Object> initialStats = new java.util.HashMap<>();
                            initialStats.put("totalCardsStudied", 1);
                            initialStats.put("totalScore", isCorrect ? 1 : 0);
                            initialStats.put("lastStudied", com.google.firebase.Timestamp.now());
                            initialStats.put("createdAt", com.google.firebase.Timestamp.now());
                            initialStats.put("updatedAt", com.google.firebase.Timestamp.now());
                            
                            documentSnapshot.getReference().set(initialStats)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FlashcardActivity", "Study stats document created and updated");
                                }).addOnFailureListener(e -> {
                                    Log.e("FlashcardActivity", "Failed to create study stats document", e);
                                });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FlashcardActivity", "Failed to check study stats document", e);
                    });
            }
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error in trackStudyModeProgress", e);
        }
    }
}
