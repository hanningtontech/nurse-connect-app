package com.example.nurse_connect.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.StudyMaterialAdapter;
import com.example.nurse_connect.databinding.ActivityUserMaterialsBinding;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.ui.comments.CommentsActivity;
import com.example.nurse_connect.ui.pdf.PdfViewerActivity;
import com.example.nurse_connect.ui.rating.RatingActivity;
import com.example.nurse_connect.ui.upload.EditDocumentActivity;
import com.example.nurse_connect.viewmodels.StudyMaterialViewModel;
import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.viewmodels.AuthViewModel;

import java.util.List;
import java.util.Locale;

public class UserMaterialsActivity extends AppCompatActivity implements StudyMaterialAdapter.OnStudyMaterialClickListener {
    
    private ActivityUserMaterialsBinding binding;
    private StudyMaterialViewModel studyMaterialViewModel;
    private StudyMaterialAdapter adapter;
    private AuthViewModel authViewModel;
    
    private String userId;
    private String userName;
    
    // Filter options
    private static final String[] FILTER_OPTIONS = {
        "Most Viewed",
        "Least Viewed", 
        "Most Downloaded",
        "Least Downloaded",
        "Latest Uploaded",
        "Oldest Uploaded"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserMaterialsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get user data from intent
        userId = getIntent().getStringExtra("user_id");
        userName = getIntent().getStringExtra("user_name");
        
        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupToolbar();
        setupRecyclerView();
        setupFilterSpinner();
        setupSearch();
        setupViewModel();
        loadUserMaterials();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(userName != null ? userName + "'s uploads" : "User Materials");
        }
    }
    
    private void setupRecyclerView() {
        adapter = new StudyMaterialAdapter();
        adapter.setOnStudyMaterialClickListener(this);
        
        // Use GridLayoutManager with 2 columns for pairs layout
        binding.rvUserMaterials.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvUserMaterials.setAdapter(adapter);
    }
    
    private void setupFilterSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            FILTER_OPTIONS
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        binding.spinnerFilter.setAdapter(spinnerAdapter);
        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(position);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // If search is empty, reload all materials
                    loadUserMaterials();
                } else {
                    // Perform search
                    searchMaterials(query);
                }
            }
        });
    }
    
    private void setupViewModel() {
        studyMaterialViewModel = new ViewModelProvider(this).get(StudyMaterialViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Set AuthViewModel in StudyMaterialViewModel
        studyMaterialViewModel.setAuthViewModel(authViewModel);
        
        // Observe study materials
        studyMaterialViewModel.getUserMaterials().observe(this, materials -> {
            adapter.setMaterials(materials);
            updateEmptyState(materials);
            updateDocumentCount(materials != null ? materials.size() : 0);
        });
        
        // Observe loading state
        studyMaterialViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observe error messages
        studyMaterialViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                studyMaterialViewModel.clearError();
            }
        });
    }
    
    private void loadUserMaterials() {
        if (userId != null) {
            studyMaterialViewModel.loadUserMaterials(userId);
        }
    }
    
    private void searchMaterials(String query) {
        if (userId != null) {
            studyMaterialViewModel.searchUserMaterials(userId, query);
        }
    }
    
    private void applyFilter(int filterPosition) {
        switch (filterPosition) {
            case 0: // Most Viewed
                studyMaterialViewModel.sortByMostViewed();
                break;
            case 1: // Least Viewed
                studyMaterialViewModel.sortByLeastViewed();
                break;
            case 2: // Most Downloaded
                studyMaterialViewModel.sortUserMaterialsByMostDownloaded();
                break;
            case 3: // Least Downloaded
                studyMaterialViewModel.sortUserMaterialsByLeastDownloaded();
                break;
            case 4: // Latest Uploaded
                studyMaterialViewModel.sortByLatestUploaded();
                break;
            case 5: // Oldest Uploaded
                studyMaterialViewModel.sortByOldestUploaded();
                break;
        }
    }
    
    private void updateEmptyState(List<StudyMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvUserMaterials.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvUserMaterials.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateDocumentCount(int count) {
        binding.tvDocumentCount.setText("(" + count + " documents)");
    }
    
    // StudyMaterialAdapter.OnStudyMaterialClickListener implementations
    @Override
    public void onMaterialClick(StudyMaterial material) {
        // Increment view count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementViewCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("UserMaterialsActivity", "View count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (studyMaterialViewModel != null) {
                    studyMaterialViewModel.loadStudyMaterialsByUser(userId);
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("UserMaterialsActivity", "Failed to increment view count for: " + material.getId(), e);
            }
        });
        
        // Open PDF viewer
        Intent intent = new Intent(this, com.example.nurse_connect.ui.pdf.PdfViewerActivity.class);
        intent.putExtra("pdf_url", material.getFileUrl());
        intent.putExtra("pdf_title", material.getTitle());
        intent.putExtra("material_id", material.getId());
        startActivity(intent);
    }
    
    @Override
    public void onDownloadClick(StudyMaterial material) {
        // Check storage permissions before downloading
        if (!com.example.nurse_connect.utils.PermissionUtils.hasStoragePermissions(this)) {
            com.example.nurse_connect.utils.PermissionUtils.requestStoragePermissions(this);
            Toast.makeText(this, "Storage permission required for downloads", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Increment download count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementDownloadCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("UserMaterialsActivity", "Download count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (studyMaterialViewModel != null) {
                    studyMaterialViewModel.loadStudyMaterialsByUser(userId);
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("UserMaterialsActivity", "Failed to increment download count for: " + material.getId(), e);
            }
        });
        
        // Launch download service to save to NURSE_CONNECT folder
        Intent downloadIntent = new Intent(this, com.example.nurse_connect.services.PdfDownloadService.class);
        downloadIntent.putExtra("pdf_url", material.getFileUrl());
        downloadIntent.putExtra("pdf_title", material.getTitle());
        downloadIntent.putExtra("material_id", material.getId());
        startService(downloadIntent);
        
        Toast.makeText(this, "Downloading PDF to NURSE_CONNECT folder...", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onLikeClick(StudyMaterial material) {
        // Get current user ID
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to like materials", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Toggle like in the main study_materials collection
        boolean currentState = material.isLikedByUser();
        studyMaterialViewModel.toggleLike(material.getId(), currentUserId, !currentState);
        
        // Also manage favorites collection
        toggleFavorite(material, currentUserId);
        
        // Update the UI immediately for better user experience
        material.setLikedByUser(!currentState);
        adapter.updateMaterialLikeState(material.getId(), !currentState);
        
        String message = !currentState ? 
            "Liked '" + material.getTitle() + "' and added to favorites!" : 
            "Unliked '" + material.getTitle() + "' and removed from favorites!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCommentsClick(StudyMaterial material) {
        // Navigate to comments activity
        Intent intent = new Intent(this, com.example.nurse_connect.ui.comments.CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onCommentClick(StudyMaterial material) {
        // Navigate to comments activity (same as onCommentsClick)
        Intent intent = new Intent(this, com.example.nurse_connect.ui.comments.CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onRatingClick(StudyMaterial material) {
        // Navigate to rating activity
        Intent intent = new Intent(this, com.example.nurse_connect.ui.rating.RatingActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1003); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onAuthorClick(StudyMaterial material) {
        // Navigate to author's profile
        Intent intent = new Intent(this, com.example.nurse_connect.ui.profile.UserProfileActivity.class);
        intent.putExtra("user_id", material.getAuthorId());
        intent.putExtra("user_name", material.getAuthorName());
        startActivity(intent);
    }
    
    @Override
    public void onEditClick(StudyMaterial material) {
        // Ensure Timestamp fields are initialized if they're null after deserialization
        if (material.getCreatedAt() == null) {
            material.setCreatedAt(com.google.firebase.Timestamp.now());
        }
        if (material.getUpdatedAt() == null) {
            material.setUpdatedAt(com.google.firebase.Timestamp.now());
        }
        
        Intent intent = new Intent(this, com.example.nurse_connect.ui.upload.EditDocumentActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material", material); // Pass the entire material object
        startActivityForResult(intent, 1001); // Use startActivityForResult to handle updates
    }

    @Override
    public void onThumbnailClick(StudyMaterial material) {
        showThumbnailDialog(material);
    }

    @Override
    public void onTitleClick(StudyMaterial material) {
        // Open custom PDF viewer
        Intent intent = new Intent(this, com.example.nurse_connect.ui.pdf.CustomPdfViewerActivity.class);
        intent.putExtra("pdf_url", material.getFileUrl());
        intent.putExtra("pdf_title", material.getTitle());
        intent.putExtra("material_id", material.getId());
        startActivity(intent);
    }

    private void showThumbnailDialog(StudyMaterial material) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_thumbnail_preview, null);
        
        ImageView ivThumbnailPreview = dialogView.findViewById(R.id.ivThumbnailPreview);
        TextView tvPreviewTitle = dialogView.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewType = dialogView.findViewById(R.id.tvPreviewType);
        TextView tvPreviewSize = dialogView.findViewById(R.id.tvPreviewSize);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        
        // Set document info
        tvPreviewTitle.setText(material.getTitle());
        tvPreviewType.setText(material.getType() + " Document");
        tvPreviewSize.setText(formatFileSize(material.getFileSize()));
        
        // Load thumbnail - use same logic as adapter
        if (material.getThumbnailURL() != null && !material.getThumbnailURL().isEmpty()) {
                         Glide.with(this)
                     .load(material.getThumbnailURL())
                     .transition(DrawableTransitionOptions.withCrossFade())
                     .placeholder(R.drawable.ic_document)
                     .error(R.drawable.ic_document)
                     .override(800, 1000) // Set higher resolution for better quality
                     .centerInside() // Ensure the image fits within bounds without cropping
                    .into(ivThumbnailPreview);
        } else {
            // Fallback to category-based icon (same as adapter)
            String category = material.getCategory();
            if (category != null) {
                switch (category.toLowerCase()) {
                    case "pdf":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                    case "video":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_video);
                        break;
                    case "audio":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_audio);
                        break;
                    case "image":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_image);
                        break;
                    default:
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                }
            } else {
                // If category is also null, use type as fallback
                switch (material.getType().toLowerCase()) {
                    case "pdf":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                    case "video":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_video);
                        break;
                    case "audio":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_audio);
                        break;
                    case "image":
                        ivThumbnailPreview.setImageResource(R.drawable.ic_image);
                        break;
                    default:
                        ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                        break;
                }
            }
        }
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    

    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            // Handle result from EditDocumentActivity
            StudyMaterial updatedMaterial = (StudyMaterial) data.getSerializableExtra("updated_material");
            String deletedMaterialId = data.getStringExtra("deleted_material_id");
            
            if (updatedMaterial != null) {
                // Update the material in the adapter
                adapter.updateMaterial(updatedMaterial);
                Toast.makeText(this, "Document updated successfully!", Toast.LENGTH_SHORT).show();
            } else if (deletedMaterialId != null) {
                // Remove the deleted material from the adapter
                adapter.removeMaterial(deletedMaterialId);
                Toast.makeText(this, "Document deleted successfully!", Toast.LENGTH_SHORT).show();
                
                // Update empty state if needed
                if (adapter.getItemCount() == 0) {
                    updateEmptyState(adapter.getMaterials());
                }
            }
        } else if (requestCode == 1002) {
            // Handle result from CommentsActivity - refresh comment counts
            if (resultCode == RESULT_OK && data != null) {
                String materialId = data.getStringExtra("material_id");
                if (materialId != null) {
                    // Refresh the counts for this material
                    studyMaterialViewModel.refreshMaterialCounts(materialId);
                }
            }
            // Note: We don't refresh counts on back press to avoid unnecessary network calls
        } else if (requestCode == 1003 && resultCode == RESULT_OK && data != null) {
            // Handle result from RatingActivity - refresh counts in case rating included a comment
            String materialId = data.getStringExtra("material_id");
            if (materialId != null) {
                // Refresh the counts for this material
                studyMaterialViewModel.refreshMaterialCounts(materialId);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
    
    private String getCurrentUserId() {
        if (authViewModel != null && authViewModel.getCurrentUser().getValue() != null) {
            return authViewModel.getCurrentUser().getValue().getUid();
        }
        return null;
    }
    
    private void toggleFavorite(StudyMaterial material, String userId) {
        com.example.nurse_connect.data.FavoritesRepository favoritesRepository = new com.example.nurse_connect.data.FavoritesRepository();
        favoritesRepository.toggleFavorite(material.getId(), new com.example.nurse_connect.data.FavoritesRepository.ToggleFavoriteCallback() {
            @Override
            public void onSuccess(boolean isFavorite) {
                Log.d("UserMaterialsActivity", "Toggled favorite successfully. Is favorite: " + isFavorite);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e("UserMaterialsActivity", "Failed to toggle favorite", e);
            }
        });
    }
} 