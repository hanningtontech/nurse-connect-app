package com.example.nurse_connect.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.nurse_connect.ui.chat.DirectMessagesFragment;
import com.example.nurse_connect.ui.chat.StudyGroupsFragment;

public class ChatPagerAdapter extends FragmentStateAdapter {

    public ChatPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DirectMessagesFragment();
            case 1:
                return new StudyGroupsFragment();
            default:
                return new DirectMessagesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
} 