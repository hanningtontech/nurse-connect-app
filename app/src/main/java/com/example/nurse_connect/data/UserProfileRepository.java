package com.example.nurse_connect.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.nurse_connect.models.User;
import com.example.nurse_connect.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for managing user profile operations
 * Handles Firestore operations, caching, and profile updates
 */
public class UserProfileRepository {
    private static final String TAG = "UserProfileRepository";
    private static final String PREFS_NAME = "UserProfilePrefs";
    private static final String KEY_CACHED_PROFILE = "cached_profile_";
    private static final String KEY_CACHED_PROFILE_TIMESTAMP = "cached_profile_timestamp_";
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;
    private final Executor executor;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    public UserProfileRepository(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Fetch user profile from Firestore
     */
    public void getUserProfile(String userId, UserProfileCallback callback) {
        // First check cache
        User cachedUser = getCachedProfile(userId);
        if (cachedUser != null && isCacheValid(userId)) {
            callback.onSuccess(cachedUser);
            return;
        }
        
        // Fetch from Firestore
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(executor, documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Cache the profile
                            cacheProfile(userId, user);
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("Failed to parse user data"));
                        }
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(executor, callback::onFailure);
    }
    
    /**
     * Update user profile in Firestore
     */
    public void updateUserProfile(User user, UserProfileCallback callback) {
        if (user == null || user.getUid() == null) {
            callback.onFailure(new Exception("Invalid user data"));
            return;
        }
        
        // Update timestamp
        user.setUpdatedAt(com.google.firebase.Timestamp.now());
        
        firestore.collection("users").document(user.getUid())
                .set(user)
                .addOnSuccessListener(executor, aVoid -> {
                    // Update cache
                    cacheProfile(user.getUid(), user);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(executor, callback::onFailure);
    }
    
    /**
     * Update specific profile fields
     */
    public void updateProfileFields(String userId, java.util.Map<String, Object> updates, UserProfileCallback callback) {
        if (userId == null || updates == null || updates.isEmpty()) {
            callback.onFailure(new Exception("Invalid update data"));
            return;
        }
        
        // Add timestamp
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        
        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(executor, aVoid -> {
                    // Refresh profile from Firestore to get updated data
                    getUserProfile(userId, callback);
                })
                .addOnFailureListener(executor, callback::onFailure);
    }
    
    /**
     * Upload profile picture to Firebase Storage
     */
    public void uploadProfilePicture(String userId, android.net.Uri imageUri, ProfilePictureCallback callback) {
        if (userId == null || imageUri == null) {
            callback.onFailure(new Exception("Invalid parameters"));
            return;
        }
        
        StorageReference storageRef = storage.getReference()
                .child("profile_pictures")
                .child(userId)
                .child("profile_" + System.currentTimeMillis() + ".jpg");
        
        storageRef.putFile(imageUri)
                .addOnSuccessListener(executor, taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(executor, downloadUri -> {
                                callback.onSuccess(downloadUri.toString());
                            })
                            .addOnFailureListener(executor, callback::onFailure);
                })
                .addOnFailureListener(executor, callback::onFailure);
    }
    
    /**
     * Delete profile picture from Firebase Storage
     */
    public void deleteProfilePicture(String userId, String photoURL, UserProfileCallback callback) {
        if (userId == null || photoURL == null) {
            callback.onFailure(new Exception("Invalid parameters"));
            return;
        }
        
        try {
            StorageReference storageRef = storage.getReferenceFromUrl(photoURL);
            storageRef.delete()
                    .addOnSuccessListener(executor, aVoid -> {
                        // Update user profile to remove photo URL
                        java.util.Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("photoURL", "");
                        updateProfileFields(userId, updates, callback);
                    })
                    .addOnFailureListener(executor, callback::onFailure);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
    
    /**
     * Search users by username or display name
     */
    public void searchUsers(String query, UserSearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuccess(new java.util.ArrayList<>());
            return;
        }
        
        String searchQuery = query.trim().toLowerCase();
        
        // Search by username (exact match)
        firestore.collection("users")
                .whereGreaterThanOrEqualTo("username", searchQuery)
                .whereLessThanOrEqualTo("username", searchQuery + '\uf8ff')
                .limit(20)
                .get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    java.util.List<User> users = new java.util.ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(executor, callback::onFailure);
    }
    
    /**
     * Get multiple user profiles by IDs
     */
    public void getMultipleUserProfiles(java.util.List<String> userIds, MultipleUserProfileCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onSuccess(new java.util.ArrayList<>());
            return;
        }
        
        // Remove duplicates and null values
        java.util.List<String> uniqueIds = new java.util.ArrayList<>();
        for (String id : userIds) {
            if (id != null && !uniqueIds.contains(id)) {
                uniqueIds.add(id);
            }
        }
        
        if (uniqueIds.isEmpty()) {
            callback.onSuccess(new java.util.ArrayList<>());
            return;
        }
        
        // Firestore can only query up to 10 documents at once
        if (uniqueIds.size() <= 10) {
            firestore.collection("users")
                    .whereIn("uid", uniqueIds)
                    .get()
                    .addOnSuccessListener(executor, querySnapshot -> {
                        java.util.List<User> users = new java.util.ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                        callback.onSuccess(users);
                    })
                    .addOnFailureListener(executor, callback::onFailure);
        } else {
            // For more than 10 users, fetch in batches
            java.util.List<User> allUsers = new java.util.ArrayList<>();
            fetchUsersInBatches(uniqueIds, allUsers, callback);
        }
    }
    
    private void fetchUsersInBatches(java.util.List<String> userIds, java.util.List<User> allUsers, 
                                   MultipleUserProfileCallback callback) {
        if (userIds.isEmpty()) {
            callback.onSuccess(allUsers);
            return;
        }
        
        java.util.List<String> batch = userIds.subList(0, Math.min(10, userIds.size()));
        java.util.List<String> remaining = userIds.subList(Math.min(10, userIds.size()), userIds.size());
        
        firestore.collection("users")
                .whereIn("uid", batch)
                .get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            allUsers.add(user);
                        }
                    }
                    // Fetch next batch
                    fetchUsersInBatches(remaining, allUsers, callback);
                })
                .addOnFailureListener(executor, callback::onFailure);
    }
    
    /**
     * Cache management methods
     */
    private void cacheProfile(String userId, User user) {
        try {
            String userJson = gson.toJson(user);
            prefs.edit()
                    .putString(KEY_CACHED_PROFILE + userId, userJson)
                    .putLong(KEY_CACHED_PROFILE_TIMESTAMP + userId, System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache profile for user: " + userId, e);
        }
    }
    
    private User getCachedProfile(String userId) {
        try {
            String userJson = prefs.getString(KEY_CACHED_PROFILE + userId, null);
            if (userJson != null) {
                return gson.fromJson(userJson, User.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve cached profile for user: " + userId, e);
        }
        return null;
    }
    
    private boolean isCacheValid(String userId) {
        long timestamp = prefs.getLong(KEY_CACHED_PROFILE_TIMESTAMP + userId, 0);
        return (System.currentTimeMillis() - timestamp) < CACHE_DURATION;
    }
    
    /**
     * Clear cache for a specific user
     */
    public void clearCache(String userId) {
        prefs.edit()
                .remove(KEY_CACHED_PROFILE + userId)
                .remove(KEY_CACHED_PROFILE_TIMESTAMP + userId)
                .apply();
    }
    
    /**
     * Clear all cached profiles
     */
    public void clearAllCache() {
        prefs.edit().clear().apply();
    }
    
    /**
     * Check if current user can edit the profile
     */
    public boolean canEditProfile(String userId) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        return currentUserId != null && currentUserId.equals(userId);
    }
    
    // Callback interfaces
    public interface UserProfileCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }
    
    public interface ProfilePictureCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }
    
    public interface UserSearchCallback {
        void onSuccess(java.util.List<User> users);
        void onFailure(Exception e);
    }
    
    public interface MultipleUserProfileCallback {
        void onSuccess(java.util.List<User> users);
        void onFailure(Exception e);
    }
}
