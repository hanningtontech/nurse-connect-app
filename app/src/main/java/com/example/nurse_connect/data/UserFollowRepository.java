package com.example.nurse_connect.data;

import androidx.annotation.NonNull;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.models.UserFollow;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserFollowRepository {
    private static final String TAG = "UserFollowRepository";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public UserFollowRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public interface FollowCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FollowersCallback {
        void onSuccess(List<User> followers);
        void onFailure(Exception e);
    }

    public interface FollowingCallback {
        void onSuccess(List<User> following);
        void onFailure(Exception e);
    }

    public interface IsFollowingCallback {
        void onSuccess(boolean isFollowing);
        void onFailure(Exception e);
    }

    // Follow a user
    public void followUser(String targetUserId, FollowCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        
        if (currentUserId.equals(targetUserId)) {
            callback.onFailure(new IllegalArgumentException("Cannot follow yourself"));
            return;
        }

        // Check if already following
        firestore.collection("user_follows")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followingId", targetUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Already following - this is not an error, just return success
                            callback.onSuccess();
                        } else {
                            // Create new follow relationship
                            UserFollow follow = new UserFollow(currentUserId, targetUserId);
                            firestore.collection("user_follows")
                                    .add(follow)
                                    .addOnSuccessListener(documentReference -> {
                                        callback.onSuccess();
                                    })
                                    .addOnFailureListener(callback::onFailure);
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Unfollow a user
    public void unfollowUser(String targetUserId, FollowCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();

        firestore.collection("user_follows")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followingId", targetUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Delete the follow relationship
                            com.google.firebase.firestore.DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            firestore.collection("user_follows")
                                    .document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                                    .addOnFailureListener(callback::onFailure);
                        } else {
                            // Not following - this is not an error, just return success
                            callback.onSuccess();
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Get followers of a user
    public void getFollowers(String userId, FollowersCallback callback) {
        firestore.collection("user_follows")
                .whereEqualTo("followingId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> followerIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserFollow follow = document.toObject(UserFollow.class);
                            if (follow != null) {
                                followerIds.add(follow.getFollowerId());
                            }
                        }
                        
                        if (followerIds.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                        } else {
                            // Get user details for each follower
                            getUsersByIds(followerIds, callback);
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Get users that a user is following
    public void getFollowing(String userId, FollowingCallback callback) {
        firestore.collection("user_follows")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> followingIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserFollow follow = document.toObject(UserFollow.class);
                            if (follow != null) {
                                followingIds.add(follow.getFollowingId());
                            }
                        }
                        
                        if (followingIds.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                        } else {
                            // Get user details for each following
                            getUsersByIds(followingIds, callback);
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Check if current user is following target user
    public void isFollowing(String targetUserId, IsFollowingCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();

        firestore.collection("user_follows")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followingId", targetUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(!task.getResult().isEmpty());
                    } else {
                        callback.onFailure(task.getException());
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Helper method to get users by IDs
    private void getUsersByIds(List<String> userIds, FollowersCallback callback) {
        List<User> users = new ArrayList<>();
        final int[] completed = {0};
        
        for (String userId : userIds) {
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        completed[0]++;
                        if (task.isSuccessful() && task.getResult().exists()) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                        
                        if (completed[0] == userIds.size()) {
                            callback.onSuccess(users);
                        }
                    });
        }
    }

    // Helper method to get users by IDs for following callback
    private void getUsersByIds(List<String> userIds, FollowingCallback callback) {
        List<User> users = new ArrayList<>();
        final int[] completed = {0};
        
        for (String userId : userIds) {
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        completed[0]++;
                        if (task.isSuccessful() && task.getResult().exists()) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                        
                        if (completed[0] == userIds.size()) {
                            callback.onSuccess(users);
                        }
                    });
        }
    }
} 