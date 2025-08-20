package com.example.nurse_connect.ui.flashcards;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityStudyProgressBinding;
import com.example.nurse_connect.services.StudyProgressManager;
import com.example.nurse_connect.services.StudyProgressManager.StudyProgress;

/**
 * Study Progress Activity
 * Displays 30-day streak counter and study progress visualization
 * Shows progress towards 40 sets of 10 questions challenge
 */
public class StudyProgressActivity extends AppCompatActivity {
    
    private ActivityStudyProgressBinding binding;
    private StudyProgressManager studyProgressManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudyProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Study Progress Manager
        studyProgressManager = new StudyProgressManager(this);
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Setup toolbar back button
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // Load and display progress
        loadStudyProgress();
        
        // Setup refresh button
        binding.btnRefresh.setOnClickListener(v -> {
            loadStudyProgress();
            Toast.makeText(this, "Progress refreshed!", Toast.LENGTH_SHORT).show();
        });
        
        // Setup reset button
        binding.btnReset.setOnClickListener(v -> {
            // Show confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset Progress")
                .setMessage("Are you sure you want to reset all your study progress? This will start your streak counter from day 1 and clear all completed sets. This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    // First reset all progress data
                    studyProgressManager.resetProgress();
                    
                    // Then force reset the first study date to start counting from today
                    studyProgressManager.resetFirstStudyDate();
                    
                    // Refresh the display
                    loadStudyProgress();
                    Toast.makeText(this, "Progress reset successfully! Starting fresh from today.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        });
    }
    
    private void loadStudyProgress() {
        StudyProgress progress = studyProgressManager.getStudyProgress();
        
        // Update streak information
        updateStreakDisplay(progress);
        
        // Update progress information
        updateProgressDisplay(progress);
        
        // Update challenge status
        updateChallengeStatus(progress);
        
        // Update progress bars
        updateProgressBars(progress);
    }
    
    private void updateStreakDisplay(StudyProgress progress) {
        // Current streak
        binding.tvCurrentStreak.setText(String.valueOf(progress.getCurrentStreak()));
        binding.tvCurrentStreakLabel.setText(progress.getCurrentStreak() == 1 ? "Day" : "Days");
        
        // Max streak
        binding.tvMaxStreak.setText(String.valueOf(progress.getMaxStreak()));
        binding.tvMaxStreakLabel.setText(progress.getMaxStreak() == 1 ? "Day" : "Days");
        
        // Streak status message
        binding.tvStreakStatus.setText(progress.getStreakStatusMessage());
        
        // Streak progress (current streak vs 30 days)
        double streakProgress = (double) progress.getCurrentStreak() / 30.0 * 100;
        binding.progressBarStreak.setProgress((int) streakProgress);
        
        // Color the streak progress based on achievement
        if (progress.getCurrentStreak() >= 30) {
            binding.progressBarStreak.setProgressTintList(ContextCompat.getColorStateList(this, R.color.success_background));
        } else if (progress.getCurrentStreak() >= 21) {
            binding.progressBarStreak.setProgressTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
        } else if (progress.getCurrentStreak() >= 7) {
            binding.progressBarStreak.setProgressTintList(ContextCompat.getColorStateList(this, R.color.colorSecondary));
        } else {
            binding.progressBarStreak.setProgressTintList(ContextCompat.getColorStateList(this, R.color.gray));
        }
    }
    
    private void updateProgressDisplay(StudyProgress progress) {
        // Completed sets
        binding.tvCompletedSets.setText(String.valueOf(progress.getCompletedSets()));
        binding.tvTargetSets.setText(String.valueOf(progress.getTargetSets()));
        
        // Progress percentage
        binding.tvProgressPercentage.setText(String.format("%.1f%%", progress.getProgressPercentage()));
        
        // Total sessions
        binding.tvTotalSessions.setText(String.valueOf(progress.getTotalSessions()));
        
        // Days information
        binding.tvDaysSinceFirst.setText(String.valueOf(progress.getDaysSinceFirst()));
        binding.tvDaysRemaining.setText(String.valueOf(progress.getDaysRemaining()));
        
        // Progress status message
        binding.tvProgressStatus.setText(progress.getProgressStatusMessage());
    }
    
    private void updateChallengeStatus(StudyProgress progress) {
        if (!progress.hasStarted()) {
            binding.tvChallengeStatus.setText("Ready to start your 30-day challenge!");
            binding.tvChallengeStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        } else if (progress.getDaysRemaining() == 0) {
            if (progress.getCompletedSets() >= progress.getTargetSets()) {
                binding.tvChallengeStatus.setText("🎉 Challenge Completed! You're amazing!");
                binding.tvChallengeStatus.setTextColor(ContextCompat.getColor(this, R.color.success_text));
            } else {
                binding.tvChallengeStatus.setText("Challenge period ended. Keep studying!");
                binding.tvChallengeStatus.setTextColor(ContextCompat.getColor(this, R.color.error_text));
            }
        } else if (progress.isOnTrack()) {
            binding.tvChallengeStatus.setText("✅ On track for success!");
            binding.tvChallengeStatus.setTextColor(ContextCompat.getColor(this, R.color.success_text));
        } else {
            binding.tvChallengeStatus.setText("⚠️ Catch up needed!");
            binding.tvChallengeStatus.setTextColor(ContextCompat.getColor(this, R.color.error_text));
        }
    }
    
    private void updateProgressBars(StudyProgress progress) {
        // Main progress bar (completed sets vs target)
        binding.progressBarMain.setMax(100);
        binding.progressBarMain.setProgress((int) progress.getProgressPercentage());
        
        // Color the main progress bar based on completion
        if (progress.getProgressPercentage() >= 100) {
            binding.progressBarMain.setProgressTintList(ContextCompat.getColorStateList(this, R.color.success_background));
        } else if (progress.getProgressPercentage() >= 75) {
            binding.progressBarMain.setProgressTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
        } else if (progress.getProgressPercentage() >= 50) {
            binding.progressBarMain.setProgressTintList(ContextCompat.getColorStateList(this, R.color.colorSecondary));
        } else if (progress.getProgressPercentage() >= 25) {
            binding.progressBarMain.setProgressTintList(ContextCompat.getColorStateList(this, R.color.theme_warning));
        } else {
            binding.progressBarMain.setProgressTintList(ContextCompat.getColorStateList(this, R.color.gray));
        }
        
        // Days progress bar (days elapsed vs 30 days)
        if (progress.hasStarted()) {
            double daysProgress = (double) progress.getDaysSinceFirst() / 30.0 * 100;
            binding.progressBarDays.setMax(100);
            binding.progressBarDays.setProgress((int) daysProgress);
            
            // Color the days progress bar
            if (daysProgress >= 100) {
                binding.progressBarDays.setProgressTintList(ContextCompat.getColorStateList(this, R.color.success_background));
            } else if (daysProgress >= 75) {
                binding.progressBarDays.setProgressTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
            } else if (daysProgress >= 50) {
                binding.progressBarDays.setProgressTintList(ContextCompat.getColorStateList(this, R.color.colorSecondary));
            } else if (daysProgress >= 25) {
                binding.progressBarDays.setProgressTintList(ContextCompat.getColorStateList(this, R.color.theme_warning));
            } else {
                binding.progressBarDays.setProgressTintList(ContextCompat.getColorStateList(this, R.color.gray));
            }
        } else {
            binding.progressBarDays.setProgress(0);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh progress when returning to the activity
        loadStudyProgress();
    }
}
