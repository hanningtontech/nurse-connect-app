package com.example.nurse_connect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.nurse_connect.databinding.ActivityMainBinding;
import com.example.nurse_connect.ui.home.HomeFragment;
import com.example.nurse_connect.ui.studyhub.StudyHubFragment;
import com.example.nurse_connect.ui.upload.UploadFragment;
import com.example.nurse_connect.ui.chat.ChatFragment;
import com.example.nurse_connect.ui.profile.ProfileFragment;
import com.example.nurse_connect.utils.NotificationHelper;
import com.example.nurse_connect.utils.PdfThumbnailGenerator;
import com.example.nurse_connect.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private boolean isGuestUser = false;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize theme before setting content view
        ThemeManager.getInstance(this).applyTheme();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Initialize PDFBox for thumbnail generation
        PdfThumbnailGenerator.initializePdfBox(this);
        
        // Create notification channel for messages
        NotificationHelper.createNotificationChannel(this);
        
        // Check if user is guest
        Intent intent = getIntent();
        if (intent != null) {
            isGuestUser = intent.getBooleanExtra("is_guest", false);
        }
        
        setupBottomNavigation();
        setupBackPressHandler();
        
        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
        }
        
        // Set user as online
        setUserOnline();

        // Start call notification service for non-guest users
        if (!isGuestUser && currentUser != null) {
            startCallNotificationService();
            android.util.Log.d("MainActivity", "Starting CallNotificationService for user: " + currentUser.getUid());
        } else {
            android.util.Log.d("MainActivity", "Not starting CallNotificationService - isGuestUser: " + isGuestUser + ", currentUser: " + (currentUser != null ? "exists" : "null"));
        }
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment selectedFragment = null;
                        
                        int itemId = item.getItemId();
                        if (itemId == R.id.nav_home) {
                            selectedFragment = new HomeFragment();
                        } else if (itemId == R.id.nav_study_hub) {
                            selectedFragment = new StudyHubFragment();
                        } else if (itemId == R.id.nav_upload) {
                            if (isGuestUser) {
                                // Show sign up prompt for guest users
                                showSignUpPrompt();
                                return false;
                            }
                            selectedFragment = new UploadFragment();
                        } else if (itemId == R.id.nav_chat) {
                            if (isGuestUser) {
                                // Show sign up prompt for guest users
                                showSignUpPrompt();
                                return false;
                            }
                            selectedFragment = new ChatFragment();
                        } else if (itemId == R.id.nav_profile) {
                            if (isGuestUser) {
                                // Show sign up prompt for guest users
                                showSignUpPrompt();
                                return false;
                            }
                            selectedFragment = new ProfileFragment();
                        }
                        
                        if (selectedFragment != null) {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainer, selectedFragment)
                                    .commit();
                            return true;
                        }
                        
                        return false;
                    }
                });
    }
    
    private void showSignUpPrompt() {
        // TODO: Show dialog prompting user to sign up for full access
        // For now, just show a toast message
        android.widget.Toast.makeText(this, 
                "Please sign up to access this feature", 
                android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
    
    private void setUserOnline() {
        if (currentUser != null && !isGuestUser) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", true);
            updates.put("lastSeen", System.currentTimeMillis());
            
            db.collection("users")
                    .document(currentUser.getUid())
                    .update(updates);
        }
    }
    
    private void setUserOffline() {
        if (currentUser != null && !isGuestUser) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", false);
            updates.put("lastSeen", System.currentTimeMillis());
            
            db.collection("users")
                    .document(currentUser.getUid())
                    .update(updates);
        }
    }

    private void startCallNotificationService() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Start the service as a regular service first, not as foreground
                Intent serviceIntent = new Intent(this, com.example.nurse_connect.services.CallNotificationService.class);
                startService(serviceIntent);
                android.util.Log.d("MainActivity", "CallNotificationService started successfully for user: " + currentUser.getUid());

                // Add a small delay and then test if service is working
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    testCallNotificationService();
                }, 2000);
            } else {
                android.util.Log.e("MainActivity", "Cannot start CallNotificationService - no authenticated user");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to start CallNotificationService", e);
        }
    }

    private void testCallNotificationService() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            android.util.Log.d("MainActivity", "Testing CallNotificationService by querying calls for user: " + currentUser.getUid());

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("calls")
                    .whereEqualTo("receiverId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        android.util.Log.d("MainActivity", "Service test: Found " + querySnapshot.size() + " calls for current user");
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            android.util.Log.d("MainActivity", "Service test: Call " + doc.getId() + " status: " + doc.getString("status"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("MainActivity", "Service test: Failed to query calls", e);
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Set user as offline when app goes to background
        setUserOffline();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Set user as offline when app is destroyed
        setUserOffline();
    }
}