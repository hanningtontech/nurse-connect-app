package com.example.nurse_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ItemUserBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.utils.ProfilePreviewDialog;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ItemUserBinding binding;

        public UserViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(User user) {
            // Set display name
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                binding.tvDisplayName.setText(displayName);
            } else {
                binding.tvDisplayName.setText("Unknown User");
            }

            // Set username
            String username = user.getUsername();
            if (username != null && !username.isEmpty()) {
                binding.tvUsername.setText("@" + username);
            } else {
                binding.tvUsername.setText("@unknown");
            }

            // Set institution if available
            if (user.getProfile() != null && user.getProfile().getInstitution() != null && !user.getProfile().getInstitution().isEmpty()) {
                binding.tvInstitution.setText(user.getProfile().getInstitution());
                binding.tvInstitution.setVisibility(View.VISIBLE);
            } else {
                binding.tvInstitution.setVisibility(View.GONE);
            }

            // Set study year if available
            if (user.getProfile() != null && user.getProfile().getStudyYear() != null && !user.getProfile().getStudyYear().isEmpty()) {
                binding.tvStudyYear.setText(user.getProfile().getStudyYear());
                binding.tvStudyYear.setVisibility(View.VISIBLE);
            } else {
                binding.tvStudyYear.setVisibility(View.GONE);
            }

            // Load profile picture using Glide
            String photoURL = user.getPhotoURL();
            if (photoURL != null && !photoURL.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(photoURL)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivProfile);
            } else {
                binding.ivProfile.setImageResource(R.drawable.ic_person);
            }

            // Add click listener to profile picture for preview
            binding.ivProfile.setOnClickListener(v -> {
                ProfilePreviewDialog.showProfilePreview(itemView.getContext(), user);
            });

            // Set click listener for the whole item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
} 