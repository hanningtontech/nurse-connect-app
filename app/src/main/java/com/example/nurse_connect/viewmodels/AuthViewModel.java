package com.example.nurse_connect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nurse_connect.data.AuthRepository;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.models.UserProfile;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;
    
    private MutableLiveData<AuthState> authState;
    private MutableLiveData<User> currentUser;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<UpdateProfileResult> updateProfileResult;

    public AuthViewModel() {
        authRepository = new AuthRepository();
        authState = new MutableLiveData<>();
        currentUser = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        updateProfileResult = new MutableLiveData<>();
        
        checkAuthState();
    }

    public LiveData<AuthState> getAuthState() { return authState; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<UpdateProfileResult> getUpdateProfileResult() { return updateProfileResult; }
    
    public AuthRepository getAuthRepository() { return authRepository; }

    private void checkAuthState() {
        isLoading.postValue(true);
        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        
        if (firebaseUser != null) {
            authRepository.getUserProfile(firebaseUser.getUid(), new AuthRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser.postValue(user);
                    // Check if user needs to complete profile
                    boolean isProfileComplete = isUserProfileComplete(user);
                    if (isProfileComplete) {
                        authState.postValue(AuthState.AUTHENTICATED);
                    } else {
                        authState.postValue(AuthState.PROFILE_INCOMPLETE);
                    }
                    isLoading.postValue(false);
                }

                @Override
                public void onFailure(Exception e) {
                    // If we can't load the user profile, assume profile is incomplete
                    authState.postValue(AuthState.PROFILE_INCOMPLETE);
                    errorMessage.postValue(e.getMessage());
                    isLoading.postValue(false);
                }
            });
        } else {
            authState.postValue(AuthState.UNAUTHENTICATED);
            isLoading.postValue(false);
        }
    }

    private boolean isUserProfileComplete(User user) {
        if (user == null || user.getProfile() == null) {
            return false;
        }
        
        UserProfile profile = user.getProfile();
        
        // Check if required fields are filled (not null and not empty)
        boolean hasInstitution = profile.getInstitution() != null && !profile.getInstitution().trim().isEmpty();
        boolean hasStudyYear = profile.getStudyYear() != null && !profile.getStudyYear().trim().isEmpty();
        boolean hasSpecialization = profile.getSpecialization() != null && !profile.getSpecialization().trim().isEmpty();
        
        return hasInstitution && hasStudyYear && hasSpecialization;
    }

    public void signInWithEmail(String email, String password) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        authRepository.signInWithEmail(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                authRepository.getUserProfile(firebaseUser.getUid(), new AuthRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        currentUser.postValue(user);
                        // Check if user needs to complete profile
                        boolean isProfileComplete = isUserProfileComplete(user);
                        if (isProfileComplete) {
                            authState.postValue(AuthState.AUTHENTICATED);
                        } else {
                            authState.postValue(AuthState.PROFILE_INCOMPLETE);
                        }
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // If we can't load the user profile, assume profile is incomplete
                        authState.postValue(AuthState.PROFILE_INCOMPLETE);
                        isLoading.postValue(false);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage() != null ? e.getMessage() : "Sign in failed");
                isLoading.postValue(false);
            }
        });
    }

    public void signUpWithEmail(String email, String password, String username, String displayName, String phoneNumber, 
                               String nursingCareer, String yearsExperience, String currentInstitution, 
                               String school, String year, String course, String description, String photoURL, String handle) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        authRepository.signUpWithEmail(email, password, username, displayName, phoneNumber, nursingCareer, yearsExperience, 
                                     currentInstitution, school, year, course, description, photoURL, handle, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                authRepository.getUserProfile(firebaseUser.getUid(), new AuthRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        currentUser.postValue(user);
                        authState.postValue(AuthState.AUTHENTICATED);
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue(e.getMessage());
                        isLoading.postValue(false);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public void signInWithGoogle(GoogleSignInAccount account) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        authRepository.signInWithGoogle(account, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                authRepository.getUserProfile(firebaseUser.getUid(), new AuthRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        currentUser.postValue(user);
                        // Check if user needs to complete profile
                        boolean isProfileComplete = isUserProfileComplete(user);
                        if (isProfileComplete) {
                            authState.postValue(AuthState.AUTHENTICATED);
                        } else {
                            authState.postValue(AuthState.PROFILE_INCOMPLETE);
                        }
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // If we can't load the user profile, assume profile is incomplete
                        authState.postValue(AuthState.PROFILE_INCOMPLETE);
                        isLoading.postValue(false);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage() != null ? e.getMessage() : "Google sign in failed");
                isLoading.postValue(false);
            }
        });
    }

    public void signOut() {
        isLoading.postValue(true);
        authRepository.signOut();
        currentUser.postValue(null);
        authState.postValue(AuthState.UNAUTHENTICATED);
        isLoading.postValue(false);
    }

    public void sendPasswordResetEmail(String email) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        authRepository.sendPasswordResetEmail(email, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                errorMessage.postValue("Password reset email sent");
                isLoading.postValue(false);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage() != null ? e.getMessage() : "Failed to send reset email");
                isLoading.postValue(false);
            }
        });
    }

    public void checkUsernameAvailability(String username, UsernameAvailabilityCallback callback) {
        authRepository.checkUsernameAvailability(username, new AuthRepository.BooleanCallback() {
            @Override
            public void onSuccess(boolean result) {
                callback.onResult(result);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onResult(false);
            }
        });
    }

    public void checkHandleAvailability(String handle, HandleAvailabilityCallback callback) {
        authRepository.checkHandleAvailability(handle, new AuthRepository.BooleanCallback() {
            @Override
            public void onSuccess(boolean result) {
                callback.onResult(result);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onResult(false); // Assume not available on error
            }
        });
    }

    public void getAllHandles(HandleArrayCallback callback) {
        authRepository.getAllHandles(new AuthRepository.StringArrayCallback() {
            @Override
            public void onSuccess(String[] handles) {
                callback.onResult(handles);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onResult(new String[0]);
            }
        });
    }

    public void clearError() {
        errorMessage.postValue(null);
    }

    public void updateUserProfile(String school, String year, String course, String description, String photoURL) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        User currentUserData = currentUser.getValue();
        if (currentUserData != null) {
            if (currentUserData.getProfile() == null) {
                currentUserData.setProfile(new com.example.nurse_connect.models.UserProfile());
            }
            currentUserData.getProfile().setInstitution(school);
            currentUserData.getProfile().setStudyYear(year);
            currentUserData.getProfile().setSpecialization(course);
            currentUserData.getProfile().setBio(description);
            
            // Set profile picture URL if provided
            if (photoURL != null && !photoURL.isEmpty()) {
                currentUserData.setPhotoURL(photoURL);
            }
            
            authRepository.updateUserProfile(currentUserData, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser firebaseUser) {
                    currentUser.postValue(currentUserData);
                    authState.postValue(AuthState.AUTHENTICATED);
                    isLoading.postValue(false);
                }

                @Override
                public void onFailure(Exception e) {
                    errorMessage.postValue(e.getMessage() != null ? e.getMessage() : "Failed to update profile");
                    isLoading.postValue(false);
                }
            });
        } else {
            errorMessage.postValue("No user data available");
            isLoading.postValue(false);
        }
    }

    // New method for comprehensive profile update
    public void updateUserProfile(String displayName, String username, String phoneNumber,
                                 String course, String studyYear, String specialization,
                                 String institution, String gpa, String bio, String photoURL) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        
        User currentUserData = currentUser.getValue();
        if (currentUserData != null) {
            // Update basic user information
            currentUserData.setDisplayName(displayName);
            currentUserData.setUsername(username);
            currentUserData.setPhoneNumber(phoneNumber);
            
            // Set profile picture URL if provided
            if (photoURL != null && !photoURL.isEmpty()) {
                currentUserData.setPhotoURL(photoURL);
            }
            
            // Update profile information
            if (currentUserData.getProfile() == null) {
                currentUserData.setProfile(new UserProfile());
            }
            currentUserData.getProfile().setCourse(course);
            currentUserData.getProfile().setStudyYear(studyYear);
            currentUserData.getProfile().setSpecialization(specialization);
            currentUserData.getProfile().setInstitution(institution);
            currentUserData.getProfile().setGpa(gpa);
            currentUserData.getProfile().setBio(bio);
            
            authRepository.updateUserProfile(currentUserData, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser firebaseUser) {
                    // Refresh user data from Firestore to ensure UI reflects the latest changes
                    authRepository.getUserProfile(currentUserData.getUid(), new AuthRepository.UserCallback() {
                        @Override
                        public void onSuccess(User updatedUser) {
                            currentUser.postValue(updatedUser);
                            authState.postValue(AuthState.AUTHENTICATED);
                            isLoading.postValue(false);
                            updateProfileResult.postValue(new UpdateProfileResult(true, null));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // If refresh fails, still use the local data
                            currentUser.postValue(currentUserData);
                            authState.postValue(AuthState.AUTHENTICATED);
                            isLoading.postValue(false);
                            updateProfileResult.postValue(new UpdateProfileResult(true, null));
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Failed to update profile";
                    errorMessage.postValue(errorMsg);
                    isLoading.postValue(false);
                    updateProfileResult.postValue(new UpdateProfileResult(false, errorMsg));
                }
            });
        } else {
            String errorMsg = "No user data available";
            errorMessage.postValue(errorMsg);
            isLoading.postValue(false);
            updateProfileResult.postValue(new UpdateProfileResult(false, errorMsg));
        }
    }

    public interface UsernameAvailabilityCallback {
        void onResult(boolean isAvailable);
    }

    public interface HandleAvailabilityCallback {
        void onResult(boolean isAvailable);
    }

    public interface HandleArrayCallback {
        void onResult(String[] handles);
    }

    public static class UpdateProfileResult {
        private final boolean success;
        private final String error;

        public UpdateProfileResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }

    public enum AuthState {
        LOADING,
        AUTHENTICATED,
        UNAUTHENTICATED,
        PROFILE_INCOMPLETE
    }
} 