package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityConnectBinding;
import com.example.nurse_connect.services.CommunityProfileService;

public class ConnectActivity extends AppCompatActivity {

    private ActivityConnectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Search button
        binding.btnSearch.setOnClickListener(v -> {
            Toast.makeText(this, "Search feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Profile button
        binding.btnProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Search for Courses
        binding.cardSearchCourses.setOnClickListener(v -> {
            Toast.makeText(this, "Course search feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to course search activity
        });

        // Search for Nurses
        binding.cardSearchNurses.setOnClickListener(v -> {
            Toast.makeText(this, "Nurse search feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to nurse search activity
        });

        // Create Community Profile
        binding.cardCreateProfile.setOnClickListener(v -> {
            // Check if user already has a community profile
            CommunityProfileService profileService = new CommunityProfileService();
            profileService.checkCommunityProfileExists(new CommunityProfileService.ProfileCheckCallback() {
                @Override
                public void onProfileExists(String profileId) {
                    // User has profile, navigate to view
                    Intent intent = new Intent(ConnectActivity.this, CommunityProfileViewActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onProfileNotExists() {
                    // User doesn't have profile, navigate to create
                    Intent intent = new Intent(ConnectActivity.this, CommunityProfileActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onError(String error) {
                    // On error, navigate to create
                    Intent intent = new Intent(ConnectActivity.this, CommunityProfileActivity.class);
                    startActivity(intent);
                }
            });
        });

        // Create Study Group
        binding.cardCreateGroup.setOnClickListener(v -> {
            Toast.makeText(this, "Group creation feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to group creation activity
        });

        // Join Public Tasks
        binding.cardPublicTasks.setOnClickListener(v -> {
            Toast.makeText(this, "Public tasks feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to public tasks activity
        });

        // Create Public Task
        binding.cardCreateTask.setOnClickListener(v -> {
            Toast.makeText(this, "Task creation feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to task creation activity
        });

        // Setup quick connect items
        setupQuickConnectItems();
    }

    private void setupQuickConnectItems() {
        // Find the quick connect container
        LinearLayout quickConnectContainer = findViewById(R.id.quick_connect_container);
        if (quickConnectContainer != null) {
            // Sample data for quick connect items
            String[] names = {"Sarah Johnson", "Mike Chen", "Emily Davis", "Alex Rodriguez"};
            
            for (int i = 0; i < quickConnectContainer.getChildCount() && i < names.length; i++) {
                View child = quickConnectContainer.getChildAt(i);
                
                // Set the name
                TextView nameText = child.findViewById(R.id.quick_connect_name);
                if (nameText != null) {
                    nameText.setText(names[i]);
                }
                
                // Set click listener for connect button
                com.google.android.material.button.MaterialButton connectButton = child.findViewById(R.id.btnQuickConnect);
                if (connectButton != null) {
                    final String nurseName = names[i];
                    connectButton.setOnClickListener(v -> {
                        Toast.makeText(this, "Connecting with " + nurseName + "...", Toast.LENGTH_SHORT).show();
                        // TODO: Implement connection logic
                    });
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 