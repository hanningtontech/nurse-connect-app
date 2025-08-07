package com.example.nurse_connect.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.databinding.FragmentGlobalChatBinding;

public class GlobalChatFragment extends Fragment {
    
    private FragmentGlobalChatBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGlobalChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupUI();
        loadGlobalChats();
    }
    
    private void setupUI() {
        binding.rvGlobalChats.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter for global chat rooms
    }
    
    private void loadGlobalChats() {
        // TODO: Load global chat rooms from repository
        // This would include: General Discussion, Study Help, Q&A Forum
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 