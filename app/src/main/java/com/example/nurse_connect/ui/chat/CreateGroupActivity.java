package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.SelectedMemberAdapter;
import com.example.nurse_connect.databinding.ActivityCreateGroupBinding;
import com.example.nurse_connect.models.GroupChat;
import com.example.nurse_connect.models.GroupInvitation;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.services.GroupInvitationService;
import com.example.nurse_connect.utils.ThemeManager;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity implements SelectedMemberAdapter.OnMemberRemoveListener {

    private ActivityCreateGroupBinding binding;
    private SelectedMemberAdapter memberAdapter;
    private List<User> selectedMembers;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private GroupInvitationService invitationService;
    private Uri selectedImageUri;
    private static final int REQUEST_INVITE_MEMBERS = 1001;

    // Activity result launchers
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        binding = ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupActivityResultLaunchers();
        setupUI();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        invitationService = new GroupInvitationService();
    }

    private void setupActivityResultLaunchers() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // Load the selected image into the ImageView
                            binding.ivGroupPhoto.setImageURI(selectedImageUri);
                        }
                    }
                }
        );

        // Permission request launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup selected members RecyclerView
        selectedMembers = new ArrayList<>();
        memberAdapter = new SelectedMemberAdapter(selectedMembers, this);
        binding.rvSelectedMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSelectedMembers.setAdapter(memberAdapter);

        // Setup click listeners
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.btnInviteMembers.setOnClickListener(v -> {
            Intent intent = new Intent(this, InviteMembersActivity.class);
            startActivityForResult(intent, REQUEST_INVITE_MEMBERS);
        });

        binding.btnCreateGroup.setOnClickListener(v -> createGroup());

        binding.btnChangePhoto.setOnClickListener(v -> {
            checkPermissionAndOpenImagePicker();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_INVITE_MEMBERS && resultCode == RESULT_OK && data != null) {
            // Get selected user IDs from invite activity
            ArrayList<String> invitedUserIds = data.getStringArrayListExtra("selected_user_ids");
            if (invitedUserIds != null) {
                // Load user objects from IDs
                loadUsersFromIds(invitedUserIds);
            }
        }
    }

    private void loadUsersFromIds(ArrayList<String> userIds) {
        for (String userId : userIds) {
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                user.setUid(documentSnapshot.getId());
                                selectedMembers.add(user);
                                memberAdapter.notifyDataSetChanged();
                                updateMembersVisibility();
                            }
                        }
                    });
        }
    }

    private void updateMembersVisibility() {
        if (selectedMembers.isEmpty()) {
            binding.rvSelectedMembers.setVisibility(View.GONE);
        } else {
            binding.rvSelectedMembers.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionAndOpenImagePicker() {
        // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                openImagePicker();
            }
        } else {
            // For older versions, use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void createGroup() {
        String title = binding.etGroupTitle.getText().toString().trim();
        String description = binding.etGroupDescription.getText().toString().trim();
        boolean isPublic = binding.rbPublic.isChecked();

        // Validate input
        if (TextUtils.isEmpty(title)) {
            binding.tilGroupTitle.setError("Group title is required");
            return;
        }

        if (title.length() < 3) {
            binding.tilGroupTitle.setError("Group title must be at least 3 characters");
            return;
        }

        // Clear any previous errors
        binding.tilGroupTitle.setError(null);

        // Show loading
        binding.btnCreateGroup.setEnabled(false);
        binding.btnCreateGroup.setText("Creating...");

        // Create group
        String groupId = db.collection("group_chats").document().getId();
        GroupChat group = new GroupChat(title, description, currentUser.getUid(), isPublic);
        group.setGroupId(groupId);

        Log.d("CreateGroup", "Creating group: " + title + " (isPublic: " + isPublic + ")");
        Log.d("CreateGroup", "Group members: " + group.getMembers());
        Log.d("CreateGroup", "Group admins: " + group.getAdmins());

        // Save group to Firestore
        db.collection("group_chats")
                .document(groupId)
                .set(group)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CreateGroup", "Group created successfully: " + groupId + " (isPublic: " + group.isPublic() + ")");

                    // Send invitations to selected members
                    if (!selectedMembers.isEmpty()) {
                        sendInvitations(group);
                    } else {
                        // No invitations to send, just finish
                        finishGroupCreation(group);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateGroup", "Error creating group", e);
                    binding.btnCreateGroup.setEnabled(true);
                    binding.btnCreateGroup.setText("Create Group");
                    Toast.makeText(this, "Error creating group", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendInvitations(GroupChat group) {
        // Get current user's name for invitation
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentUserName = documentSnapshot.getString("displayName");
                    if (currentUserName == null) {
                        currentUserName = documentSnapshot.getString("username");
                    }

                    // Send invitation to each selected member
                    for (User member : selectedMembers) {
                        sendInvitationToUser(group, member, currentUserName);
                    }

                    finishGroupCreation(group);
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateGroup", "Error getting current user info", e);
                    finishGroupCreation(group);
                });
    }

    private void sendInvitationToUser(GroupChat group, User invitedUser, String currentUserName) {
        // Create invitation document
        GroupInvitation invitation = new GroupInvitation(
                group.getGroupId(),
                group.getTitle(),
                currentUser.getUid(),
                currentUserName,
                invitedUser.getUid()
        );
        invitation.setGroupDescription(group.getDescription());
        invitation.setGroupPublic(group.isPublic());
        invitation.setMessage("You've been invited to join the group: " + group.getTitle());

        // Save invitation
        String invitationId = db.collection("group_invitations").document().getId();
        invitation.setInvitationId(invitationId);

        db.collection("group_invitations")
                .document(invitationId)
                .set(invitation)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CreateGroup", "Invitation document created for: " + invitedUser.getUsername());

                    // Add to group's pending invitations
                    group.addPendingInvitation(invitedUser.getUid());

                    // Update group document
                    db.collection("group_chats")
                            .document(group.getGroupId())
                            .update("pendingInvitations", group.getPendingInvitations())
                            .addOnSuccessListener(aVoid2 -> {
                                // Now send the invitation message to private chat
                                invitationService.sendGroupInvitationMessage(invitation, currentUser.getUid());
                                Log.d("CreateGroup", "Invitation message sent to: " + invitedUser.getUsername());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateGroup", "Error sending invitation to: " + invitedUser.getUsername(), e);
                });
    }

    private void finishGroupCreation(GroupChat group) {
        binding.btnCreateGroup.setEnabled(true);
        binding.btnCreateGroup.setText("Create Group");
        
        Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
        
        // Open the group chat
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("group_id", group.getGroupId());
        intent.putExtra("group_title", group.getTitle());
        startActivity(intent);
        
        finish();
    }

    @Override
    public void onMemberRemove(User user) {
        selectedMembers.remove(user);
        memberAdapter.notifyDataSetChanged();
        updateMembersVisibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
