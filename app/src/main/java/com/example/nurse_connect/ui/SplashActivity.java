package com.example.nurse_connect.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.nurse_connect.R;
import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.ui.onboarding.OnboardingActivity;
import com.example.nurse_connect.viewmodels.AuthViewModel;

public class SplashActivity extends AppCompatActivity {
    
    private AuthViewModel authViewModel;
    private boolean hasNavigated = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Show splash for 2-3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthState();
            }
        }, 2500);
    }
    
    private void checkAuthState() {
        if (hasNavigated) return; // Prevent multiple navigation
        
        authViewModel.getAuthState().observe(this, state -> {
            if (hasNavigated) return; // Double check
            
            switch (state) {
                case LOADING:
                    // Continue showing splash, don't navigate yet
                    break;
                case AUTHENTICATED:
                    // Navigate to main app only if user is truly authenticated
                    if (authViewModel.getCurrentUser().getValue() != null) {
                        hasNavigated = true;
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // If no user data, treat as unauthenticated
                        navigateToAuth();
                    }
                    break;
                case UNAUTHENTICATED:
                    // Always navigate to auth flow for unauthenticated users
                    navigateToAuth();
                    break;
            }
        });
        
        // Fallback: If no auth state is received within 5 seconds, go to auth
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hasNavigated) {
                    navigateToAuth();
                }
            }
        }, 5000);
    }
    
    private void navigateToAuth() {
        if (hasNavigated) return;
        hasNavigated = true;
        
        // Check if first time user
        if (isFirstTimeUser()) {
            startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LandingActivity.class));
        }
        finish();
    }
    
    private boolean isFirstTimeUser() {
        SharedPreferences sharedPrefs = getSharedPreferences("NurseConnectPrefs", MODE_PRIVATE);
        boolean isFirstTime = sharedPrefs.getBoolean("is_first_time", true);
        
        // For testing: Always show auth flow
        // Comment out the line below to enable normal flow
        // return true;
        
        return isFirstTime;
    }
    
    // Method to clear authentication state (for testing)
    private void clearAuthState() {
        SharedPreferences sharedPrefs = getSharedPreferences("NurseConnectPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("is_first_time", true);
        editor.apply();
    }
} 