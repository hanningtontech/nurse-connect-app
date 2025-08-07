package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.MyGroupAdapter;
import com.example.nurse_connect.databinding.ActivityMyGroupsBinding;
import com.example.nurse_connect.models.GroupChat;
import com.example.nurse_connect.ui.chat.CreateGroupActivity;
import com.example.nurse_connect.ui.chat.GroupChatActivity;
import com.example.nurse_connect.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyGroupsActivity extends AppCompatActivity implements MyGroupAdapter.OnGroupClickListener {

    private ActivityMyGroupsBinding binding;
    private MyGroupAdapter adapter;
    private List<GroupChat> groupList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        binding = ActivityMyGroupsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupUI();
        loadMyGroups();
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
        adapter = new MyGroupAdapter(groupList, currentUser.getUid(), this);
        binding.rvMyGroups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyGroups.setAdapter(adapter);

        // Setup click listeners
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.fabCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupActivity.class);
            startActivity(intent);
        });
    }

    private void loadMyGroups() {
        if (currentUser == null) return;

        showLoading();

        // Load groups where current user is a member
        // This includes both public and private groups the user belongs to
        db.collection("group_chats")
                .whereArrayContains("members", currentUser.getUid())
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    hideLoading();
                    
                    if (error != null) {
                        Log.e("MyGroups", "Error loading groups", error);
                        Toast.makeText(this, "Error loading groups", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    groupList.clear();
                    
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot document : value) {
                            GroupChat group = document.toObject(GroupChat.class);
                            group.setGroupId(document.getId());
                            groupList.add(group);
                        }
                        showGroups();
                    } else {
                        showEmptyState();
                    }
                    
                    adapter.notifyDataSetChanged();
                });
    }

    private void showGroups() {
        binding.emptyState.setVisibility(View.GONE);
        binding.rvMyGroups.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        binding.rvMyGroups.setVisibility(View.GONE);
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
        // Open group chat
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("group_id", group.getGroupId());
        intent.putExtra("group_title", group.getTitle());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
