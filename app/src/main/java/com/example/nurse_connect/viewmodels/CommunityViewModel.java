package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.CommunityRepository;
import com.example.nurse_connect.models.User;

import java.util.List;

public class CommunityViewModel extends ViewModel {
    
    private CommunityRepository repository;
    private MutableLiveData<List<User>> featuredNurses;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    
    public CommunityViewModel() {
        repository = new CommunityRepository();
        featuredNurses = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
    }
    
    public LiveData<List<User>> getFeaturedNurses() {
        return featuredNurses;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Load featured nurses for the community hub
     */
    public void loadFeaturedNurses() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.loadFeaturedNurses(new CommunityRepository.CommunityCallback() {
            @Override
            public void onSuccess(List<User> nurses) {
                featuredNurses.setValue(nurses);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    /**
     * Load nurses by specialty
     */
    public void loadNursesBySpecialty(String specialty) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.loadNursesBySpecialty(specialty, new CommunityRepository.CommunityCallback() {
            @Override
            public void onSuccess(List<User> nurses) {
                featuredNurses.setValue(nurses);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    /**
     * Load nurses by institution
     */
    public void loadNursesByInstitution(String institution) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.loadNursesByInstitution(institution, new CommunityRepository.CommunityCallback() {
            @Override
            public void onSuccess(List<User> nurses) {
                featuredNurses.setValue(nurses);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
    
    /**
     * Search nurses
     */
    public void searchNurses(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadFeaturedNurses();
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.searchNurses(query.trim(), new CommunityRepository.CommunityCallback() {
            @Override
            public void onSuccess(List<User> nurses) {
                featuredNurses.setValue(nurses);
                isLoading.setValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }
} 