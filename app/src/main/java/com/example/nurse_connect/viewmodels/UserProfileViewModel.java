package com.example.nurse_connect.viewmodels;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.UserProfileRepository;
import com.example.nurse_connect.models.User;

import java.util.List;
import java.util.Map;

/**
 * ViewModel for managing user profile operations
 * Provides clean interface for UI layer and handles business logic
 */
public class UserProfileViewModel extends ViewModel {
    private static final String TAG = "UserProfileViewModel";
    
    private UserProfileRepository repository;
    
    // LiveData for profile operations
    private MutableLiveData<User> currentUserProfile;
    private MutableLiveData<User> viewedUserProfile;
    private MutableLiveData<List<User>> searchResults;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Boolean> isProfileUpdated;
    
    public UserProfileViewModel() {
        currentUserProfile = new MutableLiveData<>();
        viewedUserProfile = new MutableLiveData<>();
        searchResults = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        isProfileUpdated = new MutableLiveData<>();
    }
    
    public void initialize(Context context) {
        if (repository == null) {
            repository = new UserProfileRepository(context);
        }
    }
    
    // LiveData getters
    public LiveData<User> getCurrentUserProfile() { return currentUserProfile; }
    public LiveData<User> getViewedUserProfile() { return viewedUserProfile; }
    public LiveData<List<User>> getSearchResults() { return searchResults; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsProfileUpdated() { return isProfileUpdated; }
    
    /**
     * Load current user's profile
     */
    public void loadCurrentUserProfile(String userId) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            errorMessage.setValue("Invalid user ID");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.getUserProfile(userId, new UserProfileRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserProfile.postValue(user);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Load another user's profile for viewing
     */
    public void loadUserProfile(String userId) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            errorMessage.setValue("Invalid user ID");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.getUserProfile(userId, new UserProfileRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                viewedUserProfile.postValue(user);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Update user profile
     */
    public void updateUserProfile(User user) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (user == null || user.getUid() == null) {
            errorMessage.setValue("Invalid user data");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.updateUserProfile(user, new UserProfileRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User updatedUser) {
                currentUserProfile.postValue(updatedUser);
                isProfileUpdated.postValue(true);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Update specific profile fields
     */
    public void updateProfileFields(String userId, Map<String, Object> updates) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (userId == null || updates == null || updates.isEmpty()) {
            errorMessage.setValue("Invalid update data");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.updateProfileFields(userId, updates, new UserProfileRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User updatedUser) {
                currentUserProfile.postValue(updatedUser);
                isProfileUpdated.postValue(true);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Upload profile picture
     */
    public void uploadProfilePicture(String userId, Uri imageUri) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (userId == null || imageUri == null) {
            errorMessage.setValue("Invalid parameters");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.uploadProfilePicture(userId, imageUri, new UserProfileRepository.ProfilePictureCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Update the user's profile with the new photo URL
                Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("photoURL", downloadUrl);
                updateProfileFields(userId, updates);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to upload profile picture: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Delete profile picture
     */
    public void deleteProfilePicture(String userId, String photoURL) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (userId == null || photoURL == null) {
            errorMessage.setValue("Invalid parameters");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.deleteProfilePicture(userId, photoURL, new UserProfileRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User updatedUser) {
                currentUserProfile.postValue(updatedUser);
                isProfileUpdated.postValue(true);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to delete profile picture: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Search users
     */
    public void searchUsers(String query) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(new java.util.ArrayList<>());
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.searchUsers(query, new UserProfileRepository.UserSearchCallback() {
            @Override
            public void onSuccess(List<User> users) {
                searchResults.postValue(users);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Get multiple user profiles
     */
    public void getMultipleUserProfiles(List<String> userIds) {
        if (repository == null) {
            Log.e(TAG, "Repository not initialized");
            return;
        }
        
        if (userIds == null || userIds.isEmpty()) {
            searchResults.setValue(new java.util.ArrayList<>());
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.getMultipleUserProfiles(userIds, new UserProfileRepository.MultipleUserProfileCallback() {
            @Override
            public void onSuccess(List<User> users) {
                searchResults.postValue(users);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Check if current user can edit a profile
     */
    public boolean canEditProfile(String userId) {
        return repository != null && repository.canEditProfile(userId);
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    /**
     * Clear profile updated flag
     */
    public void clearProfileUpdated() {
        isProfileUpdated.setValue(false);
    }
    
    /**
     * Clear cache for a specific user
     */
    public void clearCache(String userId) {
        if (repository != null) {
            repository.clearCache(userId);
        }
    }
    
    /**
     * Clear all cache
     */
    public void clearAllCache() {
        if (repository != null) {
            repository.clearAllCache();
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources if needed
        if (repository != null) {
            repository.clearAllCache();
        }
    }
} 