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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onMessageClick(User user);
    }

    public UserSearchAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
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

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvUsername;
        private TextView tvFullName;
        private TextView tvSpecialty;
        private MaterialButton btnMessage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            btnMessage = itemView.findViewById(R.id.btnMessage);
        }

        public void bind(User user) {
            // Set username
            tvUsername.setText("@" + user.getUsername());
            
            // Set full name
            String fullName = user.getDisplayName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = user.getUsername();
            }
            tvFullName.setText(fullName);

            // Set specialty if available
            if (user.getProfile() != null && user.getProfile().getSpecialization() != null
                && !user.getProfile().getSpecialization().trim().isEmpty()) {
                tvSpecialty.setText(user.getProfile().getSpecialization());
                tvSpecialty.setVisibility(View.VISIBLE);
            } else {
                tvSpecialty.setVisibility(View.GONE);
            }

            // Load profile image
            if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getPhotoURL())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.ic_person);
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            btnMessage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(user);
                }
            });
        }
    }
}
