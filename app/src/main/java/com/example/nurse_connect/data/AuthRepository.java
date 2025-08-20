package com.example.nurse_connect.data;

import com.example.nurse_connect.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuthRepository {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private Executor executor;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newSingleThreadExecutor();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface BooleanCallback {
        void onSuccess(boolean result);
        void onFailure(Exception e);
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(executor, authResult -> {
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(executor, callback::onFailure);
    }

    public void signUpWithEmail(String email, String password, String username, String displayName, String phoneNumber,
                               String nursingCareer, String yearsExperience, String currentInstitution,
                               String school, String year, String course, String description, String photoURL, String handle, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(executor, authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Create user profile in Firestore with enhanced profile data
                        User userProfile = new User(user.getUid(), email, username, displayName, phoneNumber, 
                                                  nursingCareer, yearsExperience, currentInstitution);
                        userProfile.setEmailVerified(user.isEmailVerified());
                        
                        // Set handle
                        if (handle != null && !handle.isEmpty()) {
                            userProfile.setHandle(handle.toLowerCase());
                        }
                        
                        // Set profile picture URL if provided
                        if (photoURL != null && !photoURL.isEmpty()) {
                            userProfile.setPhotoURL(photoURL);
                        }
                        
                        // Set enhanced profile data in UserProfile
                        if (userProfile.getProfile() == null) {
                            userProfile.setProfile(new com.example.nurse_connect.models.UserProfile());
                        }
                        
                        // Set academic/educational information
                        userProfile.getProfile().setInstitution(school);
                        userProfile.getProfile().setStudyYear(year);
                        userProfile.getProfile().setSpecialization(course);
                        userProfile.getProfile().setCourse(course);
                        userProfile.getProfile().setBio(description);
                        
                        // Set initial settings
                        if (userProfile.getSettings() == null) {
                            userProfile.setSettings(new com.example.nurse_connect.models.UserSettings());
                        }
                        
                        firestore.collection("users").document(user.getUid())
                                .set(userProfile)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        callback.onFailure(new Exception("Failed to create user"));
                    }
                })
                .addOnFailureListener(executor, callback::onFailure);
    }

    public void signInWithGoogle(GoogleSignInAccount account, AuthCallback callback) {
        String idToken = account.getIdToken();
        if (idToken != null) {
            com.google.firebase.auth.AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            auth.signInWithCredential(credential)
                    .addOnSuccessListener(executor, authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            // Check if user exists in Firestore
                            firestore.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(executor, documentSnapshot -> {
                                        if (!documentSnapshot.exists()) {
                                            // Create new user profile with minimal data
                                            User userProfile = new User(
                                                    user.getUid(),
                                                    user.getEmail() != null ? user.getEmail() : "",
                                                    user.getEmail() != null ? user.getEmail().split("@")[0] : "",
                                                    user.getDisplayName() != null ? user.getDisplayName() : ""
                                            );
                                            userProfile.setPhotoURL(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                                            userProfile.setEmailVerified(user.isEmailVerified());
                                            
                                            // Set a flag to indicate this is a new user who needs profile completion
                                            userProfile.setProfile(new com.example.nurse_connect.models.UserProfile());
                                            
                                            firestore.collection("users").document(user.getUid())
                                                    .set(userProfile)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // For new users, we'll let the ViewModel handle the profile completion flow
                                                        callback.onSuccess(user);
                                                    })
                                                    .addOnFailureListener(callback::onFailure);
                                        } else {
                                            // Existing user - check if profile is complete
                                            User existingUser = documentSnapshot.toObject(User.class);
                                            if (existingUser != null && existingUser.getProfile() != null && 
                                                existingUser.getProfile().getInstitution() != null && 
                                                !existingUser.getProfile().getInstitution().isEmpty()) {
                                                // Profile is complete
                                                callback.onSuccess(user);
                                            } else {
                                                // Profile is incomplete - still call success but ViewModel will handle it
                                                callback.onSuccess(user);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(executor, callback::onFailure);
                        } else {
                            callback.onFailure(new Exception("Failed to sign in with Google"));
                        }
                    })
                    .addOnFailureListener(executor, callback::onFailure);
        } else {
            callback.onFailure(new Exception("Failed to get ID token"));
        }
    }

    public void signOut() {
        auth.signOut();
    }

    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserProfile(String uid, UserCallback callback) {
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(executor, documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(executor, callback::onFailure);
    }

    public void updateUserProfile(User user, AuthCallback callback) {
        firestore.collection("users").document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void checkUsernameAvailability(String username, BooleanCallback callback) {
        firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    callback.onSuccess(querySnapshot.isEmpty());
                })
                .addOnFailureListener(executor, callback::onFailure);
    }

    public void checkHandleAvailability(String handle, BooleanCallback callback) {
        firestore.collection("users")
                .whereEqualTo("handle", handle.toLowerCase())
                .get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    callback.onSuccess(querySnapshot.isEmpty());
                })
                .addOnFailureListener(executor, callback::onFailure);
    }

    public void getUserByHandle(String handle, UserCallback callback) {
        firestore.collection("users")
                .whereEqualTo("handle", handle.toLowerCase())
                .get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
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

    public void getAllHandles(StringArrayCallback callback) {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    java.util.List<String> handles = new java.util.ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        if (user != null && user.getHandle() != null && !user.getHandle().isEmpty()) {
                            handles.add(user.getHandle().toLowerCase());
                        }
                    }
                    callback.onSuccess(handles.toArray(new String[0]));
                })
                .addOnFailureListener(executor, callback::onFailure);
    }

    public interface StringArrayCallback {
        void onSuccess(String[] handles);
        void onFailure(Exception e);
    }

    public void sendEmailVerification(AuthCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUser.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> callback.onSuccess(currentUser))
                    .addOnFailureListener(callback::onFailure);
        } else {
            callback.onFailure(new Exception("No user logged in"));
        }
    }
} 