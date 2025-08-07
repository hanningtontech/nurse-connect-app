package com.example.nurse_connect.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.databinding.ActivityProfileCompletionBinding;
import com.example.nurse_connect.utils.ImageUtils;
import com.example.nurse_connect.viewmodels.AuthViewModel;

import java.io.File;

public class ProfileCompletionActivity extends AppCompatActivity {
    
    private ActivityProfileCompletionBinding binding;
    private AuthViewModel authViewModel;
    
    // Profile picture variables
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileCompletionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        setupUI();
        observeViewModel();
    }
    
    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> {
            // Sign out and go back to sign in
            authViewModel.signOut();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });
        
        // Complete Profile button
        binding.btnCompleteProfile.setOnClickListener(v -> completeProfile());
        
        // Skip button (optional)
        binding.btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        
        // Profile picture button
        binding.btnAddProfilePicture.setOnClickListener(v -> showImageSourceDialog());
        
        // Setup image launchers
        setupImageLaunchers();
    }
    
    private void setupImageLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            // Save bitmap to temporary file and start cropping
                            Uri tempUri = ImageUtils.saveBitmapToFile(this, imageBitmap);
                            if (tempUri != null) {
                                startImageCropper(tempUri);
                            }
                        }
                    }
                }
            }
        );
        
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startImageCropper(uri);
                }
            }
        );

        // Crop launcher
        cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri croppedImageUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                    if (croppedImageUri != null) {
                        selectedImageUri = croppedImageUri;
                        Bitmap bitmap = ImageUtils.loadAndCompressProfileImage(this, croppedImageUri);
                        if (bitmap != null) {
                            displaySelectedImage(bitmap);
                        }
                    }
                } else if (result.getResultCode() == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
                    Throwable cropError = com.yalantis.ucrop.UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
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

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
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
    
    private void displaySelectedImage(Bitmap bitmap) {
        if (bitmap != null) {
            Glide.with(this)
                .load(bitmap)
                .circleCrop()
                .into(binding.ivProfilePicture);
        }
    }
    
    private void completeProfile() {
        String school = binding.etSchool.getText().toString().trim();
        String year = binding.etYear.getText().toString().trim();
        String course = binding.etCourse.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        
        // Validation
        if (school.isEmpty() || year.isEmpty() || course.isEmpty()) {
            Toast.makeText(this, "Please fill in School, Year, and Course", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update user profile with profile picture
        if (selectedImageUri != null) {
            uploadProfilePictureAndCompleteProfile(school, year, course, description);
        } else {
            // Update profile without profile picture
            authViewModel.updateUserProfile(school, year, course, description, null);
        }
    }
    
    private void uploadProfilePictureAndCompleteProfile(String school, String year, String course, String description) {
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnCompleteProfile.setEnabled(false);
        
        // Get current user ID
        String userId = authViewModel.getCurrentUser().getValue().getUid();
        
        // Upload profile image to Firebase Storage with enhanced compression
        ImageUtils.uploadProfileImageToFirebase(this, selectedImageUri, userId, new ImageUtils.ImageUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Update profile with profile picture URL
                runOnUiThread(() -> {
                    authViewModel.updateUserProfile(school, year, course, description, downloadUrl);
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileCompletionActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCompleteProfile.setEnabled(true);
                });
            }
        });
    }
    
    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnCompleteProfile.setEnabled(!isLoading);
        });
        
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                authViewModel.clearError();
            }
        });
        
        authViewModel.getAuthState().observe(this, authState -> {
            if (authState == AuthViewModel.AuthState.AUTHENTICATED) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
} 