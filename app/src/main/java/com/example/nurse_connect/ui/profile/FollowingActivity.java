package com.example.nurse_connect.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.UserAdapter;
import com.example.nurse_connect.databinding.ActivityFollowingBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.viewmodels.UserFollowViewModel;
import java.util.ArrayList;
import java.util.List;

public class FollowingActivity extends AppCompatActivity {
    private ActivityFollowingBinding binding;
    private UserFollowViewModel viewModel;
    private UserAdapter adapter;
    private String userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get user data from intent
        userId = getIntent().getStringExtra("user_id");
        userName = getIntent().getStringExtra("user_name");
        if (userId == null) {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        loadFollowing();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(userName != null ? userName + "'s following" : "Following");
        }
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(new ArrayList<>(), user -> {
            // Navigate to user profile
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("user_id", user.getUid());
            startActivity(intent);
        });

        binding.rvFollowing.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFollowing.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(UserFollowViewModel.class);

        // Observe following
        viewModel.getFollowing().observe(this, following -> {
            if (following != null) {
                adapter.updateUsers(following);
                updateEmptyState(following.isEmpty());
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadFollowing() {
        viewModel.loadFollowing(userId);
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvFollowing.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 