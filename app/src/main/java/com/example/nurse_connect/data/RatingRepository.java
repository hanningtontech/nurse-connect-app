package com.example.nurse_connect.data;

import android.util.Log;

import com.example.nurse_connect.models.DocumentRating;
import com.example.nurse_connect.models.RatingStats;
import com.example.nurse_connect.models.StudyMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class RatingRepository {
    
    private static final String TAG = "RatingRepository";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    public RatingRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public interface StudyMaterialCallback {
        void onSuccess(StudyMaterial material);
        void onFailure(Exception e);
    }
    
    public interface RatingStatsCallback {
        void onSuccess(RatingStats stats);
        void onFailure(Exception e);
    }
    
    public interface UserRatingCallback {
        void onSuccess(DocumentRating rating);
        void onFailure(Exception e);
    }
    
    public interface SubmissionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    // Get study material by ID
    public void getStudyMaterial(String materialId, StudyMaterialCallback callback) {
        firestore.collection("study_materials")
                .document(materialId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        StudyMaterial material = documentSnapshot.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(documentSnapshot.getId());
                            callback.onSuccess(material);
                        } else {
                            callback.onFailure(new Exception("Failed to parse document"));
                        }
                    } else {
                        callback.onFailure(new Exception("Document not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Get rating statistics for a document
    public void getRatingStats(String materialId, RatingStatsCallback callback) {
        firestore.collection("document_ratings")
                .whereEqualTo("documentId", materialId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    RatingStats stats = new RatingStats();
                    
                    for (DocumentSnapshot document : querySnapshot) {
                        DocumentRating rating = document.toObject(DocumentRating.class);
                        if (rating != null) {
                            stats.addRating(rating.getRating());
                        }
                    }
                    
                    callback.onSuccess(stats);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Get user's existing rating for a document
    public void getUserRating(String materialId, UserRatingCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        firestore.collection("document_ratings")
                .whereEqualTo("documentId", materialId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        DocumentRating rating = document.toObject(DocumentRating.class);
                        if (rating != null) {
                            rating.setId(document.getId());
                            callback.onSuccess(rating);
                        } else {
                            callback.onFailure(new Exception("Failed to parse rating"));
                        }
                    } else {
                        callback.onFailure(new Exception("No rating found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Submit or update user rating
    public void submitRating(String materialId, int rating, String comment, SubmissionCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        String userName = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Anonymous";
        
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        // First check if user already rated this document
        firestore.collection("document_ratings")
                .whereEqualTo("documentId", materialId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Update existing rating
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("rating", rating);
                        updates.put("comment", comment);
                        updates.put("updatedAt", com.google.firebase.Timestamp.now());
                        
                        firestore.collection("document_ratings")
                                .document(document.getId())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // If there's a comment, also update/create comment in comments collection
                                    if (comment != null && !comment.trim().isEmpty()) {
                                        updateOrCreateComment(materialId, userId, userName, comment, rating, callback);
                                    } else {
                                        updateDocumentRating(materialId, callback);
                                    }
                                })
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // Create new rating
                        DocumentRating newRating = new DocumentRating(materialId, userId, userName, rating, comment);
                        
                        firestore.collection("document_ratings")
                                .add(newRating)
                                .addOnSuccessListener(documentReference -> {
                                    // If there's a comment, also create comment in comments collection
                                    if (comment != null && !comment.trim().isEmpty()) {
                                        createComment(materialId, userId, userName, comment, rating, callback);
                                    } else {
                                        updateDocumentRating(materialId, callback);
                                    }
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Create a new comment in the comments collection
    private void createComment(String materialId, String userId, String userName, String comment, int rating, SubmissionCallback callback) {
        com.example.nurse_connect.models.Comment newComment = new com.example.nurse_connect.models.Comment(userId, userName, comment);
        
        firestore.collection("comments")
                .add(newComment)
                .addOnSuccessListener(documentReference -> {
                    updateDocumentRating(materialId, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Update existing comment or create new one if it doesn't exist
    private void updateOrCreateComment(String materialId, String userId, String userName, String comment, int rating, SubmissionCallback callback) {
        // Check if user already has a comment for this material
        firestore.collection("comments")
                .whereEqualTo("materialId", materialId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Update existing comment
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("text", comment);
                        updates.put("userRating", rating);
                        updates.put("createdAt", com.google.firebase.Timestamp.now());
                        
                        firestore.collection("comments")
                                .document(document.getId())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    updateDocumentRating(materialId, callback);
                                })
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // Create new comment
                        createComment(materialId, userId, userName, comment, rating, callback);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Update the average rating in the study material document
    private void updateDocumentRating(String materialId, SubmissionCallback callback) {
        getRatingStats(materialId, new RatingStatsCallback() {
            @Override
            public void onSuccess(RatingStats stats) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("rating", stats.getAverageRating());
                updates.put("reviewCount", stats.getTotalRatings());
                
                firestore.collection("study_materials")
                        .document(materialId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(callback::onFailure);
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
} 