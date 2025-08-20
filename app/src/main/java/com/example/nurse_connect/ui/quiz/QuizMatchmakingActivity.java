package com.example.nurse_connect.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.databinding.ActivityQuizMatchmakingBinding;
import com.example.nurse_connect.models.QuizMatch;
import com.example.nurse_connect.models.NursingCurriculum;
import com.example.nurse_connect.services.QuizMatchService;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class QuizMatchmakingActivity extends AppCompatActivity implements 
        QuizMatchService.MatchmakingCallback {
    
    private ActivityQuizMatchmakingBinding binding;
    private QuizMatchService quizService;
    private boolean isSearching = false;
    
    // Nursing curriculum structure
    private List<NursingCurriculum.CareerLevel> careerLevels;
    private List<NursingCurriculum.Course> currentCourses;
    private List<NursingCurriculum.Unit> currentUnits;
    
    // Current selections
    private NursingCurriculum.CareerLevel selectedCareer;
    private NursingCurriculum.Course selectedCourse;
    private NursingCurriculum.Unit selectedUnit;
    
    private final List<String> playerCounts = Arrays.asList(
            "2 Players",
            "3 Players", 
            "4 Players",
            "5 Players"
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizMatchmakingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        quizService = new QuizMatchService();
        
        // Initialize curriculum structure
        careerLevels = NursingCurriculum.getCurriculum();
        
        setupSpinners();
        setupClickListeners();
        
        // Pre-fill if coming from match results
        Intent intent = getIntent();
        if (intent.hasExtra("course")) {
            String course = intent.getStringExtra("course");
            String unit = intent.getStringExtra("unit");
            String career = intent.getStringExtra("career");
            
            // Find and set the career selection
            for (NursingCurriculum.CareerLevel careerLevel : careerLevels) {
                if (careerLevel.getName().equals(career)) {
                    selectedCareer = careerLevel;
                    updateCourseSpinner();
                    break;
                }
            }
            
            // Set course and unit if available
            if (selectedCareer != null && course != null) {
                for (NursingCurriculum.Course c : selectedCareer.getCourses()) {
                    if (c.getName().equals(course)) {
                        selectedCourse = c;
                        updateUnitSpinner();
                        break;
                    }
                }
            }
        }
    }
    
    private void setupSpinners() {
        // Career spinner
        List<String> careerNames = new ArrayList<>();
        for (NursingCurriculum.CareerLevel career : careerLevels) {
            careerNames.add(career.getName());
        }
        
        ArrayAdapter<String> careerAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, careerNames);
        careerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCareer.setAdapter(careerAdapter);
        
        // Course spinner (will be populated based on career selection)
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, new ArrayList<>());
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCourse.setAdapter(courseAdapter);
        
        // Unit spinner (will be populated based on course selection)
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, new ArrayList<>());
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerUnit.setAdapter(unitAdapter);
        
        // Player count spinner
        ArrayAdapter<String> playerCountAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, playerCounts);
        playerCountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPlayerCount.setAdapter(playerCountAdapter);
        
        // Set up career spinner listener
        binding.spinnerCareer.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCareer = careerLevels.get(position);
                updateCourseSpinner();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up course spinner listener
        binding.spinnerCourse.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (currentCourses != null && position < currentCourses.size()) {
                    selectedCourse = currentCourses.get(position);
                    updateUnitSpinner();
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up unit spinner listener
        binding.spinnerUnit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (currentUnits != null && position < currentUnits.size()) {
                    selectedUnit = currentUnits.get(position);
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Initialize with first career
        if (!careerLevels.isEmpty()) {
            binding.spinnerCareer.setSelection(0);
        }
    }
    
    private void updateCourseSpinner() {
        if (selectedCareer != null) {
            currentCourses = selectedCareer.getCourses();
            List<String> courseNames = new ArrayList<>();
            for (NursingCurriculum.Course course : currentCourses) {
                courseNames.add(course.getName());
            }
            
            ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_spinner_item, courseNames);
            courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCourse.setAdapter(courseAdapter);
            
            // Reset course and unit selections
            selectedCourse = null;
            selectedUnit = null;
            updateUnitSpinner();
        }
    }
    
    private void updateUnitSpinner() {
        if (selectedCourse != null) {
            currentUnits = selectedCourse.getUnits();
            List<String> unitNames = new ArrayList<>();
            for (NursingCurriculum.Unit unit : currentUnits) {
                unitNames.add(unit.getName());
            }
            
            ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_spinner_item, unitNames);
            unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerUnit.setAdapter(unitAdapter);
            
            // Reset unit selection
            selectedUnit = null;
        } else {
            // Clear unit spinner if no course selected
            binding.spinnerUnit.setAdapter(new ArrayAdapter<>(this, 
                    android.R.layout.simple_spinner_item, new ArrayList<>()));
        }
    }
    
    private void setupClickListeners() {
        binding.btnFindMatch.setOnClickListener(v -> {
            if (isSearching) {
                cancelMatchmaking();
            } else {
                startMatchmaking();
            }
        });
        
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnQuickMatch.setOnClickListener(v -> {
            // Quick match with random selections
            binding.spinnerCareer.setSelection(0);
            binding.spinnerCourse.setSelection(0);
            binding.spinnerUnit.setSelection(0);
            startMatchmaking();
        });
    }
    
    private void startMatchmaking() {
        if (selectedCareer == null || selectedCourse == null || selectedUnit == null) {
            Toast.makeText(this, "Please select Career, Course, and Unit", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String careerName = selectedCareer.getName();
        String courseName = selectedCourse.getName();
        String unitName = selectedUnit.getName();
        int selectedPlayerCount = binding.spinnerPlayerCount.getSelectedItemPosition() + 2; // 2, 3, 4, or 5
        
        isSearching = true;
        updateUI();
        
        // Enhanced matchmaking feedback with dynamic status
        binding.textStatus.setText("🔍 Searching for " + selectedPlayerCount + " player matches...");
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Start dynamic status updates
        startDynamicStatusUpdates();
        
        // Use the new system: find available matches or create new one
        quizService.findAvailableMatches(courseName, unitName, careerName, selectedPlayerCount, this);
    }
    
    private void startDynamicStatusUpdates() {
        // Create a handler to update status messages dynamically
        android.os.Handler statusHandler = new android.os.Handler();
        int[] statusIndex = {0};
        
        String[] statusMessages = {
            "🔍 Searching for opponents...",
            "🔍 Checking available players...",
            "🔍 Looking for compatible matches...",
            "🔍 Connecting to matchmaking servers...",
            "🔍 Finding players in your skill range..."
        };
        
        Runnable statusUpdater = new Runnable() {
            @Override
            public void run() {
                if (isSearching) {
                    binding.textStatus.setText(statusMessages[statusIndex[0] % statusMessages.length]);
                    statusIndex[0]++;
                    
                    // Continue updating every 2 seconds while searching
                    statusHandler.postDelayed(this, 2000);
                }
            }
        };
        
        // Start the first update
        statusHandler.post(statusUpdater);
    }
    
    private void cancelMatchmaking() {
        isSearching = false;
        updateUI();
        
        binding.textStatus.setText("Select your quiz preferences and find a match!");
        binding.progressBar.setVisibility(View.GONE);
        
        Toast.makeText(this, "Matchmaking cancelled", Toast.LENGTH_SHORT).show();
    }
    
    private void updateUI() {
        binding.btnFindMatch.setText(isSearching ? "Cancel Search" : "Find Match");
        binding.btnFindMatch.setEnabled(true);
        
        // Disable/enable controls during search
        binding.spinnerCareer.setEnabled(!isSearching);
        binding.spinnerCourse.setEnabled(!isSearching);
        binding.spinnerUnit.setEnabled(!isSearching);
        binding.spinnerPlayerCount.setEnabled(!isSearching);
        binding.btnQuickMatch.setEnabled(!isSearching);
    }
    
    @Override
    public void onMatchFound(QuizMatch match) {
        runOnUiThread(() -> {
            isSearching = false;
            
            // Enhanced match found feedback
            binding.textStatus.setText("🎯 Match found! Preparing quiz...");
            binding.progressBar.setVisibility(View.GONE);
            
            // Show success animation feedback
            binding.textStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            
            // Add a small delay for better UX before transitioning
            new android.os.Handler().postDelayed(() -> {
                binding.textStatus.setText("🚀 Starting quiz...");
                
                // Navigate to waiting room instead of directly to quiz
                Intent intent = new Intent(this, QuizWaitingRoomActivity.class);
                intent.putExtra("match_id", match.getMatchId());
                startActivity(intent);
                finish();
            }, 1500); // 1.5 second delay for smooth transition
        });
    }
    
    @Override
    public void onMatchmakingTimeout() {
        runOnUiThread(() -> {
            isSearching = false;
            updateUI();
            
            binding.textStatus.setText("No opponents found. Try again or choose different options.");
            binding.progressBar.setVisibility(View.GONE);
            
            Toast.makeText(this, "Matchmaking timeout. No opponents found.", Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            isSearching = false;
            updateUI();
            
            binding.textStatus.setText("Error occurred. Please try again.");
            binding.progressBar.setVisibility(View.GONE);
            
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    private void setSpinnerSelection(android.widget.Spinner spinner, String value) {
        if (value == null) return;
        
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizService != null) {
            quizService.cleanup();
        }
    }
}
