package com.example.nurse_connect.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.StudyMaterialAdapter;
import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.databinding.ActivityUserProfileBinding;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.ui.downloads.DownloadedMaterialsActivity;
import com.example.nurse_connect.ui.rating.RatingActivity;
import com.example.nurse_connect.ui.upload.EditDocumentActivity;
import com.example.nurse_connect.viewmodels.StudyMaterialViewModel;
import com.example.nurse_connect.viewmodels.UserProfileViewModel;
import com.example.nurse_connect.viewmodels.UserFollowViewModel;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity implements StudyMaterialAdapter.OnStudyMaterialClickListener {
    
    private ActivityUserProfileBinding binding;
    private UserProfileViewModel userProfileViewModel;
    private StudyMaterialViewModel studyMaterialViewModel;
    private UserFollowViewModel userFollowViewModel;
    private StudyMaterialAdapter adapter;
    private AuthViewModel authViewModel;
    private FirebaseFirestore db;

    private String userId;
    private String userName;
    
    // Filter options
    private static final String[] FILTER_OPTIONS = {
        "Most Viewed",
        "Least Viewed", 
        "Most Downloaded",
        "Least Downloaded",
        "Latest Uploaded",
        "Oldest Uploaded"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get user data from intent
        userId = getIntent().getStringExtra("user_id");
        userName = getIntent().getStringExtra("user_name");
        
        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupToolbar();
        setupRecyclerView();
        setupFilterSpinner();
        setupSearch();
        setupViewModels();
        loadUserProfile();
        loadUserDocuments();
        loadUserAchievements();
        loadFollowCounts();
        setupAchievementClickListeners();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(userName != null ? userName : "User Profile");
        }
    }
    
    private void setupRecyclerView() {
        adapter = new StudyMaterialAdapter();
        adapter.setOnStudyMaterialClickListener(this);
        
        binding.rvUserDocuments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUserDocuments.setAdapter(adapter);
    }
    
    private void setupFilterSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            FILTER_OPTIONS
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        binding.spinnerFilter.setAdapter(spinnerAdapter);
        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(position);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // If search is empty, reload all materials
                    loadUserDocuments();
                } else {
                    // Perform search
                    searchMaterials(query);
                }
            }
        });
    }
    
    private void setupViewModels() {
        userProfileViewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        studyMaterialViewModel = new ViewModelProvider(this).get(StudyMaterialViewModel.class);
        userFollowViewModel = new ViewModelProvider(this).get(UserFollowViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        db = FirebaseFirestore.getInstance();
        
        // Set AuthViewModel in StudyMaterialViewModel
        studyMaterialViewModel.setAuthViewModel(authViewModel);
        
        // Observe user profile
        userProfileViewModel.getUserProfile().observe(this, user -> {
            if (user != null) {
                updateUserProfileUI(user);
                // Check if current user is following this user
                checkFollowStatus();
            } else {
                // If user is null, show the passed username as fallback
                if (userName != null && !userName.isEmpty()) {
                    binding.tvDisplayName.setText(userName);
                    binding.tvUsername.setText("@" + userName);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(userName);
                    }
                }
            }
        });
        
        // Observe error messages from user profile loading
        userProfileViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, "Error loading profile: " + errorMessage, Toast.LENGTH_SHORT).show();
                userProfileViewModel.clearError();
            }
        });

        // Observe follow status
        userFollowViewModel.getIsFollowing().observe(this, isFollowing -> {
            updateFollowButton(isFollowing != null && isFollowing);
            // Re-enable button after operation completes
            binding.btnFollow.setEnabled(true);
        });

        // Observe follow loading state
        userFollowViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.btnFollow.setEnabled(false);
            } else {
                binding.btnFollow.setEnabled(true);
            }
        });

        // Observe follow error messages
        userFollowViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                userFollowViewModel.clearError();
                // Re-enable button on error
                binding.btnFollow.setEnabled(true);
                
                // Clear success messages after 2 seconds
                if (errorMessage.contains("Successfully")) {
                    new android.os.Handler().postDelayed(() -> {
                        userFollowViewModel.clearError();
                    }, 2000);
                }
            }
        });
        
        // Observe user documents
        studyMaterialViewModel.getUserMaterials().observe(this, documents -> {
            adapter.setMaterials(documents);
            updateEmptyState(documents);
            updateDocumentCount(documents != null ? documents.size() : 0);
        });
        
        // Observe loading state
        studyMaterialViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observe error messages
        studyMaterialViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                studyMaterialViewModel.clearError();
            }
        });

        // Observe follower counts
        userFollowViewModel.getFollowers().observe(this, followers -> {
            if (followers != null) {
                binding.tvFollowersCount.setText(String.valueOf(followers.size()));
            }
        });

        userFollowViewModel.getFollowing().observe(this, following -> {
            if (following != null) {
                binding.tvFollowingCount.setText(String.valueOf(following.size()));
            }
        });
    }
    
    private void loadUserProfile() {
        userProfileViewModel.loadUserProfile(userId);
    }
    
    private void loadUserDocuments() {
        studyMaterialViewModel.loadUserMaterials(userId);
    }
    
    private void searchMaterials(String query) {
        if (userId != null) {
            studyMaterialViewModel.searchUserMaterials(userId, query);
        }
    }
    
    private void applyFilter(int filterPosition) {
        switch (filterPosition) {
            case 0: // Most Viewed
                studyMaterialViewModel.sortByMostViewed();
                break;
            case 1: // Least Viewed
                studyMaterialViewModel.sortByLeastViewed();
                break;
            case 2: // Most Downloaded
                studyMaterialViewModel.sortUserMaterialsByMostDownloaded();
                break;
            case 3: // Least Downloaded
                studyMaterialViewModel.sortUserMaterialsByLeastDownloaded();
                break;
            case 4: // Latest Uploaded
                studyMaterialViewModel.sortByLatestUploaded();
                break;
            case 5: // Oldest Uploaded
                studyMaterialViewModel.sortByOldestUploaded();
                break;
        }
    }
    
    private void loadUserAchievements() {
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.calculateUserAchievements(userId, new StudyMaterialRepository.UserAchievementsCallback() {
            @Override
            public void onSuccess(StudyMaterialRepository.UserAchievements achievements) {
                runOnUiThread(() -> {
                    updateAchievementsUI(achievements);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, "Failed to load achievements: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadFollowCounts() {
        userFollowViewModel.loadFollowers(userId);
        userFollowViewModel.loadFollowing(userId);
    }
    
    private void updateAchievementsUI(StudyMaterialRepository.UserAchievements achievements) {
        binding.tvMaterialsCount.setText(String.valueOf(achievements.getTotalMaterials()));
        binding.tvDownloadsCount.setText(String.valueOf(achievements.getTotalDownloads()));
        binding.tvTotalLikes.setText(String.valueOf(achievements.getTotalLikes()));
        
        // Format average rating to one decimal place
        String formattedRating = String.format("%.1f", achievements.getAverageRating());
        binding.tvAverageRating.setText(formattedRating);
    }
    
    private void setupAchievementClickListeners() {
        // Materials Uploaded - Open UserMaterialsActivity
        binding.tvMaterialsCount.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserMaterialsActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_name", userName);
            startActivity(intent);
        });
        
        // Total Downloads - Open DownloadedMaterialsActivity
        binding.tvDownloadsCount.setOnClickListener(v -> {
            Intent intent = new Intent(this, DownloadedMaterialsActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_name", userName);
            startActivity(intent);
        });
        
        // Average Rating - Open CommentsActivity (for all user's materials)
        binding.tvAverageRating.setOnClickListener(v -> {
            // For now, we'll show a toast. In a full implementation, 
            // you might want to show all comments for user's materials
            Toast.makeText(this, "Rating details - showing comments for user's materials", Toast.LENGTH_SHORT).show();
        });
        
        // Total Likes - Could open a similar activity showing liked materials
        binding.tvTotalLikes.setOnClickListener(v -> {
            Toast.makeText(this, "Likes details - showing user's liked materials", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkFollowStatus() {
        userFollowViewModel.checkIsFollowing(userId);
    }

    private void updateFollowButton(boolean isFollowing) {
        // Get current user ID
        String currentUserId = getCurrentUserId();
        
        // Hide follow button if viewing own profile or if not authenticated
        if (currentUserId == null || currentUserId.equals(userId)) {
            binding.btnFollow.setVisibility(View.GONE);
            binding.btnMessage.setVisibility(View.GONE);
            return;
        }
        
        // Show follow button for other users
        binding.btnFollow.setVisibility(View.VISIBLE);
        
        if (isFollowing) {
            binding.btnFollow.setText("Unfollow");
            binding.btnFollow.setBackgroundTintList(null); // Remove any custom tint
        } else {
            binding.btnFollow.setText("Follow");
            binding.btnFollow.setBackgroundTintList(null);
        }
        
        // ALWAYS show message button when viewing other users' profiles
        binding.btnMessage.setVisibility(View.VISIBLE);
        setupMessageButton();
        
        // Set click listener for follow button
        binding.btnFollow.setOnClickListener(v -> {
            // Disable button temporarily to prevent double clicks
            binding.btnFollow.setEnabled(false);
            
            if (isFollowing) {
                userFollowViewModel.unfollowUser(userId);
            } else {
                userFollowViewModel.followUser(userId);
            }
        });
    }
    
    private void checkMutualFollow(String currentUserId) {
        // Check if the other user follows the current user
        db.collection("user_follows")
                .whereEqualTo("followerId", userId)
                .whereEqualTo("followedId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("UserProfileActivity", "Mutual follow check - query result size: " + querySnapshot.size());
                    if (!querySnapshot.isEmpty()) {
                        // Mutual follow - show message button
                        android.util.Log.d("UserProfileActivity", "Mutual follow detected - showing message button");
                        binding.btnMessage.setVisibility(View.VISIBLE);
                        setupMessageButton();
                    } else {
                        android.util.Log.d("UserProfileActivity", "No mutual follow - hiding message button");
                        binding.btnMessage.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserProfileActivity", "Error checking mutual follow: " + e.getMessage());
                    binding.btnMessage.setVisibility(View.GONE);
                });
    }
    
    private void setupMessageButton() {
        android.util.Log.d("UserProfileActivity", "Setting up message button click listener");
        binding.btnMessage.setOnClickListener(v -> {
            android.util.Log.d("UserProfileActivity", "Message button clicked");
            // Get user info for the chat
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String photoURL = documentSnapshot.getString("photoURL");
                            
                            android.util.Log.d("UserProfileActivity", "Opening private chat with user: " + username);
                            
                            // Open private chat
                            Intent intent = new Intent(this, com.example.nurse_connect.ui.chat.PrivateChatActivity.class);
                            intent.putExtra("other_user_id", userId);
                            intent.putExtra("other_user_name", username);
                            intent.putExtra("other_user_photo", photoURL);
                            startActivity(intent);
                        } else {
                            android.util.Log.e("UserProfileActivity", "User document not found");
                            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("UserProfileActivity", "Failed to load user info: " + e.getMessage());
                        Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show();
                    });
        });
    }
    
    private void updateUserProfileUI(User user) {
        // Set display name - fallback to passed username if display name is empty
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = userName != null ? userName : "Unknown User";
        }
        binding.tvDisplayName.setText(displayName);
        
        // Set username - fallback to display name if username is empty
        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            username = displayName;
        }
        binding.tvUsername.setText("@" + username);
        
        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(displayName);
        }
        
        if (user.getProfile() != null) {
            String studyYear = user.getProfile().getStudyYear();
            String specialization = user.getProfile().getSpecialization();
            String institution = user.getProfile().getInstitution();
            String bio = user.getProfile().getBio();
            
            binding.tvStudyYear.setText(studyYear != null && !studyYear.isEmpty() ? studyYear : "Not specified");
            binding.tvSpecialization.setText(specialization != null && !specialization.isEmpty() ? specialization : "Not specified");
            binding.tvInstitution.setText(institution != null && !institution.isEmpty() ? institution : "Not specified");
            binding.tvBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio available");
        } else {
            // Set default values if profile is null
            binding.tvStudyYear.setText("Not specified");
            binding.tvSpecialization.setText("Not specified");
            binding.tvInstitution.setText("Not specified");
            binding.tvBio.setText("No bio available");
        }
        
        // Load profile image using Glide
        String photoURL = user.getPhotoURL();
        android.util.Log.d("UserProfileActivity", "Loading profile picture. PhotoURL: " + photoURL);
        
        if (photoURL != null && !photoURL.isEmpty()) {
            Glide.with(this)
                .load(photoURL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfile);
        } else {
            // Set default profile picture
            android.util.Log.d("UserProfileActivity", "No photo URL, setting default profile picture");
            binding.ivProfile.setImageResource(R.drawable.ic_person);
        }
        
        // Add click listener to profile picture for preview
        binding.ivProfile.setOnClickListener(v -> {
            com.example.nurse_connect.utils.ProfilePreviewDialog.showProfilePreview(this, user);
        });
    }
    
    private void updateEmptyState(List<StudyMaterial> documents) {
        if (documents == null || documents.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvUserDocuments.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvUserDocuments.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateDocumentCount(int count) {
        binding.tvDocumentCount.setText("(" + count + " documents)");
    }
    
    // StudyMaterialAdapter.OnStudyMaterialClickListener implementation
    @Override
    public void onMaterialClick(StudyMaterial material) {
        // Increment view count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementViewCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("UserProfileActivity", "View count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (studyMaterialViewModel != null) {
                    studyMaterialViewModel.loadUserMaterials(userId);
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("UserProfileActivity", "Failed to increment view count for: " + material.getId(), e);
            }
        });
        
        // Open PDF viewer
        Intent intent = new Intent(this, com.example.nurse_connect.ui.pdf.PdfViewerActivity.class);
        intent.putExtra("pdf_url", material.getFileUrl());
        intent.putExtra("pdf_title", material.getTitle());
        intent.putExtra("material_id", material.getId());
        startActivity(intent);
    }
    
    @Override
    public void onDownloadClick(StudyMaterial material) {
        // Check storage permissions before downloading
        if (!com.example.nurse_connect.utils.PermissionUtils.hasStoragePermissions(this)) {
            com.example.nurse_connect.utils.PermissionUtils.requestStoragePermissions(this);
            Toast.makeText(this, "Storage permission required for downloads", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Increment download count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementDownloadCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("UserProfileActivity", "Download count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (studyMaterialViewModel != null) {
                    studyMaterialViewModel.loadUserMaterials(userId);
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("UserProfileActivity", "Failed to increment download count for: " + material.getId(), e);
            }
        });
        
        // Launch download service to save to NURSE_CONNECT folder
        Intent downloadIntent = new Intent(this, com.example.nurse_connect.services.PdfDownloadService.class);
        downloadIntent.putExtra("pdf_url", material.getFileUrl());
        downloadIntent.putExtra("pdf_title", material.getTitle());
        downloadIntent.putExtra("material_id", material.getId());
        startService(downloadIntent);
        
        Toast.makeText(this, "Downloading PDF to NURSE_CONNECT folder...", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onLikeClick(StudyMaterial material) {
        // Get current user ID
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to like materials", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Toggle like in the main study_materials collection
        boolean currentState = material.isLikedByUser();
        studyMaterialViewModel.toggleLike(material.getId(), currentUserId, !currentState);
        
        // Also manage favorites collection
        toggleFavorite(material, currentUserId);
        
        // Update the UI immediately for better user experience
        material.setLikedByUser(!currentState);
        adapter.updateMaterialLikeState(material.getId(), !currentState);
        
        String message = !currentState ? 
            "Liked '" + material.getTitle() + "' and added to favorites!" : 
            "Unliked '" + material.getTitle() + "' and removed from favorites!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCommentsClick(StudyMaterial material) {
        // Navigate to comments activity
        Intent intent = new Intent(this, com.example.nurse_connect.ui.comments.CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onCommentClick(StudyMaterial material) {
        // Navigate to comments activity (same as onCommentsClick)
        Intent intent = new Intent(this, com.example.nurse_connect.ui.comments.CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onRatingClick(StudyMaterial material) {
        // Navigate to rating activity
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("material_id", material.getId());
        startActivityForResult(intent, 1003); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onAuthorClick(StudyMaterial material) {
        // Navigate to author's profile (could be the same user or different user)
        Intent intent = new Intent(this, com.example.nurse_connect.ui.profile.UserProfileActivity.class);
        intent.putExtra("user_id", material.getAuthorId());
        intent.putExtra("user_name", material.getAuthorName());
        startActivity(intent);
    }

    @Override
    public void onEditClick(StudyMaterial material) {
        // Ensure Timestamp fields are initialized if they're null after deserialization
        if (material.getCreatedAt() == null) {
            material.setCreatedAt(com.google.firebase.Timestamp.now());
        }
        if (material.getUpdatedAt() == null) {
            material.setUpdatedAt(com.google.firebase.Timestamp.now());
        }
        
        // Navigate to edit document activity
        Intent intent = new Intent(this, EditDocumentActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material", material); // Pass the entire material object
        startActivityForResult(intent, 1001); // Use startActivityForResult to handle updates
    }

    @Override
    public void onThumbnailClick(StudyMaterial material) {
        showThumbnailDialog(material);
    }

    @Override
    public void onTitleClick(StudyMaterial material) {
        // Open custom PDF viewer
        Intent intent = new Intent(this, com.example.nurse_connect.ui.pdf.CustomPdfViewerActivity.class);
        intent.putExtra("pdf_url", material.getFileUrl());
        intent.putExtra("pdf_title", material.getTitle());
        intent.putExtra("material_id", material.getId());
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            // Handle result from EditDocumentActivity
            StudyMaterial updatedMaterial = (StudyMaterial) data.getSerializableExtra("updated_material");
            String deletedMaterialId = data.getStringExtra("deleted_material_id");
            
            if (updatedMaterial != null) {
                // Update the material in the adapter
                adapter.updateMaterial(updatedMaterial);
                Toast.makeText(this, "Document updated successfully!", Toast.LENGTH_SHORT).show();
            } else if (deletedMaterialId != null) {
                // Remove the deleted material from the adapter
                adapter.removeMaterial(deletedMaterialId);
                Toast.makeText(this, "Document deleted successfully!", Toast.LENGTH_SHORT).show();
                
                // Update empty state if needed
                if (adapter.getItemCount() == 0) {
                    updateEmptyState(adapter.getMaterials());
                }
            }
            
            // Refresh achievements after any document changes
            loadUserAchievements();
        } else if (requestCode == 1002) {
            // Handle result from CommentsActivity - refresh comment counts
            if (resultCode == RESULT_OK && data != null) {
                String materialId = data.getStringExtra("material_id");
                if (materialId != null) {
                    // Refresh the counts for this material
                    studyMaterialViewModel.refreshMaterialCounts(materialId);
                }
            }
            // Note: We don't refresh counts on back press to avoid unnecessary network calls
        } else if (requestCode == 1003 && resultCode == RESULT_OK && data != null) {
            // Handle result from RatingActivity - refresh counts in case rating included a comment
            String materialId = data.getStringExtra("material_id");
            if (materialId != null) {
                // Refresh the counts for this material
                studyMaterialViewModel.refreshMaterialCounts(materialId);
            }
        }
    }

    public void onDeleteClick(StudyMaterial material) {
        // This method is no longer used - deletion is now handled in EditDocumentActivity
    }

    private void showDeleteConfirmationDialog(StudyMaterial material) {
        // This method is no longer used - deletion is now handled in EditDocumentActivity
    }

    private void deleteMaterial(StudyMaterial material) {
        // This method is no longer used - deletion is now handled in EditDocumentActivity
    }

    private void showThumbnailDialog(StudyMaterial material) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_thumbnail_preview, null);
        
        ImageView ivThumbnailPreview = dialogView.findViewById(R.id.ivThumbnailPreview);
        TextView tvPreviewTitle = dialogView.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewType = dialogView.findViewById(R.id.tvPreviewType);
        TextView tvPreviewSize = dialogView.findViewById(R.id.tvPreviewSize);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        
        // Set document info
        tvPreviewTitle.setText(material.getTitle());
        tvPreviewType.setText(material.getType() + " Document");
        tvPreviewSize.setText(formatFileSize(material.getFileSize()));
        
        // Load thumbnail - use same logic as adapter
        if (material.getThumbnailURL() != null && !material.getThumbnailURL().isEmpty()) {
                         Glide.with(this)
                     .load(material.getThumbnailURL())
                     .transition(DrawableTransitionOptions.withCrossFade())
                     .placeholder(R.drawable.ic_document)
                     .error(R.drawable.ic_document)
                     .override(800, 1000) // Set higher resolution for better quality
                     .centerInside() // Ensure the image fits within bounds without cropping
                    .into(ivThumbnailPreview);
        } else {
            // Fallback to category-based icon (same as adapter)
            String category = material.getCategory();
            if (category != null) {
                switch (category.toLowerCase()) {
                    case "pdf":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                    case "video":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_video);
                        break;
                    case "audio":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_audio);
                        break;
                    case "image":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_image);
                        break;
                    default:
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                }
            } else {
                // If category is also null, use type as fallback
                switch (material.getType().toLowerCase()) {
                    case "pdf":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                    case "video":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_video);
                        break;
                    case "audio":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_audio);
                        break;
                    case "image":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_image);
                        break;
                    default:
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                }
            }
        }
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    // Achievement click methods for XML onClick
    public void onMaterialsClick(View view) {
        Intent intent = new Intent(this, UserMaterialsActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", userName);
        startActivity(intent);
    }
    
    public void onDownloadsClick(View view) {
        Intent intent = new Intent(this, DownloadedMaterialsActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", userName);
        startActivity(intent);
    }
    
    public void onRatingClick(View view) {
        Toast.makeText(this, "Rating details - showing comments for user's materials", Toast.LENGTH_SHORT).show();
    }
    
    public void onLikesClick(View view) {
        Toast.makeText(this, "Likes details - showing user's liked materials", Toast.LENGTH_SHORT).show();
    }

    public void onFollowersClick(View view) {
        Intent intent = new Intent(this, FollowersActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", userName);
        startActivity(intent);
    }

    public void onFollowingClick(View view) {
        Intent intent = new Intent(this, FollowingActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", userName);
        startActivity(intent);
    }
    
    public void toggleAchievements(View view) {
        // Toggle visibility of achievements content
        if (binding.llAchievementsContent.getVisibility() == View.VISIBLE) {
            // Collapse
            binding.llAchievementsContent.setVisibility(View.GONE);
            binding.ivAchievementsExpand.setRotation(270); // Point down
        } else {
            // Expand
            binding.llAchievementsContent.setVisibility(View.VISIBLE);
            binding.ivAchievementsExpand.setRotation(90); // Point up
        }
    }
    
    public void toggleUserProfile(View view) {
        // Toggle visibility of user profile content
        if (binding.llUserProfileContent.getVisibility() == View.VISIBLE) {
            // Collapse
            binding.llUserProfileContent.setVisibility(View.GONE);
            binding.ivUserProfileExpand.setRotation(270); // Point down
        } else {
            // Expand
            binding.llUserProfileContent.setVisibility(View.VISIBLE);
            binding.ivUserProfileExpand.setRotation(90); // Point up
        }
    }
    
    public void toggleInstitutionBio(View view) {
        // Toggle visibility of institution and bio content
        if (binding.llInstitutionBioContent.getVisibility() == View.VISIBLE) {
            // Collapse
            binding.llInstitutionBioContent.setVisibility(View.GONE);
            binding.ivInstitutionBioExpand.setRotation(270); // Point down
        } else {
            // Expand
            binding.llInstitutionBioContent.setVisibility(View.VISIBLE);
            binding.ivInstitutionBioExpand.setRotation(90); // Point up
        }
    }
    

    
    public void toggleSearchFilter(View view) {
        // Toggle visibility of search and filter content
        if (binding.llSearchFilterContent.getVisibility() == View.VISIBLE) {
            // Collapse
            binding.llSearchFilterContent.setVisibility(View.GONE);
            binding.ivSearchFilterExpand.setRotation(270); // Point down
        } else {
            // Expand
            binding.llSearchFilterContent.setVisibility(View.VISIBLE);
            binding.ivSearchFilterExpand.setRotation(90); // Point up
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
    
    private String getCurrentUserId() {
        if (authViewModel != null && authViewModel.getCurrentUser().getValue() != null) {
            return authViewModel.getCurrentUser().getValue().getUid();
        }
        return null;
    }
    
    private void toggleFavorite(StudyMaterial material, String userId) {
        com.example.nurse_connect.data.FavoritesRepository favoritesRepository = new com.example.nurse_connect.data.FavoritesRepository();
        favoritesRepository.toggleFavorite(material.getId(), new com.example.nurse_connect.data.FavoritesRepository.ToggleFavoriteCallback() {
            @Override
            public void onSuccess(boolean isFavorite) {
                Log.d("UserProfileActivity", "Toggled favorite successfully. Is favorite: " + isFavorite);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e("UserProfileActivity", "Failed to toggle favorite", e);
            }
        });
    }
} 