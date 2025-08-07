package com.example.nurse_connect.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.StudyMaterialAdapter;
import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.databinding.ActivityFavoritesBinding;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.ui.comments.CommentsActivity;
import com.example.nurse_connect.ui.rating.RatingActivity;
import com.example.nurse_connect.viewmodels.FavoritesViewModel;
import com.example.nurse_connect.viewmodels.StudyMaterialViewModel;
import com.example.nurse_connect.viewmodels.AuthViewModel;

import java.util.List;
import java.util.Locale;
import android.util.Log;

public class FavoritesActivity extends AppCompatActivity implements StudyMaterialAdapter.OnStudyMaterialClickListener {
    
    private ActivityFavoritesBinding binding;
    private FavoritesViewModel viewModel;
    private StudyMaterialAdapter adapter;
    private AuthViewModel authViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        
        // Load favorites
        viewModel.loadFavorites();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Favorites");
        }
    }
    
    private void setupRecyclerView() {
        adapter = new StudyMaterialAdapter();
        adapter.setOnStudyMaterialClickListener(this);
        
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFavorites.setAdapter(adapter);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Observe favorites
        viewModel.getFavorites().observe(this, favorites -> {
            adapter.setMaterials(favorites);
            updateEmptyState(favorites);
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
    
    private void updateEmptyState(List<StudyMaterial> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvFavorites.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvFavorites.setVisibility(View.VISIBLE);
        }
    }
    
    // StudyMaterialAdapter.OnStudyMaterialClickListener implementation
    @Override
    public void onMaterialClick(StudyMaterial material) {
        // Increment view count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementViewCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("FavoritesActivity", "View count incremented successfully for: " + material.getId());
                // Refresh the favorites list to show updated counts
                if (viewModel != null) {
                    viewModel.loadFavorites();
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("FavoritesActivity", "Failed to increment view count for: " + material.getId(), e);
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
        
        // Increment download count
        viewModel.incrementDownloadCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                Log.d("FavoritesActivity", "Download count incremented successfully for: " + material.getId());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FavoritesActivity", "Failed to increment download count for: " + material.getId(), e);
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
        viewModel.toggleFavorite(material.getId()); // This removes from favorites list
        
        // Also update the like count in the main collection
        StudyMaterialViewModel studyMaterialViewModel = new StudyMaterialViewModel();
        studyMaterialViewModel.toggleLike(material.getId(), currentUserId, !currentState);
        
        // Update the UI immediately for better user experience
        material.setLikedByUser(!currentState);
        adapter.updateMaterialLikeState(material.getId(), !currentState);
        
        String message = !currentState ? 
            "Liked '" + material.getTitle() + "'!" : 
            "Unliked '" + material.getTitle() + "'!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCommentsClick(StudyMaterial material) {
        // Navigate to comments activity
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onCommentClick(StudyMaterial material) {
        // Navigate to comments activity (same as onCommentsClick)
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onRatingClick(StudyMaterial material) {
        // Navigate to rating activity
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("material_id", material.getId());
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
        // Navigate to edit document activity
        Intent intent = new Intent(this, com.example.nurse_connect.ui.upload.EditDocumentActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material", material);
        startActivityForResult(intent, 1001);
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
                    // Create a temporary StudyMaterialViewModel to refresh counts
                    StudyMaterialViewModel studyMaterialViewModel = new StudyMaterialViewModel();
                    studyMaterialViewModel.refreshMaterialCounts(materialId);
                }
            }
            // Note: We don't refresh counts on back press to avoid unnecessary network calls
        } else if (requestCode == 1003 && resultCode == RESULT_OK && data != null) {
            // Handle result from RatingActivity - refresh counts in case rating included a comment
            String materialId = data.getStringExtra("material_id");
            if (materialId != null) {
                // Refresh the counts for this material
                // Create a temporary StudyMaterialViewModel to refresh counts
                StudyMaterialViewModel studyMaterialViewModel = new StudyMaterialViewModel();
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
} 