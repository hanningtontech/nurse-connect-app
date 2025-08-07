package com.example.nurse_connect.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.Message;
import com.example.nurse_connect.services.GroupInvitationService;
import com.example.nurse_connect.ui.chat.GroupChatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

public class InvitationMessageViewHolder extends RecyclerView.ViewHolder {
    
    private TextView tvInvitationText;
    private TextView tvGroupTitle;
    private TextView tvGroupDescription;
    private TextView tvGroupType;
    private TextView tvTimestamp;
    private TextView tvStatus;
    private MaterialButton btnAccept;
    private MaterialButton btnDecline;
    private View layoutActions;
    
    private GroupInvitationService invitationService;

    public InvitationMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        tvInvitationText = itemView.findViewById(R.id.tvInvitationText);
        tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
        tvGroupDescription = itemView.findViewById(R.id.tvGroupDescription);
        tvGroupType = itemView.findViewById(R.id.tvGroupType);
        tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        tvStatus = itemView.findViewById(R.id.tvStatus);
        btnAccept = itemView.findViewById(R.id.btnAccept);
        btnDecline = itemView.findViewById(R.id.btnDecline);
        layoutActions = itemView.findViewById(R.id.layoutActions);
        
        invitationService = new GroupInvitationService();
    }

    public void bind(Message message) {
        Map<String, Object> invitationData = message.getInvitationData();
        if (invitationData == null) return;

        // Set invitation details
        String groupTitle = (String) invitationData.get("groupTitle");
        String groupDescription = (String) invitationData.get("groupDescription");
        String invitedByName = (String) invitationData.get("invitedByName");
        Boolean isGroupPublic = (Boolean) invitationData.get("isGroupPublic");
        String status = (String) invitationData.get("status");

        tvInvitationText.setText(invitedByName + " invited you to join:");
        tvGroupTitle.setText(groupTitle);
        
        if (groupDescription != null && !groupDescription.trim().isEmpty()) {
            tvGroupDescription.setText(groupDescription);
            tvGroupDescription.setVisibility(View.VISIBLE);
        } else {
            tvGroupDescription.setVisibility(View.GONE);
        }

        tvGroupType.setText(Boolean.TRUE.equals(isGroupPublic) ? "Public Group" : "Private Group");

        // Set timestamp
        if (message.getCreatedAt() != null) {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault());
            tvTimestamp.setText(timeFormat.format(message.getCreatedAt().toDate()));
        }

        // Handle invitation status
        if ("pending".equals(status)) {
            layoutActions.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.GONE);
            
            // Set button click listeners
            btnAccept.setOnClickListener(v -> {
                String invitationId = (String) invitationData.get("invitationId");
                String groupId = (String) invitationData.get("groupId");
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Show loading state
                btnAccept.setEnabled(false);
                btnAccept.setText("Joining...");

                // Handle invitation response and clean up
                invitationService.handleInvitationResponseAndCleanup(invitationId, groupId, currentUserId, true, message);

                // Show success message and redirect to group chat
                Context context = itemView.getContext();
                Toast.makeText(context, "Joined group successfully!", Toast.LENGTH_SHORT).show();

                // Redirect to group chat after a short delay
                itemView.postDelayed(() -> {
                    Intent intent = new Intent(context, GroupChatActivity.class);
                    intent.putExtra("group_id", groupId);
                    intent.putExtra("group_title", groupTitle);
                    context.startActivity(intent);
                }, 1000); // 1 second delay
            });
            
            btnDecline.setOnClickListener(v -> {
                String invitationId = (String) invitationData.get("invitationId");
                String groupId = (String) invitationData.get("groupId");
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Handle invitation response and clean up
                invitationService.handleInvitationResponseAndCleanup(invitationId, groupId, currentUserId, false, message);

                // Show declined message
                Context context = itemView.getContext();
                Toast.makeText(context, "Invitation declined", Toast.LENGTH_SHORT).show();
            });
            
        } else {
            layoutActions.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            
            if ("accepted".equals(status)) {
                tvStatus.setText("Invitation accepted ✓");
            } else if ("declined".equals(status)) {
                tvStatus.setText("Invitation declined");
            } else if ("expired".equals(status)) {
                tvStatus.setText("Invitation expired");
            }
        }
    }
}
