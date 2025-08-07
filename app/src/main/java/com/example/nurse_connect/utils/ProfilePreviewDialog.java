package com.example.nurse_connect.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.DialogProfilePreviewBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.models.UserProfile;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfilePreviewDialog {
    
    public static void showProfilePreview(@NonNull Context context, @NonNull User user) {
        showProfilePreview(context, user, null);
    }
    
    public static void showProfilePreview(@NonNull Context context, @NonNull User user, String gpa) {
        // Create dialog
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        
        // Inflate layout
        DialogProfilePreviewBinding binding = DialogProfilePreviewBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        
        // Set dialog width to match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }
        
        // Load profile picture
        if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
            Glide.with(context)
                .load(user.getPhotoURL())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfilePreview);
        } else {
            binding.ivProfilePreview.setImageResource(R.drawable.ic_person);
        }
        
        // Set user information
        binding.tvDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
        binding.tvUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "@unknown");
        
        // Set GPA and course/institution
        UserProfile profile = user.getProfile();
        if (profile != null && profile.getGpa() != null && !profile.getGpa().isEmpty()) {
            binding.tvGpa.setText(profile.getGpa());
        } else if (gpa != null && !gpa.isEmpty()) {
            binding.tvGpa.setText(gpa);
        } else {
            binding.tvGpa.setText("N/A");
        }
        
        // Set course and institution
        if (profile != null) {
            if (profile.getCourse() != null && !profile.getCourse().isEmpty()) {
                binding.tvCourse.setText(profile.getCourse());
            } else {
                binding.tvCourse.setText("Course not specified");
            }
            
            if (profile.getInstitution() != null && !profile.getInstitution().isEmpty()) {
                binding.tvInstitution.setText(profile.getInstitution());
            } else {
                binding.tvInstitution.setText("Institution not specified");
            }
        } else {
            binding.tvCourse.setText("Course not specified");
            binding.tvInstitution.setText("Institution not specified");
        }
        
        // Set close button click listener
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Show dialog
        dialog.show();
    }
} 