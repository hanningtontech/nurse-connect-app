package com.example.nurse_connect.ui.upload;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.FragmentUploadBinding;
import com.example.nurse_connect.models.StudyCategory;
import com.example.nurse_connect.utils.PdfThumbnailGenerator;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.example.nurse_connect.viewmodels.StudyMaterialViewModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadFragment extends Fragment {
    
    private static final String TAG = "UploadFragment";
    private FragmentUploadBinding binding;
    private StudyMaterialViewModel studyMaterialViewModel;
    private AuthViewModel authViewModel;
    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;
    private int selectedFilePageCount;
    private ExecutorService executorService;
    
    private ActivityResultLauncher<Intent> filePickerLauncher;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        studyMaterialViewModel = new ViewModelProvider(this).get(StudyMaterialViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        executorService = Executors.newSingleThreadExecutor();
        
        // Set AuthViewModel in StudyMaterialViewModel for user authentication
        studyMaterialViewModel.setAuthViewModel(authViewModel);
        
        setupFilePicker();
        setupUI();
        observeViewModel();
    }
    
    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        selectedFileName = getFileName(selectedFileUri);
                        selectedFileSize = getFileSize(selectedFileUri);
                        processSelectedFile();
                    }
                }
            }
        );
    }
    
    private void setupUI() {
        // Setup category spinner
        String[] categories = {"Fundamentals", "Medical-Surgical", "Pediatric", "Obstetric", "Psychiatric", "Community Health", "Research", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);
        
        // Setup privacy spinner
        String[] privacyOptions = {"Public", "Private"};
        ArrayAdapter<String> privacyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, privacyOptions);
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPrivacy.setAdapter(privacyAdapter);
        
        // Setup year of study spinner
        String[] years = {"1st Year", "2nd Year", "3rd Year", "4th Year", "Graduate", "Post-Graduate", "Professional Development"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerYear.setAdapter(yearAdapter);
        
        // Setup material type spinner
        String[] materialTypes = {"Lecture Notes", "Study Guide", "Practice Exam", "Case Study", "Research Paper", "Clinical Guidelines", "Reference Material", "Other"};
        ArrayAdapter<String> materialTypeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, materialTypes);
        materialTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMaterialType.setAdapter(materialTypeAdapter);
        
        // Choose PDF button
        binding.btnChoosePdf.setOnClickListener(v -> selectFile());
        
        // Remove document button
        binding.btnRemoveDocument.setOnClickListener(v -> removeSelectedFile());
        
        // Upload button
        binding.btnUpload.setOnClickListener(v -> uploadFile());
        
        // Initially hide document preview card
        binding.cardDocumentPreview.setVisibility(View.GONE);
    }
    
    private void processSelectedFile() {
        if (selectedFileUri == null) return;
        
        // Show document preview card
        binding.cardDocumentPreview.setVisibility(View.VISIBLE);
        
        // Update document info
        binding.tvDocumentName.setText(selectedFileName);
        binding.tvDocumentSize.setText(formatFileSize(selectedFileSize));
        binding.tvDocumentType.setText("PDF");
        
        // Generate thumbnail and get page count in background
        executorService.execute(() -> {
            try {
                // Generate thumbnail
                Bitmap thumbnail = PdfThumbnailGenerator.generateThumbnail(requireContext(), selectedFileUri);
                
                // Get page count
                selectedFilePageCount = PdfThumbnailGenerator.getPageCount(requireContext(), selectedFileUri);
                
                // Update UI on main thread
                requireActivity().runOnUiThread(() -> {
                    if (thumbnail != null) {
                        binding.ivDocumentPreview.setImageBitmap(thumbnail);
                    }
                    binding.tvPageCount.setText(String.valueOf(selectedFilePageCount));
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing PDF file", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error processing PDF file", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void observeViewModel() {
        studyMaterialViewModel.getUploadProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress > 0) {
                binding.llProgress.setVisibility(View.VISIBLE);
                binding.progressBar.setProgress((int) Math.round(progress));
                binding.tvProgressPercentage.setText((int) Math.round(progress) + "%");
            } else {
                binding.llProgress.setVisibility(View.GONE);
            }
        });
        
        studyMaterialViewModel.getUploadSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(requireContext(), "Study material uploaded successfully!", Toast.LENGTH_LONG).show();
                clearForm();
                binding.llProgress.setVisibility(View.GONE);
            }
        });
        
        studyMaterialViewModel.getUploadError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show();
                binding.llProgress.setVisibility(View.GONE);
            }
        });
    }
    
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }
    
    private void removeSelectedFile() {
        selectedFileUri = null;
        selectedFileName = null;
        selectedFileSize = 0;
        selectedFilePageCount = 0;
        binding.cardDocumentPreview.setVisibility(View.GONE);
    }
    
    private void uploadFile() {
        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Please select a PDF file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String tags = binding.etTags.getText().toString().trim();
        String sourceUrl = binding.etSourceUrl.getText().toString().trim();
        
        if (title.isEmpty()) {
            binding.etTitle.setError("Title is required");
            return;
        }
        
        if (description.isEmpty()) {
            binding.etDescription.setError("Description is required");
            return;
        }
        
        String category = binding.spinnerCategory.getSelectedItem().toString();
        String privacy = binding.spinnerPrivacy.getSelectedItem().toString();
        String year = binding.spinnerYear.getSelectedItem().toString();
        String materialType = binding.spinnerMaterialType.getSelectedItem().toString();
        
        // Get current user info
        if (authViewModel.getCurrentUser().getValue() != null) {
            String authorId = authViewModel.getCurrentUser().getValue().getUid();
            String authorName = authViewModel.getCurrentUser().getValue().getDisplayName();
            
            // Create additional metadata
            String additionalMetadata = String.format("Year: %s, Type: %s, Tags: %s, Pages: %d", 
                year, materialType, tags, selectedFilePageCount);
            
            if (!sourceUrl.isEmpty()) {
                additionalMetadata += ", Source: " + sourceUrl;
            }
            
            // Upload the file
            studyMaterialViewModel.uploadStudyMaterial(
                requireContext(),
                selectedFileUri,
                title,
                description + "\n\n" + additionalMetadata,
                category,
                authorId,
                authorName,
                privacy
            );
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearForm() {
        binding.etTitle.setText("");
        binding.etDescription.setText("");
        binding.etTags.setText("");
        binding.etSourceUrl.setText("");
        binding.spinnerCategory.setSelection(0);
        binding.spinnerPrivacy.setSelection(0);
        binding.spinnerYear.setSelection(0);
        binding.spinnerMaterialType.setSelection(0);
        removeSelectedFile();
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (InputStream cursor = requireContext().getContentResolver().openInputStream(uri)) {
                if (cursor != null) {
                    // Try to get the display name from the content resolver
                    try (android.database.Cursor cursor2 = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                        if (cursor2 != null && cursor2.moveToFirst()) {
                            int nameIndex = cursor2.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (nameIndex >= 0) {
                                result = cursor2.getString(nameIndex);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    private long getFileSize(Uri uri) {
        try {
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    return inputStream.available();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        }
        return 0;
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
        binding = null;
    }
} 