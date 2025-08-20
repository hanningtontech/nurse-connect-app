package com.example.nurse_connect.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;
    
    /**
     * Check if storage permissions are granted
     */
    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ doesn't need storage permissions for app-specific directories
            return true;
        } else {
            // Android 10 and below need storage permissions
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Request storage permissions if not granted
     */
    public static void requestStoragePermissions(Activity activity) {
        if (hasStoragePermissions(activity)) {
            return; // Permissions already granted
        }
        
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Android 10 and below need storage permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), STORAGE_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Check if camera permission is granted
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Request camera permission if not granted
     */
    public static void requestCameraPermission(Activity activity) {
        if (!hasCameraPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                1002); // CAMERA_PERMISSION_REQUEST_CODE
        }
    }

    /**
     * Check if audio recording permission is granted
     */
    public static boolean hasAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request audio recording permission if not granted
     */
    public static void requestAudioPermission(Activity activity) {
        if (!hasAudioPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                1003); // AUDIO_PERMISSION_REQUEST_CODE
        }
    }

    /**
     * Check if all WebRTC permissions are granted
     */
    public static boolean hasWebRTCPermissions(Context context) {
        return hasAudioPermission(context) &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request all WebRTC permissions if not granted
     */
    public static void requestWebRTCPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();

        if (!hasAudioPermission(activity)) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                permissions.toArray(new String[0]),
                1004); // WEBRTC_PERMISSION_REQUEST_CODE
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pre-Android 13 doesn't need explicit notification permission
    }
    
    /**
     * Request notification permission if not granted (Android 13+)
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(activity, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    1003); // NOTIFICATION_PERMISSION_REQUEST_CODE
            }
        }
    }

    /**
     * Check if all video WebRTC permissions are granted
     */
    public static boolean hasVideoWebRTCPermissions(Context context) {
        return hasWebRTCPermissions(context) && hasCameraPermission(context);
    }

    /**
     * Request all video WebRTC permissions if not granted
     */
    public static void requestVideoWebRTCPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();

        // Add audio permissions
        if (!hasAudioPermission(activity)) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        }

        // Add camera permission
        if (!hasCameraPermission(activity)) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                permissions.toArray(new String[0]),
                1005); // VIDEO_WEBRTC_PERMISSION_REQUEST_CODE
        }
    }
} 