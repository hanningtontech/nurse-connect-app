package com.example.nurse_connect.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.data.FavoritesRepository;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.viewmodels.AuthViewModel;

import java.util.List;

public class StudyMaterialViewModel extends ViewModel {
    private StudyMaterialRepository repository;
    private AuthViewModel authViewModel;
    
    private MutableLiveData<List<StudyMaterial>> studyMaterials;
    private MutableLiveData<List<StudyMaterial>> userMaterials;
    private MutableLiveData<List<StudyMaterial>> downloadedMaterials;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Double> uploadProgress;
    private MutableLiveData<StudyMaterial> uploadedMaterial;
    
    public StudyMaterialViewModel() {
        repository = new StudyMaterialRepository();
        studyMaterials = new MutableLiveData<>();
        userMaterials = new MutableLiveData<>();
        downloadedMaterials = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        uploadProgress = new MutableLiveData<>();
        uploadedMaterial = new MutableLiveData<>();
        uploadSuccess = new MutableLiveData<>();
        uploadError = new MutableLiveData<>();
    }
    
    public LiveData<List<StudyMaterial>> getStudyMaterials() { return studyMaterials; }
    public LiveData<List<StudyMaterial>> getUserMaterials() { return userMaterials; }
    public LiveData<List<StudyMaterial>> getDownloadedMaterials() { return downloadedMaterials; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Double> getUploadProgress() { return uploadProgress; }
    public LiveData<StudyMaterial> getUploadedMaterial() { return uploadedMaterial; }
    
    // Add missing methods for upload success and error
    private MutableLiveData<Boolean> uploadSuccess;
    private MutableLiveData<String> uploadError;
    
    public LiveData<Boolean> getUploadSuccess() { return uploadSuccess; }
    public LiveData<String> getUploadError() { return uploadError; }
    
    // Load all study materials
    public void loadStudyMaterials() {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.getAllStudyMaterials(new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                studyMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Load study materials by category
    public void loadStudyMaterialsByCategory(String category) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.getStudyMaterialsByCategory(category, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                studyMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Load study materials by user
    public void loadStudyMaterialsByUser(String userId) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.getStudyMaterialsByUser(userId, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                studyMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Load downloaded materials for current user
    public void loadDownloadedMaterials() {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        String currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            // For now, we'll load all materials and filter for downloaded ones
            // In the future, we can implement a proper downloaded materials query
            repository.getAllStudyMaterials(new StudyMaterialRepository.StudyMaterialCallback() {
                @Override
                public void onSuccess(List<StudyMaterial> materials) {
                    // Filter materials that have been downloaded by the current user
                    // This is a placeholder - you'll need to implement proper download tracking
                    studyMaterials.postValue(materials);
                    isLoading.postValue(false);
                }
                
                @Override
                public void onFailure(Exception e) {
                    errorMessage.postValue(e.getMessage());
                    isLoading.postValue(false);
                }
            });
        } else {
            errorMessage.postValue("User not authenticated");
            isLoading.postValue(false);
        }
    }
    
    // Upload study material (legacy method)
    public void uploadStudyMaterial(android.content.Context context, Uri fileUri, String title, String description, 
                                  String category, String privacy) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        uploadProgress.postValue(0.0);
        uploadSuccess.postValue(false);
        uploadError.postValue(null);
        
        // Get current user information
        String currentUserId = getCurrentUserId();
        String currentUserName = getCurrentUserName();
        
        if (currentUserId == null || currentUserName == null) {
            errorMessage.postValue("User not authenticated");
            uploadError.postValue("User not authenticated");
            isLoading.postValue(false);
            return;
        }
        
        repository.uploadStudyMaterial(context, fileUri, title, description, category, 
                currentUserId, currentUserName, privacy, new StudyMaterialRepository.UploadCallback() {
            @Override
            public void onSuccess(StudyMaterial material) {
                uploadedMaterial.postValue(material);
                uploadSuccess.postValue(true);
                isLoading.postValue(false);
                uploadProgress.postValue(100.0);
                // Reload materials to show the new upload
                loadStudyMaterials();
            }
            
            @Override
            public void onProgress(double progress) {
                uploadProgress.postValue(progress);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                uploadError.postValue(e.getMessage());
                isLoading.postValue(false);
                uploadProgress.postValue(0.0);
            }
        });
    }
    
    // Upload study material (new method with explicit author parameters)
    public void uploadStudyMaterial(android.content.Context context, Uri fileUri, String title, String description, 
                                  String category, String authorId, String authorName, String privacy) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        uploadProgress.postValue(0.0);
        uploadSuccess.postValue(false);
        uploadError.postValue(null);
        
        if (authorId == null || authorName == null) {
            errorMessage.postValue("Author information is required");
            uploadError.postValue("Author information is required");
            isLoading.postValue(false);
            return;
        }
        
        repository.uploadStudyMaterial(context, fileUri, title, description, category, 
                authorId, authorName, privacy, new StudyMaterialRepository.UploadCallback() {
            @Override
            public void onSuccess(StudyMaterial material) {
                uploadedMaterial.postValue(material);
                uploadSuccess.postValue(true);
                isLoading.postValue(false);
                uploadProgress.postValue(100.0);
                // Reload materials to show the new upload
                loadStudyMaterials();
            }
            
            @Override
            public void onProgress(double progress) {
                uploadProgress.postValue(progress);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                uploadError.postValue(e.getMessage());
                isLoading.postValue(false);
                uploadProgress.postValue(0.0);
            }
        });
    }
    
    // Get current user ID
    public String getCurrentUserId() {
        if (authViewModel != null && authViewModel.getCurrentUser().getValue() != null) {
            return authViewModel.getCurrentUser().getValue().getUid();
        }
        return null;
    }
    
    // Get current user name
    private String getCurrentUserName() {
        if (authViewModel != null && authViewModel.getCurrentUser().getValue() != null) {
            return authViewModel.getCurrentUser().getValue().getDisplayName();
        }
        return null;
    }
    
    // Set AuthViewModel for getting current user info
    public void setAuthViewModel(AuthViewModel authViewModel) {
        this.authViewModel = authViewModel;
    }
    
    // Download study material
    public void downloadStudyMaterial(String fileUrl, StudyMaterialRepository.DownloadCallback callback) {
        repository.downloadStudyMaterial(fileUrl, callback);
    }
    
    // Increment download count
    public void incrementDownloadCount(String materialId) {
        repository.incrementDownloadCount(materialId);
    }
    
    // Increment download count with callback
    public void incrementDownloadCount(String materialId, StudyMaterialRepository.IncrementCallback callback) {
        repository.incrementDownloadCount(materialId, callback);
    }
    
    // Toggle like
    public void toggleLike(String materialId, String userId, boolean isLiked) {
        repository.toggleLike(materialId, userId, isLiked);
        
        // Refresh the counts after toggling like
        refreshMaterialCounts(materialId);
    }
    
    // Refresh material counts
    public void refreshMaterialCounts(String materialId) {
        repository.getMaterialCounts(materialId, new StudyMaterialRepository.MaterialCountsCallback() {
            @Override
            public void onSuccess(int likes, int commentCount) {
                // Update the material in the current list with new counts
                updateMaterialCounts(materialId, likes, commentCount);
            }
            
            @Override
            public void onFailure(Exception e) {
                // Log error but don't show to user
                android.util.Log.e("StudyMaterialViewModel", "Error refreshing counts", e);
            }
        });
    }
    
    // Update material counts in the current list
    private void updateMaterialCounts(String materialId, int likes, int commentCount) {
        List<StudyMaterial> currentMaterials = studyMaterials.getValue();
        if (currentMaterials != null) {
            for (StudyMaterial material : currentMaterials) {
                if (material.getId().equals(materialId)) {
                    material.setLikes(likes);
                    material.setCommentCount(commentCount);
                    break;
                }
            }
            studyMaterials.postValue(currentMaterials);
        }
        
        // Also update in user materials if they exist
        List<StudyMaterial> currentUserMaterials = userMaterials.getValue();
        if (currentUserMaterials != null) {
            for (StudyMaterial material : currentUserMaterials) {
                if (material.getId().equals(materialId)) {
                    material.setLikes(likes);
                    material.setCommentCount(commentCount);
                    break;
                }
            }
            userMaterials.postValue(currentUserMaterials);
        }
    }
    
    // Toggle favorite (add/remove from user favorites)
    public void toggleFavorite(String materialId) {
        FavoritesRepository favoritesRepository = new FavoritesRepository();
        favoritesRepository.toggleFavorite(materialId, new FavoritesRepository.ToggleFavoriteCallback() {
            @Override
            public void onSuccess(boolean isFavorite) {
                // Update the UI to reflect the new favorite state
                // You can emit a success message or update the material list
                String message = isFavorite ? "Added to favorites" : "Removed from favorites";
                // You could add a success message LiveData if needed
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to update favorite: " + e.getMessage());
            }
        });
    }
    
    // Add material to favorites
    public void addToFavorites(StudyMaterial material) {
        // TODO: Implement when user authentication is ready
        // This will add the material to the user's private favorites collection
    }
    
    // Remove material from favorites
    public void removeFromFavorites(String materialId) {
        // TODO: Implement when user authentication is ready
        // This will remove the material from the user's private favorites collection
    }
    
    // Check if material is in user's favorites
    public boolean isFavorite(String materialId) {
        // TODO: Implement when user authentication is ready
        // This will check if the material is in the user's favorites
        return false;
    }
    
    // Search study materials
    public void searchStudyMaterials(String query) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.searchStudyMaterials(query, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                studyMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Search study materials within a specific category
    public void searchStudyMaterialsByCategory(String query, String category) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.searchStudyMaterialsByCategory(query, category, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                studyMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Search user by username
    public void searchUserByUsername(String username, UserSearchCallback callback) {
        repository.searchUserByUsername(username, new StudyMaterialRepository.UserSearchCallback() {
            @Override
            public void onSuccess(com.example.nurse_connect.models.User user) {
                callback.onSuccess(user);
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    
    public interface UserSearchCallback {
        void onSuccess(com.example.nurse_connect.models.User user);
        void onFailure(Exception e);
    }
    
    // Delete study material
    public void deleteStudyMaterial(String materialId, String fileName) {
        repository.deleteStudyMaterial(materialId, fileName, () -> {
            // Reload materials after deletion
            loadStudyMaterials();
        });
    }
    
    // Clear error message
    public void clearError() {
        errorMessage.postValue(null);
    }
    
    // Clear upload progress
    public void clearUploadProgress() {
        uploadProgress.postValue(0.0);
    }
    
    // Load user materials for UserMaterialsActivity
    public void loadUserMaterials(String userId) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.getStudyMaterialsByUser(userId, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                userMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Load downloaded materials for DownloadedMaterialsActivity
    public void loadDownloadedMaterials(String userId) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.getDownloadedMaterialsByUser(userId, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                downloadedMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Sorting methods for downloaded materials
    public void sortByMostDownloaded() {
        List<StudyMaterial> currentList = downloadedMaterials.getValue();
        if (currentList != null) {
            currentList.sort((a, b) -> Integer.compare(b.getDownloads(), a.getDownloads()));
            downloadedMaterials.postValue(currentList);
        }
    }
    
    public void sortByLeastDownloaded() {
        List<StudyMaterial> currentList = downloadedMaterials.getValue();
        if (currentList != null) {
            currentList.sort((a, b) -> Integer.compare(a.getDownloads(), b.getDownloads()));
            downloadedMaterials.postValue(currentList);
        }
    }
    
    public void sortByLatestDownloaded() {
        List<StudyMaterial> currentList = downloadedMaterials.getValue();
        if (currentList != null) {
            currentList.sort((a, b) -> Long.compare(b.getUploadDate(), a.getUploadDate()));
            downloadedMaterials.postValue(currentList);
        }
    }
    
    public void sortByOldestDownloaded() {
        List<StudyMaterial> materials = downloadedMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> {
                if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
                if (m1.getCreatedAt() == null) return 1;
                if (m2.getCreatedAt() == null) return -1;
                return m1.getCreatedAt().compareTo(m2.getCreatedAt());
            });
            downloadedMaterials.postValue(materials);
        }
    }
    
    // Search user materials
    public void searchUserMaterials(String userId, String query) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        repository.searchStudyMaterialsByUser(userId, query, new StudyMaterialRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> materials) {
                userMaterials.postValue(materials);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    // Sorting methods for user materials
    public void sortByMostViewed() {
        List<StudyMaterial> materials = userMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> Integer.compare(m2.getViews(), m1.getViews()));
            userMaterials.postValue(materials);
        }
    }
    
    public void sortByLeastViewed() {
        List<StudyMaterial> materials = userMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> Integer.compare(m1.getViews(), m2.getViews()));
            userMaterials.postValue(materials);
        }
    }
    
    public void sortUserMaterialsByMostDownloaded() {
        List<StudyMaterial> materials = userMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> Integer.compare(m2.getDownloads(), m1.getDownloads()));
            userMaterials.postValue(materials);
        }
    }
    
    public void sortUserMaterialsByLeastDownloaded() {
        List<StudyMaterial> materials = userMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> Integer.compare(m1.getDownloads(), m2.getDownloads()));
            userMaterials.postValue(materials);
        }
    }
    
    public void sortByLatestUploaded() {
        List<StudyMaterial> materials = userMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> {
                if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
                if (m1.getCreatedAt() == null) return 1;
                if (m2.getCreatedAt() == null) return -1;
                return m2.getCreatedAt().compareTo(m1.getCreatedAt()); // Latest first
            });
            userMaterials.postValue(materials);
        }
    }
    
    public void sortByOldestUploaded() {
        List<StudyMaterial> materials = userMaterials.getValue();
        if (materials != null) {
            materials.sort((m1, m2) -> {
                if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
                if (m1.getCreatedAt() == null) return 1;
                if (m2.getCreatedAt() == null) return -1;
                return m1.getCreatedAt().compareTo(m2.getCreatedAt()); // Oldest first
            });
            userMaterials.postValue(materials);
        }
    }
} 