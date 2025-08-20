package com.example.nurse_connect.ui.flashcards;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.databinding.ActivityFlashcardProgressBinding;

/**
 * Flashcard Progress Activity
 * Shows user's learning progress and statistics
 */
public class FlashcardProgressActivity extends AppCompatActivity {
    
    private ActivityFlashcardProgressBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get parameters from intent
        String career = getIntent().getStringExtra("career");
        String course = getIntent().getStringExtra("course");
        String unit = getIntent().getStringExtra("unit");
        
        setupUI(career, course, unit);
    }
    
    private void setupUI(String career, String course, String unit) {
        // Set title
        binding.textTitle.setText(String.format("Progress: %s - %s - %s", career, course, unit));
        
        // Setup back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Show placeholder message
        Toast.makeText(this, "Progress tracking coming soon!", Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
