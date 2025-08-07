package com.example.nurse_connect.ui.studyhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.StudyMaterialAdapter;
import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.databinding.FragmentStudyHubBinding;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.ui.upload.EditDocumentActivity;
import com.example.nurse_connect.viewmodels.AuthViewModel;
import com.example.nurse_connect.viewmodels.StudyMaterialViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;
import android.util.Log;

public class StudyHubFragment extends Fragment implements StudyMaterialAdapter.OnStudyMaterialClickListener {
    
    private FragmentStudyHubBinding binding;
    private StudyMaterialViewModel viewModel;
    private AuthViewModel authViewModel;
    private StudyMaterialAdapter adapter;
    private String currentCategoryFilter = "All Categories";
    private boolean isKeyboardVisible = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudyHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(StudyMaterialViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        
        // Set AuthViewModel in StudyMaterialViewModel for user authentication
        viewModel.setAuthViewModel(authViewModel);
        
        setupRecyclerView();
        setupUI();
        observeViewModel();
        
        // Update initial filter status
        updateFilterStatus();
        
        // Load study materials
        viewModel.loadStudyMaterials();
    }
    
    private void setupRecyclerView() {
        adapter = new StudyMaterialAdapter();
        adapter.setOnStudyMaterialClickListener(this);
        
        binding.rvMaterials.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMaterials.setAdapter(adapter);
    }
    
    private void setupUI() {
        // Setup category spinner
        setupCategorySpinner();
        

        
        // Search functionality - TextInputEditText with expandable design
        binding.searchView.setOnEditorActionListener((v, actionId, event) -> {
            String searchQuery = binding.searchView.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                // Check if it's a user search (starts with @)
                if (searchQuery.startsWith("@")) {
                    String username = searchQuery.substring(1); // Remove @ symbol
                    searchUserByUsername(username);
                } else {
                    // Search for documents (will search within current category if one is selected)
                    searchMaterials(searchQuery);
                }
            } else {
                // If search is empty, reload based on current category
                clearSearch();
            }
            return true;
        });
        
