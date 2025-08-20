package com.example.nurse_connect.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.databinding.ActivityQuizSetupBinding;

public class QuizSetupActivity extends AppCompatActivity {
    
    private ActivityQuizSetupBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupUI();
    }
    
    private void setupUI() {
        binding.btnStartQuiz.setOnClickListener(v -> startQuiz());
        binding.btnBack.setOnClickListener(v -> finish());
    }
    
    private void startQuiz() {
        Intent intent = new Intent(this, QuizMatchmakingActivity.class);
        startActivity(intent);
    }
}
