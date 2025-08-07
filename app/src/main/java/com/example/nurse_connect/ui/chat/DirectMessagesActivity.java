package com.example.nurse_connect.ui.chat;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityDirectMessagesBinding;

/**
 * Activity to host the DirectMessagesFragment
 * This provides a dedicated screen for private messaging with unread badges
 */
public class DirectMessagesActivity extends AppCompatActivity {
    
    private ActivityDirectMessagesBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityDirectMessagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupDirectMessagesFragment();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Direct Messages");
        }
    }
    
    private void setupDirectMessagesFragment() {
        // Add the DirectMessagesFragment to the container
        DirectMessagesFragment fragment = new DirectMessagesFragment();
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
