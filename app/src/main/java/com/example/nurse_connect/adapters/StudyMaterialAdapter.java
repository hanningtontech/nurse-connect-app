package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.StudyMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudyMaterialAdapter extends RecyclerView.Adapter<StudyMaterialAdapter.StudyMaterialViewHolder> {
    
    private List<StudyMaterial> materials;
    private OnStudyMaterialClickListener listener;
    private SimpleDateFormat dateFormat;
    
    public interface OnStudyMaterialClickListener {
        void onMaterialClick(StudyMaterial material);
        void onDownloadClick(StudyMaterial material);
        void onLikeClick(StudyMaterial material);
        void onCommentsClick(StudyMaterial material);

        void onCommentClick(StudyMaterial material);

        void onRatingClick(StudyMaterial material);
        void onAuthorClick(StudyMaterial material);
        void onEditClick(StudyMaterial material); // Changed from onDeleteClick
        void onThumbnailClick(StudyMaterial material);
        void onTitleClick(StudyMaterial material);
    }
    
    public StudyMaterialAdapter() {
        this.materials = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    public void setOnStudyMaterialClickListener(OnStudyMaterialClickListener listener) {
        this.listener = listener;
    }
    
    public void setMaterials(List<StudyMaterial> materials) {
        this.materials = materials != null ? materials : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addMaterial(StudyMaterial material) {
        this.materials.add(0, material);
        notifyItemInserted(0);
    }
    
    public void removeMaterial(String materialId) {
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateMaterial(StudyMaterial updatedMaterial) {
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(updatedMaterial.getId())) {
                materials.set(i, updatedMaterial);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public List<StudyMaterial> getMaterials() {
        return materials;
    }
    
    public void updateMaterialLikeState(String materialId, boolean isLiked) {
        // Find the material and update its like state
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.get(i).setLikedByUser(isLiked);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void updateMaterialRatingState(String materialId, boolean hasRated) {
        // Find the material and update its rating state
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.get(i).setHasRatedByUser(hasRated);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void updateMaterialDownloadCount(String materialId, int newDownloadCount) {
        // Find the material and update its download count
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.get(i).setDownloads(newDownloadCount);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void updateMaterialViewCount(String materialId, int newViewCount) {
        // Find the material and update its view count
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.get(i).setViews(newViewCount);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateMaterialLikeCount(String materialId, int newLikeCount) {
        // Find the material and update its like count
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.get(i).setLikes(newLikeCount);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateMaterialCommentCount(String materialId, int newCommentCount) {
        // Find the material and update its comment count
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId().equals(materialId)) {
                materials.get(i).setCommentCount(newCommentCount);
                notifyItemChanged(i);
                break;
            }
        }
    }


    
    @NonNull
    @Override
    public StudyMaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_material, parent, false);
        return new StudyMaterialViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StudyMaterialViewHolder holder, int position) {
        StudyMaterial material = materials.get(position);
        holder.bind(material);
    }
    
    @Override
    public int getItemCount() {
        return materials.size();
    }
    
    class StudyMaterialViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvExamDetails;
        private TextView tvRatingCount;
        private TextView tvRatingStars;
        private TextView tvPrice;
        private TextView tvSales;
        private TextView tvViews;
        private TextView tvAuthorName;
        private ImageView ivDocumentThumbnail;
        private ImageButton btnLike;
        private ImageButton btnComments;
        private ImageButton btnRating;
        private ImageButton btnDownload;
        private ImageButton btnEdit; // Changed from btnDelete
        private TextView tvLikeCount;
        private TextView tvCommentCount;

        public StudyMaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvExamDetails = itemView.findViewById(R.id.tvExamDetails);
            tvRatingCount = itemView.findViewById(R.id.tvRatingCount);
            tvRatingStars = itemView.findViewById(R.id.tvRatingStars);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSales = itemView.findViewById(R.id.tvSales);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            ivDocumentThumbnail = itemView.findViewById(R.id.ivDocumentThumbnail);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComments = itemView.findViewById(R.id.btnComments);
            btnRating = itemView.findViewById(R.id.btnRating);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnEdit = itemView.findViewById(R.id.btnEdit); // Changed from btnDelete
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
        }
        
        public void bind(StudyMaterial material) {
            // Set material title
            tvTitle.setText(material.getTitle());
            
            // Set material description
            tvDescription.setText(material.getDescription());
            
            // Set exam details (category, pages, date)
            String examDetails = material.getCategory() + " • " + 
                               formatFileSize(material.getFileSize()) + " • uploaded " + 
                               dateFormat.format(new Date(material.getUploadDate()));
            tvExamDetails.setText(examDetails);
            
            // Set author name and make it clickable
            String authorName = material.getAuthorName() != null && !material.getAuthorName().isEmpty() 
                               ? material.getAuthorName() : "Unknown Author";
            tvAuthorName.setText("by " + authorName);
            tvAuthorName.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAuthorClick(material);
                }
            });
            
            // Set rating stars and count
            updateRatingStars(material.getRating());
            tvRatingCount.setText("(" + material.getReviewCount() + ")");
            
            // Update like and rating button states
            updateLikeState(material.isLikedByUser());
            updateRatingState(material.hasRatedByUser());
            
            // Set price (free for now, can be updated later)
            tvPrice.setText("Free");
            
            // Set sales/downloads count
            tvSales.setText(material.getDownloads() + "x downloaded");
            
            // Set views count
            tvViews.setText(material.getViews() + "x viewed");
            
            // Set like count badge
            updateLikeCountBadge(material.getLikes());
            
            // Set comment count badge
            updateCommentCountBadge(material.getCommentCount());
            
            // Set document thumbnail
            setDocumentThumbnail(material);
            
            // Show/hide edit button based on ownership
            showEditButtonIfOwner(material);
            
            // Set click listeners
            // Remove overall layout click - now individual elements are clickable
            
            // Thumbnail click listener
            ivDocumentThumbnail.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThumbnailClick(material);
                }
            });
            
            // Title click listener
            tvTitle.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTitleClick(material);
                }
            });
            
            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(material);
                }
            });
            
            btnComments.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentsClick(material);
                }
            });
            
            btnRating.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRatingClick(material);
                }
            });
            
            btnDownload.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadClick(material);
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(material);
                }
            });
        }
        
        private void setDocumentThumbnail(StudyMaterial material) {
            // Check if material has a thumbnail URL
            if (material.getThumbnailURL() != null && !material.getThumbnailURL().isEmpty()) {
                // Load thumbnail from URL using Glide
                Glide.with(ivDocumentThumbnail.getContext())
                        .load(material.getThumbnailURL())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.ic_document)
                        .error(R.drawable.ic_document)
                        .into(ivDocumentThumbnail);
            } else {
                // Fallback to category-based icon
                String category = material.getCategory();
                if (category != null) {
                    switch (category.toLowerCase()) {
                        case "pdf":
                            ivDocumentThumbnail.setImageResource(R.drawable.ic_document);
                            break;
                        case "video":
                            ivDocumentThumbnail.setImageResource(R.drawable.ic_video);
                            break;
                        case "audio":
                            ivDocumentThumbnail.setImageResource(R.drawable.ic_audio);
                            break;
                        case "image":
                            ivDocumentThumbnail.setImageResource(R.drawable.ic_image);
                            break;
                        default:
                            ivDocumentThumbnail.setImageResource(R.drawable.ic_document);
                            break;
                    }
                } else {
                    // If category is null, use type as fallback
                    String type = material.getType();
                    if (type != null) {
                        switch (type.toLowerCase()) {
                            case "pdf":
                                ivDocumentThumbnail.setImageResource(R.drawable.ic_document);
                                break;
                            case "video":
                                ivDocumentThumbnail.setImageResource(R.drawable.ic_video);
                                break;
                            case "audio":
                                ivDocumentThumbnail.setImageResource(R.drawable.ic_audio);
                                break;
                            case "image":
                                ivDocumentThumbnail.setImageResource(R.drawable.ic_image);
                                break;
                            default:
                                ivDocumentThumbnail.setImageResource(R.drawable.ic_document);
                                break;
                        }
                    } else {
                        // Final fallback
                        ivDocumentThumbnail.setImageResource(R.drawable.ic_document);
                    }
                }
            }
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
        
        private void updateRatingStars(float rating) {
            // Convert rating to integer (1-5 stars)
            int starCount = Math.round(rating);
            starCount = Math.max(0, Math.min(5, starCount)); // Clamp between 0-5
            
            // Create star string with filled and empty stars
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < starCount) {
                    stars.append("★"); // Filled star
                } else {
                    stars.append("☆"); // Empty star
                }
            }
            
            tvRatingStars.setText(stars.toString());
        }
        
        public void updateLikeState(boolean isLiked) {
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_favorite_filled);
                btnLike.setColorFilter(android.graphics.Color.parseColor("#E91E63")); // Red color
            } else {
                btnLike.setImageResource(R.drawable.ic_favorite_border);
                btnLike.setColorFilter(android.graphics.Color.parseColor("#E91E63")); // Red color
            }
        }
        
        public void updateRatingState(boolean hasRated) {
            if (hasRated) {
                btnRating.setImageResource(R.drawable.ic_star_filled);
                btnRating.setColorFilter(android.graphics.Color.parseColor("#FFD700")); // Gold color
            } else {
                btnRating.setImageResource(R.drawable.ic_star);
                btnRating.setColorFilter(android.graphics.Color.parseColor("#FFD700")); // Gold color
            }
        }

        private void showEditButtonIfOwner(StudyMaterial material) {
            // Get current user ID from Firebase Auth
            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null 
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : null;
            
            // Show delete button only if current user is the author
            if (currentUserId != null && currentUserId.equals(material.getAuthorId())) {
                btnEdit.setVisibility(View.VISIBLE);
            } else {
                btnEdit.setVisibility(View.GONE);
            }
        }

        private void updateLikeCountBadge(int likeCount) {
            if (likeCount > 0) {
                tvLikeCount.setText(String.valueOf(likeCount));
                tvLikeCount.setVisibility(View.VISIBLE);
            } else {
                tvLikeCount.setVisibility(View.GONE);
            }
        }

        private void updateCommentCountBadge(int commentCount) {
            if (commentCount > 0) {
                tvCommentCount.setText(String.valueOf(commentCount));
                tvCommentCount.setVisibility(View.VISIBLE);
            } else {
                tvCommentCount.setVisibility(View.GONE);
            }
        }
    }
} 