package com.example.nurse_connect.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nurse_connect.R;
import com.example.nurse_connect.data.AuthRepository;
import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.databinding.FragmentProfileBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.ui.auth.SignInActivity;
import com.example.nurse_connect.utils.ThemeManager;
import com.example.nurse_connect.ui.downloads.DownloadedMaterialsActivity;
import com.example.nurse_connect.utils.ProfilePreviewDialog;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.example.nurse_connect.viewmodels.UserFollowViewModel;

public class ProfileFragment extends Fragment {
    private static final int EDIT_PROFILE_REQUEST_CODE = 1001;
    
    private FragmentProfileBinding binding;
    private AuthViewModel authViewModel;
    private UserFollowViewModel followViewModel;
    private StudyMaterialRepository studyMaterialRepository;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        followViewModel = new ViewModelProvider(this).get(UserFollowViewModel.class);
        studyMaterialRepository = new StudyMaterialRepository();
        setupUI();
        observeViewModel();
    }
    
    private void setupUI() {
        // Menu button
        binding.btnMenu.setOnClickListener(v -> showMenuDialog());
        
        // Edit Profile button
        binding.btnEditProfile.setOnClickListener(v -> editProfile());
        
        // View User Profile button
        binding.btnViewUserProfile.setOnClickListener(v -> viewUserProfile());

        // Follow/Followers sections
        binding.llFollowers.setOnClickListener(v -> openFollowers());
        binding.llFollowing.setOnClickListener(v -> openFollowing());
        
        // Profile picture click listener
        binding.ivProfilePicture.setOnClickListener(v -> showProfilePreview());
        
        // Achievements header click listener
        binding.llAchievementsHeader.setOnClickListener(v -> toggleAchievements());
        
        // Achievement click listeners
        setupAchievementClickListeners();
    }
    
    private void observeViewModel() {
        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateProfileUI(user);
                loadFollowCounts(user.getUid());
                loadUserAchievements(user.getUid());
            }
        });

        // Observe follow counts
        followViewModel.getFollowers().observe(getViewLifecycleOwner(), followers -> {
            if (followers != null) {
                binding.tvFollowersCount.setText(String.valueOf(followers.size()));
            }
        });

        followViewModel.getFollowing().observe(getViewLifecycleOwner(), following -> {
            if (following != null) {
                binding.tvFollowingCount.setText(String.valueOf(following.size()));
            }
        });

        // Observe error messages
        followViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void updateProfileUI(com.example.nurse_connect.models.User user) {
        // Set display name with fallback
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "Unknown User";
        }
        binding.tvDisplayName.setText(displayName);
        
        // Set handle with fallback to username
        String handle = user.getHandle();
        if (handle == null || handle.isEmpty()) {
            // Fallback to username if handle is not set
            String username = user.getUsername();
            if (username == null || username.isEmpty()) {
                username = displayName;
            }
            handle = username;
        }
        binding.tvUsername.setText("@" + handle);
        
        // Load profile image using Glide
        String photoURL = user.getPhotoURL();
        Log.d("ProfileFragment", "Loading profile picture. PhotoURL: " + photoURL);
        
        if (photoURL != null && !photoURL.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(photoURL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfilePicture);
        } else {
            // Set default profile picture
            Log.d("ProfileFragment", "No photo URL, setting default profile picture");
            binding.ivProfilePicture.setImageResource(R.drawable.ic_person);
        }
        
        // Update study info with fallbacks
        if (user.getProfile() != null) {
            String studyYear = user.getProfile().getStudyYear();
            String specialization = user.getProfile().getSpecialization();
            String institution = user.getProfile().getInstitution();
            String bio = user.getProfile().getBio();
            
            binding.tvStudyYear.setText(studyYear != null && !studyYear.isEmpty() ? studyYear : "Not specified");
            binding.tvSpecialization.setText(specialization != null && !specialization.isEmpty() ? specialization : "Not specified");
            binding.tvInstitution.setText(institution != null && !institution.isEmpty() ? institution : "Not specified");
            binding.tvBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio available");
            
            // Set GPA (you can add this field to your User model if needed)
            // For now, we'll set a default GPA or calculate it from achievements
            binding.tvGPA.setText("4.2"); // Default GPA, you can make this dynamic
        } else {
            // Set default values if profile is null
            binding.tvStudyYear.setText("Not specified");
            binding.tvSpecialization.setText("Not specified");
            binding.tvInstitution.setText("Not specified");
            binding.tvBio.setText("No bio available");
            binding.tvGPA.setText("0.0");
        }
    }

    private void loadFollowCounts(String userId) {
        followViewModel.loadFollowers(userId);
        followViewModel.loadFollowing(userId);
    }
    
    private void loadUserAchievements(String userId) {
        studyMaterialRepository.calculateUserAchievements(userId, new StudyMaterialRepository.UserAchievementsCallback() {
            @Override
            public void onSuccess(StudyMaterialRepository.UserAchievements achievements) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateAchievementsUI(achievements);
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to load achievements: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
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
            Intent intent = new Intent(getActivity(), UserMaterialsActivity.class);
            intent.putExtra("user_id", authViewModel.getCurrentUser().getValue().getUid());
            intent.putExtra("user_name", authViewModel.getCurrentUser().getValue().getDisplayName());
            startActivity(intent);
        });
        
        // Total Downloads - Open DownloadedMaterialsActivity
        binding.tvDownloadsCount.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DownloadedMaterialsActivity.class);
            intent.putExtra("user_id", authViewModel.getCurrentUser().getValue().getUid());
            intent.putExtra("user_name", authViewModel.getCurrentUser().getValue().getDisplayName());
            startActivity(intent);
        });
        
        // Average Rating - Open CommentsActivity (for all user's materials)
        binding.tvAverageRating.setOnClickListener(v -> {
            // For now, we'll show a toast. In a full implementation, 
            // you might want to show all comments for user's materials
            Toast.makeText(getContext(), "Rating details - showing comments for user's materials", Toast.LENGTH_SHORT).show();
        });
        
        // Total Likes - Could open a similar activity showing liked materials
        binding.tvTotalLikes.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Likes details - showing user's liked materials", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void showMenuDialog() {
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        String currentTheme = themeManager.getThemeModeDisplayName(themeManager.getThemeMode());
        String[] menuItems = {"Theme: " + currentTheme, "Settings", "About", "Help", "FAQ", "Sign Out"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Menu");
        builder.setItems(menuItems, (dialog, which) -> {
            switch (which) {
                case 0: // Theme
                    showThemeDialog();
                    break;
                case 1: // Settings
                    openSettings();
                    break;
                case 2: // About
                    openAbout();
                    break;
                case 3: // Help
                    openHelp();
                    break;
                case 4: // FAQ
                    openFAQ();
                    break;
                case 5: // Sign Out
                    signOut();
                    break;
            }
        });
        builder.show();
    }
    
    private void editProfile() {
        // Navigate to edit profile screen
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE);
    }
    
    private void viewUserProfile() {
        // Navigate to current user's profile view (same as UserProfileActivity but for current user)
        String currentUserId = authViewModel.getCurrentUser().getValue().getUid();
        String currentUserName = authViewModel.getCurrentUser().getValue().getDisplayName();
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        intent.putExtra("user_id", currentUserId);
        intent.putExtra("user_name", currentUserName);
        startActivity(intent);
    }
    
    private void showThemeDialog() {
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        String[] themeOptions = {"Light", "Dark", "System Default"};
        int currentTheme = themeManager.getThemeMode();

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Theme");
        builder.setSingleChoiceItems(themeOptions, currentTheme, (dialog, which) -> {
            themeManager.setThemeMode(which);
            dialog.dismiss();

            // Recreate activity to apply theme immediately
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openSettings() {
        // TODO: Navigate to settings screen
        Toast.makeText(getContext(), "Settings coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openMyMaterials() {
        // TODO: Navigate to my materials screen
        Toast.makeText(getContext(), "My materials coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openFavorites() {
        // Navigate to favorites screen
        Intent intent = new Intent(getActivity(), com.example.nurse_connect.ui.favorites.FavoritesActivity.class);
        startActivity(intent);
    }

    private void openFollowers() {
        // Navigate to followers screen
        String currentUserId = authViewModel.getCurrentUser().getValue().getUid();
        String currentUserName = authViewModel.getCurrentUser().getValue().getDisplayName();
        Intent intent = new Intent(getActivity(), FollowersActivity.class);
        intent.putExtra("user_id", currentUserId);
        intent.putExtra("user_name", currentUserName);
        startActivity(intent);
    }

    private void openFollowing() {
        // Navigate to following screen
        String currentUserId = authViewModel.getCurrentUser().getValue().getUid();
        String currentUserName = authViewModel.getCurrentUser().getValue().getDisplayName();
        Intent intent = new Intent(getActivity(), FollowingActivity.class);
        intent.putExtra("user_id", currentUserId);
        intent.putExtra("user_name", currentUserName);
        startActivity(intent);
    }
    
    private void openStudyGroups() {
        // TODO: Navigate to study groups screen
        Toast.makeText(getContext(), "Study groups coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openHelp() {
        // TODO: Navigate to help screen
        Toast.makeText(getContext(), "Help & support coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openAbout() {
        // TODO: Navigate to about screen
        Toast.makeText(getContext(), "About coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openFAQ() {
        // TODO: Navigate to FAQ screen
        Toast.makeText(getContext(), "FAQ coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showProfilePreview() {
        User currentUser = authViewModel.getCurrentUser().getValue();
        if (currentUser != null) {
            ProfilePreviewDialog.showProfilePreview(requireContext(), currentUser);
        }
    }
    
    private void toggleAchievements() {
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
    
    private void signOut() {
        authViewModel.signOut();
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            // Profile was updated successfully, refresh the UI
            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            // Force refresh the current user data to ensure UI reflects the latest changes
            if (authViewModel.getCurrentUser().getValue() != null) {
                String userId = authViewModel.getCurrentUser().getValue().getUid();
                authViewModel.getAuthRepository().getUserProfile(userId, new AuthRepository.UserCallback() {
                    @Override
                    public void onSuccess(com.example.nurse_connect.models.User user) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                updateProfileUI(user);
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("ProfileFragment", "Failed to refresh user data", e);
                    }
                });
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh achievements when returning to the profile
        if (authViewModel.getCurrentUser().getValue() != null) {
            loadUserAchievements(authViewModel.getCurrentUser().getValue().getUid());
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 