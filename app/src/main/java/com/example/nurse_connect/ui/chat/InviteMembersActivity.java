package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.adapters.UserInviteAdapter;
import com.example.nurse_connect.databinding.ActivityInviteMembersBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InviteMembersActivity extends AppCompatActivity implements UserInviteAdapter.OnUserSelectionListener {

    private ActivityInviteMembersBinding binding;
    private UserInviteAdapter adapter;
    private List<User> userList;
    private List<User> selectedUsers;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        binding = ActivityInviteMembersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupUI();
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

        // Initialize lists
        userList = new ArrayList<>();
        selectedUsers = new ArrayList<>();

        // Setup RecyclerView
        adapter = new UserInviteAdapter(userList, selectedUsers, this);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(adapter);

        // Setup search functionality
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    clearResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup click listeners
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.btnDone.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            ArrayList<String> selectedUserIds = new ArrayList<>();
            for (User user : selectedUsers) {
                selectedUserIds.add(user.getUid());
            }
            resultIntent.putStringArrayListExtra("selected_user_ids", selectedUserIds);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        updateSelectedCount();
    }

    private void searchUsers(String query) {
        if (currentUser == null) return;

        showLoading();

        // Search users by username (case-insensitive)
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query.toLowerCase())
                .whereLessThanOrEqualTo("username", query.toLowerCase() + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hideLoading();
                    List<User> searchResults = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        
                        // Don't include current user in results
                        if (!user.getUid().equals(currentUser.getUid())) {
                            searchResults.add(user);
                        }
                    }

                    if (searchResults.isEmpty()) {
                        showEmptyResults();
                    } else {
                        showResults(searchResults);
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e("InviteMembers", "Error searching users", e);
                    Toast.makeText(this, "Error searching users", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearResults() {
        userList.clear();
        adapter.notifyDataSetChanged();
        showEmptyState();
    }

    private void showResults(List<User> results) {
        binding.emptyState.setVisibility(View.GONE);
        binding.rvUsers.setVisibility(View.VISIBLE);
        userList.clear();
        userList.addAll(results);
        adapter.notifyDataSetChanged();
    }

    private void showEmptyResults() {
        binding.rvUsers.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        binding.rvUsers.setVisibility(View.GONE);
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
    public void onUserSelected(User user, boolean isSelected) {
        if (isSelected) {
            if (!selectedUsers.contains(user)) {
                selectedUsers.add(user);
            }
        } else {
            selectedUsers.remove(user);
        }
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = selectedUsers.size();
        if (count > 0) {
            binding.tvSelectedCount.setVisibility(View.VISIBLE);
            binding.tvSelectedCount.setText(count + " member" + (count == 1 ? "" : "s") + " selected");
        } else {
            binding.tvSelectedCount.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
