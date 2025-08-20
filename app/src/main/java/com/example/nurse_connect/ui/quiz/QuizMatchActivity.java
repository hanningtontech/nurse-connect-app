package com.example.nurse_connect.ui.quiz;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.QuizOptionAdapter;
import com.example.nurse_connect.databinding.ActivityQuizMatchBinding;
import com.example.nurse_connect.models.QuizMatch;
import com.example.nurse_connect.models.QuizQuestion;
import com.example.nurse_connect.services.QuizMatchService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;

public class QuizMatchActivity extends AppCompatActivity implements 
        QuizMatchService.QuizMatchCallback, QuizOptionAdapter.OnOptionClickListener {
    
    private static final String TAG = "QuizMatchActivity";
    private ActivityQuizMatchBinding binding;
    
    private QuizMatchService quizService;
    private FirebaseFirestore db;
    private String currentUserId;
    private QuizMatch currentMatch;
    private QuizQuestion currentQuestion;
    private CountDownTimer questionTimer;
    private QuizOptionAdapter optionAdapter;
    private boolean isAnswering = false; // Debouncing flag to prevent multiple rapid clicks
    
    // Sound effects
    private SoundPool soundPool;
    private int correctSoundId;
    private int incorrectSoundId;
    private int tickingSoundId; // Added ticking sound
    private boolean soundsEnabled = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizMatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize services
        quizService = new QuizMatchService();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Get match ID from intent
        String matchId = getIntent().getStringExtra("match_id");
        if (matchId == null) {
            Toast.makeText(this, "Invalid match", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupUI();
        
        // Initialize sound effects
        initializeSounds();
        
        // Listen to match updates
        quizService.listenToMatch(matchId, this);
        
        // Don't automatically set player as ready - let them click the button
    }
    
    private void setupUI() {
        Log.d(TAG, "Setting up UI for user: " + currentUserId);
        
        // Setup options RecyclerView
        optionAdapter = new QuizOptionAdapter(this);
        binding.recyclerViewOptions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewOptions.setAdapter(optionAdapter);
        
        // Setup ready button
        Log.d(TAG, "Setting up Ready button click listener");
        binding.btnReady.setOnClickListener(v -> {
            String matchId = getIntent().getStringExtra("match_id");
            Log.d(TAG, "🎯 Ready button clicked! Match ID: " + matchId + ", Current user: " + currentUserId);
            if (matchId != null) {
                Log.d(TAG, "✅ Calling setPlayerReady for user: " + currentUserId);
                quizService.setPlayerReady(matchId, true);
                binding.btnReady.setEnabled(false);
                binding.btnReady.setText("Ready!");
                binding.textWaitingMessage.setText("Waiting for opponent to get ready...");
                Log.d(TAG, "✅ Ready button state updated successfully");
            } else {
                Log.e(TAG, "❌ Match ID is null!");
            }
        });
        
        Log.d(TAG, "Ready button setup complete. Button enabled: " + binding.btnReady.isEnabled());
        Log.d(TAG, "Ready button text: " + binding.btnReady.getText());
        Log.d(TAG, "Ready button visibility: " + binding.btnReady.getVisibility());
        
        // Setup leave match button
        binding.btnLeaveMatch.setOnClickListener(v -> {
            showLeaveConfirmation();
        });
        
        // Setup next question button
        setupNextQuestionButton();
        
        // Initial state
        showWaitingState();
    }
    
    private void showWaitingState() {
        binding.layoutWaiting.setVisibility(View.VISIBLE);
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutResults.setVisibility(View.GONE);
        binding.textWaitingMessage.setText("Click Ready to start the match!");
    }
    
    private void showQuestionState() {
        binding.layoutWaiting.setVisibility(View.GONE);
        binding.layoutQuestion.setVisibility(View.VISIBLE);
        binding.layoutResults.setVisibility(View.GONE);
    }
    
    private void showResultsState() {
        binding.layoutWaiting.setVisibility(View.GONE);
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutResults.setVisibility(View.VISIBLE);
    }
    
    private void showQuestionCompletedState() {
        binding.layoutWaiting.setVisibility(View.GONE);
        binding.layoutQuestion.setVisibility(View.VISIBLE);
        binding.layoutResults.setVisibility(View.GONE);
        
        // Show next question button
        binding.layoutNextQuestion.setVisibility(View.VISIBLE);
        
        // Check if current player has already pressed next
        Map<String, Boolean> playersPressedNext = currentMatch.getPlayersPressedNext();
        boolean hasPressedNext = playersPressedNext != null ? 
            playersPressedNext.getOrDefault(currentUserId, false) : false;
        
        if (hasPressedNext) {
            binding.textNextQuestionStatus.setText("Waiting for opponent to press Next...");
            binding.btnNextQuestion.setEnabled(false);
        } else {
            binding.textNextQuestionStatus.setText("Question Completed! Press Next to continue...");
            binding.btnNextQuestion.setEnabled(true);
        }
        
        // Update status text - show it prominently above the question
        binding.textQuestionStatus.setText("Question Completed! Press Next to continue...");
        binding.textQuestionStatus.setVisibility(View.VISIBLE);
        binding.textQuestionStatus.setAlpha(1.0f); // Ensure full opacity
        
        // Keep question and options fully visible (no dimming)
        binding.textQuestion.setAlpha(1.0f);
        binding.recyclerViewOptions.setAlpha(1.0f);
        
        // Disable options but keep them visually clear
        optionAdapter.setEnabled(false);
    }
    
    private void setupNextQuestionButton() {
        binding.btnNextQuestion.setOnClickListener(v -> {
            // Advance to next question
            quizService.advanceToNextQuestion(currentMatch.getMatchId(), new QuizMatchService.MatchmakingCallback() {
                @Override
                public void onMatchFound(QuizMatch match) {
                    // UI will be updated via onMatchUpdated callback
                    Log.d(TAG, "🎯 Next question button pressed, advancing...");
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(QuizMatchActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onMatchmakingTimeout() {
                    // This shouldn't happen for next question advancement, but handle it gracefully
                    Log.w(TAG, "⚠️ Unexpected matchmaking timeout during next question advancement");
                    Toast.makeText(QuizMatchActivity.this, "Advancement timeout, please try again", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onMatchUpdated(QuizMatch match) {
        Log.d(TAG, "🔄 Match updated: " + match.getMatchId() + 
              " status: " + match.getStatus() + 
              " currentQuestionId: " + match.getCurrentQuestionId() + 
              " questionIds size: " + match.getQuestionIds().size());
        
        Log.d(TAG, "📊 Player ready status:");
        for (String playerId : match.getPlayerIds()) {
            boolean isReady = match.isPlayerReady(playerId);
            Log.d(TAG, "  - Player " + playerId + " ready: " + isReady);
        }
        Log.d(TAG, "🎯 All players ready: " + match.areAllPlayersReady());
        
        currentMatch = match;
        updateUI();
        
        if ("active".equals(match.getStatus())) {
            Log.d(TAG, "🚀 Match is now active, checking if we need to load question");
            if (currentQuestion == null || 
                !match.getCurrentQuestionId().equals(currentQuestion.getQuestionId())) {
                Log.d(TAG, "📚 Loading current question: " + match.getCurrentQuestionId());
                loadCurrentQuestion();
            } else {
                Log.d(TAG, "📚 Question already loaded: " + currentQuestion.getQuestionId());
            }
        }
    }
    
    @Override
    public void onMatchCompleted(QuizMatch match) {
        currentMatch = match;
        showMatchResults();
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Match error: " + error);
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
    }
    
    private void updateUI() {
        if (currentMatch == null) return;
        
        // Update player scores
        updatePlayerScores();
        
        // Update match status
        if ("waiting".equals(currentMatch.getStatus())) {
            showWaitingState();
            
            // Check if current player is ready
            boolean isCurrentPlayerReady = currentMatch.isPlayerReady(currentUserId);
            binding.btnReady.setEnabled(!isCurrentPlayerReady);
            binding.btnReady.setText(isCurrentPlayerReady ? "Ready!" : "Ready");
            
            // Update waiting message based on ready status
            if (isCurrentPlayerReady) {
                if (currentMatch.areAllPlayersReady()) {
                    binding.textWaitingMessage.setText("All players ready! Starting match...");
                } else {
                    binding.textWaitingMessage.setText("Waiting for opponent to get ready...");
                }
            } else {
                binding.textWaitingMessage.setText("Click Ready to start the match!");
            }
            
        } else if ("active".equals(currentMatch.getStatus())) {
            showQuestionState();
        } else if ("question_completed".equals(currentMatch.getStatus())) {
            showQuestionCompletedState();
        }
        
        // Update progress
        int totalQuestions = currentMatch.getTotalQuestions();
        int currentIndex = currentMatch.getCurrentQuestionIndex();
        binding.progressBar.setMax(totalQuestions);
        binding.progressBar.setProgress(currentIndex);
        binding.textProgress.setText(String.format(Locale.getDefault(), 
                "%d / %d", currentIndex + 1, totalQuestions));
    }
    
    private void updatePlayerScores() {
        if (currentMatch == null || currentMatch.getPlayerIds().size() < 2) return;
        
        String player1Id = currentMatch.getPlayerIds().get(0);
        String player2Id = currentMatch.getPlayerIds().get(1);
        
        String player1Name = currentMatch.getPlayerNames().get(player1Id);
        String player2Name = currentMatch.getPlayerNames().get(player2Id);
        
        int player1Score = currentMatch.getPlayerScores().getOrDefault(player1Id, 0);
        int player2Score = currentMatch.getPlayerScores().getOrDefault(player2Id, 0);
        
        // Highlight current player
        if (player1Id.equals(currentUserId)) {
            binding.textPlayer1.setText("You: " + player1Score);
            binding.textPlayer2.setText(player2Name + ": " + player2Score);
            binding.textPlayer1.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            binding.textPlayer1.setText(player1Name + ": " + player1Score);
            binding.textPlayer2.setText("You: " + player2Score);
            binding.textPlayer2.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }
    
    private void loadCurrentQuestion() {
        if (currentMatch == null || currentMatch.getCurrentQuestionId() == null) {
            Log.w(TAG, "Cannot load question: currentMatch=" + (currentMatch == null ? "null" : currentMatch.getMatchId()) + 
                  ", currentQuestionId=" + (currentMatch == null ? "null" : currentMatch.getCurrentQuestionId()));
            return;
        }
        
        Log.d(TAG, "Loading question: " + currentMatch.getCurrentQuestionId() + 
              " for match: " + currentMatch.getMatchId() + 
              " status: " + currentMatch.getStatus());
        
        db.collection("quiz_questions")
                .document(currentMatch.getCurrentQuestionId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentQuestion = documentSnapshot.toObject(QuizQuestion.class);
                        if (currentQuestion != null) {
                            Log.d(TAG, "Question loaded successfully: " + currentQuestion.getQuestion());
                            displayQuestion();
                            startQuestionTimer();
                            
                            // Show question transition message if this is not the first question
                            if (currentMatch.getCurrentQuestionIndex() > 0) {
                                showQuestionTransition();
                            }
                        } else {
                            Log.e(TAG, "Failed to parse question document");
                            Toast.makeText(this, "Failed to parse question", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Question document does not exist: " + currentMatch.getCurrentQuestionId());
                        Toast.makeText(this, "Question not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load question", e);
                    Toast.makeText(this, "Failed to load question", Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Show transition message when moving to next question with smooth animations
     */
    private void showQuestionTransition() {
        // Fade out current question elements
        binding.textQuestion.animate()
                .alpha(0.3f)
                .setDuration(300)
                .withEndAction(() -> {
                    // Show transition message
                    binding.textQuestionStatus.setText("🔄 Next Question Loading...");
                    binding.textQuestionStatus.setVisibility(View.VISIBLE);
                    binding.textQuestionStatus.setAlpha(0f);
                    
                    // Fade in transition message
                    binding.textQuestionStatus.animate()
                            .alpha(1.0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                // Hide after 2 seconds with fade out
                                new android.os.Handler().postDelayed(() -> {
                                    binding.textQuestionStatus.animate()
                                            .alpha(0f)
                                            .setDuration(300)
                                            .withEndAction(() -> {
                                                binding.textQuestionStatus.setVisibility(View.GONE);
                                                binding.textQuestionStatus.setAlpha(1.0f);
                                            });
                                }, 2000);
                            });
                });
        
        // Also fade out options
        binding.recyclerViewOptions.animate()
                .alpha(0.3f)
                .setDuration(300);
    }
    
    private void displayQuestion() {
        if (currentQuestion == null) return;
        
        // Hide next question button for new question
        binding.layoutNextQuestion.setVisibility(View.GONE);
        
        // Reset question display with full opacity
        binding.textQuestion.setText(currentQuestion.getQuestion());
        binding.textQuestion.setAlpha(1.0f); // Ensure full opacity
        
        // Reset question status
        binding.textQuestionStatus.setVisibility(View.GONE);
        binding.textQuestionStatus.setAlpha(1.0f);
        
        // Reset options with full opacity
        optionAdapter.setOptions(currentQuestion.getOptions());
        optionAdapter.setCorrectAnswer(-1); // Hide correct answer during gameplay
        optionAdapter.clearSelection();
        optionAdapter.setEnabled(true);
        
        // Ensure options are fully visible (no dimming)
        binding.recyclerViewOptions.setAlpha(1.0f);
        
        // Update progress
        int currentQuestionNumber = currentMatch.getCurrentQuestionIndex() + 1;
        int totalQuestions = currentMatch.getQuestionIds().size();
        binding.textProgress.setText(currentQuestionNumber + " / " + totalQuestions);
        
        Log.d(TAG, "📝 Displaying question " + currentQuestionNumber + " of " + totalQuestions + " with full opacity");
    }
    
    private void startQuestionTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        
        int timeLimit = currentQuestion.getTimeLimit();
        Log.d(TAG, "⏰ Starting timer for question " + (currentMatch.getCurrentQuestionIndex() + 1) + 
              " with " + timeLimit + " seconds");
        
        // Get references to our new circular timer components
        View circularTimerView = findViewById(R.id.circular_timer);
        ProgressBar timerProgress = circularTimerView.findViewById(R.id.timerProgress);
        TextView timerText = circularTimerView.findViewById(R.id.timerText);
        
        // Reset timer to full opacity and normal color
        timerText.setAlpha(1.0f);
        timerText.setTextColor(getResources().getColor(R.color.medical_timer_normal));
        
        // Set up the circular progress bar
        timerProgress.setMax(timeLimit);
        timerProgress.setProgress(timeLimit);
        
        // Reset progress bar color
        timerProgress.getProgressDrawable().setColorFilter(
            getResources().getColor(R.color.medical_timer_normal), 
            android.graphics.PorterDuff.Mode.SRC_IN
        );
        
        questionTimer = new CountDownTimer(timeLimit * 1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                int progress = (int) (millisUntilFinished / 1000);
                
                // Update timer text
                timerText.setText(String.format(Locale.getDefault(), "%02d", secondsLeft));
                
                // Update circular progress
                timerProgress.setProgress(progress);
                
                // Play ticking sound when time is running low (last 10 seconds)
                if (secondsLeft <= 10 && secondsLeft > 0 && soundsEnabled) {
                    soundPool.play(tickingSoundId, 0.5f, 0.5f, 1, 0, 1.0f);
                }
                
                // Change timer color based on time remaining
                if (secondsLeft <= 10) {
                    timerText.setTextColor(getResources().getColor(R.color.medical_timer_danger));
                    timerProgress.getProgressDrawable().setColorFilter(
                        getResources().getColor(R.color.medical_timer_danger), 
                        android.graphics.PorterDuff.Mode.SRC_IN
                    );
                } else if (secondsLeft <= 20) {
                    timerText.setTextColor(getResources().getColor(R.color.medical_timer_warning));
                    timerProgress.getProgressDrawable().setColorFilter(
                        getResources().getColor(R.color.medical_timer_warning), 
                        android.graphics.PorterDuff.Mode.SRC_IN
                    );
                } else {
                    timerText.setTextColor(getResources().getColor(R.color.medical_timer_normal));
                    timerProgress.getProgressDrawable().setColorFilter(
                        getResources().getColor(R.color.medical_timer_normal), 
                        android.graphics.PorterDuff.Mode.SRC_IN
                    );
                }
            }
            
            @Override
            public void onFinish() {
                timerText.setText("00");
                timerProgress.setProgress(0);
                Log.d(TAG, "⏰ Time's up for question " + (currentMatch.getCurrentQuestionIndex() + 1));
                
                // Time's up - disable options and show status
                optionAdapter.setEnabled(false);
                binding.textQuestionStatus.setText("⏰ Time's up! Waiting for opponent...");
                binding.textQuestionStatus.setVisibility(View.VISIBLE);
                
                // If this player hasn't answered yet, mark them as answered
                if (!currentMatch.getPlayersAnsweredCurrentQuestion().getOrDefault(currentUserId, false)) {
                    currentMatch.markPlayerAnswered(currentUserId);
                    Log.d(TAG, "⏰ Marked player " + currentUserId + " as answered due to timeout");
                }
            }
        };
        questionTimer.start();
    }
    
    @Override
    public void onOptionClick(int position) {
        if (currentQuestion == null || currentMatch == null) return;
        
        // Debouncing: prevent multiple rapid clicks
        if (isAnswering) {
            Log.d(TAG, "🛡️ Debouncing: preventing rapid click on option " + position);
            return;
        }
        
        // Check if this player has already answered
        if (currentMatch.getPlayersAnsweredCurrentQuestion().getOrDefault(currentUserId, false)) {
            Toast.makeText(this, "You have already answered this question", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Set debouncing flag and disable UI immediately
        isAnswering = true;
        optionAdapter.setEnabled(false);
        
        Log.d(TAG, "🎯 Answer submitted for option " + position + " by user " + currentUserId);
        
        // Stop timer
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        
        // Show selected answer
        optionAdapter.setSelectedOption(position);
        
        // Submit answer
        quizService.submitAnswer(currentMatch.getMatchId(), position, new QuizMatchService.AnswerCallback() {
            @Override
            public void onAnswerResult(boolean correct, String message) {
                runOnUiThread(() -> {
                    if (correct) {
                        // Show correct answer and disable further interaction
                        optionAdapter.showCorrectAnswer(position);
                        optionAdapter.setEnabled(false);
                        
                        // Show success message
                        binding.textQuestionStatus.setText("🎯 Correct! Waiting for opponent...");
                        binding.textQuestionStatus.setVisibility(View.VISIBLE);
                        
                        Toast.makeText(QuizMatchActivity.this, message, Toast.LENGTH_LONG).show();
                        
                        // Update scores immediately
                        updatePlayerScores();
                        playCorrectSound(); // Play sound for correct answer
                    } else {
                        // Show incorrect answer but allow retry
                        optionAdapter.showIncorrectAnswer(position, currentQuestion.getCorrectAnswerIndex());
                        
                        // Check if question is already completed by opponent
                        if (message.contains("Question already completed")) {
                            // Question completed by opponent, disable options
                            optionAdapter.setEnabled(false);
                            binding.textQuestionStatus.setText("❌ Question completed by opponent. Moving to next question...");
                        } else {
                            // Question not completed, re-enable options for retry
                            optionAdapter.setEnabled(true);
                            binding.textQuestionStatus.setText("❌ Incorrect. Try again or wait for opponent...");
                        }
                        
                        binding.textQuestionStatus.setVisibility(View.VISIBLE);
                        Toast.makeText(QuizMatchActivity.this, message, Toast.LENGTH_LONG).show();
                        playIncorrectSound(); // Play sound for incorrect answer
                    }
                    
                    // Reset debouncing flag after answer is processed
                    isAnswering = false;
                    Log.d(TAG, "🔄 Debouncing flag reset after answer processing");
                });
            }
        });
    }
    
    private void showMatchResults() {
        showResultsState();
        
        if (currentMatch == null) return;
        
        // Determine winner
        String winnerId = currentMatch.getWinnerId();
        boolean isWinner = currentUserId.equals(winnerId);
        
        // Show winner message
        if (winnerId != null) {
            String winnerName = currentMatch.getPlayerNames().get(winnerId);
            if (isWinner) {
                binding.textResultTitle.setText("🏆 Victory!");
                binding.textResultTitle.setTextColor(getResources().getColor(R.color.medical_success));
            } else {
                binding.textResultTitle.setText("🎯 Defeat");
                binding.textResultTitle.setTextColor(getResources().getColor(R.color.medical_error));
            }
            
            // Show winner name
            binding.textMatchDuration.setText("Winner: " + winnerName);
        } else {
            binding.textResultTitle.setText("🤝 Tie Game");
            binding.textResultTitle.setTextColor(getResources().getColor(R.color.medical_text_secondary));
            binding.textMatchDuration.setText("Both players scored equally!");
        }
        
        // Show final scores
        updatePlayerScores();
        
        // Show match duration
        long duration = currentMatch.getEndTime() - currentMatch.getStartTime();
        int minutes = (int) (duration / 60000);
        int seconds = (int) ((duration % 60000) / 1000);
        
        // Update the final scores display
        if (currentMatch.getPlayerIds().size() >= 2) {
            String player1Id = currentMatch.getPlayerIds().get(0);
            String player2Id = currentMatch.getPlayerIds().get(1);
            
            String player1Name = currentMatch.getPlayerNames().get(player1Id);
            String player2Name = currentMatch.getPlayerNames().get(player2Id);
            
            int player1Score = currentMatch.getPlayerScores().getOrDefault(player1Id, 0);
            int player2Score = currentMatch.getPlayerScores().getOrDefault(player2Id, 0);
            
            // Highlight current player
            if (player1Id.equals(currentUserId)) {
                binding.textFinalPlayer1.setText("You: " + player1Score);
                binding.textFinalPlayer2.setText(player2Name + ": " + player2Score);
                binding.textFinalPlayer1.setTextColor(getResources().getColor(R.color.medical_primary));
            } else {
                binding.textFinalPlayer1.setText(player1Name + ": " + player1Score);
                binding.textFinalPlayer2.setText("You: " + player2Score);
                binding.textFinalPlayer2.setTextColor(getResources().getColor(R.color.medical_primary));
            }
        }
        
        // Setup buttons
        binding.btnPlayAgain.setOnClickListener(v -> {
            // Go back to matchmaking with same career settings
            Intent intent = new Intent(this, QuizMatchmakingActivity.class);
            intent.putExtra("course", currentMatch.getCourse());
            intent.putExtra("unit", currentMatch.getUnit());
            intent.putExtra("career", currentMatch.getCareer());
            startActivity(intent);
            finish();
        });
        
        binding.btnBackToMenu.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void showLeaveConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Leave Match")
                .setMessage("Are you sure you want to leave this match? You will forfeit the game.")
                .setPositiveButton("Leave", (dialog, which) -> {
                    // TODO: Implement forfeit logic
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void initializeSounds() {
        if (soundsEnabled) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(10)
                    .build();

            correctSoundId = soundPool.load(this, R.raw.correct_sound, 1);
            incorrectSoundId = soundPool.load(this, R.raw.incorrect_sound, 1);
            tickingSoundId = soundPool.load(this, R.raw.ticking_sound, 1); // Load ticking sound
        }
    }
    
    /**
     * Play sound effect for correct answer
     */
    private void playCorrectSound() {
        if (soundsEnabled && soundPool != null && correctSoundId != 0) {
            soundPool.play(correctSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
    
    /**
     * Play sound effect for incorrect answer
     */
    private void playIncorrectSound() {
        if (soundsEnabled && soundPool != null && incorrectSoundId != 0) {
            soundPool.play(incorrectSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
    
    /**
     * Toggle sound effects on/off
     */
    public void toggleSounds() {
        soundsEnabled = !soundsEnabled;
        // You could save this preference to SharedPreferences
        Log.d(TAG, "🔊 Sounds " + (soundsEnabled ? "enabled" : "disabled"));
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "🧹 QuizMatchActivity onDestroy - cleaning up resources");
        
        // Cancel the question timer
        if (questionTimer != null) {
            Log.d(TAG, "🧹 Cancelling question timer");
            questionTimer.cancel();
            questionTimer = null;
        }
        
        // Clean up sound resources
        if (soundPool != null) {
            Log.d(TAG, "🧹 Releasing sound pool");
            soundPool.release();
            soundPool = null;
        }
        
        // Clean up the quiz service and detach listeners
        if (quizService != null) {
            Log.d(TAG, "🧹 Cleaning up quiz service");
            quizService.cleanup();
            quizService = null;
        }
        
        // Clear any pending callbacks or handlers
        if (binding != null) {
            binding.recyclerViewOptions.setAdapter(null);
        }
        
        Log.d(TAG, "🧹 QuizMatchActivity cleanup completed");
        super.onDestroy();
    }
}
