package com.example.nurse_connect.ui.downloads;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.StudyMaterialAdapter;
import com.example.nurse_connect.databinding.ActivityDownloadsBinding;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.ui.comments.CommentsActivity;
import com.example.nurse_connect.ui.pdf.PdfViewerActivity;
import com.example.nurse_connect.ui.rating.RatingActivity;
import com.example.nurse_connect.ui.upload.EditDocumentActivity;
import com.example.nurse_connect.viewmodels.StudyMaterialViewModel;
import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.viewmodels.AuthViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class DownloadsActivity extends AppCompatActivity implements StudyMaterialAdapter.OnStudyMaterialClickListener {
    private static final String TAG = "DownloadsActivity";
    private ActivityDownloadsBinding binding;
    private StudyMaterialViewModel viewModel;
    private StudyMaterialAdapter adapter;
    private AuthViewModel authViewModel;
    private List<StudyMaterial> downloadedMaterials = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        loadDownloadedMaterials();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Downloads");
        }
    }

    private void setupRecyclerView() {
        adapter = new StudyMaterialAdapter();
        adapter.setOnStudyMaterialClickListener(this);
        adapter.setMaterials(downloadedMaterials);
        binding.rvDownloads.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDownloads.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(StudyMaterialViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Set AuthViewModel in StudyMaterialViewModel
        viewModel.setAuthViewModel(authViewModel);
        
        viewModel.getStudyMaterials().observe(this, materials -> {
            if (materials != null) {
                // Filter to show only downloaded materials
                List<StudyMaterial> downloaded = new ArrayList<>();
                for (StudyMaterial material : materials) {
                    if (material.isDownloadedByUser()) {
                        downloaded.add(material);
                    }
                }
                downloadedMaterials.clear();
                downloadedMaterials.addAll(downloaded);
                adapter.setMaterials(downloadedMaterials);
                
                updateEmptyState();
            }
        });
    }

    private void loadDownloadedMaterials() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadDownloadedMaterials();
    }

    private void updateEmptyState() {
        if (downloadedMaterials.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvDownloads.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvDownloads.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onMaterialClick(StudyMaterial material) {
        // Increment view count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementViewCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("DownloadsActivity", "View count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (viewModel != null) {
                    viewModel.loadDownloadedMaterials();
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("DownloadsActivity", "Failed to increment view count for: " + material.getId(), e);
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
                Log.d("DownloadsActivity", "Download count incremented successfully for: " + material.getId());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DownloadsActivity", "Failed to increment download count for: " + material.getId(), e);
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
        viewModel.toggleLike(material.getId(), currentUserId, !currentState);
        
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
    public void onCommentClick(StudyMaterial material) {
        // Open comments activity
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    @Override
    public void onCommentsClick(StudyMaterial material) {
        // Open comments activity (same as onCommentClick)
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }

    @Override
    public void onRatingClick(StudyMaterial material) {
        // Open rating activity
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1003); // Use startActivityForResult to refresh counts
    }

    @Override
    public void onEditClick(StudyMaterial material) {
        // Open edit document activity
        Intent intent = new Intent(this, EditDocumentActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material", material);
        startActivityForResult(intent, 1001);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            if (data != null) {
                StudyMaterial updatedMaterial = (StudyMaterial) data.getSerializableExtra("updated_material");
                String deletedMaterialId = data.getStringExtra("deleted_material_id");
                
                if (updatedMaterial != null) {
                    // Update the material in the list
                    for (int i = 0; i < downloadedMaterials.size(); i++) {
                        if (downloadedMaterials.get(i).getId().equals(updatedMaterial.getId())) {
                            downloadedMaterials.set(i, updatedMaterial);
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                } else if (deletedMaterialId != null) {
                    // Remove the deleted material from the list
                    for (int i = 0; i < downloadedMaterials.size(); i++) {
                        if (downloadedMaterials.get(i).getId().equals(deletedMaterialId)) {
                            downloadedMaterials.remove(i);
                            adapter.notifyItemRemoved(i);
                            updateEmptyState();
                            break;
                        }
                    }
                }
            }
        } else if (requestCode == 1002) {
            // Handle result from CommentsActivity - refresh comment counts
            if (resultCode == RESULT_OK && data != null) {
                String materialId = data.getStringExtra("material_id");
                if (materialId != null) {
                    // Refresh the counts for this material
                    viewModel.refreshMaterialCounts(materialId);
                }
            }
            // Note: We don't refresh counts on back press to avoid unnecessary network calls
        } else if (requestCode == 1003 && resultCode == RESULT_OK && data != null) {
            // Handle result from RatingActivity - refresh counts in case rating included a comment
            String materialId = data.getStringExtra("material_id");
            if (materialId != null) {
                // Refresh the counts for this material
                viewModel.refreshMaterialCounts(materialId);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
                Log.d("DownloadsActivity", "Toggled favorite successfully. Is favorite: " + isFavorite);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e("DownloadsActivity", "Failed to toggle favorite", e);
            }
        });
    }
} 