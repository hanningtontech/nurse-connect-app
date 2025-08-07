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
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

public class UserInviteAdapter extends RecyclerView.Adapter<UserInviteAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> selectedUsers;
    private OnUserSelectionListener listener;

    public interface OnUserSelectionListener {
        void onUserSelected(User user, boolean isSelected);
    }

    public UserInviteAdapter(List<User> users, List<User> selectedUsers, OnUserSelectionListener listener) {
        this.users = users;
        this.selectedUsers = selectedUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_invite, parent, false);
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

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvUsername;
        private TextView tvFullName;
        private MaterialCheckBox cbSelected;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            cbSelected = itemView.findViewById(R.id.cbSelected);
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

            // Set checkbox state
            boolean isSelected = selectedUsers.contains(user);
            cbSelected.setChecked(isSelected);

            // Set click listeners
            View.OnClickListener clickListener = v -> {
                boolean newState = !cbSelected.isChecked();
                cbSelected.setChecked(newState);
                
                if (listener != null) {
                    listener.onUserSelected(user, newState);
                }
            };

            itemView.setOnClickListener(clickListener);
            cbSelected.setOnClickListener(clickListener);
        }
    }
}
