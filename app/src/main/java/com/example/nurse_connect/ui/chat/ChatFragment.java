package com.example.nurse_connect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.example.nurse_connect.MainActivity;
import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.ChatPagerAdapter;
import com.example.nurse_connect.databinding.FragmentChatBinding;
import com.example.nurse_connect.ui.community.CommunityHubActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatPagerAdapter chatPagerAdapter;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TabLayoutMediator tabLayoutMediator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFirebase();
        setupUI();
        loadUnreadCounts();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupUI() {
        // Setup ViewPager2 with fragments
        chatPagerAdapter = new ChatPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(chatPagerAdapter);

        // Connect TabLayout with ViewPager2
        tabLayoutMediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Direct Messages");
                            break;
                        case 1:
                            tab.setText("Study Groups");
                            break;
                    }
                });
        tabLayoutMediator.attach();
    }

    private void loadUnreadCounts() {
        if (currentUser == null) return;

        // Load unread counts for Direct Messages (private chats)
        loadDirectMessagesUnreadCount();

        // Load unread counts for Study Groups
        loadStudyGroupsUnreadCount();
    }

    private void loadDirectMessagesUnreadCount() {
        db.collection("private_chats")
                .whereArrayContains("participants", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatFragment", "Error loading private chats", error);
                        return;
                    }

                    int totalUnread = 0;
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Object unreadCountsObj = document.get("unreadCounts." + currentUser.getUid());
                            if (unreadCountsObj instanceof Long) {
                                totalUnread += ((Long) unreadCountsObj).intValue();
                            } else if (unreadCountsObj instanceof Integer) {
                                totalUnread += (Integer) unreadCountsObj;
                            }
                        }
                    }

                    updateTabBadge(0, totalUnread); // Direct Messages tab
                });
    }

    private void loadStudyGroupsUnreadCount() {
        db.collection("group_chats")
                .whereArrayContains("members", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatFragment", "Error loading group chats", error);
                        return;
                    }

                    int totalUnread = 0;
                    if (value != null) {
                        Log.d("ChatFragment", "Found " + value.size() + " groups for user: " + currentUser.getUid());
                        for (QueryDocumentSnapshot document : value) {
                            String groupTitle = document.getString("title");
                            String groupId = document.getId();

                            // Debug: Show all unreadCounts data
                            Object allUnreadCounts = document.get("unreadCounts");
                            Log.d("ChatFragment", "Group '" + groupTitle + "' (" + groupId + ") unreadCounts: " + allUnreadCounts);

                            Object unreadCountsObj = document.get("unreadCounts." + currentUser.getUid());
                            int groupUnread = 0;

                            if (unreadCountsObj instanceof Long) {
                                groupUnread = ((Long) unreadCountsObj).intValue();
                            } else if (unreadCountsObj instanceof Integer) {
                                groupUnread = (Integer) unreadCountsObj;
                            }

                            totalUnread += groupUnread;
                            Log.d("ChatFragment", "Group '" + groupTitle + "' has " + groupUnread + " unread messages for user " + currentUser.getUid());
                        }
                    }

                    Log.d("ChatFragment", "Total Study Groups unread count: " + totalUnread);
                    updateTabBadge(1, totalUnread); // Study Groups tab
                });
    }

    private void updateTabBadge(int tabPosition, int count) {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(tabPosition);
        String tabName = tabPosition == 0 ? "Direct Messages" : "Study Groups";

        Log.d("ChatFragment", "Updating " + tabName + " badge with count: " + count);

        if (tab != null) {
            BadgeDrawable badge = tab.getOrCreateBadge();
            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
                badge.setBackgroundColor(getResources().getColor(R.color.theme_error, null));
                Log.d("ChatFragment", tabName + " badge set to visible with count: " + count);
            } else {
                badge.setVisible(false);
                Log.d("ChatFragment", tabName + " badge hidden (count is 0)");
            }
        } else {
            Log.e("ChatFragment", "Tab at position " + tabPosition + " is null!");
        }
    }





    @Override
    public void onResume() {
        super.onResume();
        // Refresh unread counts when fragment resumes
        Log.d("ChatFragment", "Fragment resumed, refreshing unread counts");
        if (currentUser != null) {
            loadUnreadCounts();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        binding = null;
    }
}