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

public class SelectedMemberAdapter extends RecyclerView.Adapter<SelectedMemberAdapter.MemberViewHolder> {

    private List<User> members;
    private OnMemberRemoveListener listener;

    public interface OnMemberRemoveListener {
        void onMemberRemove(User user);
    }

    public SelectedMemberAdapter(List<User> members, OnMemberRemoveListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = members.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvUsername;
        private TextView tvStatus;
        private ImageView ivRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivRemove = itemView.findViewById(R.id.ivRemove);
        }

        public void bind(User user) {
            // Set username
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = user.getUsername();
            }
            tvUsername.setText(displayName);
            
            // Set status
            tvStatus.setText("Invitation pending");

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

            // Set remove click listener
            ivRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberRemove(user);
                }
            });
        }
    }
}
