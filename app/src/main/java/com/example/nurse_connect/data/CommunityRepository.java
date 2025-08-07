package com.example.nurse_connect.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nurse_connect.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class CommunityRepository {
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    public CommunityRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public interface CommunityCallback {
        void onSuccess(List<User> nurses);
        void onFailure(Exception e);
    }
    
    /**
     * Load featured nurses for the community hub
     */
    public void loadFeaturedNurses(CommunityCallback callback) {
        firestore.collection("users")
                .whereNotEqualTo("uid", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> nurses = queryDocumentSnapshots.toObjects(User.class);
                    callback.onSuccess(nurses);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Load nurses by specialty or institution
     */
    public void loadNursesBySpecialty(String specialty, CommunityCallback callback) {
        firestore.collection("users")
                .whereNotEqualTo("uid", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "")
                .whereEqualTo("profile.specialization", specialty)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> nurses = queryDocumentSnapshots.toObjects(User.class);
                    callback.onSuccess(nurses);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Load nurses by institution
     */
    public void loadNursesByInstitution(String institution, CommunityCallback callback) {
        firestore.collection("users")
                .whereNotEqualTo("uid", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "")
                .whereEqualTo("profile.institution", institution)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> nurses = queryDocumentSnapshots.toObjects(User.class);
                    callback.onSuccess(nurses);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Search nurses by name or username
     */
    public void searchNurses(String query, CommunityCallback callback) {
        firestore.collection("users")
                .whereNotEqualTo("uid", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> nurses = queryDocumentSnapshots.toObjects(User.class);
                    callback.onSuccess(nurses);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Get current user's profile
     */
    public void getCurrentUser(CommunityCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        firestore.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        callback.onSuccess(List.of(user));
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
} 