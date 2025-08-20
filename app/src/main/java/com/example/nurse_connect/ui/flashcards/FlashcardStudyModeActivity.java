package com.example.nurse_connect.ui.flashcards;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.SavedSessionsAdapter;
import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardDeck;
import com.example.nurse_connect.models.FlashcardGameMode;
import com.example.nurse_connect.models.FlashcardSession;
import com.example.nurse_connect.services.FlashcardService;
import com.example.nurse_connect.services.GeminiFlashcardService;
import com.example.nurse_connect.services.StudyProgressManager;
import com.example.nurse_connect.services.StudyProgressManager.StudyProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlashcardStudyModeActivity extends AppCompatActivity implements SavedSessionsAdapter.OnSessionClickListener {
    private static final String TAG = "FlashcardStudyMode";
    
    // UI Components
    private TextView tvStudyMode;
    private ImageView ivNursingAvatar;
    private LinearLayout cardDaysRemaining;
    private TextView tvProgressPercentage;
    private TextView tvProgressCount;
    private LinearLayout llProgressDescription;
    private ProgressBar practiceProgressBar;
    private Button btnStartStudying;
    private ProgressBar progressBar;
    private androidx.cardview.widget.CardView cardPracticeProgress;
    private TextView tvSavedSessionsTitle;
    private RecyclerView recyclerSavedSessions;
    private LinearLayout layoutEmptySessions;
    
    // Data
    private String userCareer;
    private String userCourse;
    private int currentStreak = 0;
    private int totalScore = 0;
    private int totalCardsStudied = 0;
    private List<FlashcardSession> savedSessions;
    
    // Services
    private FlashcardService flashcardService;
    private GeminiFlashcardService geminiService;
    private StudyProgressManager studyProgressManager;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private SavedSessionsAdapter savedSessionsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_study_mode);
        
        initializeViews();
        setupServices();
        loadNursingAvatar();
        loadUserProfile();
        loadStudyStats();
        loadSavedSessions();
        setupButtons();
    }
    
    private void initializeViews() {
        tvStudyMode = findViewById(R.id.tv_study_mode);
        ivNursingAvatar = findViewById(R.id.iv_nursing_avatar);
        cardDaysRemaining = findViewById(R.id.card_days_remaining);
        tvProgressPercentage = findViewById(R.id.tv_progress_percentage);
        tvProgressCount = findViewById(R.id.tv_progress_count);
        llProgressDescription = findViewById(R.id.ll_progress_description);
        practiceProgressBar = findViewById(R.id.practice_progress_bar);
        btnStartStudying = findViewById(R.id.btn_start_studying);
        progressBar = findViewById(R.id.progress_bar);
        cardPracticeProgress = findViewById(R.id.card_practice_progress);
        tvSavedSessionsTitle = findViewById(R.id.tv_saved_sessions_title);
        recyclerSavedSessions = findViewById(R.id.recycler_saved_sessions);
        layoutEmptySessions = findViewById(R.id.layout_empty_sessions);
        
        // Setup RecyclerView
        recyclerSavedSessions.setLayoutManager(new LinearLayoutManager(this));
        recyclerSavedSessions.setHasFixedSize(true);
        
        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);
    }
    
    private void loadNursingAvatar() {
        String imageUrl = "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2FStudy%20mode_nurse.png?alt=media&token=b644dad1-997a-46d0-8e37-a76da4173e42";
        
        try {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_avatar_new)
                .error(R.drawable.ic_avatar_new)
                .centerCrop()
                .into(ivNursingAvatar);
                
            Log.d(TAG, "Loading nursing avatar from: " + imageUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error loading nursing avatar", e);
            ivNursingAvatar.setImageResource(R.drawable.ic_avatar_new);
        }
    }
    
    private void setupServices() {
        flashcardService = new FlashcardService();
        geminiService = new GeminiFlashcardService();
        studyProgressManager = new StudyProgressManager(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
    
    private void loadUserProfile() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userCareer = documentSnapshot.getString("career");
                        userCourse = documentSnapshot.getString("course");
                        
                        if (userCareer == null || userCourse == null) {
                            setDefaultUserPreferences();
                        }
                    } else {
                        setDefaultUserPreferences();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user profile", e);
                    setDefaultUserPreferences();
                });
        } else {
            setDefaultUserPreferences();
        }
    }
    
    private void setDefaultUserPreferences() {
        userCareer = "Certified Nursing Assistant (CNA)";
        userCourse = "Basic Nursing Skills / Nurse Aide Training";
        saveUserPreferences();
    }
    
    private void saveUserPreferences() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                .update("career", userCareer, "course", userCourse)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User preferences saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving preferences", e));
        }
    }
    
    private void loadStudyStats() {
        // Use StudyProgressManager to get current progress
        StudyProgress progress = studyProgressManager.getStudyProgress();
        
        // Update current streak
        currentStreak = progress.getCurrentStreak();
        
        // Update total cards studied (convert from sets to individual cards)
        totalCardsStudied = progress.getCompletedSets() * 10; // 10 questions per set
        
        // Update total score (for now, use completed sets as score)
        totalScore = progress.getCompletedSets();
        
        updateStatsUI();
    }
    
    private void updateStatsUI() {
        // Update days countdown - show current streak
        if (cardDaysRemaining instanceof LinearLayout) {
            LinearLayout daysLayout = (LinearLayout) cardDaysRemaining;
            if (daysLayout.getChildCount() > 0) {
                TextView daysText = (TextView) daysLayout.getChildAt(0);
                daysText.setText(String.valueOf(currentStreak));
            }
        }
        

        
        // Get current session progress and total progress
        int currentSessionQuestions = studyProgressManager.getCurrentSessionProgress();
        int totalQuestionsStudied = studyProgressManager.getTotalQuestionsStudied();
        
        // Update progress percentage and count (40 sets of 10 questions = 400 total)
        int totalTarget = 400; // 40 sets × 10 questions
        int progressPercent = totalQuestionsStudied > 0 ? Math.min(100, (totalQuestionsStudied * 100) / totalTarget) : 0;
        tvProgressPercentage.setText(progressPercent + "%");
        
        // Show current session progress instead of total
        if (currentSessionQuestions == 0) {
            tvProgressCount.setText("0/400 questions");
        } else if (currentSessionQuestions == 1) {
            tvProgressCount.setText("1/400 question (this session)");
        } else {
            tvProgressCount.setText(currentSessionQuestions + "/400 questions (this session)");
        }
        
        // Update practice progress bar with current session progress
        int sessionProgressPercent = currentSessionQuestions > 0 ? Math.min(100, (currentSessionQuestions * 100) / 10) : 0; // 10 questions per session
        practiceProgressBar.setProgress(sessionProgressPercent);
        
        // Update progress bar colors based on current session progress
        if (sessionProgressPercent >= 75) {
            // High session progress - show intense flame colors
            practiceProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.flame_intense)));
        } else if (sessionProgressPercent >= 50) {
            // Medium session progress - show medium flame colors
            practiceProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.flame_medium)));
        } else if (sessionProgressPercent >= 25) {
            // Low session progress - show light flame colors
            practiceProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.flame_light)));
        } else {
            // Very low session progress - show basic flame colors
            practiceProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.flame_basic)));
        }
        
        // Update progress summary text based on current session progress
        if (llProgressDescription instanceof LinearLayout) {
            LinearLayout summaryLayout = (LinearLayout) llProgressDescription;
            if (summaryLayout.getChildCount() > 1) {
                TextView summaryText = (TextView) summaryLayout.getChildAt(1);
                if (sessionProgressPercent == 0) {
                    summaryText.setText("Start your study session!");
                } else if (sessionProgressPercent < 25) {
                    summaryText.setText("Great start! Keep going! 🔥");
                } else if (sessionProgressPercent < 50) {
                    summaryText.setText("You're building momentum! 🔥🔥");
                } else if (sessionProgressPercent < 75) {
                    summaryText.setText("Halfway through this session! 🔥🔥🔥");
                } else if (sessionProgressPercent < 100) {
                    summaryText.setText("Almost done with this session! 🔥🔥🔥🔥");
                } else {
                    summaryText.setText("Session completed! Great job! 🔥🔥🔥🔥🔥");
                }
            }
        }
    }
    
    private void loadSavedSessions() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                .collection("flashcard_sessions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10) // Show last 10 sessions
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    savedSessions = new ArrayList<>();
                    for (var document : querySnapshot) {
                        FlashcardSession session = document.toObject(FlashcardSession.class);
                        if (session != null) {
                            session.setSessionId(document.getId());
                            savedSessions.add(session);
                        }
                    }
                    updateSavedSessionsUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading saved sessions", e);
                    savedSessions = new ArrayList<>();
                    updateSavedSessionsUI();
                });
        } else {
            savedSessions = new ArrayList<>();
            updateSavedSessionsUI();
        }
    }
    
    private void updateSavedSessionsUI() {
        if (savedSessions.isEmpty()) {
            recyclerSavedSessions.setVisibility(View.GONE);
            layoutEmptySessions.setVisibility(View.VISIBLE);
        } else {
            recyclerSavedSessions.setVisibility(View.VISIBLE);
            layoutEmptySessions.setVisibility(View.GONE);
            
            if (savedSessionsAdapter == null) {
                savedSessionsAdapter = new SavedSessionsAdapter(savedSessions, this);
                recyclerSavedSessions.setAdapter(savedSessionsAdapter);
            } else {
                savedSessionsAdapter.updateSessions(savedSessions);
            }
        }
    }
    
    private void setupButtons() {
        btnStartStudying.setOnClickListener(v -> showUnitSelectionDialog());
        
        // Setup Practice Progress card click listener
        cardPracticeProgress.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudyProgressActivity.class);
            startActivityForResult(intent, 1001); // Use startActivityForResult to refresh data
        });
        
        // TEMPORARY: Add test button for debugging AI
        // Uncomment this line to test AI generation
        // btnTestAI.setOnClickListener(v -> testAI());
    }
    
    // TEMPORARY: Test method for debugging AI
    private void testAI() {
        Log.d(TAG, "Testing AI generation...");
        geminiService.testAI(userCareer, userCourse, "Unit 1: Introduction to Healthcare", 
            new FlashcardService.FlashcardCallback() {
                @Override
                public void onFlashcardsLoaded(List<Flashcard> flashcards) {
                    Log.d(TAG, "AI Test Success: Generated " + flashcards.size() + " flashcards");
                    for (int i = 0; i < flashcards.size(); i++) {
                        Flashcard card = flashcards.get(i);
                        Log.d(TAG, "Test Card " + i + ": " + card.getQuestion());
                        Log.d(TAG, "Test Card " + i + " Options: " + card.getOptions());
                        Log.d(TAG, "Test Card " + i + " Correct: " + card.getAnswer());
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "AI Test Failed: " + error);
                }
            });
    }
    
    private void showUnitSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_unit_selection, null);
        
        Spinner spinnerUnits = dialogView.findViewById(R.id.spinner_units);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnStart = dialogView.findViewById(R.id.btn_start);
        
        // Setup units spinner
        String[] units = getAvailableUnits();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnits.setAdapter(adapter);
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnStart.setOnClickListener(v -> {
            String selectedUnit = spinnerUnits.getSelectedItem().toString();
            dialog.dismiss();
            startStudying(selectedUnit);
        });
        
        dialog.show();
    }
    
    private String[] getAvailableUnits() {
        // Return available units based on user's career and course
        if ("Certified Nursing Assistant (CNA)".equals(userCareer)) {
            return new String[]{
                "Unit 1: Introduction to Healthcare",
                "Unit 2: Patient Safety and Infection Control",
                "Unit 3: Basic Care Skills",
                "Unit 4: Communication and Interpersonal Skills",
                "Unit 5: Documentation and Reporting",
                "Unit 6: Emergency Procedures",
                "Unit 7: Special Populations",
                "Unit 8: Professional Development"
            };
        } else {
            return new String[]{
                "Unit 1: Introduction to Healthcare",
                "Unit 2: Patient Safety",
                "Unit 3: Basic Care Skills",
                "Unit 4: Communication",
                "Unit 5: Documentation"
            };
        }
    }
    
    private void startStudying(String selectedUnit) {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnStartStudying.setEnabled(false);
        btnStartStudying.setText("Generating Flashcards...");
        
        // Load user history to avoid repetition
        loadUserHistoryAndGenerateFlashcards(selectedUnit);
    }
    
    private void loadUserHistoryAndGenerateFlashcards(String selectedUnit) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
        if (currentUserId != null) {
            // Get user's study history for this unit to avoid repetition
            db.collection("users").document(currentUserId)
                .collection("flashcard_sessions")
                .whereEqualTo("career", userCareer)
                .whereEqualTo("course", userCourse)
                .whereEqualTo("unit", selectedUnit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> userHistory = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (var doc : querySnapshot) {
                            String heading = doc.getString("heading");
                            if (heading != null && !heading.isEmpty()) {
                                userHistory.add(heading);
                            }
                        }
                        Log.d(TAG, "Loaded " + userHistory.size() + " study history items for unit: " + selectedUnit);
                    }
                    
                    // Now generate flashcards with history
                    generateFlashcardsWithHistory(selectedUnit, userHistory);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user history, proceeding without history", e);
                    generateFlashcardsWithHistory(selectedUnit, new ArrayList<>());
                });
        } else {
            generateFlashcardsWithHistory(selectedUnit, new ArrayList<>());
        }
    }
    
    private void generateFlashcardsWithHistory(String selectedUnit, List<String> userHistory) {
        // Generate flashcards using AI for the selected unit - Always generate 10 questions
        int cardCount = 10; // Fixed to 10 questions for study mode
        String recommendedDifficulty = calculateOptimalDifficulty();
        
        Log.d(TAG, "Starting study session for unit: " + selectedUnit);
        Log.d(TAG, "Card count: " + cardCount + " (fixed for study mode), Difficulty: " + recommendedDifficulty);
        Log.d(TAG, "User history count: " + userHistory.size());
        
        geminiService.generateFlashcardsWithAI(
            userCareer, 
            userCourse, 
            selectedUnit, 
            cardCount, 
            FlashcardGameMode.STUDY_MODE, 
            recommendedDifficulty, 
            "No time limit",
            userHistory,
            new FlashcardService.FlashcardCallback() {
                @Override
                public void onFlashcardsLoaded(List<Flashcard> flashcards) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartStudying.setEnabled(true);
                        btnStartStudying.setText("Start Studying");
                        
                        Log.d(TAG, "Successfully generated " + flashcards.size() + " flashcards for unit: " + selectedUnit);
                        
                        // Debug: Log the first flashcard
                        if (!flashcards.isEmpty()) {
                            Flashcard firstCard = flashcards.get(0);
                            Log.d(TAG, "First flashcard question: " + firstCard.getQuestion());
                            if (firstCard.getOptions() != null) {
                                Log.d(TAG, "First flashcard options count: " + firstCard.getOptions().size());
                                for (int i = 0; i < firstCard.getOptions().size(); i++) {
                                    Log.d(TAG, "Option " + i + ": '" + firstCard.getOptions().get(i) + "'");
                                }
                            } else {
                                Log.w(TAG, "First flashcard has no options!");
                            }
                        }
                        
                        // Create study session deck
                        FlashcardDeck deck = new FlashcardDeck();
                        deck.setName("Study Session - " + selectedUnit);
                        deck.setCareer(userCareer);
                        deck.setCourse(userCourse);
                        deck.setUnit(selectedUnit);
                        deck.setTotalFlashcards(flashcards.size());
                        deck.setSource("AI Study Mode - " + cardCount + " questions - " + recommendedDifficulty);
                        
                        List<String> flashcardIds = new ArrayList<>();
                        for (Flashcard flashcard : flashcards) {
                            flashcardIds.add(flashcard.getFlashcardId());
                        }
                        deck.setFlashcardIds(flashcardIds);
                        
                        // Start new study session (reset counter)
                        studyProgressManager.startNewStudySession();
                        
                        // Navigate to study activity
                        Intent intent = new Intent(FlashcardStudyModeActivity.this, FlashcardActivity.class);
                        intent.putExtra("selected_deck", deck);
                        intent.putExtra("selected_game_mode", FlashcardGameMode.STUDY_MODE);
                        intent.putExtra("is_study_mode", true);
                        intent.putExtra("flashcards", new ArrayList<>(flashcards));
                        startActivityForResult(intent, 1002); // Use 1002 for quiz completion
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartStudying.setEnabled(true);
                        btnStartStudying.setText("Start Studying");
                        
                        Log.e(TAG, "AI flashcard generation failed: " + error);
                        Toast.makeText(FlashcardStudyModeActivity.this, 
                            "Failed to generate flashcards: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
    
    private int calculateOptimalCardCount() {
        if (totalCardsStudied < 10) {
            return 5; // Beginner: start small
        } else if (totalCardsStudied < 50) {
            return 8; // Intermediate: moderate challenge
        } else if (totalCardsStudied < 100) {
            return 10; // Advanced: full session
        } else {
            return 12; // Expert: extended session
        }
    }
    
    private String calculateOptimalDifficulty() {
        double accuracy = totalScore > 0 ? (double) totalScore / totalCardsStudied : 0.0;
        
        if (accuracy < 0.4) {
            return "Easy"; // Struggling - build confidence
        } else if (accuracy < 0.7) {
            return "Medium"; // Learning - maintain challenge
        } else if (accuracy < 0.9) {
            return "Hard"; // Proficient - push boundaries
        } else {
            return "Expert"; // Master - advanced concepts
        }
    }
    
    @Override
    public void onSessionClick(FlashcardSession session) {
        // Open session detail activity
        Intent intent = new Intent(this, FlashcardSessionDetailActivity.class);
        intent.putExtra("session_id", session.getSessionId());
        startActivity(intent);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            // Refresh study stats when returning from StudyProgressActivity
            loadStudyStats();
        } else if (requestCode == 1002) {
            // Handle results from FlashcardActivity (completed quiz)
            if (resultCode == RESULT_OK && data != null) {
                int questionsCompleted = data.getIntExtra("questions_completed", 0);
                int totalAnswered = data.getIntExtra("total_answered", 0);
                int correctAnswers = data.getIntExtra("correct_answers", 0);
                double accuracy = data.getDoubleExtra("accuracy", 0.0);
                
                Log.d(TAG, "Quiz completed! Questions: " + questionsCompleted + 
                    ", Total: " + totalAnswered + ", Correct: " + correctAnswers + 
                    ", Accuracy: " + accuracy + "%");
                
                // Update progress immediately
                updateProgressAfterQuiz(questionsCompleted);
                
                // Show success message
                Toast.makeText(this, 
                    "Quiz completed! " + questionsCompleted + " questions finished. Accuracy: " + 
                    String.format("%.1f", accuracy) + "%", Toast.LENGTH_LONG).show();
            } else {
                // Just refresh stats if no specific data
                loadStudyStats();
            }
        }
    }
    
    // Method to be called when flashcards are completed
    public void updateProgressAfterQuiz(int questionsCompleted) {
        // Update the total cards studied
        totalCardsStudied += questionsCompleted;
        
        // Update the UI immediately
        updateStatsUI();
        
        // Also update the StudyProgressManager
        if (studyProgressManager != null) {
            studyProgressManager.recordStudySession(questionsCompleted);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh saved sessions when returning to this activity
        loadSavedSessions();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flashcardService != null) {
            flashcardService.cleanup();
        }
        if (geminiService != null) {
            geminiService.cleanup();
        }
    }
}
