package com.example.nurse_connect.data;

import android.util.Log;

import com.example.nurse_connect.models.StudyMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesRepository {
    
    private static final String TAG = "FavoritesRepository";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    public FavoritesRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public interface FavoritesCallback {
        void onSuccess(List<StudyMaterial> favorites);
        void onFailure(Exception e);
    }
    
    public interface ToggleFavoriteCallback {
        void onSuccess(boolean isFavorite);
        void onFailure(Exception e);
    }
    
    public interface CheckFavoriteCallback {
        void onSuccess(boolean isFavorite);
        void onFailure(Exception e);
    }
    
    // Get user's favorite documents
    public void getUserFavorites(FavoritesCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        firestore.collection("user_favorites")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> favoriteIds = new ArrayList<>();
                    
                    for (DocumentSnapshot document : querySnapshot) {
                        String materialId = document.getString("materialId");
                        if (materialId != null) {
                            favoriteIds.add(materialId);
                        }
                    }
                    
                    if (favoriteIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    // Get the actual study materials
                    getStudyMaterialsByIds(favoriteIds, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Get study materials by their IDs
    private void getStudyMaterialsByIds(List<String> materialIds, FavoritesCallback callback) {
        List<StudyMaterial> favorites = new ArrayList<>();
        final int[] completedCount = {0};
        final int totalIds = materialIds.size();
        
        for (String materialId : materialIds) {
            firestore.collection("study_materials")
                    .document(materialId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            StudyMaterial material = documentSnapshot.toObject(StudyMaterial.class);
                            if (material != null) {
                                material.setId(documentSnapshot.getId());
                                material.setLikedByUser(true); // Mark as liked since it's in favorites
                                favorites.add(material);
                            }
                        }
                        
                        completedCount[0]++;
                        if (completedCount[0] >= totalIds) {
                            callback.onSuccess(favorites);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedCount[0]++;
                        if (completedCount[0] >= totalIds) {
                            callback.onSuccess(favorites);
                        }
                    });
        }
    }
    
    // Toggle favorite status for a material
    public void toggleFavorite(String materialId, ToggleFavoriteCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        // Check if already favorited
        firestore.collection("user_favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("materialId", materialId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Remove from favorites
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        firestore.collection("user_favorites")
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    callback.onSuccess(false); // No longer favorite
                                })
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // Add to favorites
                        Map<String, Object> favorite = new HashMap<>();
                        favorite.put("userId", userId);
                        favorite.put("materialId", materialId);
                        favorite.put("addedAt", com.google.firebase.Timestamp.now());
                        
                        firestore.collection("user_favorites")
                                .add(favorite)
                                .addOnSuccessListener(documentReference -> {
                                    callback.onSuccess(true); // Now favorite
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Check if a material is favorited by the current user
    public void checkIfFavorite(String materialId, CheckFavoriteCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            callback.onSuccess(false);
            return;
        }
        
        firestore.collection("user_favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("materialId", materialId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onSuccess(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(callback::onFailure);
    }
} 