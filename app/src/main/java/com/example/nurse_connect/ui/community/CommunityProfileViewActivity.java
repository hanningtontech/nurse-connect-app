package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityCommunityProfileViewBinding;
import com.example.nurse_connect.services.CommunityProfileService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommunityProfileViewActivity extends AppCompatActivity {

    private ActivityCommunityProfileViewBinding binding;
    private CommunityProfileService profileService;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommunityProfileViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase services
        profileService = new CommunityProfileService();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        if (!profileService.isUserAuthenticated()) {
            Toast.makeText(this, "Please sign in to view your profile", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupClickListeners();
        loadProfileData();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Edit button
        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadProfileData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Load community profile data from Firestore
        db.collection("community_profiles")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayCommunityProfileData(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Community profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Load main user data (username, GPA, etc.)
        loadMainUserData();
    }

    private void loadMainUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Set username from Firebase Auth
            String username = currentUser.getDisplayName();
            if (username != null && !username.isEmpty()) {
                binding.tvUsername.setText(username);
            } else {
                binding.tvUsername.setText(currentUser.getEmail());
            }

            // Load additional user data from Firestore users collection
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Set GPA if available
                            if (documentSnapshot.contains("gpa")) {
                                Double gpa = documentSnapshot.getDouble("gpa");
                                if (gpa != null) {
                                    binding.tvGpa.setText(String.format("GPA: %.1f", gpa));
                                }
                            }

                            // Set profile image if available
                            if (documentSnapshot.contains("photoURL")) {
                                String photoURL = documentSnapshot.getString("photoURL");
                                if (photoURL != null && !photoURL.isEmpty()) {
                                    Glide.with(this)
                                            .load(photoURL)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .into(binding.ivProfilePhoto);
                                }
                            }
                        }
                    });
        }
    }

    private void displayCommunityProfileData(DocumentSnapshot document) {
        // Set nurse type
        if (document.contains("nurseType")) {
            String nurseType = document.getString("nurseType");
            binding.tvNurseType.setText(nurseType);

            // Show/hide appropriate sections based on nurse type
            if ("Student Nurse".equals(nurseType)) {
                binding.studentDetailsSection.setVisibility(View.VISIBLE);
                binding.registeredDetailsSection.setVisibility(View.GONE);

                // Set student-specific fields
                if (document.contains("nursingLevel")) {
                    binding.tvNursingLevel.setText(document.getString("nursingLevel"));
                }
                if (document.contains("nursingCourse")) {
                    binding.tvNursingCourse.setText(document.getString("nursingCourse"));
                }
            } else {
                binding.studentDetailsSection.setVisibility(View.GONE);
                binding.registeredDetailsSection.setVisibility(View.VISIBLE);

                // Set registered/practicing nurse fields
                if (document.contains("specialization")) {
                    binding.tvSpecialization.setText(document.getString("specialization"));
                }
                if (document.contains("experience")) {
                    binding.tvExperience.setText(document.getString("experience"));
                }
                if (document.contains("institution")) {
                    binding.tvInstitution.setText(document.getString("institution"));
                }
                if (document.contains("certifications")) {
                    binding.tvCertifications.setText(document.getString("certifications"));
                }
            }
        }

        // Set bio
        if (document.contains("bio")) {
            binding.tvBio.setText(document.getString("bio"));
        }

        // Set community engagement settings
        if (document.contains("shareContent")) {
            boolean shareContent = document.getBoolean("shareContent");
            binding.ivShareContent.setVisibility(shareContent ? View.VISIBLE : View.GONE);
        }

        if (document.contains("createGroups")) {
            boolean createGroups = document.getBoolean("createGroups");
            binding.ivCreateGroups.setVisibility(createGroups ? View.VISIBLE : View.GONE);
        }

        if (document.contains("createTasks")) {
            boolean createTasks = document.getBoolean("createTasks");
            binding.ivCreateTasks.setVisibility(createTasks ? View.VISIBLE : View.GONE);
        }

        // Set profile image from community profile
        if (document.contains("photoURL")) {
            String photoURL = document.getString("photoURL");
            if (photoURL != null && !photoURL.isEmpty()) {
                Glide.with(this)
                        .load(photoURL)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.ivProfilePhoto);
            }
        }

        // Set creation and update dates
        if (document.contains("createdAt")) {
            Long createdAt = document.getLong("createdAt");
            if (createdAt != null) {
                String createdDate = formatDate(createdAt);
                binding.tvCreatedDate.setText(createdDate);
            }
        }

        if (document.contains("updatedAt")) {
            Long updatedAt = document.getLong("updatedAt");
            if (updatedAt != null) {
                String updatedDate = formatDate(updatedAt);
                binding.tvUpdatedDate.setText(updatedDate);
            }
        }
    }

    private String formatDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when returning from edit
        loadProfileData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 