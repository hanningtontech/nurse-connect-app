package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.MyGroupAdapter;
import com.example.nurse_connect.databinding.FragmentStudyGroupsBinding;
import com.example.nurse_connect.models.GroupChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudyGroupsFragment extends Fragment implements MyGroupAdapter.OnGroupClickListener {

    private FragmentStudyGroupsBinding binding;
    private MyGroupAdapter adapter;
    private List<GroupChat> groupList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudyGroupsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFirebase();
        setupUI();
        loadStudyGroups();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }
    
    private void setupUI() {
        // Setup RecyclerView
        groupList = new ArrayList<>();
        adapter = new MyGroupAdapter(groupList, currentUser != null ? currentUser.getUid() : "", this);
        binding.rvStudyGroups.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStudyGroups.setAdapter(adapter);

        // Create new group button
        binding.fabCreateGroup.setOnClickListener(v -> createNewGroup());
    }
    
    private void loadStudyGroups() {
        if (currentUser == null) return;

        // Load groups where current user is a member
        db.collection("group_chats")
                .whereArrayContains("members", currentUser.getUid())
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("StudyGroups", "Error loading groups", error);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading groups", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    groupList.clear();

                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot document : value) {
                            GroupChat group = document.toObject(GroupChat.class);
                            group.setGroupId(document.getId());

                            // Debug: Log unread count data
                            Log.d("StudyGroups", "Group: " + group.getTitle());
                            Log.d("StudyGroups", "Group ID: " + group.getGroupId());
                            Log.d("StudyGroups", "All unread counts: " + group.getUnreadCounts());
                            Log.d("StudyGroups", "Unread count for current user (" + currentUser.getUid() + "): " + group.getUnreadCountForUser(currentUser.getUid()));

                            groupList.add(group);
                        }
                        showGroups();
                    } else {
                        showEmptyState();
                    }

                    // Force adapter refresh to update badges
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                        Log.d("StudyGroups", "Adapter refreshed with " + groupList.size() + " groups");
                    }
                });
    }

    private void showGroups() {
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.rvStudyGroups.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        binding.rvStudyGroups.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGroupClick(GroupChat group) {
        // Open group chat
        Intent intent = new Intent(getContext(), GroupChatActivity.class);
        intent.putExtra("group_id", group.getGroupId());
        intent.putExtra("group_title", group.getTitle());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh group data when fragment resumes to update badges
        Log.d("StudyGroups", "Fragment resumed, refreshing group data");
        if (currentUser != null) {
            loadStudyGroups();
        }
    }
    
    private void createNewGroup() {
        Intent intent = new Intent(getContext(), CreateGroupActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 