package com.example.nurse_connect.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityUserProfileBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.utils.ImageUtils;
import com.example.nurse_connect.viewmodels.UserProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.yalantis.ucrop.UCrop;

import java.io.File;

/**
 * Activity for displaying and editing user profiles
 * Supports both viewing own profile and other users' profiles
 */
public class UserProfileActivity extends AppCompatActivity {
    
    private ActivityUserProfileBinding binding;
    private UserProfileViewModel viewModel;
    private User currentUser;
    private String viewedUserId;
    private boolean isOwnProfile = false;
    
    // Profile picture variables
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        viewModel.initialize(this);
        
        // Get user ID from intent
        viewedUserId = getIntent().getStringExtra("user_id");
        if (viewedUserId == null) {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Check if this is the current user's profile
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                              FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        isOwnProfile = currentUserId != null && currentUserId.equals(viewedUserId);
        
        setupUI();
        setupImageLaunchers();
        observeViewModel();
        loadUserProfile();
    }
    
    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }
        
        // Setup profile picture click listener for editing
        if (isOwnProfile) {
            binding.ivProfilePicture.setOnClickListener(v -> showImageSourceDialog());
            binding.btnEditProfile.setVisibility(View.VISIBLE);
            binding.btnEditProfile.setOnClickListener(v -> openEditProfile());
        } else {
            binding.btnEditProfile.setVisibility(View.GONE);
        }
        
        // Setup follow button (if not own profile)
        if (!isOwnProfile) {
            binding.btnFollow.setVisibility(View.VISIBLE);
            binding.btnFollow.setOnClickListener(v -> handleFollowAction());
        } else {
            binding.btnFollow.setVisibility(View.GONE);
        }
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
                    Uri croppedImageUri = UCrop.getOutput(result.getData());
                    if (croppedImageUri != null) {
                        selectedImageUri = croppedImageUri;
                        uploadProfilePicture();
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    Throwable cropError = UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    private void observeViewModel() {
        // Observe current user profile (for editing)
        viewModel.getCurrentUserProfile().observe(this, user -> {
            if (user != null && user.getUid().equals(viewedUserId)) {
                currentUser = user;
                displayUserProfile(user);
            }
        });
        
        // Observe viewed user profile
        viewModel.getViewedUserProfile().observe(this, user -> {
            if (user != null) {
                displayUserProfile(user);
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnEditProfile.setEnabled(!isLoading);
            binding.btnFollow.setEnabled(!isLoading);
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
        
        // Observe profile updates
        viewModel.getIsProfileUpdated().observe(this, isUpdated -> {
            if (isUpdated) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                viewModel.clearProfileUpdated();
            }
        });
    }
    
    private void loadUserProfile() {
        if (isOwnProfile) {
            viewModel.loadCurrentUserProfile(viewedUserId);
        } else {
            viewModel.loadUserProfile(viewedUserId);
        }
    }
    
    private void displayUserProfile(User user) {
        if (user == null) return;
        
        // Display basic information
        binding.tvDisplayName.setText(user.getDisplayNameOrUsername());
        binding.tvUsername.setText("@" + user.getUsername());
        
        // Display profile picture
        if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
            Glide.with(this)
                .load(user.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfilePicture);
        } else {
            binding.ivProfilePicture.setImageResource(R.drawable.ic_person);
        }
        
        // Display professional information
        if (user.getNursingCareer() != null && !user.getNursingCareer().isEmpty()) {
            binding.tvNursingCareer.setText(user.getNursingCareer());
            binding.layoutNursingCareer.setVisibility(View.VISIBLE);
        } else {
            binding.layoutNursingCareer.setVisibility(View.GONE);
        }
        
        if (user.getYearsExperience() != null && !user.getYearsExperience().isEmpty()) {
            binding.tvYearsExperience.setText(user.getYearsExperience() + " years");
            binding.layoutYearsExperience.setVisibility(View.VISIBLE);
        } else {
            binding.layoutYearsExperience.setVisibility(View.GONE);
        }
        
        if (user.getCurrentInstitution() != null && !user.getCurrentInstitution().isEmpty()) {
            binding.tvCurrentInstitution.setText(user.getCurrentInstitution());
            binding.layoutCurrentInstitution.setVisibility(View.VISIBLE);
        } else {
            binding.layoutCurrentInstitution.setVisibility(View.GONE);
        }
        
        // Display academic information
        if (user.getProfile() != null) {
            UserProfile profile = user.getProfile();
            
            if (profile.getInstitution() != null && !profile.getInstitution().isEmpty()) {
                binding.tvInstitution.setText(profile.getInstitution());
                binding.layoutInstitution.setVisibility(View.VISIBLE);
            } else {
                binding.layoutInstitution.setVisibility(View.GONE);
            }
            
            if (profile.getStudyYear() != null && !profile.getStudyYear().isEmpty()) {
                binding.tvStudyYear.setText(profile.getStudyYear());
                binding.layoutStudyYear.setVisibility(View.VISIBLE);
            } else {
                binding.layoutStudyYear.setVisibility(View.GONE);
            }
            
            if (profile.getSpecialization() != null && !profile.getSpecialization().isEmpty()) {
                binding.tvSpecialization.setText(profile.getSpecialization());
                binding.layoutSpecialization.setVisibility(View.VISIBLE);
            } else {
                binding.layoutSpecialization.setVisibility(View.GONE);
            }
            
            if (profile.getBio() != null && !profile.getBio().isEmpty()) {
                binding.tvBio.setText(profile.getBio());
                binding.layoutBio.setVisibility(View.VISIBLE);
            } else {
                binding.layoutBio.setVisibility(View.GONE);
            }
        }
        
        // Display contact information (only for own profile or if public)
        if (isOwnProfile || isContactInfoPublic(user)) {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                binding.tvEmail.setText(user.getEmail());
                binding.layoutEmail.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmail.setVisibility(View.GONE);
            }
            
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                binding.tvPhoneNumber.setText(user.getPhoneNumber());
                binding.layoutPhoneNumber.setVisibility(View.VISIBLE);
            } else {
                binding.layoutPhoneNumber.setVisibility(View.GONE);
            }
        } else {
            binding.layoutEmail.setVisibility(View.GONE);
            binding.layoutPhoneNumber.setVisibility(View.GONE);
        }
        
        // Display statistics
        if (user.getProfile() != null) {
            UserProfile profile = user.getProfile();
            binding.tvFollowersCount.setText(String.valueOf(profile.getFollowersCount()));
            binding.tvFollowingCount.setText(String.valueOf(profile.getFollowingCount()));
            binding.tvTotalMaterials.setText(String.valueOf(profile.getTotalMaterials()));
            binding.tvAverageRating.setText(String.format("%.1f", profile.getAverageRating()));
        }
    }
    
    private boolean isContactInfoPublic(User user) {
        // Check user's privacy settings
        return user.getSettings() != null && "public".equals(user.getSettings().getPrivacy());
    }
    
    private void showImageSourceDialog() {
        if (!isOwnProfile) return;
        
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
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // Square aspect ratio for profile pictures
                .withMaxResultSize(512, 512)
                .start(this);
    }
    
    private void uploadProfilePicture() {
        if (selectedImageUri != null && isOwnProfile) {
            viewModel.uploadProfilePicture(viewedUserId, selectedImageUri);
        }
    }
    
    private void openEditProfile() {
        if (!isOwnProfile) return;
        
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra("user_id", viewedUserId);
        startActivity(intent);
    }
    
    private void handleFollowAction() {
        if (isOwnProfile) return;
        
        // TODO: Implement follow/unfollow functionality
        Toast.makeText(this, "Follow functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isOwnProfile) {
            getMenuInflater().inflate(R.menu.menu_user_profile, menu);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_edit_profile) {
            openEditProfile();
            return true;
        } else if (id == R.id.action_settings) {
            // TODO: Open profile settings
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when returning from edit
        loadUserProfile();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear cache for this user to ensure fresh data next time
        if (viewedUserId != null) {
            viewModel.clearCache(viewedUserId);
        }
    }
} 