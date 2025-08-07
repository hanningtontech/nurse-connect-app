package com.example.nurse_connect.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.OnboardingAdapter;
import com.example.nurse_connect.databinding.ActivityOnboardingBinding;
import com.example.nurse_connect.models.OnboardingItem;
import com.example.nurse_connect.ui.LandingActivity;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
    
    private ActivityOnboardingBinding binding;
    private OnboardingAdapter onboardingAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupOnboarding();
    }
    
    private void setupOnboarding() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_onboarding_study,
                "Welcome to NurseStudy",
                "Your nursing education companion",
                "Next"
        ));
        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_onboarding_community,
                "Share & Learn Together",
                "Upload materials, connect with peers",
                "Next"
        ));
        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_onboarding_mobile,
                "Access Anytime, Anywhere",
                "Study materials at your fingertips",
                "Get Started"
        ));
        
        onboardingAdapter = new OnboardingAdapter(onboardingItems);
        
        binding.viewPager.setAdapter(onboardingAdapter);
        
        // Skip button
        binding.btnSkip.setOnClickListener(v -> {
            markFirstTimeComplete();
            startActivity(new Intent(OnboardingActivity.this, LandingActivity.class));
            finish();
        });
        
        // Page indicator
        // TODO: Implement page indicator functionality
        // binding.pageIndicator.setViewPager2(binding.viewPager);
    }
    
    private void markFirstTimeComplete() {
        SharedPreferences sharedPrefs = getSharedPreferences("NurseConnectPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("is_first_time", false);
        editor.apply();
    }
} 