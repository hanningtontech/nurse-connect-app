package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.GroupMemberAdapter;
import com.example.nurse_connect.databinding.ActivityGroupInfoBinding;
import com.example.nurse_connect.models.GroupChat;
import com.example.nurse_connect.models.User;
// import com.example.nurse_connect.ui.profile.ProfileActivity; // TODO: Create ProfileActivity
import com.example.nurse_connect.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupInfoActivity extends AppCompatActivity implements GroupMemberAdapter.OnMemberClickListener {

    private static final String TAG = "GroupInfoActivity";
    
    private ActivityGroupInfoBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String groupId;
    private GroupChat currentGroup;
    private GroupMemberAdapter memberAdapter;
    private List<User> memberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme
        ThemeManager.getInstance(this).applyTheme();
        
        binding = ActivityGroupInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get group data from intent
        groupId = getIntent().getStringExtra("group_id");
        if (groupId == null) {
            finish();
            return;
        }

        setupFirebase();
        setupUI();
        loadGroupInfo();
        loadGroupMembers();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Group Info");
        }

        // Setup RecyclerView for members
        memberList = new ArrayList<>();
        memberAdapter = new GroupMemberAdapter(memberList, currentUser.getUid(), this);
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMembers.setAdapter(memberAdapter);
    }

    private void loadGroupInfo() {
        db.collection("group_chats")
                .document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentGroup = documentSnapshot.toObject(GroupChat.class);
                        if (currentGroup != null) {
                            currentGroup.setGroupId(documentSnapshot.getId());
                            updateGroupUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading group info", e);
                    Toast.makeText(this, "Error loading group info", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateGroupUI() {
        if (currentGroup != null) {
            binding.tvGroupTitle.setText(currentGroup.getTitle());
            binding.tvGroupDescription.setText(currentGroup.getDescription());
            binding.tvGroupType.setText(currentGroup.isPublic() ? "Public Group" : "Private Group");
            
            int memberCount = currentGroup.getMembers() != null ? currentGroup.getMembers().size() : 0;
            binding.tvMemberCount.setText(memberCount + " member" + (memberCount == 1 ? "" : "s"));
        }
    }

    private void loadGroupMembers() {
        if (currentGroup == null || currentGroup.getMembers() == null) return;

        memberList.clear();
        
        for (String memberId : currentGroup.getMembers()) {
            db.collection("users")
                    .document(memberId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                user.setUid(documentSnapshot.getId());
                                memberList.add(user);
                                memberAdapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading member info", e);
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_info, menu);
        
        // Show/hide menu items based on user role
        if (currentGroup != null) {
            boolean isAdmin = currentGroup.isAdmin(currentUser.getUid());
            boolean isCreator = currentUser.getUid().equals(currentGroup.getCreatedBy());
            
            menu.findItem(R.id.action_edit_group).setVisible(isAdmin);
            menu.findItem(R.id.action_delete_group).setVisible(isCreator);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_leave_group) {
            showLeaveGroupDialog();
            return true;
        } else if (id == R.id.action_clear_chat) {
            showClearChatDialog();
            return true;
        } else if (id == R.id.action_edit_group) {
            // TODO: Implement edit group functionality
            Toast.makeText(this, "Edit group coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_delete_group) {
            showDeleteGroupDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void showLeaveGroupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("This will delete all messages in this group for you. This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearChat())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteGroupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("This will permanently delete the group for all members. This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup() {
        db.collection("group_chats")
                .document(groupId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(currentUser.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left group successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving group", e);
                    Toast.makeText(this, "Error leaving group", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearChat() {
        // TODO: Implement clear chat functionality
        Toast.makeText(this, "Clear chat functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void deleteGroup() {
        // TODO: Implement delete group functionality
        Toast.makeText(this, "Delete group functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMemberClick(User member) {
        // TODO: Open member's profile when ProfileActivity is created
        String memberName = member.getDisplayName();
        if (memberName == null || memberName.isEmpty()) {
            memberName = member.getUsername();
        }
        Toast.makeText(this, "Profile for " + memberName + " - Coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
