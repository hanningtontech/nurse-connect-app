package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.RatingRepository;
import com.example.nurse_connect.models.DocumentRating;
import com.example.nurse_connect.models.RatingStats;
import com.example.nurse_connect.models.StudyMaterial;

public class RatingViewModel extends ViewModel {
    
    private RatingRepository repository;
    
    private MutableLiveData<StudyMaterial> studyMaterial;
    private MutableLiveData<RatingStats> ratingStats;
    private MutableLiveData<DocumentRating> userRating;
    private MutableLiveData<Boolean> submissionResult;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Boolean> isLoading;
    
    public RatingViewModel() {
        repository = new RatingRepository();
        studyMaterial = new MutableLiveData<>();
        ratingStats = new MutableLiveData<>();
        userRating = new MutableLiveData<>();
        submissionResult = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
    }
    
    public LiveData<StudyMaterial> getStudyMaterial() { return studyMaterial; }
    public LiveData<RatingStats> getRatingStats() { return ratingStats; }
    public LiveData<DocumentRating> getUserRating() { return userRating; }
    public LiveData<Boolean> getSubmissionResult() { return submissionResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    
    public void loadRatingData(String materialId) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        // Load study material
        repository.getStudyMaterial(materialId, new RatingRepository.StudyMaterialCallback() {
            @Override
            public void onSuccess(StudyMaterial material) {
                studyMaterial.postValue(material);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to load document: " + e.getMessage());
            }
        });
        
        // Load rating statistics
        repository.getRatingStats(materialId, new RatingRepository.RatingStatsCallback() {
            @Override
            public void onSuccess(RatingStats stats) {
                ratingStats.postValue(stats);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to load rating statistics: " + e.getMessage());
            }
        });
        
        // Load user's existing rating
        repository.getUserRating(materialId, new RatingRepository.UserRatingCallback() {
            @Override
            public void onSuccess(DocumentRating rating) {
                userRating.postValue(rating);
            }
            
            @Override
            public void onFailure(Exception e) {
                // User hasn't rated yet, this is normal
                userRating.postValue(null);
            }
        });
        
        isLoading.postValue(false);
    }
    
    public void submitRating(int rating, String comment) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        String materialId = studyMaterial.getValue() != null ? studyMaterial.getValue().getId() : null;
        if (materialId == null) {
            errorMessage.postValue("Document not loaded");
            isLoading.postValue(false);
            return;
        }
        
        repository.submitRating(materialId, rating, comment, new RatingRepository.SubmissionCallback() {
            @Override
            public void onSuccess() {
                submissionResult.postValue(true);
                // Reload rating data to update statistics
                loadRatingData(studyMaterial.getValue().getId());
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Failed to submit rating: " + e.getMessage());
                submissionResult.postValue(false);
            }
        });
        
        isLoading.postValue(false);
    }
    
    public void clearError() {
        errorMessage.postValue(null);
    }
    
    public void clearSubmissionResult() {
        submissionResult.postValue(null);
    }
} 