package com.example.nurse_connect.services;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class CommunityProfileService {
    private static final String TAG = "CommunityProfileService";
    private static final String COMMUNITY_PROFILES_COLLECTION = "community_profiles";
    private static final String POSTS_COLLECTION = "posts";
    
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    
    public interface CommunityProfileCallback {
        void onSuccess(String profileId);
        void onFailure(String error);
    }
    
    public interface ProfileCheckCallback {
        void onProfileExists(String profileId);
        void onProfileNotExists();
        void onError(String error);
    }
    
    public interface PostCreationCallback {
        void onSuccess(String postId);
        void onFailure(String error);
    }
    
    public CommunityProfileService() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Check if the current user has a community profile
     */
    public void checkCommunityProfileExists(ProfileCheckCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not authenticated");
            return;
        }
        
        String userId = currentUser.getUid();
        db.collection(COMMUNITY_PROFILES_COLLECTION)
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                callback.onProfileExists(document.getId());
                            } else {
                                callback.onProfileNotExists();
                            }
                        } else {
                            callback.onError("Failed to check profile: " + task.getException().getMessage());
                        }
                    }
                });
    }
    
    /**
     * Create a new community profile
     */
    public void createCommunityProfile(Map<String, Object> profileData, CommunityProfileCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        
        String userId = currentUser.getUid();
        
        // Add user ID and timestamp to profile data
        profileData.put("userId", userId);
        profileData.put("createdAt", System.currentTimeMillis());
        profileData.put("updatedAt", System.currentTimeMillis());
        
        db.collection(COMMUNITY_PROFILES_COLLECTION)
                .document(userId)
                .set(profileData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Community profile created successfully");
                        callback.onSuccess(userId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating community profile", e);
                        callback.onFailure("Failed to create profile: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Upload profile image to Firebase Storage
     */
    public void uploadProfileImage(Uri imageUri, OnSuccessListener<Uri> successCallback, OnFailureListener failureCallback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            failureCallback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        String userId = currentUser.getUid();
        String fileName = "profile_images/" + userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storage.getReference().child(fileName);
        
        imageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(successCallback)
                                .addOnFailureListener(failureCallback);
                    }
                })
                .addOnFailureListener(failureCallback);
    }
    
    /**
     * Create a new post
     */
    public void createPost(String content, String hashtags, String postType, Uri mediaUri, PostCreationCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        
        String userId = currentUser.getUid();
        
        // First, check if user has community profile
        checkCommunityProfileExists(new ProfileCheckCallback() {
            @Override
            public void onProfileExists(String profileId) {
                // User has profile, proceed with post creation
                createPostWithProfile(userId, content, hashtags, postType, mediaUri, callback);
            }
            
            @Override
            public void onProfileNotExists() {
                callback.onFailure("Please create a community profile first");
            }
            
            @Override
            public void onError(String error) {
                callback.onFailure(error);
            }
        });
    }
    
    private void createPostWithProfile(String userId, String content, String hashtags, String postType, Uri mediaUri, PostCreationCallback callback) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", userId);
        postData.put("content", content);
        postData.put("hashtags", hashtags);
        postData.put("postType", postType);
        postData.put("createdAt", System.currentTimeMillis());
        postData.put("likes", 0);
        postData.put("comments", 0);
        postData.put("shares", 0);
        
        // If there's media, upload it first and detect aspect ratio
        if (mediaUri != null && (postType.equals("image") || postType.equals("video"))) {
            // Detect aspect ratio for images
            if (postType.equals("image")) {
                try {
                    android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeFile(mediaUri.getPath(), options);
                    
                    int originalWidth = options.outWidth;
                    int originalHeight = options.outHeight;
                    float aspectRatio = (float) originalWidth / originalHeight;
                    
                    // Store aspect ratio info
                    postData.put("originalAspectRatio", aspectRatio);
                    postData.put("originalWidth", originalWidth);
                    postData.put("originalHeight", originalHeight);
                    
                    // For free-form cropping, we'll store the original aspect ratio
                    // The actual cropped aspect ratio will be determined after cropping
                    postData.put("cropAspectRatio", "free"); // Indicates free-form cropping
                    
                } catch (Exception e) {
                    // Fallback to standard
                    postData.put("cropAspectRatio", "16:9");
                }
            }
            
            uploadPostMedia(mediaUri, postType, new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri downloadUri) {
                    postData.put("mediaUrl", downloadUri.toString());
                    savePostToFirestore(postData, callback);
                }
            }, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    callback.onFailure("Failed to upload media: " + e.getMessage());
                }
            });
        } else {
            // No media, save post directly
            savePostToFirestore(postData, callback);
        }
    }
    
    private void uploadPostMedia(Uri mediaUri, String postType, OnSuccessListener<Uri> successCallback, OnFailureListener failureCallback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            failureCallback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        String userId = currentUser.getUid();
        String fileName = "post_media/" + userId + "_" + System.currentTimeMillis() + 
                (postType.equals("image") ? ".jpg" : ".mp4");
        StorageReference mediaRef = storage.getReference().child(fileName);
        
        // Set metadata for better compression info
        UploadTask.TaskSnapshot taskSnapshot = null;
        if (postType.equals("image")) {
            // For images, we can add metadata about compression
            mediaRef.putFile(mediaUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d(TAG, "Image uploaded successfully. Size: " + 
                                    (taskSnapshot.getBytesTransferred() / 1024) + "KB");
                            mediaRef.getDownloadUrl()
                                    .addOnSuccessListener(successCallback)
                                    .addOnFailureListener(failureCallback);
                        }
                    })
                    .addOnFailureListener(failureCallback);
        } else {
            // For videos, use standard upload
            mediaRef.putFile(mediaUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mediaRef.getDownloadUrl()
                                    .addOnSuccessListener(successCallback)
                                    .addOnFailureListener(failureCallback);
                        }
                    })
                    .addOnFailureListener(failureCallback);
        }
    }
    
    private void savePostToFirestore(Map<String, Object> postData, PostCreationCallback callback) {
        db.collection(POSTS_COLLECTION)
                .add(postData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Post created successfully with ID: " + documentReference.getId());
                        callback.onSuccess(documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating post", e);
                        callback.onFailure("Failed to create post: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isUserAuthenticated() {
        return auth.getCurrentUser() != null;
    }
} 