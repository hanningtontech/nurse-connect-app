package com.example.nurse_connect.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.FragmentCommunityHubBinding;
import com.example.nurse_connect.models.User;
import com.example.nurse_connect.ui.chat.ChatActivity;
import com.example.nurse_connect.viewmodels.CommunityViewModel;

import java.util.List;

public class CommunityHubFragment extends Fragment {
    
    private FragmentCommunityHubBinding binding;
    private CommunityViewModel viewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        setupUI();
        setupClickListeners();
        loadNurseAvatars();
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);
        
        // Observe featured nurses
        viewModel.getFeaturedNurses().observe(getViewLifecycleOwner(), nurses -> {
            if (nurses != null && !nurses.isEmpty()) {
                displayNurseAvatars(nurses);
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // You can show/hide loading indicators here if needed
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupUI() {
        // Set default placeholder images for all avatars
        binding.ivFeaturedNurse.setImageResource(R.drawable.ic_nurse_avatar_placeholder);

    }
    
    private void loadNurseAvatars() {
        viewModel.loadFeaturedNurses();
        

         loadAvatar(binding.ivFeaturedNurse, "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fspecial%2Fadditional_nursing_avatar_1%20(2).png?alt=media&token=9aad52a0-0025-4a38-88f0-0f877532d8a2");
        loadAvatar(binding.globalnursehubAvatar, "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fstudy-hub%2Fnursing_global_hub_avatar_3d_transparent.png?alt=media&token=a28579bc-afb3-4792-98d6-ab2c9a773d87");
        loadAvatar(binding.publicgroubsAvatar, "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fstudy-hub%2Fgroup_of_happy_nurses_3d_avatar_transparent.png?alt=media&token=87a93bc6-6897-43d0-aa3b-479ae4449c64");
        loadAvatar(binding.mygroubsAvatar, "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fstudy-hub%2Fnursing_study_hub_avatar_new.png?alt=media&token=54fb157e-d896-4387-b0dc-64fabb6c6749");
        loadAvatar(binding.publictaskAvatar, "https://firebasestorage.googleapis.com/v0/b/nurseconnect-c68eb.firebasestorage.app/o/nursing-avatars%2Fstudy-hub%2Fnursing_notebook_avatar.png?alt=media&token=23560934-433f-4f3f-b4dc-512b1eb40e58");
    }
    
    private void displayNurseAvatars(List<User> nurses) {
        if (nurses == null || nurses.isEmpty()) return;
        
        // Load featured nurse (first nurse)
        if (nurses.size() > 0) {
            loadAvatar(binding.ivFeaturedNurse, nurses.get(0).getPhotoURL());
        }
    }
    
    private void loadAvatar(android.widget.ImageView imageView, String photoURL) {
        if (photoURL != null && !photoURL.isEmpty()) {
            Glide.with(requireContext())
                    .load(photoURL)
                    .placeholder(R.drawable.ic_nurse_avatar_placeholder)
                    .error(R.drawable.ic_nurse_avatar_placeholder)
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_nurse_avatar_placeholder);
        }
    }
    
    private void setupClickListeners() {
        // Connect with Other Nurses button
        binding.btnConnect.setOnClickListener(v -> {
            // Navigate to Connect activity
            Intent intent = new Intent(requireContext(), ConnectActivity.class);
            startActivity(intent);
        });
        
        // Search button
        binding.btnSearch.setOnClickListener(v -> {
            // TODO: Implement search functionality with nurse avatars
            Toast.makeText(requireContext(), "Search feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        // Notifications button
        binding.btnNotifications.setOnClickListener(v -> {
            // TODO: Implement notifications functionality
            Toast.makeText(requireContext(), "Notifications feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        // Global Nurse Hub card
        binding.cardGlobalHub.setOnClickListener(v -> {
            // Navigate to Global Nurse Hub activity
            Intent intent = new Intent(requireContext(), GlobalNurseHubActivity.class);
            startActivity(intent);
        });
        
        // Public Groups card
        binding.cardPublicGroups.setOnClickListener(v -> {
            // Navigate to public groups
            Intent intent = new Intent(requireContext(), PublicGroupsActivity.class);
            startActivity(intent);
        });

        // My Groups card
        binding.cardMyGroups.setOnClickListener(v -> {
            // Navigate to my groups
            Intent intent = new Intent(requireContext(), MyGroupsActivity.class);
            startActivity(intent);
        });
        
        // Public Tasks card
        binding.cardPublicTasks.setOnClickListener(v -> {
            // TODO: Navigate to public tasks
            Toast.makeText(requireContext(), "Public Tasks feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 