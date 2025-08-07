package com.example.nurse_connect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.services.CommunityProfileService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthCheckActivity extends AppCompatActivity {

    private CommunityProfileService profileService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Firebase service
        profileService = new CommunityProfileService();
        
        // Check authentication status
        checkAuthenticationStatus();
    }

    private void checkAuthenticationStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // User is signed in, proceed to main app
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            // User is not signed in, redirect to sign in
            Toast.makeText(this, "Please sign in to access the app", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
        }
    }
} 