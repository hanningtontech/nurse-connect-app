package com.example.nurse_connect.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressionUtils {
    private static final String TAG = "ImageCompressionUtils";
    
    // WhatsApp-like compression settings
    private static final int MAX_IMAGE_WIDTH = 1080;  // WhatsApp max width
    private static final int MAX_IMAGE_HEIGHT = 1920; // WhatsApp max height
    private static final int COMPRESSION_QUALITY = 85; // WhatsApp-like quality (85%)
    private static final int MAX_FILE_SIZE_KB = 1024; // Target max file size (1MB)
    
    /**
     * Compress and resize image to WhatsApp-like standards
     * @param context Application context
     * @param imageUri Original image URI
     * @return Compressed image file
     */
    public static File compressImage(Context context, Uri imageUri) throws IOException {
        // Decode image dimensions first
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        // Calculate sample size for initial decoding
        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
        options.inJustDecodeBounds = false;
        
        // Decode bitmap with sample size
        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        if (originalBitmap == null) {
            throw new IOException("Failed to decode image");
        }
        
        // Resize if needed
        Bitmap resizedBitmap = resizeBitmap(originalBitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
        
        // Compress with quality settings
        File compressedFile = compressBitmapToFile(context, resizedBitmap, COMPRESSION_QUALITY);
        
        // Clean up bitmaps
        if (originalBitmap != resizedBitmap) {
            originalBitmap.recycle();
        }
        resizedBitmap.recycle();
        
        return compressedFile;
    }
    
    /**
     * Calculate optimal sample size for initial decoding
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Check if resizing is needed
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }
        
        // Calculate new dimensions maintaining aspect ratio
        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        // Create scaled bitmap
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
    
    /**
     * Compress bitmap to file with specified quality
     */
    private static File compressBitmapToFile(Context context, Bitmap bitmap, int quality) throws IOException {
        // Create temporary file
        File tempFile = File.createTempFile("compressed_image_", ".jpg", context.getCacheDir());
        
        // Compress to file
        FileOutputStream fos = new FileOutputStream(tempFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        fos.close();
        
        // Check file size and compress further if needed
        long fileSize = tempFile.length();
        if (fileSize > MAX_FILE_SIZE_KB * 1024) {
            // File is too large, compress further
            int newQuality = (int) (quality * (MAX_FILE_SIZE_KB * 1024.0 / fileSize));
            newQuality = Math.max(50, newQuality); // Don't go below 50% quality
            
            fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, newQuality, fos);
            fos.close();
            
            Log.d(TAG, "Further compressed image with quality: " + newQuality);
        }
        
        Log.d(TAG, "Compressed image saved to: " + tempFile.getAbsolutePath() + 
                " (Size: " + (tempFile.length() / 1024) + "KB)");
        
        return tempFile;
    }
    
    /**
     * Get compression info for logging
     */
    public static String getCompressionInfo(Context context, Uri originalUri, File compressedFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(originalUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            long originalSize = getFileSize(context, originalUri);
            long compressedSize = compressedFile.length();
            
            return String.format("Original: %dx%d, %dKB | Compressed: %dKB | Ratio: %.1f%%", 
                    options.outWidth, options.outHeight, originalSize / 1024, 
                    compressedSize / 1024, (compressedSize * 100.0 / originalSize));
        } catch (Exception e) {
            return "Compression info unavailable";
        }
    }
    
    /**
     * Get file size from URI
     */
    private static long getFileSize(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            long size = inputStream.available();
            inputStream.close();
            return size;
        } catch (Exception e) {
            return 0;
        }
    }
} 