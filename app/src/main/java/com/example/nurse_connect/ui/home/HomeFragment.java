package com.example.nurse_connect.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.FragmentHomeBinding;
import com.example.nurse_connect.ui.community.CommunityHubActivity;

public class HomeFragment extends Fragment {
    
    private FragmentHomeBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupUI();
        loadFeed();
    }
    
    private void setupUI() {
        // Setup RecyclerView
        binding.rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter for feed items

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadFeed();
            }
        });

        // Setup Community Hub button
        binding.btnCommunityHub.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CommunityHubActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadFeed() {
        // TODO: Load feed data from repository
        // For now, just stop refreshing
        binding.swipeRefreshLayout.setRefreshing(false);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 