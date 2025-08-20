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
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.services.UniversalSearchService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UniversalSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_STUDY_MATERIAL = 1;
    private static final int TYPE_USER = 2;
    private static final int TYPE_GROUP = 3;
    private static final int TYPE_POST = 4;
    private static final int TYPE_TASK = 5;

    private List<Object> items = new ArrayList<>();

    public void updateResults(UniversalSearchService.SearchResults results) {
        items.clear();

        // Add study materials section
        if (!results.getStudyMaterials().isEmpty()) {
            items.add("Study Materials");
            items.addAll(results.getStudyMaterials());
        }

        // Add users section
        if (!results.getUsers().isEmpty()) {
            items.add("Users");
            items.addAll(results.getUsers());
        }

        // Add groups section
        if (!results.getGroups().isEmpty()) {
            items.add("Groups");
            items.addAll(results.getGroups());
        }

        // Add posts section
        if (!results.getPosts().isEmpty()) {
            items.add("Posts");
            items.addAll(results.getPosts());
        }

        // Add tasks section
        if (!results.getTasks().isEmpty()) {
            items.add("Tasks");
            items.addAll(results.getTasks());
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) {
            return TYPE_HEADER;
        } else if (item instanceof StudyMaterial) {
            return TYPE_STUDY_MATERIAL;
        } else if (item instanceof User) {
            return TYPE_USER;
        } else if (item instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) item;
            if (map.containsKey("groupName")) {
                return TYPE_GROUP;
            } else if (map.containsKey("content")) {
                return TYPE_POST;
            } else if (map.containsKey("title")) {
                return TYPE_TASK;
            }
        }
        return TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_search_header, parent, false);
                return new HeaderViewHolder(headerView);
            case TYPE_STUDY_MATERIAL:
            case TYPE_USER:
            case TYPE_GROUP:
            case TYPE_POST:
            case TYPE_TASK:
            default:
                View itemView = inflater.inflate(R.layout.item_search_result, parent, false);
                return new SearchResultViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else if (holder instanceof SearchResultViewHolder) {
            ((SearchResultViewHolder) holder).bind(item, getItemViewType(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }

        public void bind(String header) {
            tvHeader.setText(header);
        }
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivIcon;
        private TextView tvTitle;
        private TextView tvSubtitle;
        private TextView tvDescription;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }

        public void bind(Object item, int viewType) {
            switch (viewType) {
                case TYPE_STUDY_MATERIAL:
                    bindStudyMaterial((StudyMaterial) item);
                    break;
                case TYPE_USER:
                    bindUser((User) item);
                    break;
                case TYPE_GROUP:
                    bindGroup((Map<String, Object>) item);
                    break;
                case TYPE_POST:
                    bindPost((Map<String, Object>) item);
                    break;
                case TYPE_TASK:
                    bindTask((Map<String, Object>) item);
                    break;
            }
        }

        private void bindStudyMaterial(StudyMaterial material) {
            tvTitle.setText(material.getTitle());
            tvSubtitle.setText(material.getCategory());
            tvDescription.setText(material.getDescription());
            
            if (material.getThumbnailURL() != null && !material.getThumbnailURL().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(material.getThumbnailURL())
                        .placeholder(R.drawable.ic_book)
                        .error(R.drawable.ic_book)
                        .into(ivIcon);
            } else {
                ivIcon.setImageResource(R.drawable.ic_book);
            }
        }

        private void bindUser(User user) {
            tvTitle.setText(user.getDisplayName());
            tvSubtitle.setText("@" + user.getUsername());
            
            String description = "";
            if (user.getProfile() != null && user.getProfile().getSpecialization() != null) {
                description = user.getProfile().getSpecialization();
            }
            tvDescription.setText(description);
            
            if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getPhotoURL())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivIcon);
            } else {
                ivIcon.setImageResource(R.drawable.ic_person);
            }
        }

        private void bindGroup(Map<String, Object> group) {
            tvTitle.setText((String) group.get("groupName"));
            tvSubtitle.setText("Study Group");
            tvDescription.setText((String) group.get("description"));
            ivIcon.setImageResource(R.drawable.ic_group);
        }

        private void bindPost(Map<String, Object> post) {
            String content = (String) post.get("content");
            tvTitle.setText(content != null && content.length() > 50 ? 
                    content.substring(0, 50) + "..." : content);
            tvSubtitle.setText("Community Post");
            tvDescription.setText((String) post.get("authorName"));
            ivIcon.setImageResource(R.drawable.ic_chat);
        }

        private void bindTask(Map<String, Object> task) {
            tvTitle.setText((String) task.get("title"));
            tvSubtitle.setText("Study Task");
            tvDescription.setText((String) task.get("description"));
            ivIcon.setImageResource(R.drawable.ic_task);
        }
    }
}
