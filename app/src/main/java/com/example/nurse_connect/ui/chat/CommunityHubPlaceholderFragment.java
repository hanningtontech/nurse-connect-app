package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nurse_connect.databinding.FragmentCommunityHubPlaceholderBinding;
import com.example.nurse_connect.ui.community.CommunityHubActivity;

/**
 * Placeholder fragment for Community Hub tab
 * Automatically opens CommunityHubActivity when this tab is selected
 */
public class CommunityHubPlaceholderFragment extends Fragment {
    
    private FragmentCommunityHubPlaceholderBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityHubPlaceholderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup click listener for the button
        binding.btnOpenCommunityHub.setOnClickListener(v -> openCommunityHub());
    }
    
    private void openCommunityHub() {
        Intent intent = new Intent(requireContext(), CommunityHubActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
