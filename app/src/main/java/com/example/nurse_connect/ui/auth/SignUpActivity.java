package com.example.nurse_connect.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivitySignUpBinding;
import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.utils.ImageUtils;
import com.example.nurse_connect.utils.HandleUtils;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {
    
    private ActivitySignUpBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    
    private TextInputEditText etEmail;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etUsername;
    private TextInputEditText etDisplayName;
    private AutoCompleteTextView spinnerNursingCareer;
    private TextInputEditText etYearsExperience;
    private TextInputEditText etCurrentInstitution;
    private TextInputEditText etSchool;
    private TextInputEditText etYear;
    private TextInputEditText etCourse;
    private TextInputEditText etDescription;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    
    private android.os.Handler usernameCheckHandler = new android.os.Handler();
    private Runnable usernameCheckRunnable;
    
    // Profile picture variables
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    
    // Nursing career options
    private static final List<String> NURSING_CAREERS = Arrays.asList(
        "LPN / LVN",
        "RN",
        "NP",
        "CNS",
        "CRNA",
        "CNM",
        "Nursing Administrator",
        "Nurse Educator",
        "Public Health Nurse",
        "Travel Nurse"
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupGoogleSignIn();
        setupUI();
        setupNursingCareerSpinner();
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
        
        // Google Sign Up button
        binding.btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());
        
        // Create Account button
        binding.btnSignUp.setOnClickListener(v -> createAccount());
        
        // Sign In link
        binding.tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });
        
        // Continue as Guest link
        binding.tvGuest.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            intent.putExtra("is_guest", true);
            startActivity(intent);
            finish();
        });
        
        // Profile picture button
        binding.btnAddProfilePicture.setOnClickListener(v -> showImageSourceDialog());
        
        // Setup image launchers
        setupImageLaunchers();
        
        // Real-time validation
        setupValidation();
    }
    
    private void setupNursingCareerSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            NURSING_CAREERS
        );
        
        binding.spinnerNursingCareer.setAdapter(adapter);
        
        // Set default selection to first item
        if (!NURSING_CAREERS.isEmpty()) {
            binding.spinnerNursingCareer.setText(NURSING_CAREERS.get(0), false);
        }
        
        // Add validation for career selection
        binding.spinnerNursingCareer.setOnItemClickListener((parent, view, position, id) -> {
            validateNursingCareer();
        });
    }
    
    private void setupValidation() {
        // Email validation
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Phone number validation
        binding.etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhoneNumber();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Username validation
        binding.etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Password validation
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Confirm password validation
        binding.etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirmPassword();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void validateEmail() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            binding.etEmail.setError("Email is required");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email format");
        } else {
            binding.etEmail.setError(null);
        }
    }
    
    private void validatePhoneNumber() {
        String phone = binding.etPhoneNumber.getText().toString().trim();
        if (phone.isEmpty()) {
            binding.etPhoneNumber.setError("Phone number is required");
        } else if (phone.length() < 10) {
            binding.etPhoneNumber.setError("Phone number must be at least 10 digits");
        } else if (!phone.matches("^[+]?[0-9\\s\\-\\(\\)]+$")) {
            binding.etPhoneNumber.setError("Invalid phone number format");
        } else {
            binding.etPhoneNumber.setError(null);
        }
    }
    
    private void validateUsername() {
        String username = binding.etUsername.getText().toString().trim();
        
        if (username.isEmpty()) {
            binding.etUsername.setError("Username is required");
            return;
        }
        
        if (username.length() < 3) {
            binding.etUsername.setError("Username must be at least 3 characters");
            return;
        }
        
        if (username.length() > 20) {
            binding.etUsername.setError("Username must be less than 20 characters");
            return;
        }
        
        // Check for valid characters (alphanumeric and underscore only)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            binding.etUsername.setError("Username can only contain letters, numbers, and underscores");
            return;
        }
        
        // Clear any previous errors
        binding.etUsername.setError(null);
        
        // Only check availability if username is valid
        if (username.length() >= 3) {
            // Cancel previous username check
            if (usernameCheckRunnable != null) {
                usernameCheckHandler.removeCallbacks(usernameCheckRunnable);
            }
            
            // Create new username check with debounce
            usernameCheckRunnable = () -> {
                authViewModel.checkUsernameAvailability(username, isAvailable -> {
                    runOnUiThread(() -> {
                        if (!isAvailable) {
                            binding.etUsername.setError("Username is already taken");
                        } else {
                            // Clear any previous error
                            binding.etUsername.setError(null);
                        }
                    });
                });
            };
            
            // Delay the check by 500ms
            usernameCheckHandler.postDelayed(usernameCheckRunnable, 500);
        }
    }
    
    private void validatePassword() {
        String password = binding.etPassword.getText().toString();
        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
        } else if (password.length() < 8) {
            binding.etPassword.setError("Password must be at least 8 characters");
        } else {
            binding.etPassword.setError(null);
        }
    }
    
    private void validateConfirmPassword() {
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();
        
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.setError("Please confirm your password");
        } else if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
        } else {
            binding.etConfirmPassword.setError(null);
        }
    }
    
    private void validateNursingCareer() {
        String selectedCareer = binding.spinnerNursingCareer.getText().toString().trim();
        if (selectedCareer.isEmpty() || !NURSING_CAREERS.contains(selectedCareer)) {
            binding.spinnerNursingCareer.setError("Please select a nursing career");
        } else {
            binding.spinnerNursingCareer.setError(null);
        }
    }
    
    private boolean validateAllFields() {
        boolean isValid = true;
        
        // Validate required fields
        validateEmail();
        validatePhoneNumber();
        validateUsername();
        validatePassword();
        validateConfirmPassword();
        validateNursingCareer();
        
        // Check if any field has errors
        if (binding.etEmail.getError() != null ||
            binding.etPhoneNumber.getError() != null ||
            binding.etUsername.getError() != null ||
            binding.etPassword.getError() != null ||
            binding.etConfirmPassword.getError() != null ||
            binding.spinnerNursingCareer.getError() != null) {
            isValid = false;
        }
        
        // Check required field values
        if (binding.etEmail.getText().toString().trim().isEmpty() ||
            binding.etPhoneNumber.getText().toString().trim().isEmpty() ||
            binding.etUsername.getText().toString().trim().isEmpty() ||
            binding.etDisplayName.getText().toString().trim().isEmpty() ||
            binding.etPassword.getText().toString().isEmpty() ||
            binding.etConfirmPassword.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        // Check terms and conditions
        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        return isValid;
    }
    
    private void createAccount() {
        if (!validateAllFields()) {
            return;
        }
        
        String email = binding.etEmail.getText().toString().trim();
        String phoneNumber = binding.etPhoneNumber.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();
        String displayName = binding.etDisplayName.getText().toString().trim();
        String nursingCareer = binding.spinnerNursingCareer.getText().toString().trim();
        String yearsExperience = binding.etYearsExperience.getText().toString().trim();
        String currentInstitution = binding.etCurrentInstitution.getText().toString().trim();
        String school = binding.etSchool.getText().toString().trim();
        String year = binding.etYear.getText().toString().trim();
        String course = binding.etCourse.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        
        // Final password match validation
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            return;
        }
        
        // Generate handle from username
        String baseHandle = HandleUtils.generateHandle(username);
        
        // Check handle availability and generate unique handle
        authViewModel.getAllHandles(existingHandles -> {
            String uniqueHandle = HandleUtils.generateUniqueHandle(baseHandle, existingHandles);
            
            // Final username availability check
            authViewModel.checkUsernameAvailability(username, isAvailable -> {
                runOnUiThread(() -> {
                    if (!isAvailable) {
                        binding.etUsername.setError("Username is already taken");
                        Toast.makeText(SignUpActivity.this, "Please choose a different username", Toast.LENGTH_SHORT).show();
                    } else {
                        // Clear any previous errors
                        binding.etUsername.setError(null);
                        // Create account with profile picture and handle
                        if (selectedImageUri != null) {
                            // Upload image first, then create account
                            uploadProfilePictureAndCreateAccount(email, phoneNumber, username, password, displayName, 
                                nursingCareer, yearsExperience, currentInstitution, school, year, course, description, uniqueHandle);
                        } else {
                            // Create account without profile picture
                            authViewModel.signUpWithEmail(email, password, username, displayName, phoneNumber,
                                nursingCareer, yearsExperience, currentInstitution, school, year, course, description, null, uniqueHandle);
                        }
                    }
                });
            });
        });
    }
    
    private void uploadProfilePictureAndCreateAccount(String email, String phoneNumber, String username, String password, 
                                                    String displayName, String nursingCareer, String yearsExperience,
                                                    String currentInstitution, String school, String year, 
                                                    String course, String description, String handle) {
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSignUp.setEnabled(false);
        
        // Upload profile image to Firebase Storage with enhanced compression
        ImageUtils.uploadProfileImageToFirebase(this, selectedImageUri, "temp_user_id", new ImageUtils.ImageUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Create account with profile picture URL and handle
                runOnUiThread(() -> {
                    authViewModel.signUpWithEmail(email, password, username, displayName, phoneNumber,
                        nursingCareer, yearsExperience, currentInstitution, school, year, course, description, downloadUrl, handle);
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSignUp.setEnabled(true);
                });
            }
        });
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
    
    private void setupImageLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            // Save bitmap to temporary file and start cropping
                            Uri tempUri = ImageUtils.saveBitmapToFile(this, imageBitmap);
                            if (tempUri != null) {
                                startImageCropper(tempUri);
                            }
                        }
                    }
                }
            }
        );
        
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startImageCropper(uri);
                }
            }
        );

        // Crop launcher
        cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri croppedImageUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                    if (croppedImageUri != null) {
                        selectedImageUri = croppedImageUri;
                        Bitmap bitmap = ImageUtils.loadAndCompressProfileImage(this, croppedImageUri);
                        if (bitmap != null) {
                            displaySelectedImage(bitmap);
                        }
                    }
                } else if (result.getResultCode() == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
                    Throwable cropError = com.yalantis.ucrop.UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    private void startImageCropper(Uri sourceUri) {
        // Create destination URI for cropped image
        File cacheDir = getCacheDir();
        File croppedImageFile = new File(cacheDir, "cropped_profile_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(croppedImageFile);

        // Configure UCrop
        com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // Square aspect ratio for profile pictures
                .withMaxResultSize(512, 512)
                .start(this);
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else if (which == 1) {
                // Gallery
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }
    
    private void displaySelectedImage(Bitmap bitmap) {
        if (bitmap != null) {
            Glide.with(this)
                .load(bitmap)
                .circleCrop()
                .into(binding.ivProfilePicture);
        }
    }
    
    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSignUp.setEnabled(!isLoading);
            binding.btnSignUp.setEnabled(!isLoading);
            binding.btnGoogleSignUp.setEnabled(!isLoading);
        });
        
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                authViewModel.clearError();
            }
        });
        
        authViewModel.getAuthState().observe(this, authState -> {
            if (authState == AuthViewModel.AuthState.AUTHENTICATED) {
                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                finish();
            } else if (authState == AuthViewModel.AuthState.PROFILE_INCOMPLETE) {
                startActivity(new Intent(SignUpActivity.this, ProfileCompletionActivity.class));
                finish();
            }
        });
    }
} 