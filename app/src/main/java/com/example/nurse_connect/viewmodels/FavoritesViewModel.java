package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.FavoritesRepository;
import com.example.nurse_connect.models.StudyMaterial;

import java.util.ArrayList;
import java.util.List;

public class FavoritesViewModel extends ViewModel {
    
    private FavoritesRepository repository;
    private MutableLiveData<List<StudyMaterial>> favorites;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    
    public FavoritesViewModel() {
        repository = new FavoritesRepository();
        favorites = new MutableLiveData<>(new ArrayList<>());
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
    }
    
    public LiveData<List<StudyMaterial>> getFavorites() {
        return favorites;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public void loadFavorites() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.getUserFavorites(new FavoritesRepository.FavoritesCallback() {
            @Override
            public void onSuccess(List<StudyMaterial> favoritesList) {
                favorites.setValue(favoritesList);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to load favorites: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    public void toggleFavorite(String materialId) {
        repository.toggleFavorite(materialId, new FavoritesRepository.ToggleFavoriteCallback() {
            @Override
            public void onSuccess(boolean isFavorite) {
                // Reload favorites to get updated list
                loadFavorites();
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to update favorite: " + e.getMessage());
            }
        });
    }
    
    public void checkIfFavorite(String materialId, FavoritesRepository.CheckFavoriteCallback callback) {
        repository.checkIfFavorite(materialId, callback);
    }
    
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    // Increment download count with callback
    public void incrementDownloadCount(String materialId, com.example.nurse_connect.data.StudyMaterialRepository.IncrementCallback callback) {
        com.example.nurse_connect.data.StudyMaterialRepository repository = new com.example.nurse_connect.data.StudyMaterialRepository();
        repository.incrementDownloadCount(materialId, callback);
    }
} 