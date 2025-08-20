package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.models.FeaturedContent;
import com.google.android.material.chip.Chip;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FeaturedContentAdapter extends RecyclerView.Adapter<FeaturedContentAdapter.FeaturedContentViewHolder> {

    private List<FeaturedContent> featuredContentList;
    private OnFeaturedContentClickListener listener;

    public interface OnFeaturedContentClickListener {
        void onFeaturedContentClick(FeaturedContent content);
    }

    public FeaturedContentAdapter(List<FeaturedContent> featuredContentList, OnFeaturedContentClickListener listener) {
        this.featuredContentList = featuredContentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeaturedContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_content, parent, false);
        return new FeaturedContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedContentViewHolder holder, int position) {
        FeaturedContent content = featuredContentList.get(position);
        holder.bind(content);
    }

    @Override
    public int getItemCount() {
        return featuredContentList.size();
    }

    public void updateContent(List<FeaturedContent> newContent) {
        this.featuredContentList.clear();
        this.featuredContentList.addAll(newContent);
        notifyDataSetChanged();
    }

    class FeaturedContentViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivContentImage;
        private ImageView ivCourseRelevant;
        private Chip chipContentType;
        private TextView tvTitle;
        private CircleImageView ivAuthorPhoto;
        private TextView tvAuthorName;
        private TextView tvLikes;
        private TextView tvViews;
        private TextView tvComments;

        public FeaturedContentViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivContentImage = itemView.findViewById(R.id.ivContentImage);
            ivCourseRelevant = itemView.findViewById(R.id.ivCourseRelevant);
            chipContentType = itemView.findViewById(R.id.chipContentType);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ivAuthorPhoto = itemView.findViewById(R.id.ivAuthorPhoto);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvComments = itemView.findViewById(R.id.tvComments);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onFeaturedContentClick(featuredContentList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(FeaturedContent content) {
            // Set title
            tvTitle.setText(content.getTitle());

            // Set content type chip
            String contentTypeText = getContentTypeDisplayText(content.getContentType());
            chipContentType.setText(contentTypeText);

            // Show course relevance indicator
            ivCourseRelevant.setVisibility(content.isRelevantToCourse() ? View.VISIBLE : View.GONE);

            // Set author info
            tvAuthorName.setText(content.getAuthorName() != null ? content.getAuthorName() : "Unknown");
            
            // Load author photo
            if (content.getAuthorPhotoUrl() != null && !content.getAuthorPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(content.getAuthorPhotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAuthorPhoto);
            } else {
                ivAuthorPhoto.setImageResource(R.drawable.ic_person);
            }

            // Load content image
            if (content.getImageUrl() != null && !content.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(content.getImageUrl())
                        .placeholder(getDefaultImageForContentType(content.getContentType()))
                        .error(getDefaultImageForContentType(content.getContentType()))
                        .into(ivContentImage);
            } else {
                ivContentImage.setImageResource(getDefaultImageForContentType(content.getContentType()));
            }

            // Set engagement stats
            tvLikes.setText(formatCount(content.getLikes()));
            tvViews.setText(formatCount(content.getViews()));
            tvComments.setText(formatCount(content.getComments()));
        }

        private String getContentTypeDisplayText(String contentType) {
            switch (contentType) {
                case "study_material":
                    return "Study Material";
                case "group":
                    return "Study Group";
                case "task":
                    return "Task";
                case "user":
                    return "Featured Nurse";
                default:
                    return "Content";
            }
        }

        private int getDefaultImageForContentType(String contentType) {
            switch (contentType) {
                case "study_material":
                    return R.drawable.ic_book;
                case "group":
                    return R.drawable.ic_group;
                case "task":
                    return R.drawable.ic_task;
                case "user":
                    return R.drawable.ic_person;
                default:
                    return R.drawable.ic_book;
            }
        }

        private String formatCount(int count) {
            if (count >= 1000) {
                return String.format("%.1fk", count / 1000.0);
            }
            return String.valueOf(count);
        }
    }
}
