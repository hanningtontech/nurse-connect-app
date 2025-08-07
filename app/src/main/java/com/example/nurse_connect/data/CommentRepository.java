package com.example.nurse_connect.data;

import android.util.Log;

import com.example.nurse_connect.models.Comment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommentRepository {
    
    private static final String TAG = "CommentRepository";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    public CommentRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public interface CommentsCallback {
        void onSuccess(List<Comment> comments);
        void onFailure(Exception e);
    }
    
    public interface AddCommentCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    public interface AddReplyCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    public interface RepliesCallback {
        void onSuccess(List<Comment> replies);
        void onFailure(Exception e);
    }
    
    public interface DeleteCommentCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    // Load comments for a specific material (top-level comments only)
    public void loadComments(String materialId, CommentsCallback callback) {
        Log.d(TAG, "Loading comments for material: " + materialId);
        
        firestore.collection("comments")
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " total comments for material: " + materialId);
                    List<Comment> comments = new ArrayList<>();
                    
                    for (DocumentSnapshot document : querySnapshot) {
                        Comment comment = document.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(document.getId());
                            
                            // Handle old comments that don't have parentCommentId field
                            if (comment.getParentCommentId() == null) {
                                comment.setParentCommentId("");
                                Log.d(TAG, "Fixed null parentCommentId for comment: " + comment.getId());
                            }
                            
                            // Only include top-level comments (no parent or empty parent)
                            if (comment.getParentCommentId().isEmpty()) {
                                comments.add(comment);
                                Log.d(TAG, "Added top-level comment: " + comment.getId() + " with text: " + comment.getText().substring(0, Math.min(50, comment.getText().length())) + "...");
                            } else {
                                Log.d(TAG, "Skipped reply comment: " + comment.getId() + " (parent: " + comment.getParentCommentId() + ")");
                            }
                        }
                    }
                    
                    Log.d(TAG, "Final top-level comments count: " + comments.size());
                    
                    // Sort comments by createdAt in descending order (newest first) in memory
                    comments.sort((c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                    
                    // Fetch user data for each comment to ensure we have correct usernames
                    fetchUserDataForComments(comments, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading comments for material: " + materialId, e);
                    callback.onFailure(e);
                });
    }
    
    // Load replies for a specific comment
    public void loadReplies(String parentCommentId, RepliesCallback callback) {
        firestore.collection("comments")
                .whereEqualTo("parentCommentId", parentCommentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> replies = new ArrayList<>();
                    
                    for (DocumentSnapshot document : querySnapshot) {
                        Comment reply = document.toObject(Comment.class);
                        if (reply != null) {
                            reply.setId(document.getId());
                            replies.add(reply);
                        }
                    }
                    
                    // Sort replies by createdAt in ascending order (oldest first)
                    replies.sort((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()));
                    
                    // Fetch user data for each reply
                    fetchUserDataForComments(replies, new CommentsCallback() {
                        @Override
                        public void onSuccess(List<Comment> comments) {
                            callback.onSuccess(comments);
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Fetch user data for comments to get correct usernames
    private void fetchUserDataForComments(List<Comment> comments, CommentsCallback callback) {
        if (comments.isEmpty()) {
            callback.onSuccess(comments);
            return;
        }
        
        final int[] completedCount = {0};
        final int totalComments = comments.size();
        
        for (Comment comment : comments) {
            if (comment.getUserId() != null && !comment.getUserId().isEmpty()) {
                firestore.collection("users")
                        .document(comment.getUserId())
                        .get()
                        .addOnSuccessListener(userDocument -> {
                            if (userDocument.exists()) {
                                String userName = userDocument.getString("displayName");
                                if (userName == null || userName.trim().isEmpty()) {
                                    userName = userDocument.getString("email");
                                }
                                if (userName != null && !userName.trim().isEmpty()) {
                                    comment.setUsername(userName);
                                }
                            }
                            
                            completedCount[0]++;
                            if (completedCount[0] >= totalComments) {
                                callback.onSuccess(comments);
                            }
                        })
                        .addOnFailureListener(e -> {
                            completedCount[0]++;
                            if (completedCount[0] >= totalComments) {
                                callback.onSuccess(comments);
                            }
                        });
            } else {
                completedCount[0]++;
                if (completedCount[0] >= totalComments) {
                    callback.onSuccess(comments);
                }
            }
        }
    }
    
    // Add a new comment
    public void addComment(String materialId, String commentText, AddCommentCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        // Get user data from users collection to get the actual username
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    String userName = "Anonymous";
                    String userPhotoURL = null;
                    if (userDocument.exists()) {
                        userName = userDocument.getString("displayName");
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = userDocument.getString("email");
                        }
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = "Anonymous";
                        }
                        // Get the user's profile picture URL
                        userPhotoURL = userDocument.getString("photoURL");
                    }
                    
                    Comment newComment = new Comment(userId, userName, commentText);
                    
                    firestore.collection("comments")
                            .add(newComment)
                            .addOnSuccessListener(documentReference -> {
                                // Update the comment count in the study material
                                updateCommentCount(materialId, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Add a reply to a comment
    public void addReply(String materialId, String parentCommentId, String replyText, AddReplyCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        // Get user data from users collection to get the actual username
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    String userName = "Anonymous";
                    String userPhotoURL = null;
                    if (userDocument.exists()) {
                        userName = userDocument.getString("displayName");
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = userDocument.getString("email");
                        }
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = "Anonymous";
                        }
                        // Get the user's profile picture URL
                        userPhotoURL = userDocument.getString("photoURL");
                    }
                    
                    Comment newReply = new Comment(userId, userName, replyText);
                    newReply.setParentCommentId(parentCommentId);
                    
                    firestore.collection("comments")
                            .add(newReply)
                            .addOnSuccessListener(documentReference -> {
                                // Update the reply count for the parent comment
                                updateReplyCount(parentCommentId, materialId, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Update reply count for a parent comment
    private void updateReplyCount(String parentCommentId, String materialId, AddReplyCallback callback) {
        // Count replies for this parent comment
        firestore.collection("comments")
                .whereEqualTo("parentCommentId", parentCommentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int replyCount = querySnapshot.size();
                    
                    // Update the parent comment with new reply count
                    firestore.collection("comments")
                            .document(parentCommentId)
                            .update("replyCount", replyCount)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Reply count updated to: " + replyCount);
                                // Also update the material's comment count
                                updateCommentCount(materialId, new AddCommentCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess();
                                    }
                                    
                                    @Override
                                    public void onFailure(Exception e) {
                                        callback.onSuccess(); // Still call success even if count update fails
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating reply count", e);
                                callback.onSuccess(); // Still call success even if count update fails
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error counting replies", e);
                    callback.onSuccess(); // Still call success even if count fails
                });
    }
    
    // Update comment count in study material
    private void updateCommentCount(String materialId, AddCommentCallback callback) {
        // Count all comments for this material (including replies)
        firestore.collection("comments")
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int commentCount = querySnapshot.size();
                    
                    // Update the study material with new comment count
                    firestore.collection("study_materials")
                            .document(materialId)
                            .update("commentCount", commentCount)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Comment count updated to: " + commentCount);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating comment count", e);
                                callback.onSuccess(); // Still call success even if count update fails
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error counting comments", e);
                    callback.onSuccess(); // Still call success even if count fails
                });
    }
    
    // Delete a comment and update counts
    public void deleteComment(String commentId, String materialId, DeleteCommentCallback callback) {
        firestore.collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Comment deleted successfully: " + commentId);
                    // Update the comment count
                    updateCommentCount(materialId, new AddCommentCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error updating comment count after deletion", e);
                            callback.onSuccess(); // Still call success even if count update fails
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting comment: " + commentId, e);
                    callback.onFailure(e);
                });
    }
    
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
} 