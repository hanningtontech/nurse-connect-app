package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ItemFollowedUserBinding;
import com.example.nurse_connect.models.User;

import java.util.List;

public class FollowedUsersAdapter extends RecyclerView.Adapter<FollowedUsersAdapter.FollowedUserViewHolder> {
    
    private List<User> users;
    private OnUserClickListener listener;
    
    public interface OnUserClickListener {
        void onUserClick(User user);
    }
    
    public FollowedUsersAdapter(android.content.Context context, List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FollowedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFollowedUserBinding binding = ItemFollowedUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FollowedUserViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FollowedUserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    public void updateUsers(List<User> newUsers) {
        this.users.clear();
        this.users.addAll(newUsers);
        notifyDataSetChanged();
    }
    
    class FollowedUserViewHolder extends RecyclerView.ViewHolder {
        private ItemFollowedUserBinding binding;
        
        public FollowedUserViewHolder(ItemFollowedUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        public void bind(User user) {
            binding.textUserName.setText(user.getDisplayName());
            binding.textUserHandle.setText("@" + user.getHandle());
            
            // Load profile image
            if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(user.getPhotoURL())
                        .placeholder(R.drawable.default_profile_pic)
                        .error(R.drawable.default_profile_pic)
                        .circleCrop()
                        .into(binding.imageUserProfile);
            } else {
                binding.imageUserProfile.setImageResource(R.drawable.default_profile_pic);
            }
            
            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
