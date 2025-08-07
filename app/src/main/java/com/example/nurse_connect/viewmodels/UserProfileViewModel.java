package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.AuthRepository;
import com.example.nurse_connect.models.User;

public class UserProfileViewModel extends ViewModel {
    private AuthRepository authRepository;
    
    private MutableLiveData<User> userProfile;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    
    public UserProfileViewModel() {
        authRepository = new AuthRepository();
        userProfile = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
    }
    
    public LiveData<User> getUserProfile() { return userProfile; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    public void loadUserProfile(String userId) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        authRepository.getUserProfile(userId, new AuthRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                userProfile.postValue(user);
                isLoading.postValue(false);
            }
            
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    public void clearError() {
        errorMessage.postValue(null);
    }
} 