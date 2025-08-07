package com.example.nurse_connect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.databinding.ActivityLandingBinding;
import com.example.nurse_connect.ui.auth.SignInActivity;
import com.example.nurse_connect.ui.auth.SignUpActivity;

public class LandingActivity extends AppCompatActivity {
    
    private ActivityLandingBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLandingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupUI();
    }
    
    private void setupUI() {
        // Sign Up button
        binding.btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LandingActivity.this, SignUpActivity.class));
        });
        
        // Sign In button
        binding.btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(LandingActivity.this, SignInActivity.class));
        });
        
        // Continue as Guest button
        binding.btnGuest.setOnClickListener(v -> {
            // Navigate to main app with guest access
            Intent intent = new Intent(LandingActivity.this, MainActivity.class);
            intent.putExtra("is_guest", true);
            startActivity(intent);
            finish();
        });
        
        // Animated counter for user count
        animateUserCount();
    }
    
    private void animateUserCount() {
        // Simulate animated counter
        final int targetCount = 10000;
        final long duration = 2000L;
        final long interval = 50L;
        final int increment = (int) (targetCount / (duration / interval));
        
        final int[] currentCount = {0};
        final Handler handler = new Handler(Looper.getMainLooper());
        
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                currentCount[0] += increment;
                if (currentCount[0] < targetCount) {
                    binding.tvUserCount.setText("Join " + currentCount[0] + "+ nursing students");
                    handler.postDelayed(this, interval);
                } else {
                    binding.tvUserCount.setText("Join " + targetCount + "+ nursing students");
                }
            }
        };
        
        handler.post(runnable);
    }
} 