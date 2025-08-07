package com.example.nurse_connect.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityEditProfileBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.models.UserProfile;
import com.example.nurse_connect.utils.ImageUtils;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    
    private ActivityEditProfileBinding binding;
    private AuthViewModel authViewModel;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private User currentUser;
    private UserProfile currentUserProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new AuthViewModel();
        setupImageLaunchers();
        setupUI();
        loadCurrentUserData();
    }

    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        Uri tempUri = ImageUtils.saveBitmapToFile(this, photo);
                        if (tempUri != null) {
                            startImageCropper(tempUri);
                        }
                    }
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startImageCropper(uri);
                }
            }
        );
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnSave.setOnClickListener(v -> saveProfile());
        
        binding.btnChangePhoto.setOnClickListener(v -> showImageSourceDialog());
        
        binding.btnRemovePhoto.setOnClickListener(v -> removeProfilePhoto());
    }

    private void loadCurrentUserData() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                currentUserProfile = user.getProfile();
                populateFields();
            }
        });
    }

    private void populateFields() {
        if (currentUser != null) {
            // Basic Information
            binding.etDisplayName.setText(currentUser.getDisplayName());
            binding.etUsername.setText(currentUser.getUsername());
            binding.etPhoneNumber.setText(currentUser.getPhoneNumber());
            
            // Academic Information
            if (currentUserProfile != null) {
                binding.etCourse.setText(currentUserProfile.getCourse());
                binding.etStudyYear.setText(currentUserProfile.getStudyYear());
                binding.etSpecialization.setText(currentUserProfile.getSpecialization());
                binding.etInstitution.setText(currentUserProfile.getInstitution());
                binding.etGpa.setText(currentUserProfile.getGpa());
                binding.etBio.setText(currentUserProfile.getBio());
            }
            
            // Profile Picture
            if (currentUser.getPhotoURL() != null && !currentUser.getPhotoURL().isEmpty()) {
                Glide.with(this)
                    .load(currentUser.getPhotoURL())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivProfilePicture);
            }
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else if (which == 1) {
                // Gallery
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }

    private void startImageCropper(Uri sourceUri) {
        // Create destination URI for cropped image
        File cacheDir = getCacheDir();
        File croppedImageFile = new File(cacheDir, "cropped_profile_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(croppedImageFile);

        // Configure UCrop
        com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // Square aspect ratio for profile pictures
                .withMaxResultSize(512, 512)
                .start(this);
    }

    private void displaySelectedImage(Bitmap bitmap) {
        Glide.with(this)
            .load(bitmap)
            .circleCrop()
            .into(binding.ivProfilePicture);
    }

    private void removeProfilePhoto() {
        selectedImageUri = null;
        binding.ivProfilePicture.setImageResource(R.drawable.ic_person);
        Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show();
    }

    private void saveProfile() {
        // Show loading state
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Saving...");

        // Get values from fields
        String displayName = binding.etDisplayName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String phoneNumber = binding.etPhoneNumber.getText().toString().trim();
        String course = binding.etCourse.getText().toString().trim();
        String studyYear = binding.etStudyYear.getText().toString().trim();
        String specialization = binding.etSpecialization.getText().toString().trim();
        String institution = binding.etInstitution.getText().toString().trim();
        String gpa = binding.etGpa.getText().toString().trim();
        String bio = binding.etBio.getText().toString().trim();

        // Validate required fields
        if (displayName.isEmpty()) {
            Toast.makeText(this, "Display name is required", Toast.LENGTH_SHORT).show();
            resetSaveButton();
            return;
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
            resetSaveButton();
            return;
        }

        // Update user profile
        Log.d(TAG, "Save profile - selectedImageUri: " + (selectedImageUri != null ? selectedImageUri.toString() : "null"));
        if (selectedImageUri != null) {
            // Upload new profile picture
            Log.d(TAG, "Uploading new profile picture");
            uploadProfilePictureAndSaveProfile(displayName, username, phoneNumber, 
                course, studyYear, specialization, institution, gpa, bio);
        } else {
            // Save without changing profile picture
            Log.d(TAG, "Saving without changing profile picture");
            saveProfileData(displayName, username, phoneNumber, 
                course, studyYear, specialization, institution, gpa, bio, 
                currentUser != null ? currentUser.getPhotoURL() : null);
        }
    }

    private void uploadProfilePictureAndSaveProfile(String displayName, String username, 
            String phoneNumber, String course, String studyYear, String specialization, 
            String institution, String gpa, String bio) {
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Starting upload with URI: " + selectedImageUri.toString());
        
        ImageUtils.uploadProfileImageToFirebase(this, selectedImageUri, currentUserId, 
            new ImageUtils.ImageUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    saveProfileData(displayName, username, phoneNumber, 
                        course, studyYear, specialization, institution, gpa, bio, downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to upload profile picture", e);
                    Toast.makeText(EditProfileActivity.this, 
                        "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                }
            });
    }

    private void saveProfileData(String displayName, String username, String phoneNumber,
            String course, String studyYear, String specialization, String institution, 
            String gpa, String bio, String photoURL) {
        
        authViewModel.updateUserProfile(displayName, username, phoneNumber, 
            course, studyYear, specialization, institution, gpa, bio, photoURL);
        
        authViewModel.getUpdateProfileResult().observe(this, result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update profile: " + result.getError(), 
                        Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                }
            }
        });
    }

    private void resetSaveButton() {
        binding.btnSave.setEnabled(true);
        binding.btnSave.setText("Save");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == com.yalantis.ucrop.UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                Uri croppedImageUri = com.yalantis.ucrop.UCrop.getOutput(data);
                if (croppedImageUri != null) {
                    selectedImageUri = croppedImageUri;
                    Log.d(TAG, "Cropped image URI set: " + croppedImageUri.toString());
                    Bitmap bitmap = ImageUtils.loadAndCompressProfileImage(this, croppedImageUri);
                    if (bitmap != null) {
                        displaySelectedImage(bitmap);
                        Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (resultCode == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
                Throwable cropError = com.yalantis.ucrop.UCrop.getError(data);
                Toast.makeText(this, "Crop failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 