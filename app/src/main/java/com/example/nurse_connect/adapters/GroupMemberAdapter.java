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
import com.example.nurse_connect.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private List<User> memberList;
    private String currentUserId;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(User member);
    }

    public GroupMemberAdapter(List<User> memberList, String currentUserId, OnMemberClickListener listener) {
        this.memberList = memberList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = memberList.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivMemberPhoto;
        private TextView tvMemberName;
        private TextView tvMemberRole;
        private ImageView ivOnlineStatus;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberPhoto = itemView.findViewById(R.id.ivMemberPhoto);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
            ivOnlineStatus = itemView.findViewById(R.id.ivOnlineStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMemberClick(memberList.get(position));
                }
            });
        }

        public void bind(User member) {
            // Set member name
            String displayName = member.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = member.getUsername();
            }
            tvMemberName.setText(displayName);

            // Set member role
            if (member.getUid().equals(currentUserId)) {
                tvMemberRole.setText("You");
                tvMemberRole.setTextColor(itemView.getContext().getColor(R.color.theme_primary));
            } else {
                tvMemberRole.setText("Member");
                tvMemberRole.setTextColor(itemView.getContext().getColor(R.color.theme_text_secondary));
            }

            // Load member photo
            if (member.getPhotoURL() != null && !member.getPhotoURL().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(member.getPhotoURL())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivMemberPhoto);
            } else {
                ivMemberPhoto.setImageResource(R.drawable.ic_person);
            }

            // Set online status (you can implement this based on your online status system)
            // For now, we'll hide it
            ivOnlineStatus.setVisibility(View.GONE);
        }
    }
}
