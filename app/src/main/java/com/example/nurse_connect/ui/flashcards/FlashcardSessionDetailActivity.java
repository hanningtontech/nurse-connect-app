package com.example.nurse_connect.ui.flashcards;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityFlashcardSessionDetailBinding;
import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FlashcardSessionDetailActivity extends AppCompatActivity {
    private static final String TAG = "FlashcardSessionDetail";
    
    private ActivityFlashcardSessionDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    private String sessionId;
    private FlashcardSession session;
    private List<Flashcard> flashcards;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardSessionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Get session ID from intent
        sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null) {
            Toast.makeText(this, "Session ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupUI();
        loadSessionDetails();
    }
    
    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Setup RecyclerView for flashcards
        binding.recyclerFlashcards.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFlashcards.setHasFixedSize(true);
    }
    
    private void loadSessionDetails() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        showLoading(true);
        
        // Load session details
        db.collection("users").document(currentUserId)
            .collection("flashcard_sessions").document(sessionId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    session = documentSnapshot.toObject(FlashcardSession.class);
                    if (session != null) {
                        session.setSessionId(sessionId);
                        displaySessionInfo();
                        loadFlashcards();
                    } else {
                        showError("Failed to parse session data");
                    }
                } else {
                    showError("Session not found");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading session", e);
                showError("Failed to load session: " + e.getMessage());
            });
    }
    
    private void displaySessionInfo() {
        if (session == null) return;
        
        // Set session title
        binding.tvSessionTitle.setText(session.getHeading());
        
        // Set session date
        if (session.getTimestamp() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date timestamp = session.getTimestamp().toDate();
            String dateStr = dateFormat.format(timestamp);
            binding.tvSessionDate.setText(dateStr);
        }
        
        // Set session score
        int totalCards = session.getTotalAnswered();
        int correctAnswers = session.getCorrectAnswers();
        String scoreText = correctAnswers + "/" + totalCards;
        binding.tvSessionScore.setText(scoreText);
        
        // Set performance indicator
        double accuracy = totalCards > 0 ? (double) correctAnswers / totalCards * 100 : 0;
        String performanceText;
        if (accuracy >= 80) {
            performanceText = "Excellent";
        } else if (accuracy >= 60) {
            performanceText = "Good";
        } else if (accuracy >= 40) {
            performanceText = "Fair";
        } else {
            performanceText = "Needs Practice";
        }
        binding.tvSessionPerformance.setText(performanceText);
        
        // Set accuracy progress bar
        binding.progressBarAccuracy.setMax(100);
        binding.progressBarAccuracy.setProgress((int) accuracy);
        binding.tvAccuracyPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", accuracy));
    }
    
    private void loadFlashcards() {
        if (session == null || session.getFlashcards() == null) {
            showEmptyState();
            return;
        }
        
        List<Map<String, Object>> flashcardData = session.getFlashcards();
        flashcards = new ArrayList<>();
        
        // Convert the stored flashcard data to Flashcard objects
        for (Map<String, Object> cardData : flashcardData) {
            try {
                String question = (String) cardData.get("question");
                String correctAnswer = (String) cardData.get("correctAnswer");
                String rationale = (String) cardData.get("rationale");
                @SuppressWarnings("unchecked")
                List<String> options = (List<String>) cardData.get("options");
                
                if (question != null && correctAnswer != null && rationale != null && options != null) {
                    Flashcard flashcard = new Flashcard(question, correctAnswer, rationale, 
                        session.getCareer(), session.getCourse(), session.getUnit());
                    flashcard.setOptions(options);
                    flashcard.setFlashcardId("session_" + System.currentTimeMillis() + "_" + flashcards.size());
                    flashcards.add(flashcard);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing flashcard data", e);
            }
        }
        
        // Display flashcards immediately since we're not loading from Firestore
        if (!flashcards.isEmpty()) {
            displayFlashcards();
        } else {
            showEmptyState();
        }
    }
    
    private void displayFlashcards() {
        showLoading(false);
        
        if (flashcards.isEmpty()) {
            showEmptyState();
            return;
        }
        
        // Create and set adapter for flashcards
        FlashcardDetailAdapter adapter = new FlashcardDetailAdapter(flashcards);
        binding.recyclerFlashcards.setAdapter(adapter);
        
        // Show flashcards section
        binding.tvFlashcardsHeader.setVisibility(View.VISIBLE);
        binding.recyclerFlashcards.setVisibility(View.VISIBLE);
    }
    
    private void showLoading(boolean show) {
        binding.progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.cardSessionInfo.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.tvFlashcardsHeader.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.recyclerFlashcards.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showEmptyState() {
        showLoading(false);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
        binding.tvFlashcardsHeader.setVisibility(View.GONE);
        binding.recyclerFlashcards.setVisibility(View.GONE);
    }
    
    private void showError(String message) {
        showLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
