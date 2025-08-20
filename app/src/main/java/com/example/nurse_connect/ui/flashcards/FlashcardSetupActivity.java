package com.example.nurse_connect.ui.flashcards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Flashcard;
import com.example.nurse_connect.models.FlashcardDeck;
import com.example.nurse_connect.models.FlashcardGameMode;
import com.example.nurse_connect.models.NursingCurriculum;
import com.example.nurse_connect.services.FlashcardService;
import com.example.nurse_connect.services.GeminiFlashcardService;

import java.util.ArrayList;
import java.util.List;

public class FlashcardSetupActivity extends AppCompatActivity {
    private static final String TAG = "FlashcardSetupActivity";
    
    // UI Components
    private Spinner careerSpinner;
    private Spinner courseSpinner;
    private Spinner unitSpinner;
    private Button startStudyingButton;
    private ProgressBar progressBar;
    private TextView statusText;
    
    // Data
    private String selectedCareer;
    private String selectedCourse;
    private String selectedUnit;
    
    // Game mode settings from previous activity
    private FlashcardGameMode selectedGameMode;
    private String selectedDifficulty;
    private String selectedTimeLimit;
    
    // Services
    private FlashcardService flashcardService;
    private GeminiFlashcardService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_setup);
        
        // Get game mode settings from intent
        selectedGameMode = (FlashcardGameMode) getIntent().getSerializableExtra("selected_game_mode");
        selectedDifficulty = getIntent().getStringExtra("selected_difficulty");
        selectedTimeLimit = getIntent().getStringExtra("selected_time_limit");
        
        if (selectedGameMode == null) {
            // Fallback to study mode if not provided
            selectedGameMode = FlashcardGameMode.STUDY_MODE;
        }
        
        if (selectedDifficulty == null) {
            selectedDifficulty = "Medium";
        }
        
        if (selectedTimeLimit == null) {
            selectedTimeLimit = "5 minutes";
        }
        
        Log.d(TAG, "Game Mode: " + selectedGameMode.getDisplayName() + 
                   ", Difficulty: " + selectedDifficulty + 
                   ", Time Limit: " + selectedTimeLimit);
        
        initializeViews();
        setupSpinners();
        setupServices();
        updateHeader();
    }
    
    private void initializeViews() {
        careerSpinner = findViewById(R.id.spinner_career);
        courseSpinner = findViewById(R.id.spinner_course);
        unitSpinner = findViewById(R.id.spinner_unit);
        startStudyingButton = findViewById(R.id.btn_start_studying);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        
        startStudyingButton.setOnClickListener(v -> startAIFlashcardGeneration());
        // btnViewProgress.setOnClickListener(v -> openProgressActivity()); // Removed as per new_code
        // btnSettings.setOnClickListener(v -> openSettingsActivity()); // Removed as per new_code
        
        // Initially disable start button until all selections are made
        startStudyingButton.setEnabled(false);
        progressBar.setVisibility(View.GONE);
    }
    
    private void setupSpinners() {
        // Career options
        String[] careers = {"Certified Nursing Assistant (CNA)", "Licensed Practical Nurse (LPN)", "Registered Nurse (RN)", "Nurse Practitioner (NP)"};
        ArrayAdapter<String> careerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, careers);
        careerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        careerSpinner.setAdapter(careerAdapter);
        
        // Course options
        String[] courses = {"Basic Nursing Skills / Nurse Aide Training", "Fundamentals of Nursing", "Medical-Surgical Nursing", "Pediatric Nursing", "Mental Health Nursing"};
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courses);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(courseAdapter);
        
        // Unit options
        String[] units = {"Unit 1: Introduction to Healthcare", "Unit 2: Patient Safety", "Unit 3: Basic Care Skills", "Unit 4: Communication", "Unit 5: Documentation"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        
        // Set up spinner listeners
        careerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCareer = careers[position];
                checkSelections();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCourse = courses[position];
                checkSelections();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedUnit = units[position];
                checkSelections();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void checkSelections() {
        if (selectedCareer != null && selectedCourse != null && selectedUnit != null) {
            startStudyingButton.setEnabled(true);
        } else {
            startStudyingButton.setEnabled(false);
        }
    }
    
    private void setupServices() {
        flashcardService = new FlashcardService();
        geminiService = new GeminiFlashcardService();
    }
    
    private void updateHeader() {
        TextView headerText = findViewById(R.id.header_text);
        TextView infoText = findViewById(R.id.info_text);
        
        headerText.setText("Select Your Study Content");
        infoText.setText(String.format("Mode: %s • Difficulty: %s • Time: %s\nChoose your career, course, and unit to generate personalized flashcards.",
            selectedGameMode.getDisplayName(), selectedDifficulty, selectedTimeLimit));
    }
    
    private void startAIFlashcardGeneration() {
        if (selectedCareer == null || selectedCourse == null || selectedUnit == null) {
            Toast.makeText(this, "Please select all options", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Starting AI flashcard generation for: " + selectedCareer + " - " + selectedCourse + " - " + selectedUnit);
        
        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        startStudyingButton.setEnabled(false);
        startStudyingButton.setText("Generating Flashcards...");
        
        // Generate 10 flashcards using AI
        geminiService.generateFlashcardsWithAI(selectedCareer, selectedCourse, selectedUnit, 10, selectedGameMode, selectedDifficulty, selectedTimeLimit, new FlashcardService.FlashcardCallback() {
            @Override
            public void onFlashcardsLoaded(List<Flashcard> flashcards) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    startStudyingButton.setEnabled(true);
                    startStudyingButton.setText("Start Studying");
                    
                    Log.d(TAG, "Successfully generated " + flashcards.size() + " flashcards with AI");
                    Toast.makeText(FlashcardSetupActivity.this, 
                        "Generated " + flashcards.size() + " flashcards successfully!", Toast.LENGTH_LONG).show();
                    
                    // Create a deck with the game mode settings
                    FlashcardDeck deck = new FlashcardDeck();
                    deck.setName(selectedCareer + " - " + selectedCourse + " - " + selectedUnit);
                    deck.setCareer(selectedCareer);
                    deck.setCourse(selectedCourse);
                    deck.setUnit(selectedUnit);
                    deck.setTotalFlashcards(flashcards.size());
                    deck.setSource("AI Generated - " + selectedGameMode.getDisplayName());
                    
                    List<String> flashcardIds = new ArrayList<>();
                    for (Flashcard flashcard : flashcards) {
                        flashcardIds.add(flashcard.getFlashcardId());
                    }
                    deck.setFlashcardIds(flashcardIds);
                    
                    // Navigate to the appropriate game mode activity
                    Intent intent;
                    switch (selectedGameMode) {
                        case TIMED_MODE:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardTimedModeActivity.class);
                            break;
                        case QUIZ_MODE:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardQuizModeActivity.class);
                            break;
                        case SPACED_REPETITION:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardSpacedRepetitionActivity.class);
                            break;
                        case MASTERY_MODE:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardMasteryModeActivity.class);
                            break;
                        case DAILY_CHALLENGE:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardDailyChallengeActivity.class);
                            break;
                        case MATCHING_MODE:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardActivity.class); // Fallback for now
                            break;
                        case SCENARIO_MODE:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardActivity.class); // Fallback for now
                            break;
                        default:
                            intent = new Intent(FlashcardSetupActivity.this, FlashcardActivity.class);
                            break;
                    }
                    
                    // Pass the deck and game mode settings
                    intent.putExtra("selected_deck", deck);
                    intent.putExtra("selected_game_mode", selectedGameMode);
                    intent.putExtra("selected_difficulty", selectedDifficulty);
                    intent.putExtra("selected_time_limit", selectedTimeLimit);
                    intent.putExtra("flashcards", new ArrayList<>(flashcards));
                    
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    startStudyingButton.setEnabled(true);
                    startStudyingButton.setText("Start Studying");
                    
                    Log.e(TAG, "AI flashcard generation failed: " + error);
                    Toast.makeText(FlashcardSetupActivity.this, 
                        "Failed to generate flashcards: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    // Removed openProgressActivity() and openSettingsActivity() as they are no longer used
    
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
