package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.nurse_connect.data.UserFollowRepository;
import com.example.nurse_connect.models.User;
import java.util.List;

public class UserFollowViewModel extends ViewModel {
    private UserFollowRepository repository;
    private MutableLiveData<List<User>> followers;
    private MutableLiveData<List<User>> following;
    private MutableLiveData<Boolean> isFollowing;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;

    public UserFollowViewModel() {
        repository = new UserFollowRepository();
        followers = new MutableLiveData<>();
        following = new MutableLiveData<>();
        isFollowing = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
    }

    public LiveData<List<User>> getFollowers() { return followers; }
    public LiveData<List<User>> getFollowing() { return following; }
    public LiveData<Boolean> getIsFollowing() { return isFollowing; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadFollowers(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.getFollowers(userId, new UserFollowRepository.FollowersCallback() {
            @Override
            public void onSuccess(List<User> followersList) {
                followers.setValue(followersList);
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to load followers: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void loadFollowing(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.getFollowing(userId, new UserFollowRepository.FollowingCallback() {
            @Override
            public void onSuccess(List<User> followingList) {
                following.setValue(followingList);
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to load following: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void checkIsFollowing(String targetUserId) {
        repository.isFollowing(targetUserId, new UserFollowRepository.IsFollowingCallback() {
            @Override
            public void onSuccess(boolean following) {
                isFollowing.setValue(following);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to check follow status: " + e.getMessage());
            }
        });
    }

    public void followUser(String targetUserId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.followUser(targetUserId, new UserFollowRepository.FollowCallback() {
            @Override
            public void onSuccess() {
                isFollowing.setValue(true);
                isLoading.setValue(false);
                // Add success message
                errorMessage.setValue("Successfully followed user!");
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to follow user: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void unfollowUser(String targetUserId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        repository.unfollowUser(targetUserId, new UserFollowRepository.FollowCallback() {
            @Override
            public void onSuccess() {
                isFollowing.setValue(false);
                isLoading.setValue(false);
                // Add success message
                errorMessage.setValue("Successfully unfollowed user!");
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue("Failed to unfollow user: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void toggleFollow(String targetUserId) {
        Boolean currentFollowing = isFollowing.getValue();
        if (currentFollowing != null && currentFollowing) {
            unfollowUser(targetUserId);
        } else {
            followUser(targetUserId);
        }
    }

    public void clearError() {
        errorMessage.setValue(null);
    }
} 