        // Add text change listener for real-time search
        binding.searchView.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String searchQuery = s.toString().trim();
                if (searchQuery.isEmpty()) {
                    clearSearch();
                }
            }
        });

        // Setup Favorites button
        binding.btnFavorites.setOnClickListener(v -> openFavorites());
        
        // Setup Downloads button
        binding.btnDownloads.setOnClickListener(v -> openDownloads());
        
        // Setup Search button - Expandable search functionality
        binding.btnSearch.setOnClickListener(v -> {
            Log.d("StudyHubFragment", "Search button clicked");
            toggleSearchVisibility();
        });
        
        // Setup close search button
        binding.btnCloseSearch.setOnClickListener(v -> {
            hideSearch();
        });
        
        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!currentCategoryFilter.equals("All Categories")) {
                viewModel.loadStudyMaterialsByCategory(currentCategoryFilter);
            } else {
                viewModel.loadStudyMaterials();
            }
        });
        
        // Setup keyboard visibility detection
        setupKeyboardVisibilityDetection();
    }
    
    private void setupCategorySpinner() {
        // Define categories (matching the upload categories)
        String[] categories = {"All Categories", "Fundamentals", "Medical-Surgical", "Pediatric", "Obstetric", "Psychiatric", "Community Health", "Research", "Other"};
        
        android.widget.ArrayAdapter<String> categoryAdapter = new android.widget.ArrayAdapter<>(
            requireContext(), 
            android.R.layout.simple_spinner_item, 
            categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);
        
        // Set initial selection to "All Categories"
        binding.spinnerCategory.setSelection(0);
        
        // Category selection listener
        binding.spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                currentCategoryFilter = selectedCategory;
                
                // Clear search when changing categories
                binding.searchView.setText("");
                
                // Update filter status
                updateFilterStatus();
                
                if (!selectedCategory.equals("All Categories")) {
                    viewModel.loadStudyMaterialsByCategory(selectedCategory);
                } else {
                    viewModel.loadStudyMaterials();
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                currentCategoryFilter = "All Categories";
                viewModel.loadStudyMaterials();
            }
        });
    }
    
    private void observeViewModel() {
        viewModel.getStudyMaterials().observe(getViewLifecycleOwner(), materials -> {
            adapter.setMaterials(materials);
            updateEmptyState(materials);
            

        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefreshLayout.setRefreshing(isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
        
        viewModel.getUploadedMaterial().observe(getViewLifecycleOwner(), material -> {
            if (material != null) {
                Toast.makeText(getContext(), "Study material uploaded successfully!", Toast.LENGTH_SHORT).show();
                viewModel.clearUploadProgress();
            }
        });
    }
    
    private void updateEmptyState(List<StudyMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvMaterials.setVisibility(View.GONE);
            
            // Update empty state message based on current filter
            if (!currentCategoryFilter.equals("All Categories")) {
                binding.tvEmptyStateText.setText("No materials found in " + currentCategoryFilter + " category");
            } else {
                binding.tvEmptyStateText.setText("No study materials available");
            }
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvMaterials.setVisibility(View.VISIBLE);
        }
    }
    
    // StudyMaterialAdapter.OnStudyMaterialClickListener implementation
    @Override
    public void onMaterialClick(StudyMaterial material) {
        // Show material details or open PDF viewer
        showMaterialDetails(material);
    }
    
    @Override
    public void onDownloadClick(StudyMaterial material) {
        downloadMaterial(material);
    }
    
    @Override
    public void onLikeClick(StudyMaterial material) {
        // Get current user ID
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Please sign in to like materials", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCommentsClick(StudyMaterial material) {
        // Navigate to comments activity
        openCommentsActivity(material);
    }
    
    @Override
    public void onCommentClick(StudyMaterial material) {
        // Navigate to comments activity (same as onCommentsClick)
        openCommentsActivity(material);
    }
    
    @Override
    public void onRatingClick(StudyMaterial material) {
        // Navigate to rating activity
        openRatingActivity(material);
    }
    
    @Override
    public void onAuthorClick(StudyMaterial material) {
        // Navigate to author's profile
        openAuthorProfile(material);
    }

    @Override
    public void onEditClick(StudyMaterial material) {
        openEditDocumentActivity(material);
    }

    @Override
    public void onThumbnailClick(StudyMaterial material) {
        showThumbnailDialog(material);
    }

    @Override
    public void onTitleClick(StudyMaterial material) {
        // Open the document directly
        showMaterialDetails(material);
    }
    
    private void showMaterialDetails(StudyMaterial material) {
        // Increment view count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementViewCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                Log.d("StudyHubFragment", "View count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (viewModel != null) {
                    viewModel.loadStudyMaterials();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("StudyHubFragment", "Failed to increment view count for: " + material.getId(), e);
            }
        });
        
        // Open custom PDF viewer
        Intent intent = new Intent(getActivity(), com.example.nurse_connect.ui.pdf.CustomPdfViewerActivity.class);
        intent.putExtra("pdf_url", material.getFileUrl());
        intent.putExtra("pdf_title", material.getTitle());
        intent.putExtra("material_id", material.getId());
        startActivity(intent);
    }
    
    private void downloadMaterial(StudyMaterial material) {
        // Check storage permissions before downloading
        if (!com.example.nurse_connect.utils.PermissionUtils.hasStoragePermissions(requireContext())) {
            com.example.nurse_connect.utils.PermissionUtils.requestStoragePermissions(requireActivity());
            Toast.makeText(getContext(), "Storage permission required for downloads", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Increment download count for all users (including owners and guests)
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.incrementDownloadCount(material.getId(), new StudyMaterialRepository.IncrementCallback() {
            @Override
            public void onSuccess() {
                Log.d("StudyHubFragment", "Download count incremented successfully for: " + material.getId());
                // Refresh the material list to show updated counts
                if (viewModel != null) {
                    viewModel.loadStudyMaterials();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("StudyHubFragment", "Failed to increment download count for: " + material.getId(), e);
            }
        });
        
        // Launch download service to save to NURSE_CONNECT folder
        Intent downloadIntent = new Intent(getActivity(), com.example.nurse_connect.services.PdfDownloadService.class);
        downloadIntent.putExtra("pdf_url", material.getFileUrl());
        downloadIntent.putExtra("pdf_title", material.getTitle());
        downloadIntent.putExtra("material_id", material.getId());
        getActivity().startService(downloadIntent);
        
        Toast.makeText(getContext(), "Downloading PDF to NURSE_CONNECT folder...", Toast.LENGTH_LONG).show();
    }
    
    private void openCommentsActivity(StudyMaterial material) {
        Intent intent = new Intent(getContext(), com.example.nurse_connect.ui.comments.CommentsActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1002); // Use startActivityForResult to refresh counts
    }
    
    private void openAuthorProfile(StudyMaterial material) {
        Intent intent = new Intent(getContext(), com.example.nurse_connect.ui.profile.UserProfileActivity.class);
        intent.putExtra("user_id", material.getAuthorId());
        intent.putExtra("user_name", material.getAuthorName());
        startActivity(intent);
    }
    
    private void openRatingActivity(StudyMaterial material) {
        Intent intent = new Intent(getActivity(), com.example.nurse_connect.ui.rating.RatingActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material_title", material.getTitle());
        startActivityForResult(intent, 1003); // Use startActivityForResult to refresh counts
    }

    private void openEditDocumentActivity(StudyMaterial material) {
        // Ensure Timestamp fields are initialized if they're null after deserialization
        if (material.getCreatedAt() == null) {
            material.setCreatedAt(com.google.firebase.Timestamp.now());
        }
        if (material.getUpdatedAt() == null) {
            material.setUpdatedAt(com.google.firebase.Timestamp.now());
        }
        
        Intent intent = new Intent(getContext(), EditDocumentActivity.class);
        intent.putExtra("material_id", material.getId());
        intent.putExtra("material", material); // Pass the entire material object
        startActivityForResult(intent, 1001); // Use startActivityForResult to handle updates
    }

    private void showThumbnailDialog(StudyMaterial material) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_thumbnail_preview, null);
        
        ImageView ivThumbnailPreview = dialogView.findViewById(R.id.ivThumbnailPreview);
        TextView tvPreviewTitle = dialogView.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewType = dialogView.findViewById(R.id.tvPreviewType);
        TextView tvPreviewSize = dialogView.findViewById(R.id.tvPreviewSize);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        
        // Set document info
        tvPreviewTitle.setText(material.getTitle());
        tvPreviewType.setText(material.getType() + " Document");
        tvPreviewSize.setText(formatFileSize(material.getFileSize()));
        
        // Add logging to debug the issue
        Log.d("ThumbnailDialog", "Material title: " + material.getTitle());
        Log.d("ThumbnailDialog", "Thumbnail URL: " + material.getThumbnailURL());
        Log.d("ThumbnailDialog", "Category: " + material.getCategory());
        Log.d("ThumbnailDialog", "Type: " + material.getType());
        Log.d("ThumbnailDialog", "ImageView found: " + (ivThumbnailPreview != null));
        if (ivThumbnailPreview != null) {
            Log.d("ThumbnailDialog", "ImageView visibility: " + ivThumbnailPreview.getVisibility());
            Log.d("ThumbnailDialog", "ImageView width: " + ivThumbnailPreview.getWidth());
            Log.d("ThumbnailDialog", "ImageView height: " + ivThumbnailPreview.getHeight());
        }
        
        // Load thumbnail - use same logic as adapter
        if (material.getThumbnailURL() != null && !material.getThumbnailURL().isEmpty()) {
            Log.d("ThumbnailDialog", "Loading thumbnail from URL: " + material.getThumbnailURL());
                         Glide.with(requireContext())
                     .load(material.getThumbnailURL())
                     .transition(DrawableTransitionOptions.withCrossFade())
                     .placeholder(R.drawable.ic_document)
                     .error(R.drawable.ic_document)
                     .override(800, 1000) // Set higher resolution for better quality
                     .centerInside() // Ensure the image fits within bounds without cropping
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e("ThumbnailDialog", "Failed to load image: " + (e != null ? e.getMessage() : "Unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("ThumbnailDialog", "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(ivThumbnailPreview);
        } else {
            Log.d("ThumbnailDialog", "No thumbnail URL, using fallback icon");
            // Fallback to category-based icon (same as adapter)
            String category = material.getCategory();
            if (category != null) {
                Log.d("ThumbnailDialog", "Using category fallback: " + category);
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
                Log.d("ThumbnailDialog", "Category is null, using type fallback: " + material.getType());
                // If category is also null, use type as fallback
                String type = material.getType();
                if (type != null) {
                    switch (type.toLowerCase()) {
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
                    Log.d("ThumbnailDialog", "Type is also null, using default icon");
                    // Final fallback
                    ivThumbnailPreview.setImageResource(R.drawable.ic_document);
                }
            }
        }
        
        // Add logging after setting the image
        Log.d("ThumbnailDialog", "Image set, checking final state");
        Log.d("ThumbnailDialog", "ImageView drawable: " + (ivThumbnailPreview.getDrawable() != null));

             builder.setView(dialogView);
             AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Configure dialog window properties
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            // Add margins to prevent dialog from touching screen edges
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT - 32, 
                           android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            Log.d("ThumbnailDialog", "Dialog window configured");
        }
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        Log.d("ThumbnailDialog", "Dialog shown");
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

    private void showDeleteConfirmationDialog(StudyMaterial material) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete \"" + material.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteMaterial(material);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMaterial(StudyMaterial material) {
        // Show loading state
        Toast.makeText(requireContext(), "Deleting document...", Toast.LENGTH_SHORT).show();
        
        // Get the file name from the file URL
        String fileName = material.getFileUrl();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        
        // Delete the material
        StudyMaterialRepository repository = new StudyMaterialRepository();
        repository.deleteStudyMaterial(material.getId(), fileName, () -> {
            // Success callback
            requireActivity().runOnUiThread(() -> {
                // Remove from adapter
                adapter.removeMaterial(material.getId());
                
                // Show success message
                Toast.makeText(requireContext(), "Document deleted successfully", Toast.LENGTH_SHORT).show();
                
                // Update empty state if needed
                if (adapter.getItemCount() == 0) {
                    updateEmptyState(adapter.getMaterials());
                }
            });
        });
    }

    private void openFollowers() {
        // Get current user ID and open followers
        String currentUserId = authViewModel.getCurrentUser().getValue().getUid();
        Intent intent = new Intent(getContext(), com.example.nurse_connect.ui.profile.FollowersActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivity(intent);
    }

    private void openFollowing() {
        // Get current user ID and open following
        String currentUserId = authViewModel.getCurrentUser().getValue().getUid();
        Intent intent = new Intent(getContext(), com.example.nurse_connect.ui.profile.FollowingActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivity(intent);
    }
    
    private void openFavorites() {
        Intent intent = new Intent(getActivity(), com.example.nurse_connect.ui.favorites.FavoritesActivity.class);
        startActivity(intent);
    }
    
    private void openDownloads() {
        Intent intent = new Intent(getActivity(), com.example.nurse_connect.ui.downloads.DownloadsActivity.class);
        startActivity(intent);
    }

    private void toggleSearchVisibility() {
        if (binding.searchCard.getVisibility() == View.VISIBLE) {
            hideSearch();
        } else {
            showSearch();
        }
    }
    
    private void showSearch() {
        binding.searchCard.setVisibility(View.VISIBLE);
        binding.searchView.requestFocus();
        // Animate the search card appearance
        binding.searchCard.setAlpha(0f);
        binding.searchCard.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }
    
    private void hideSearch() {
        binding.searchView.setText("");
        binding.searchCard.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    binding.searchCard.setVisibility(View.GONE);
                    binding.searchCard.setAlpha(1f);
                })
                .start();
        // Clear search results
        clearSearch();
        
        // Ensure UI elements are visible when search is hidden
        if (!isKeyboardVisible) {
            showUIElements();
        }
    }
    
    private void performSearch() {
        String searchQuery = binding.searchView.getText().toString().trim();
        
        if (!searchQuery.isEmpty()) {
            if (searchQuery.startsWith("@")) {
                String username = searchQuery.substring(1);
                searchUserByUsername(username);
            } else {
                searchMaterials(searchQuery);
            }
        } else {
            Toast.makeText(getContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void searchMaterials(String query) {
        Log.d("StudyHubFragment", "searchMaterials called with query: " + query);
        Log.d("StudyHubFragment", "Current category filter: " + currentCategoryFilter);
        
        // Search within the current category filter
        if (!currentCategoryFilter.equals("All Categories")) {
            // Search within the specific category
            Log.d("StudyHubFragment", "Searching within category: " + currentCategoryFilter);
            viewModel.searchStudyMaterialsByCategory(query, currentCategoryFilter);
        } else {
            // Search all materials
            Log.d("StudyHubFragment", "Searching all materials");
            viewModel.searchStudyMaterials(query);
        }
    }
    
    private void clearSearch() {
        // Clear search and reload based on current category
        updateFilterStatus();
        if (!currentCategoryFilter.equals("All Categories")) {
            viewModel.loadStudyMaterialsByCategory(currentCategoryFilter);
        } else {
            viewModel.loadStudyMaterials();
        }
    }
    
    private void updateFilterStatus() {
        if (!currentCategoryFilter.equals("All Categories")) {
            binding.tvFilterStatus.setText("Filtered by: " + currentCategoryFilter);
            binding.tvFilterStatus.setVisibility(View.VISIBLE);
            // Update search hint to reflect category filter
            setSearchHint("Search in " + currentCategoryFilter + " or @username...");
        } else {
            binding.tvFilterStatus.setVisibility(View.GONE);
            // Update search hint for all categories
            setSearchHint("Search all documents ...");
        }
    }
    
    private void setSearchHint(String hint) {
        // Find the TextInputLayout by traversing up the view hierarchy
        ViewParent parent = binding.searchView.getParent();
        while (parent != null && !(parent instanceof com.google.android.material.textfield.TextInputLayout)) {
            parent = parent.getParent();
        }
        if (parent instanceof com.google.android.material.textfield.TextInputLayout) {
            ((com.google.android.material.textfield.TextInputLayout) parent).setHint(hint);
        }
    }
    
    private void setupKeyboardVisibilityDetection() {
        // Check if activity is available
        if (getActivity() == null) {
            return;
        }
        
        // Get the root view to detect keyboard visibility
        View rootView = getActivity().getWindow().getDecorView().getRootView();
        
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Check if fragment is still attached and binding is available
                if (!isAdded() || binding == null || getActivity() == null) {
                    return;
                }
                
                try {
                    // Get the visible display frame
                    android.graphics.Rect r = new android.graphics.Rect();
                    rootView.getWindowVisibleDisplayFrame(r);
                    
                    // Calculate the height difference
                    int screenHeight = rootView.getHeight();
                    int keypadHeight = screenHeight - r.bottom;
                    
                    // Determine if keyboard is visible (keypad height > 15% of screen height)
                    boolean keyboardVisible = keypadHeight > screenHeight * 0.15;
                    
                    if (keyboardVisible != isKeyboardVisible) {
                        isKeyboardVisible = keyboardVisible;
                        onKeyboardVisibilityChanged(keyboardVisible);
                    }
                } catch (Exception e) {
                    // Log error but don't crash
                    Log.e("StudyHubFragment", "Error in keyboard visibility detection", e);
                }
            }
        });
    }
    
    private void onKeyboardVisibilityChanged(boolean visible) {
        if (visible) {
            hideUIElements();
        } else {
            showUIElements();
        }
    }
    
    private void hideUIElements() {
        // Check if binding is null or view is not attached
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        
        // Hide favorites and downloads buttons
        if (binding.btnFavorites != null) {
            binding.btnFavorites.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (binding != null && binding.btnFavorites != null) {
                            binding.btnFavorites.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
                
        if (binding.btnDownloads != null) {
            binding.btnDownloads.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (binding != null && binding.btnDownloads != null) {
                            binding.btnDownloads.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
        
        // Move filter and search layout down by 5dp from search layout top
        View filterSearchLayout = binding.getRoot().findViewById(com.example.nurse_connect.R.id.filterSearchLayout);
        if (filterSearchLayout != null) {
            // Move it down by 5dp from the search layout position
            filterSearchLayout.animate()
                    .translationY(5)
                    .setDuration(200)
                    .start();
        }
        
        // Ensure the category spinner remains visible and functional
        if (binding.spinnerCategory != null) {
            binding.spinnerCategory.setEnabled(true);
            binding.spinnerCategory.setVisibility(View.VISIBLE);
            // Make sure the spinner is clickable and visible
            binding.spinnerCategory.setClickable(true);
            binding.spinnerCategory.setFocusable(true);
        }
        
        // Resize AppBarLayout to accommodate both search card and filter
        View appBarLayout = binding.getRoot().findViewById(com.example.nurse_connect.R.id.appBarLayout);
        View searchCard = binding.getRoot().findViewById(com.example.nurse_connect.R.id.searchCard);
        if (appBarLayout != null && searchCard != null && filterSearchLayout != null) {
            // Calculate the height needed for AppBarLayout (Study Hub text + search card + filter)
            int studyHubTextHeight = binding.toolbar.getHeight();
            int searchCardHeight = searchCard.getHeight();
            int filterHeight = filterSearchLayout.getHeight();
            int totalHeight = studyHubTextHeight + searchCardHeight + filterHeight + 24; // 24dp for margins
            
            appBarLayout.animate()
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (binding != null && appBarLayout != null) {
                            ViewGroup.LayoutParams params = appBarLayout.getLayoutParams();
                            params.height = totalHeight;
                            appBarLayout.setLayoutParams(params);
                        }
                    })
                    .start();
        }
        
        // Hide bottom navigation completely (if accessible from fragment)
        View bottomNav = getActivity().findViewById(com.example.nurse_connect.R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (bottomNav != null) {
                            bottomNav.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }
    
    private void showUIElements() {
        // Check if binding is null or view is not attached
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        
        // Show favorites and downloads buttons
        if (binding.btnFavorites != null) {
            binding.btnFavorites.setVisibility(View.VISIBLE);
            binding.btnFavorites.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
                
        if (binding.btnDownloads != null) {
            binding.btnDownloads.setVisibility(View.VISIBLE);
            binding.btnDownloads.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
        
        // Move filter and search layout back to original position
        View filterSearchLayout = binding.getRoot().findViewById(com.example.nurse_connect.R.id.filterSearchLayout);
        if (filterSearchLayout != null) {
            filterSearchLayout.animate()
                    .translationY(0)
                    .setDuration(200)
                    .start();
        }
        
        // Ensure the category spinner remains functional
        if (binding.spinnerCategory != null) {
            binding.spinnerCategory.setEnabled(true);
            binding.spinnerCategory.setVisibility(View.VISIBLE);
        }
        
        // Restore AppBarLayout to original size
        View appBarLayout = binding.getRoot().findViewById(com.example.nurse_connect.R.id.appBarLayout);
        if (appBarLayout != null) {
            appBarLayout.animate()
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (binding != null && appBarLayout != null) {
                            ViewGroup.LayoutParams params = appBarLayout.getLayoutParams();
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            appBarLayout.setLayoutParams(params);
                        }
                    })
                    .start();
        }
        
        // Show bottom navigation (if accessible from fragment)
        View bottomNav = getActivity().findViewById(com.example.nurse_connect.R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }
    
    private void searchUserByUsername(String username) {
        viewModel.searchUserByUsername(username, new StudyMaterialViewModel.UserSearchCallback() {
            @Override
            public void onSuccess(com.example.nurse_connect.models.User user) {
                // Navigate to user profile
                Intent intent = new Intent(getContext(), com.example.nurse_connect.ui.profile.UserProfileActivity.class);
                intent.putExtra("user_id", user.getUid());
                intent.putExtra("user_name", user.getDisplayName());
                startActivity(intent);
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "User not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            // Handle result from EditDocumentActivity
            StudyMaterial updatedMaterial = (StudyMaterial) data.getSerializableExtra("updated_material");
            String deletedMaterialId = data.getStringExtra("deleted_material_id");
            
            if (updatedMaterial != null) {
                // Update the material in the adapter
                adapter.updateMaterial(updatedMaterial);
                Toast.makeText(getContext(), "Document updated successfully!", Toast.LENGTH_SHORT).show();
            } else if (deletedMaterialId != null) {
                // Remove the deleted material from the adapter
                adapter.removeMaterial(deletedMaterialId);
                Toast.makeText(getContext(), "Document deleted successfully!", Toast.LENGTH_SHORT).show();
                
                // Update empty state if needed
                if (adapter.getItemCount() == 0) {
                    updateEmptyState(adapter.getMaterials());
                }
            }
        } else if (requestCode == 1002) {
            // Handle result from CommentsActivity - refresh comment counts
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                String materialId = data.getStringExtra("material_id");
                if (materialId != null) {
                    // Refresh the counts for this material
                    viewModel.refreshMaterialCounts(materialId);
                }
            }
            // Note: We don't refresh counts on back press to avoid unnecessary network calls
        } else if (requestCode == 1003 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            // Handle result from RatingActivity - refresh counts in case rating included a comment
            String materialId = data.getStringExtra("material_id");
            if (materialId != null) {
                // Refresh the counts for this material
                viewModel.refreshMaterialCounts(materialId);
            }
        }
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
                Log.d("StudyHubFragment", "Toggled favorite successfully. Is favorite: " + isFavorite);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e("StudyHubFragment", "Failed to toggle favorite", e);
            }
        });
    }
} 