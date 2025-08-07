package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityCreatePostBinding;
import com.example.nurse_connect.services.CommunityProfileService;
import com.example.nurse_connect.utils.ImageCompressionUtils;
import com.bumptech.glide.Glide;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityCreatePostBinding binding;
    private CommunityProfileService profileService;
    private String selectedPostType = "text"; // Default to text post
    private Uri selectedMediaUri = null;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Launch crop activity instead of directly showing preview
                    launchCropActivity(uri);
                }
            }
    );
    
    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Get cropped image URI
                    String croppedUriString = result.getData().getStringExtra(ImageCropActivity.EXTRA_CROPPED_URI);
                    if (croppedUriString != null) {
                        Uri croppedUri = Uri.parse(croppedUriString);
                        selectedMediaUri = croppedUri;
                        
                        // Load cropped image with Glide for preview
                        Glide.with(this)
                                .load(croppedUri)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .centerCrop()
                                .into(binding.ivMediaPreview);
                        binding.mediaPreviewSection.setVisibility(View.VISIBLE);
                        
                        // Show re-crop button for images
                        binding.btnRecrop.setVisibility(View.VISIBLE);
                        
                        // Show compression info
                        showCompressionInfo(croppedUri);
                    }
                } else {
                    // User cancelled cropping, reset selection
                    selectedMediaUri = null;
                    binding.mediaPreviewSection.setVisibility(View.GONE);
                    binding.btnRecrop.setVisibility(View.GONE);
                }
            }
    );

    private final ActivityResultLauncher<String> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedMediaUri = uri;
                    // For video, we'll show a video thumbnail or placeholder
                    binding.ivMediaPreview.setImageResource(R.drawable.ic_video);
                    binding.mediaPreviewSection.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase service
        profileService = new CommunityProfileService();
        
        // Check if user is authenticated
        if (!profileService.isUserAuthenticated()) {
            Toast.makeText(this, "Please sign in to create posts", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupClickListeners();
        setupPostTypeSelection();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Post button
        binding.btnPost.setOnClickListener(v -> createPost());

        // Remove media button
        binding.btnRemoveMedia.setOnClickListener(v -> {
            selectedMediaUri = null;
            binding.mediaPreviewSection.setVisibility(View.GONE);
            binding.btnRecrop.setVisibility(View.GONE);
        });
        
        // Re-crop button
        binding.btnRecrop.setOnClickListener(v -> {
            if (selectedMediaUri != null) {
                launchCropActivity(selectedMediaUri);
            }
        });
    }

    private void setupPostTypeSelection() {
        // Text post
        binding.cardTextPost.setOnClickListener(v -> {
            selectedPostType = "text";
            updatePostTypeSelection();
            binding.mediaPreviewSection.setVisibility(View.GONE);
            binding.tvCompressionStatus.setVisibility(View.GONE);
            binding.btnRecrop.setVisibility(View.GONE);
            selectedMediaUri = null;
        });

        // Image post
        binding.cardImagePost.setOnClickListener(v -> {
            selectedPostType = "image";
            updatePostTypeSelection();
            openImagePicker();
        });

        // Video post
        binding.cardVideoPost.setOnClickListener(v -> {
            selectedPostType = "video";
            updatePostTypeSelection();
            openVideoPicker();
        });

        // Set initial selection
        updatePostTypeSelection();
    }

    private void updatePostTypeSelection() {
        // Reset all cards
        binding.cardTextPost.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        binding.cardImagePost.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        binding.cardVideoPost.setCardBackgroundColor(getResources().getColor(android.R.color.white));

        // Highlight selected card
        switch (selectedPostType) {
            case "text":
                binding.cardTextPost.setCardBackgroundColor(getResources().getColor(R.color.light_blue_50));
                break;
            case "image":
                binding.cardImagePost.setCardBackgroundColor(getResources().getColor(R.color.light_green_50));
                break;
            case "video":
                binding.cardVideoPost.setCardBackgroundColor(getResources().getColor(R.color.light_red_50));
                break;
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void openVideoPicker() {
        videoPickerLauncher.launch("video/*");
    }

    private void createPost() {
        String content = binding.etPostContent.getText().toString().trim();
        String hashtags = binding.etHashtags.getText().toString().trim();

        // Validate content
        if (content.isEmpty()) {
            binding.etPostContent.setError("Please enter post content");
            return;
        }

        // Validate media for image/video posts
        if ((selectedPostType.equals("image") || selectedPostType.equals("video")) && selectedMediaUri == null) {
            Toast.makeText(this, "Please select " + selectedPostType, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        binding.btnPost.setEnabled(false);
        binding.btnPost.setText("Processing...");

        // Handle image compression for image posts
        if (selectedPostType.equals("image") && selectedMediaUri != null) {
            compressAndCreatePost(content, hashtags);
        } else {
            // For text or video posts, proceed normally
            createPostWithMedia(content, hashtags, selectedMediaUri);
        }
    }
    
    private void compressAndCreatePost(String content, String hashtags) {
        // Show compression status
        binding.tvCompressionStatus.setText("Compressing...");
        binding.tvCompressionStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        
        // Run compression in background
        new Thread(() -> {
            try {
                // Compress image
                File compressedFile = ImageCompressionUtils.compressImage(this, selectedMediaUri);
                
                // Convert file back to URI for upload
                Uri compressedUri = Uri.fromFile(compressedFile);
                
                // Show compression success on main thread
                runOnUiThread(() -> {
                    binding.tvCompressionStatus.setText("Optimized ✓");
                    binding.tvCompressionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    
                    // Create post with compressed image
                    createPostWithMedia(content, hashtags, compressedUri);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.tvCompressionStatus.setText("Compression Failed");
                    binding.tvCompressionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    Toast.makeText(CreatePostActivity.this, "Failed to compress image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetPostButton();
                });
            }
        }).start();
    }
    
    private void createPostWithMedia(String content, String hashtags, Uri mediaUri) {
        // Update button text
        binding.btnPost.setText("Posting...");
        
        // Create post using Firebase service
        profileService.createPost(content, hashtags, selectedPostType, mediaUri, new CommunityProfileService.PostCreationCallback() {
            @Override
            public void onSuccess(String postId) {
                Toast.makeText(CreatePostActivity.this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                
                // Return to Global Nurse Hub
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CreatePostActivity.this, "Failed to create post: " + error, Toast.LENGTH_SHORT).show();
                resetPostButton();
            }
        });
    }
    
    private void launchCropActivity(Uri imageUri) {
        Intent cropIntent = new Intent(this, ImageCropActivity.class);
        cropIntent.putExtra(ImageCropActivity.EXTRA_IMAGE_URI, imageUri.toString());
        cropLauncher.launch(cropIntent);
    }
    
    private void showCompressionInfo(Uri imageUri) {
        // Show compression info in a subtle way
        try {
            String info = ImageCompressionUtils.getCompressionInfo(this, imageUri, null);
            Log.d("CreatePost", "Image compression info: " + info);
        } catch (Exception e) {
            Log.e("CreatePost", "Error getting compression info", e);
        }
    }

    private void resetPostButton() {
        binding.btnPost.setEnabled(true);
        binding.btnPost.setText("Post");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 