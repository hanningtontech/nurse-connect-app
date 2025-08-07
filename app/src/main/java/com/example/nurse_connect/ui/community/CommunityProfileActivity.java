package com.example.nurse_connect.ui.community;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityCommunityProfileBinding;
import com.example.nurse_connect.services.CommunityProfileService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class CommunityProfileActivity extends AppCompatActivity {

    private ActivityCommunityProfileBinding binding;
    private CommunityProfileService profileService;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivProfilePhoto.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommunityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase service
        profileService = new CommunityProfileService();
        
        // Check if user is authenticated
        if (!profileService.isUserAuthenticated()) {
            Toast.makeText(this, "Please sign in to create a community profile", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Save button
        binding.btnSave.setOnClickListener(v -> {
            saveCommunityProfile();
        });

        // Change photo button
        binding.btnChangePhoto.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Nurse type selection
        binding.rgNurseType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStudentNurse) {
                showStudentFields();
            } else if (checkedId == R.id.rbRegisteredNurse || checkedId == R.id.rbPracticingNurse) {
                showRegisteredFields();
            }
        });

        // Community engagement switches
        binding.cardShareContent.setOnClickListener(v -> {
            binding.switchShareContent.setChecked(!binding.switchShareContent.isChecked());
        });

        binding.cardCreateGroups.setOnClickListener(v -> {
            binding.switchCreateGroups.setChecked(!binding.switchCreateGroups.isChecked());
        });

        binding.cardCreateTasks.setOnClickListener(v -> {
            binding.switchCreateTasks.setChecked(!binding.switchCreateTasks.isChecked());
        });

        // Set default selection
        binding.rbRegisteredNurse.setChecked(true);
        showRegisteredFields();
    }

    private void showStudentFields() {
        binding.studentFields.setVisibility(View.VISIBLE);
        binding.registeredFields.setVisibility(View.GONE);
    }

    private void showRegisteredFields() {
        binding.studentFields.setVisibility(View.GONE);
        binding.registeredFields.setVisibility(View.VISIBLE);
    }

    private void saveCommunityProfile() {
        // Get nurse type
        String nurseType = "";
        if (binding.rbStudentNurse.isChecked()) {
            nurseType = "Student Nurse";
        } else if (binding.rbRegisteredNurse.isChecked()) {
            nurseType = "Registered Nurse";
        } else if (binding.rbPracticingNurse.isChecked()) {
            nurseType = "Practicing Nurse";
        }

        // Validate nurse type selection
        if (nurseType.isEmpty()) {
            Toast.makeText(this, "Please select your nurse type", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get common fields
        String bio = binding.etBio.getText().toString().trim();

        // Validate bio
        if (bio.isEmpty()) {
            binding.etBio.setError("Please enter your professional bio");
            return;
        }

        // Create profile data map
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("nurseType", nurseType);
        profileData.put("bio", bio);

        // Get nurse type specific fields
        if (nurseType.equals("Student Nurse")) {
            String nursingLevel = binding.etNursingLevel.getText().toString().trim();
            String nursingCourse = binding.etNursingCourse.getText().toString().trim();

            if (nursingLevel.isEmpty()) {
                binding.etNursingLevel.setError("Please enter your nursing level");
                return;
            }

            if (nursingCourse.isEmpty()) {
                binding.etNursingCourse.setError("Please enter your nursing course");
                return;
            }

            profileData.put("nursingLevel", nursingLevel);
            profileData.put("nursingCourse", nursingCourse);

        } else {
            // Registered or Practicing Nurse
            String specialization = binding.etSpecialization.getText().toString().trim();
            String experience = binding.etExperience.getText().toString().trim();
            String institution = binding.etInstitution.getText().toString().trim();
            String certifications = binding.etCertifications.getText().toString().trim();

            if (specialization.isEmpty()) {
                binding.etSpecialization.setError("Please enter your specialization");
                return;
            }

            if (experience.isEmpty()) {
                binding.etExperience.setError("Please enter years of experience");
                return;
            }

            if (institution.isEmpty()) {
                binding.etInstitution.setError("Please enter your institution/workplace");
                return;
            }

            profileData.put("specialization", specialization);
            profileData.put("experience", experience);
            profileData.put("institution", institution);
            profileData.put("certifications", certifications);
        }

        // Get switch states
        boolean shareContent = binding.switchShareContent.isChecked();
        boolean createGroups = binding.switchCreateGroups.isChecked();
        boolean createTasks = binding.switchCreateTasks.isChecked();

        profileData.put("shareContent", shareContent);
        profileData.put("createGroups", createGroups);
        profileData.put("createTasks", createTasks);

        // Show loading state
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Saving...");

        // Save profile to Firebase
        if (selectedImageUri != null) {
            // Upload image first, then save profile
            profileService.uploadProfileImage(selectedImageUri,
                    downloadUri -> {
                        profileData.put("photoURL", downloadUri.toString());
                        saveProfileToFirebase(profileData);
                    },
                    exception -> {
                        Toast.makeText(this, "Failed to upload image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
        } else {
            // Save profile without image
            saveProfileToFirebase(profileData);
        }
    }

    private void saveProfileToFirebase(Map<String, Object> profileData) {
        profileService.createCommunityProfile(profileData, new CommunityProfileService.CommunityProfileCallback() {
            @Override
            public void onSuccess(String profileId) {
                Toast.makeText(CommunityProfileActivity.this, "Community profile created successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CommunityProfileActivity.this, "Failed to create profile: " + error, Toast.LENGTH_SHORT).show();
                resetSaveButton();
            }
        });
    }

    private void resetSaveButton() {
        binding.btnSave.setEnabled(true);
        binding.btnSave.setText("Save");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 