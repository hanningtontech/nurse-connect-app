package com.example.nurse_connect.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class ImageCropActivity extends AppCompatActivity {
    
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_CROPPED_URI = "cropped_uri";
    
    private Uri sourceImageUri;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        
        // Get the image URI from intent
        String imageUriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (imageUriString != null) {
            sourceImageUri = Uri.parse(imageUriString);
            startCrop();
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void startCrop() {
        // Create destination URI for cropped image
        File destinationFile = new File(getCacheDir(), "cropped_image_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(destinationFile);
        
        // Detect image aspect ratio and choose appropriate crop settings
        detectImageAspectRatio(sourceImageUri, destinationUri);
    }
    
    private void detectImageAspectRatio(Uri imageUri, Uri destinationUri) {
        try {
            // Get image dimensions
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(imageUri.getPath(), options);
            
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            
            float aspectRatio = (float) originalWidth / originalHeight;
            
            // Choose crop strategy based on original aspect ratio
            if (aspectRatio < 1.0f) {
                // Portrait image (9:16, 3:4, etc.) - Use 9:16 crop to preserve content
                startPortraitCrop(imageUri, destinationUri);
            } else if (aspectRatio > 1.5f) {
                // Wide landscape (2:1, 3:1, etc.) - Use 16:9 crop
                startLandscapeCrop(imageUri, destinationUri);
            } else {
                // Square-ish or moderate landscape - Use 16:9 crop
                startStandardCrop(imageUri, destinationUri);
            }
            
        } catch (Exception e) {
            // Fallback to standard 16:9 crop
            startStandardCrop(imageUri, destinationUri);
        }
    }
    
    private void startPortraitCrop(Uri imageUri, Uri destinationUri) {
        // For portrait images, allow free cropping to preserve full content
        UCrop.of(imageUri, destinationUri)
                .withMaxResultSize(1080, 1920) // Max dimensions
                .withOptions(getCropOptions())
                .start(this);
    }
    
    private void startLandscapeCrop(Uri imageUri, Uri destinationUri) {
        // For landscape images, allow free cropping
        UCrop.of(imageUri, destinationUri)
                .withMaxResultSize(1080, 1920) // Max dimensions
                .withOptions(getCropOptions())
                .start(this);
    }
    
    private void startStandardCrop(Uri imageUri, Uri destinationUri) {
        // For all images, allow free cropping
        UCrop.of(imageUri, destinationUri)
                .withMaxResultSize(1080, 1920) // Max dimensions
                .withOptions(getCropOptions())
                .start(this);
    }
    
    private UCrop.Options getCropOptions() {
        UCrop.Options options = new UCrop.Options();
        
        // UI customization
        options.setStatusBarColor(getResources().getColor(R.color.primary_color));
        options.setToolbarColor(getResources().getColor(R.color.primary_color));
        options.setToolbarTitle("Crop Image");
        
        // Crop settings - Allow free-style cropping for better content preservation
        options.setFreeStyleCropEnabled(true); // Enable free-style for flexible aspect ratios
        options.setShowCropGrid(true); // Show crop grid
        options.setCropGridColor(getResources().getColor(android.R.color.white));
        options.setCropGridStrokeWidth(2);
        
        // Enable zoom and pan for positioning
        options.setHideBottomControls(false);
        options.setCompressionQuality(50); // High quality output
        
        // Allow extreme zoom out (up to 1% of original size)
        options.setMaxScaleMultiplier(100f); // Allow 100x zoom out (1% of original)
        
        return options;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                // Crop successful
                Uri croppedUri = UCrop.getOutput(data);
                if (croppedUri != null) {
                    // Return the cropped image URI
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_CROPPED_URI, croppedUri.toString());
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to get cropped image", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                // Crop failed
                Throwable cropError = UCrop.getError(data);
                Toast.makeText(this, "Crop failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_CANCELED);
                finish();
            } else {
                // User cancelled
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
    }
} 