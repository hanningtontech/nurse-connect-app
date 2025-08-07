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
import com.example.nurse_connect.databinding.ActivityFollowersBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.viewmodels.UserFollowViewModel;
import java.util.ArrayList;
import java.util.List;

public class FollowersActivity extends AppCompatActivity {
    private ActivityFollowersBinding binding;
    private UserFollowViewModel viewModel;
    private UserAdapter adapter;
    private String userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowersBinding.inflate(getLayoutInflater());
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
        loadFollowers();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(userName != null ? userName + "'s followers" : "Followers");
        }
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(new ArrayList<>(), user -> {
            // Navigate to user profile
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("user_id", user.getUid());
            startActivity(intent);
        });

        binding.rvFollowers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFollowers.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(UserFollowViewModel.class);

        // Observe followers
        viewModel.getFollowers().observe(this, followers -> {
            if (followers != null) {
                adapter.updateUsers(followers);
                updateEmptyState(followers.isEmpty());
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

    private void loadFollowers() {
        viewModel.loadFollowers(userId);
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvFollowers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 