package com.example.nurse_connect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivitySignInBinding;
import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class SignInActivity extends AppCompatActivity {
    
    private ActivitySignInBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupGoogleSignIn();
        setupUI();
        observeViewModel();
    }
    
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    
    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Google Sign In button
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        
        // Sign In button
        binding.btnSignIn.setOnClickListener(v -> signInWithEmail());
        
        // Forgot Password link
        binding.tvForgotPassword.setOnClickListener(v -> {
            // TODO: Navigate to forgot password screen
            Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Sign Up link
        binding.tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            finish();
        });
        
        // Continue as Guest link
        binding.tvGuest.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            intent.putExtra("is_guest", true);
            startActivity(intent);
            finish();
        });
    }
    
    private void signInWithEmail() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        
        // Validation
        if (email.isEmpty()) {
            binding.etEmail.setError("Email is required");
            return;
        }
        
        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
            return;
        }
        
        // Sign in
        authViewModel.signInWithEmail(email, password);
    }
    
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                authViewModel.signInWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSignIn.setEnabled(!isLoading);
            binding.btnGoogleSignIn.setEnabled(!isLoading);
        });
        
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                authViewModel.clearError();
            }
        });
        
        authViewModel.getAuthState().observe(this, authState -> {
            if (authState == AuthViewModel.AuthState.AUTHENTICATED) {
                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                finish();
            } else if (authState == AuthViewModel.AuthState.PROFILE_INCOMPLETE) {
                startActivity(new Intent(SignInActivity.this, ProfileCompletionActivity.class));
                finish();
            }
        });
    }
} 