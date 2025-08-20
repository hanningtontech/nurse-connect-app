package com.example.nurse_connect.ui.flashcards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.FlashcardGameMode;
import com.example.nurse_connect.ui.flashcards.adapters.GameModeAdapter;
import com.example.nurse_connect.ui.flashcards.adapters.GameModeAdapter.OnGameModeClickListener;

public class FlashcardGameModeSelectionActivity extends AppCompatActivity implements OnGameModeClickListener {
    private static final String TAG = "FlashcardGameModeSelection";
    
    private RecyclerView gameModeRecyclerView;
    private GameModeAdapter gameModeAdapter;
    private Spinner difficultySpinner;
    private Spinner timeLimitSpinner;
    private TextView headerText;
    private TextView infoText;
    
    private FlashcardGameMode selectedGameMode;
    private String selectedDifficulty = "Medium";
    private String selectedTimeLimit = "5 minutes";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_game_mode_selection);
        
        initializeViews();
        setupGameModeSelection();
        setupSpinners();
        updateHeader();
    }
    
    private void initializeViews() {
        headerText = findViewById(R.id.header_text);
        infoText = findViewById(R.id.info_text);
        gameModeRecyclerView = findViewById(R.id.game_mode_recycler_view);
        difficultySpinner = findViewById(R.id.difficulty_spinner);
        timeLimitSpinner = findViewById(R.id.time_limit_spinner);
        
        // Deck info card was removed from layout - no need to hide it
    }
    
    private void setupGameModeSelection() {
        gameModeAdapter = new GameModeAdapter(this);
        gameModeRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        gameModeRecyclerView.setAdapter(gameModeAdapter);
    }
    
    private void setupSpinners() {
        // Difficulty options
        String[] difficulties = {"Easy", "Medium", "Hard", "Expert"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, difficulties);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);
        
        // Time limit options (for timed modes)
        String[] timeLimits = {"1 minute", "3 minutes", "5 minutes", "10 minutes", "15 minutes"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, timeLimits);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeLimitSpinner.setAdapter(timeAdapter);
        
        // Set default values
        difficultySpinner.setSelection(1); // Medium
        timeLimitSpinner.setSelection(2); // 5 minutes
    }
    
    private void updateHeader() {
        headerText.setText("Choose Your Learning Style");
        infoText.setText("Select a game mode that matches your learning preferences, then choose difficulty and settings.");
    }
    
    @Override
    public void onGameModeClick(FlashcardGameMode gameMode) {
        selectedGameMode = gameMode;
        Log.d(TAG, "Selected game mode: " + gameMode.getDisplayName());
        
        if (gameMode == FlashcardGameMode.STUDY_MODE) {
            // Study Mode goes directly to Study Mode activity (no setup needed)
            Intent intent = new Intent(this, FlashcardStudyModeActivity.class);
            startActivity(intent);
        } else {
            // Other game modes go to setup activity
            Intent intent = new Intent(this, FlashcardSetupActivity.class);
            intent.putExtra("selected_game_mode", gameMode);
            intent.putExtra("selected_difficulty", selectedDifficulty);
            intent.putExtra("selected_time_limit", selectedTimeLimit);
            startActivity(intent);
        }
    }
    
    @Override
    public void onBackPressed() {
        // Go back to main app (not to flashcard setup)
        super.onBackPressed();
    }
}
