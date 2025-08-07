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
import com.example.nurse_connect.models.GroupChat;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PublicGroupAdapter extends RecyclerView.Adapter<PublicGroupAdapter.GroupViewHolder> {

    private List<GroupChat> groups;
    private String currentUserId;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(GroupChat group);
        void onJoinRequest(GroupChat group);
    }

    public PublicGroupAdapter(List<GroupChat> groups, String currentUserId, OnGroupClickListener listener) {
        this.groups = groups;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_public_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupChat group = groups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivGroupPhoto;
        private TextView tvGroupTitle;
        private TextView tvGroupDescription;
        private TextView tvGroupType;
        private TextView tvMemberCount;
        private MaterialButton btnJoinRequest;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGroupPhoto = itemView.findViewById(R.id.ivGroupPhoto);
            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            tvGroupDescription = itemView.findViewById(R.id.tvGroupDescription);
            tvGroupType = itemView.findViewById(R.id.tvGroupType);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            btnJoinRequest = itemView.findViewById(R.id.btnJoinRequest);
        }

        public void bind(GroupChat group) {
            // Set group title
            tvGroupTitle.setText(group.getTitle());
            
            // Set description
            if (group.getDescription() != null && !group.getDescription().trim().isEmpty()) {
                tvGroupDescription.setText(group.getDescription());
                tvGroupDescription.setVisibility(View.VISIBLE);
            } else {
                tvGroupDescription.setVisibility(View.GONE);
            }

            // Set group type
            tvGroupType.setText(group.isPublic() ? "Public" : "Private");

            // Set member count
            int memberCount = group.getMembers() != null ? group.getMembers().size() : 0;
            tvMemberCount.setText(memberCount + " member" + (memberCount == 1 ? "" : "s"));

            // Load group photo
            if (group.getGroupPhotoURL() != null && !group.getGroupPhotoURL().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(group.getGroupPhotoURL())
                        .placeholder(R.drawable.ic_group)
                        .error(R.drawable.ic_group)
                        .into(ivGroupPhoto);
            } else {
                ivGroupPhoto.setImageResource(R.drawable.ic_group);
            }

            // Set button state based on membership
            if (group.isMember(currentUserId)) {
                btnJoinRequest.setText("Open");
                btnJoinRequest.setEnabled(true);
            } else if (group.hasPendingInvitation(currentUserId)) {
                btnJoinRequest.setText("Pending");
                btnJoinRequest.setEnabled(false);
            } else {
                btnJoinRequest.setText("Join");
                btnJoinRequest.setEnabled(true);
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGroupClick(group);
                }
            });

            btnJoinRequest.setOnClickListener(v -> {
                if (listener != null) {
                    if (group.isMember(currentUserId)) {
                        listener.onGroupClick(group);
                    } else {
                        listener.onJoinRequest(group);
                    }
                }
            });
        }
    }
}
