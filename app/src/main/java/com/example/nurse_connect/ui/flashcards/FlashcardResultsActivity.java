package com.example.nurse_connect.ui.flashcards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityFlashcardResultsBinding;
import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardDeck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Flashcard Results Activity
 * Shows completion results and allows saving flashcard sessions
 */
public class FlashcardResultsActivity extends AppCompatActivity {
    
    private ActivityFlashcardResultsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    // Data from previous activity
    private int totalAnswered;
    private int correctAnswers;
    private boolean isStudyMode;
    private String deckId;
    private List<Flashcard> flashcards;
    private FlashcardDeck deck;
    private double accuracy;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("FlashcardResultsActivity", "onCreate started");
        
        try {
            binding = ActivityFlashcardResultsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Log.d("FlashcardResultsActivity", "Layout inflated and set successfully");
            
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            Log.d("FlashcardResultsActivity", "Firebase initialized");
            
            // Get data from intent
            getIntentData();
            
            // Setup UI
            setupUI();
            Log.d("FlashcardResultsActivity", "UI setup completed");
            
            // Show results
            displayResults();
            Log.d("FlashcardResultsActivity", "Results displayed successfully");
            
        } catch (Exception e) {
            Log.e("FlashcardResultsActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error initializing results: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        
        // Test logging to ensure activity is working
        Log.d("FlashcardResultsActivity", "onCreate completed successfully");
    }
    
    private void getIntentData() {
        totalAnswered = getIntent().getIntExtra("total_answered", 0);
        correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        isStudyMode = getIntent().getBooleanExtra("is_study_mode", false);
        deckId = getIntent().getStringExtra("deck_id");
        
        // Log the received data for debugging
        Log.d("FlashcardResultsActivity", "Received intent data - Total: " + totalAnswered + 
              ", Correct: " + correctAnswers + ", Study Mode: " + isStudyMode + ", Deck ID: " + deckId);
        
        // Get flashcards if available
        ArrayList<Flashcard> passedFlashcards = getIntent().getParcelableArrayListExtra("flashcards");
        if (passedFlashcards != null) {
            flashcards = passedFlashcards;
            Log.d("FlashcardResultsActivity", "Received " + flashcards.size() + " flashcards");
        } else {
            Log.d("FlashcardResultsActivity", "No flashcards received in intent");
        }
        
        // Get deck info if available
        String career = getIntent().getStringExtra("career");
        String course = getIntent().getStringExtra("course");
        String unit = getIntent().getStringExtra("unit");
        if (career != null && course != null && unit != null) {
            deck = new FlashcardDeck(career + " - " + course, "Study Session", career, course, unit);
            if (deckId != null) deck.setDeckId(deckId);
            Log.d("FlashcardResultsActivity", "Deck info: " + career + " - " + course + " - " + unit);
        } else {
            Log.d("FlashcardResultsActivity", "No deck info received in intent");
        }
        
        // Calculate accuracy
        accuracy = totalAnswered > 0 ? (double) correctAnswers / totalAnswered * 100 : 0;
        Log.d("FlashcardResultsActivity", "Calculated accuracy: " + accuracy + "%");
    }
    
    private void setupUI() {
        try {
            Log.d("FlashcardResultsActivity", "Setting up UI...");
            
            // Check if binding is valid
            if (binding == null) {
                Log.e("FlashcardResultsActivity", "Binding is null in setupUI");
                return;
            }
            
            // Setup toolbar
            if (binding.toolbar != null) {
                setSupportActionBar(binding.toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                }
                
                // Toolbar back button
                binding.toolbar.setNavigationOnClickListener(v -> finish());
                Log.d("FlashcardResultsActivity", "Toolbar setup completed");
            } else {
                Log.w("FlashcardResultsActivity", "Toolbar binding is null");
            }
            
            // Save button
            if (binding.btnSaveSession != null) {
                binding.btnSaveSession.setOnClickListener(v -> saveFlashcardSession());
                Log.d("FlashcardResultsActivity", "Save button setup completed");
            } else {
                Log.w("FlashcardResultsActivity", "Save button binding is null");
            }
            
            // View History button
            if (binding.btnViewHistory != null) {
                binding.btnViewHistory.setOnClickListener(v -> openHistory());
                Log.d("FlashcardResultsActivity", "View History button setup completed");
            } else {
                Log.w("FlashcardResultsActivity", "View History button binding is null");
            }
            
            // Try Again button (only show if failed)
            if (binding.btnTryAgain != null) {
                if (accuracy < 60) {
                    binding.btnTryAgain.setVisibility(View.VISIBLE);
                    binding.btnTryAgain.setOnClickListener(v -> tryAgain());
                    Log.d("FlashcardResultsActivity", "Try Again button setup completed (visible)");
                } else {
                    binding.btnTryAgain.setVisibility(View.GONE);
                    Log.d("FlashcardResultsActivity", "Try Again button hidden (accuracy >= 60)");
                }
            } else {
                Log.w("FlashcardResultsActivity", "Try Again button binding is null");
            }
            
            Log.d("FlashcardResultsActivity", "UI setup completed successfully");
            
        } catch (Exception e) {
            Log.e("FlashcardResultsActivity", "Error in setupUI", e);
        }
    }
    
    private void displayResults() {
        try {
            Log.d("FlashcardResultsActivity", "Displaying results...");
            
            // Check if binding is valid
            if (binding == null) {
                Log.e("FlashcardResultsActivity", "Binding is null in displayResults");
                return;
            }
            
            // Set accuracy text
            if (binding.textAccuracy != null) {
                binding.textAccuracy.setText(String.format(Locale.getDefault(), "%.1f%%", accuracy));
                Log.d("FlashcardResultsActivity", "Accuracy text set: " + String.format(Locale.getDefault(), "%.1f%%", accuracy));
            } else {
                Log.w("FlashcardResultsActivity", "Accuracy text binding is null");
            }
            
            // Set score details
            if (binding.textScoreDetails != null) {
                binding.textScoreDetails.setText(String.format(Locale.getDefault(), 
                    "You answered %d out of %d questions correctly", correctAnswers, totalAnswered));
                Log.d("FlashcardResultsActivity", "Score details text set");
            } else {
                Log.w("FlashcardResultsActivity", "Score details text binding is null");
            }
        
        // Show appropriate message based on performance
        if (accuracy >= 60) {
            // Passed - Show congratulations
            if (binding.textResultMessage != null) {
                binding.textResultMessage.setText("🎉 Congratulations! You've passed!");
                binding.textResultMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            if (binding.textEncouragement != null) {
                binding.textEncouragement.setText("Excellent work! Keep up the great studying!");
                binding.textEncouragement.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            
            // Update UI for success
            if (binding.layoutSuccess != null) binding.layoutSuccess.setVisibility(View.VISIBLE);
            if (binding.layoutFailure != null) binding.layoutFailure.setVisibility(View.GONE);
            
        } else {
            // Failed - Show encouragement
            if (binding.textResultMessage != null) {
                binding.textResultMessage.setText("💪 Keep Going! You're Learning!");
                binding.textResultMessage.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
            if (binding.textEncouragement != null) {
                binding.textEncouragement.setText("Don't give up! Every mistake is a learning opportunity. Review the material and try again!");
                binding.textEncouragement.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
            
            // Update UI for failure
            if (binding.layoutSuccess != null) binding.layoutSuccess.setVisibility(View.GONE);
            if (binding.layoutFailure != null) binding.layoutFailure.setVisibility(View.VISIBLE);
        }
        
        // Set progress bar
        if (binding.progressBarAccuracy != null) {
            binding.progressBarAccuracy.setMax(100);
            binding.progressBarAccuracy.setProgress((int) accuracy);
            
            // Set progress bar color based on performance
            if (accuracy >= 80) {
                binding.progressBarAccuracy.setProgressTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
            } else if (accuracy >= 60) {
                binding.progressBarAccuracy.setProgressTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
            } else {
                binding.progressBarAccuracy.setProgressTintList(getResources().getColorStateList(android.R.color.holo_orange_dark));
            }
        }
        
        Log.d("FlashcardResultsActivity", "Results display completed successfully");
        
        } catch (Exception e) {
            Log.e("FlashcardResultsActivity", "Error in displayResults", e);
        }
    }
    
    private void saveFlashcardSession() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to save your session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Generate auto heading
        String autoHeading = generateAutoHeading();
        
        // Create session data
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", currentUserId);
        sessionData.put("heading", autoHeading);
        sessionData.put("totalAnswered", totalAnswered);
        sessionData.put("correctAnswers", correctAnswers);
        sessionData.put("accuracy", accuracy);
        sessionData.put("isStudyMode", isStudyMode);
        sessionData.put("timestamp", Timestamp.now());
        sessionData.put("date", new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()));
        sessionData.put("time", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        
        // Add deck info if available
        if (deck != null) {
            sessionData.put("career", deck.getCareer());
            sessionData.put("course", deck.getCourse());
            sessionData.put("unit", deck.getUnit());
            sessionData.put("deckName", deck.getName());
        }
        if (deckId != null) {
            sessionData.put("deckId", deckId);
        }
        
        // Add flashcard data if available
        if (flashcards != null && !flashcards.isEmpty()) {
            List<Map<String, Object>> flashcardData = new ArrayList<>();
            for (Flashcard card : flashcards) {
                Map<String, Object> cardData = new HashMap<>();
                cardData.put("question", card.getQuestion());
                cardData.put("options", card.getOptions());
                cardData.put("correctAnswer", card.getAnswer());
                cardData.put("rationale", card.getRationale());
                flashcardData.add(cardData);
            }
            sessionData.put("flashcards", flashcardData);
        }
        
        // Save to Firebase
        db.collection("users").document(currentUserId)
            .collection("flashcard_sessions")
            .add(sessionData)
            .addOnSuccessListener(documentReference -> {
                Log.d("FlashcardResults", "Session saved with ID: " + documentReference.getId());
                Toast.makeText(this, "Session saved successfully!", Toast.LENGTH_SHORT).show();
                
                // Update save button
                binding.btnSaveSession.setText("✓ Saved!");
                binding.btnSaveSession.setEnabled(false);
                binding.btnSaveSession.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
                
            })
            .addOnFailureListener(e -> {
                Log.e("FlashcardResults", "Error saving session", e);
                Toast.makeText(this, "Failed to save session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private String generateAutoHeading() {
        String baseHeading = "Flashcard Study Session";
        
        if (deck != null) {
            if (deck.getCareer() != null && !deck.getCareer().isEmpty()) {
                baseHeading = deck.getCareer();
                if (deck.getCourse() != null && !deck.getCourse().isEmpty()) {
                    baseHeading += " - " + deck.getCourse();
                    if (deck.getUnit() != null && !deck.getUnit().isEmpty()) {
                        baseHeading += " - " + deck.getUnit();
                    }
                }
            }
        }
        
        // Add date and performance indicator
        String date = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date());
        String performance = accuracy >= 80 ? "Excellent" : accuracy >= 60 ? "Good" : "Practice";
        
        return String.format("%s (%s) - %s", baseHeading, date, performance);
    }
    
    private void openHistory() {
        Intent intent = new Intent(this, FlashcardHistoryActivity.class);
        startActivity(intent);
    }
    
    private void tryAgain() {
        // Go back to study mode or restart flashcard session
        if (isStudyMode && flashcards != null && !flashcards.isEmpty()) {
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra("flashcards", new ArrayList<>(flashcards));
            intent.putExtra("is_study_mode", true);
            if (deck != null) {
                intent.putExtra("career", deck.getCareer());
                intent.putExtra("course", deck.getCourse());
                intent.putExtra("unit", deck.getUnit());
            }
            startActivity(intent);
            finish();
        } else {
            // Go back to study mode activity
            finish();
        }
    }
}
