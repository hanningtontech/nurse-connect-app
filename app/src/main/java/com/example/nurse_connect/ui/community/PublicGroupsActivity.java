package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.PublicGroupAdapter;
import com.example.nurse_connect.databinding.ActivityPublicGroupsBinding;
import com.example.nurse_connect.models.GroupChat;
import com.example.nurse_connect.ui.chat.GroupChatActivity;
import com.example.nurse_connect.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PublicGroupsActivity extends AppCompatActivity implements PublicGroupAdapter.OnGroupClickListener {

    private ActivityPublicGroupsBinding binding;
    private PublicGroupAdapter adapter;
    private List<GroupChat> groupList;
    private List<GroupChat> allGroups;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        binding = ActivityPublicGroupsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupUI();
        loadPublicGroups();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup RecyclerView
        groupList = new ArrayList<>();
        allGroups = new ArrayList<>();
        adapter = new PublicGroupAdapter(groupList, currentUser.getUid(), this);
        binding.rvGroups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGroups.setAdapter(adapter);

        // Setup search functionality
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                filterGroups(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPublicGroups() {
        if (currentUser == null) return;

        showLoading();

        // Load all public groups
        db.collection("group_chats")
                .whereEqualTo("isPublic", true)
                .addSnapshotListener((value, error) -> {
                    hideLoading();

                    if (error != null) {
                        Log.e("PublicGroups", "Error loading groups", error);
                        Toast.makeText(this, "Error loading groups: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allGroups.clear();
                    groupList.clear();

                    if (value != null) {
                        Log.d("PublicGroups", "Found " + value.size() + " public groups");
                        for (QueryDocumentSnapshot document : value) {
                            GroupChat group = document.toObject(GroupChat.class);
                            group.setGroupId(document.getId());
                            Log.d("PublicGroups", "Public group: " + group.getTitle() + " (isPublic: " + group.isPublic() + ")");
                            allGroups.add(group);
                            groupList.add(group);
                        }

                        if (!groupList.isEmpty()) {
                            showGroups();
                        } else {
                            showEmptyState();
                        }
                    } else {
                        Log.d("PublicGroups", "No public groups found");
                        showEmptyState();
                    }
                    
                    adapter.notifyDataSetChanged();
                });
    }

    private void filterGroups(String query) {
        groupList.clear();
        
        if (query.isEmpty()) {
            groupList.addAll(allGroups);
        } else {
            String lowerQuery = query.toLowerCase();
            for (GroupChat group : allGroups) {
                if (group.getTitle().toLowerCase().contains(lowerQuery) ||
                    (group.getDescription() != null && group.getDescription().toLowerCase().contains(lowerQuery))) {
                    groupList.add(group);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (groupList.isEmpty() && !query.isEmpty()) {
            showEmptyState();
        } else if (!groupList.isEmpty()) {
            showGroups();
        }
    }

    private void showGroups() {
        binding.emptyState.setVisibility(View.GONE);
        binding.rvGroups.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        binding.rvGroups.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onGroupClick(GroupChat group) {
        // Check if user is already a member
        if (group.isMember(currentUser.getUid())) {
            // Open group chat
            Intent intent = new Intent(this, GroupChatActivity.class);
            intent.putExtra("group_id", group.getGroupId());
            intent.putExtra("group_title", group.getTitle());
            startActivity(intent);
        } else {
            // Show join request dialog or handle join logic
            handleJoinRequest(group);
        }
    }

    @Override
    public void onJoinRequest(GroupChat group) {
        handleJoinRequest(group);
    }

    private void handleJoinRequest(GroupChat group) {
        // For public groups, users can join directly or request to join
        // This could be immediate join or require admin approval
        
        Toast.makeText(this, "Join request functionality coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement join request logic
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
