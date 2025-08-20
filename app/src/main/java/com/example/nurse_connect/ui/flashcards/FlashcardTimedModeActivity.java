package com.example.nurse_connect.ui.flashcards;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardDeck;
import com.example.nurse_connect.models.FlashcardGameMode;
import com.example.nurse_connect.models.FlashcardGameSession;
import com.example.nurse_connect.services.FlashcardService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FlashcardTimedModeActivity extends AppCompatActivity {
    private static final String TAG = "FlashcardTimedMode";
    
    private FlashcardGameSession gameSession;
    private FlashcardDeck selectedDeck;
    private List<Flashcard> flashcards;
    private int currentCardIndex = 0;
    
    // UI Components
    private TextView timerText;
    private TextView questionText;
    private TextView progressText;
    private TextView scoreText;
    private TextView streakText;
    private ProgressBar progressBar;
    private Button[] optionButtons;
    private Button nextButton;
    private Button pauseButton;
    
    // Game State
    private CountDownTimer countDownTimer;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private MediaPlayer correctSound;
    private MediaPlayer incorrectSound;
    
    // Services
    private FlashcardService flashcardService;
    private FirebaseFirestore firestore;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_timed_mode);
        
        // Get game session and deck from intent
        gameSession = getIntent().getParcelableExtra("game_session");
        selectedDeck = getIntent().getParcelableExtra("selected_deck");
        
        if (gameSession == null || selectedDeck == null) {
            finish();
            return;
        }
        
        initializeViews();
        initializeSounds();
        setupServices();
        loadFlashcards();
    }
    
    private void initializeViews() {
        timerText = findViewById(R.id.timer_text);
        questionText = findViewById(R.id.question_text);
        progressText = findViewById(R.id.progress_text);
        scoreText = findViewById(R.id.score_text);
        streakText = findViewById(R.id.streak_text);
        progressBar = findViewById(R.id.progress_bar);
        nextButton = findViewById(R.id.next_button);
        pauseButton = findViewById(R.id.pause_button);
        
        // Initialize option buttons
        optionButtons = new Button[4];
        optionButtons[0] = findViewById(R.id.option_1);
        optionButtons[1] = findViewById(R.id.option_2);
        optionButtons[2] = findViewById(R.id.option_3);
        optionButtons[3] = findViewById(R.id.option_4);
        
        // Set click listeners
        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> selectOption(index));
        }
        
        nextButton.setOnClickListener(v -> showNextCard());
        pauseButton.setOnClickListener(v -> togglePause());
        
        // Update UI with game session info
        updateScoreDisplay();
        updateStreakDisplay();
    }
    
    private void initializeSounds() {
        try {
            correctSound = MediaPlayer.create(this, R.raw.correct_sound);
            incorrectSound = MediaPlayer.create(this, R.raw.incorrect_sound);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing sounds", e);
        }
    }
    
    private void setupServices() {
        flashcardService = new FlashcardService();
        firestore = FirebaseFirestore.getInstance();
    }
    
    private void loadFlashcards() {
        if (selectedDeck.getFlashcardIds() == null || selectedDeck.getFlashcardIds().isEmpty()) {
            Log.e(TAG, "No flashcard IDs in deck");
            finish();
            return;
        }
        
        flashcardService.getFlashcardsByIds(selectedDeck.getFlashcardIds(), new FlashcardService.FlashcardCallback() {
            @Override
            public void onFlashcardsLoaded(List<Flashcard> loadedFlashcards) {
                flashcards = loadedFlashcards;
                if (flashcards != null && !flashcards.isEmpty()) {
                    gameSession.setFlashcardIds(selectedDeck.getFlashcardIds());
                    gameSession.setTotalCards(flashcards.size());
                    startGame();
                } else {
                    Log.e(TAG, "No flashcards loaded");
                    finish();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading flashcards: " + error);
                finish();
            }
        });
    }
    
    private void startGame() {
        gameSession.startTimer();
        startTimer();
        showCurrentCard();
        updateProgress();
    }
    
    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        countDownTimer = new CountDownTimer(gameSession.getTimeLimit(), 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isPaused) {
                    gameSession.updateTimer();
                    updateTimerDisplay();
                }
            }
            
            @Override
            public void onFinish() {
                gameOver();
            }
        }.start();
    }
    
    private void updateTimerDisplay() {
        long timeRemaining = gameSession.getTimeRemaining();
        long seconds = timeRemaining / 1000;
        long milliseconds = (timeRemaining % 1000) / 100;
        
        String timeString = String.format("%02d:%02d", seconds, milliseconds);
        timerText.setText(timeString);
        
        // Change color based on time remaining
        if (timeRemaining <= 10000) { // Last 10 seconds
            timerText.setTextColor(getResources().getColor(R.color.incorrect_answer_color));
        } else if (timeRemaining <= 30000) { // Last 30 seconds
            timerText.setTextColor(getResources().getColor(R.color.correct_answer_color));
        } else {
            timerText.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }
    
    private void showCurrentCard() {
        if (flashcards == null || currentCardIndex >= flashcards.size()) {
            return;
        }
        
        Flashcard currentCard = flashcards.get(currentCardIndex);
        questionText.setText(currentCard.getQuestion());
        
        // Set option buttons
        List<String> options = currentCard.getOptions();
        if (options != null && options.size() == 4) {
            for (int i = 0; i < optionButtons.length; i++) {
                optionButtons[i].setText(options.get(i));
                optionButtons[i].setVisibility(View.VISIBLE);
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundResource(R.drawable.option_button_normal);
            }
        }
        
        // Update progress
        updateProgress();
        
        // Hide next button initially
        nextButton.setVisibility(View.GONE);
    }
    
    private void selectOption(int selectedIndex) {
        if (isGameOver) return;
        
        Flashcard currentCard = flashcards.get(currentCardIndex);
        int correctIndex = getCorrectAnswerIndex(currentCard);
        
        // Disable all option buttons
        for (Button button : optionButtons) {
            button.setEnabled(false);
        }
        
        // Show correct/incorrect feedback
        if (selectedIndex == correctIndex) {
            // Correct answer
            optionButtons[selectedIndex].setBackgroundResource(R.drawable.option_button_correct);
            playSound(correctSound);
            gameSession.recordCorrectAnswer();
            updateScoreDisplay();
            updateStreakDisplay();
        } else {
            // Incorrect answer
            optionButtons[selectedIndex].setBackgroundResource(R.drawable.option_button_incorrect);
            optionButtons[correctIndex].setBackgroundResource(R.drawable.option_button_correct);
            playSound(incorrectSound);
            gameSession.recordIncorrectAnswer();
            updateScoreDisplay();
            updateStreakDisplay();
        }
        
        // Show next button after delay
        new Handler().postDelayed(() -> {
            nextButton.setVisibility(View.VISIBLE);
        }, 1000);
    }
    
    private int getCorrectAnswerIndex(Flashcard flashcard) {
        List<String> options = flashcard.getOptions();
        String correctAnswer = flashcard.getAnswer();
        
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(correctAnswer)) {
                return i;
            }
        }
        return 0; // Fallback
    }
    
    private void showNextCard() {
        currentCardIndex++;
        gameSession.setCurrentCardIndex(currentCardIndex);
        
        if (currentCardIndex >= flashcards.size()) {
            gameOver();
        } else {
            showCurrentCard();
        }
    }
    
    private void updateProgress() {
        int progress = (int) ((double) currentCardIndex / flashcards.size() * 100);
        progressBar.setProgress(progress);
        progressText.setText(String.format("%d/%d", currentCardIndex + 1, flashcards.size()));
    }
    
    private void updateScoreDisplay() {
        scoreText.setText("Score: " + gameSession.getScore());
    }
    
    private void updateStreakDisplay() {
        streakText.setText("Streak: " + gameSession.getStreak());
    }
    
    private void togglePause() {
        if (isPaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }
    
    private void pauseGame() {
        isPaused = true;
        pauseButton.setText("Resume");
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
    
    private void resumeGame() {
        isPaused = false;
        pauseButton.setText("Pause");
        startTimer();
    }
    
    private void gameOver() {
        isGameOver = true;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        // Save game session
        saveGameSession();
        
        // Show results
        Intent intent = new Intent(this, FlashcardResultsActivity.class);
        intent.putExtra("game_session", gameSession);
        intent.putExtra("selected_deck", selectedDeck);
        startActivity(intent);
        finish();
    }
    
    private void saveGameSession() {
        gameSession.setCompleted(true);
        
        // Save to Firestore
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users")
            .document(userId)
            .collection("game_sessions")
            .add(gameSession)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Game session saved with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving game session", e);
            });
    }
    
    private void playSound(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "Error playing sound", e);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (correctSound != null) {
            correctSound.release();
        }
        if (incorrectSound != null) {
            incorrectSound.release();
        }
    }
}
