package com.example.nurse_connect.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import android.os.Bundle;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    
    // Profile picture specific settings
    private static final int PROFILE_IMAGE_SIZE = 512; // Optimal size for profile pictures
    private static final int PROFILE_COMPRESSION_QUALITY = 85; // Good balance between quality and size
    private static final int MAX_FILE_SIZE_BYTES = 500 * 1024; // 500KB max file size
    
    // General image settings
    private static final int MAX_IMAGE_SIZE = 1024; // Max width/height in pixels
    private static final int COMPRESSION_QUALITY = 80; // JPEG compression quality

    public interface ImageUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public interface ImageSelectionCallback {
        void onImageSelected(Uri imageUri);
        void onCancelled();
    }

    public interface ImageCropCallback {
        void onCropSuccess(Uri croppedImageUri);
        void onCropError(Throwable error);
    }

    /**
     * Shows a dialog to choose between camera and gallery
     */
    public static void showImageSourceDialog(Context context, ImageSelectionCallback callback) {
        String[] options = {"Camera", "Gallery"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Camera
                callback.onImageSelected(null); // Will be handled by camera intent
            } else if (which == 1) {
                // Gallery
                callback.onImageSelected(null); // Will be handled by gallery intent
            }
        });
        builder.show();
    }

    /**
     * Enhanced compression algorithm for profile pictures
     */
    public static Bitmap compressProfileImage(Bitmap originalBitmap) {
        if (originalBitmap == null) return null;

        // Step 1: Resize to optimal profile picture size
        Bitmap resizedBitmap = resizeBitmap(originalBitmap, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE);
        
        // Step 2: Apply additional compression if file size is still too large
        byte[] imageData = bitmapToByteArray(resizedBitmap, PROFILE_COMPRESSION_QUALITY);
        
        if (imageData.length > MAX_FILE_SIZE_BYTES) {
            // Apply progressive compression until file size is acceptable
            int quality = PROFILE_COMPRESSION_QUALITY;
            while (imageData.length > MAX_FILE_SIZE_BYTES && quality > 30) {
                quality -= 10;
                imageData = bitmapToByteArray(resizedBitmap, quality);
            }
            
            // If still too large, resize further
            if (imageData.length > MAX_FILE_SIZE_BYTES) {
                int newSize = (int) (PROFILE_IMAGE_SIZE * 0.8);
                resizedBitmap = resizeBitmap(originalBitmap, newSize, newSize);
                imageData = bitmapToByteArray(resizedBitmap, quality);
            }
        }
        
        return resizedBitmap;
    }

    /**
     * Smart compression algorithm that adapts based on image content
     */
    public static Bitmap compressImage(Bitmap originalBitmap) {
        if (originalBitmap == null) return null;

        // Calculate new dimensions while maintaining aspect ratio
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        
        float ratio = Math.min(
            (float) MAX_IMAGE_SIZE / originalWidth,
            (float) MAX_IMAGE_SIZE / originalHeight
        );
        
        int newWidth = Math.round(originalWidth * ratio);
        int newHeight = Math.round(originalHeight * ratio);
        
        // Resize bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
        
        return resizedBitmap;
    }

    /**
     * Resizes bitmap to specified dimensions while maintaining aspect ratio
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        
        // Calculate aspect ratio
        float aspectRatio = (float) originalWidth / originalHeight;
        
        int newWidth, newHeight;
        
        if (originalWidth > originalHeight) {
            // Landscape image
            newWidth = maxWidth;
            newHeight = (int) (maxWidth / aspectRatio);
        } else {
            // Portrait or square image
            newHeight = maxHeight;
            newWidth = (int) (maxHeight * aspectRatio);
        }
        
        // Ensure dimensions don't exceed maximum
        if (newWidth > maxWidth) {
            newWidth = maxWidth;
            newHeight = (int) (maxWidth / aspectRatio);
        }
        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = (int) (maxHeight * aspectRatio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Converts bitmap to byte array with specified compression quality
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        if (bitmap == null) return null;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    /**
     * Converts bitmap to byte array for upload (legacy method)
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        return bitmapToByteArray(bitmap, COMPRESSION_QUALITY);
    }

    /**
     * Uploads profile image to Firebase Storage with enhanced compression
     */
    public static void uploadProfileImageToFirebase(Context context, Uri imageUri, String userId, ImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new IllegalArgumentException("Image URI is null"));
            return;
        }

        // Load and compress the image specifically for profile pictures
        Bitmap bitmap = loadAndCompressProfileImage(context, imageUri);
        if (bitmap == null) {
            callback.onFailure(new IllegalArgumentException("Failed to load image from URI"));
            return;
        }

        // Convert to byte array with profile-specific compression
        byte[] imageData = bitmapToByteArray(bitmap, PROFILE_COMPRESSION_QUALITY);
        if (imageData == null) {
            callback.onFailure(new IllegalArgumentException("Failed to convert image to byte array"));
            return;
        }

        // Upload using the existing method
        uploadImageToFirebase(context, imageData, userId, callback);
    }

    /**
     * Loads and compresses image from URI specifically for profile pictures
     */
    public static Bitmap loadAndCompressProfileImage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            return compressProfileImage(originalBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Error loading profile image", e);
            return null;
        }
    }

    /**
     * Uploads image to Firebase Storage
     */
    public static void uploadImageToFirebase(Context context, byte[] imageData, String userId, ImageUploadCallback callback) {
        if (imageData == null) {
            callback.onFailure(new IllegalArgumentException("Image data is null"));
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        
        // Create unique filename
        String filename = "profile_pictures/" + userId + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(filename);
        
        UploadTask uploadTask = imageRef.putBytes(imageData);
        
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "Image uploaded successfully: " + uri.toString());
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get download URL", e);
                callback.onFailure(e);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload image", e);
            callback.onFailure(e);
        });
    }

    /**
     * Uploads image from URI to Firebase Storage
     */
    public static void uploadImageToFirebase(Context context, Uri imageUri, String userId, ImageUploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new IllegalArgumentException("Image URI is null"));
            return;
        }

        // Load and compress the image
        Bitmap bitmap = loadAndCompressImage(context, imageUri);
        if (bitmap == null) {
            callback.onFailure(new IllegalArgumentException("Failed to load image from URI"));
            return;
        }

        // Convert to byte array
        byte[] imageData = bitmapToByteArray(bitmap);
        if (imageData == null) {
            callback.onFailure(new IllegalArgumentException("Failed to convert image to byte array"));
            return;
        }

        // Upload using the existing method
        uploadImageToFirebase(context, imageData, userId, callback);
    }

    /**
     * Loads and compresses image from URI
     */
    public static Bitmap loadAndCompressImage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            return compressImage(originalBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image", e);
            return null;
        }
    }

    /**
     * Starts image cropping activity using UCrop
     */
    public static void startImageCropper(Activity activity, Uri sourceUri, ImageCropCallback callback) {
        // Create destination URI for cropped image
        File cacheDir = activity.getCacheDir();
        File croppedImageFile = new File(cacheDir, "cropped_profile_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(croppedImageFile);

        // Configure UCrop
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // Square aspect ratio for profile pictures
                .withMaxResultSize(PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE)
                .start(activity);
    }

    /**
     * Creates ActivityResultLauncher for camera
     */
    public static ActivityResultLauncher<Intent> createCameraLauncher(Fragment fragment, ImageSelectionCallback callback) {
        return fragment.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Handle camera result
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            // Save bitmap to temporary file and get URI
                            Uri imageUri = saveBitmapToFile(fragment.requireContext(), imageBitmap);
                            callback.onImageSelected(imageUri);
                        }
                    }
                } else {
                    callback.onCancelled();
                }
            }
        );
    }

    /**
     * Creates ActivityResultLauncher for gallery
     */
    public static ActivityResultLauncher<String> createGalleryLauncher(Fragment fragment, ImageSelectionCallback callback) {
        return fragment.registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    callback.onImageSelected(uri);
                } else {
                    callback.onCancelled();
                }
            }
        );
    }

    /**
     * Creates ActivityResultLauncher for image cropping
     */
    public static ActivityResultLauncher<Intent> createCropLauncher(Fragment fragment, ImageCropCallback callback) {
        return fragment.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri croppedImageUri = UCrop.getOutput(result.getData());
                    if (croppedImageUri != null) {
                        callback.onCropSuccess(croppedImageUri);
                    } else {
                        callback.onCropError(new Exception("Failed to get cropped image"));
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    Throwable cropError = UCrop.getError(result.getData());
                    callback.onCropError(cropError != null ? cropError : new Exception("Crop operation failed"));
                } else {
                    callback.onCropError(new Exception("Crop operation cancelled"));
                }
            }
        );
    }

    /**
     * Saves bitmap to temporary file and returns URI
     */
    public static Uri saveBitmapToFile(Context context, Bitmap bitmap) {
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs();
            
            File imageFile = new File(cachePath, "temp_image.jpg");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file", e);
            return null;
        }
    }

    /**
     * Gets file size in human readable format
     */
    public static String getFileSizeString(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Validates if image file size is acceptable
     */
    public static boolean isImageSizeAcceptable(long fileSizeBytes) {
        return fileSizeBytes <= MAX_FILE_SIZE_BYTES;
    }
} 