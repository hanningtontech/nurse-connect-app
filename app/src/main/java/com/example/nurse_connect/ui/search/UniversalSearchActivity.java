package com.example.nurse_connect.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nurse_connect.R;
import com.example.nurse_connect.adapters.UniversalSearchAdapter;
import com.example.nurse_connect.databinding.ActivityUniversalSearchBinding;
import com.example.nurse_connect.services.UniversalSearchService;
import com.example.nurse_connect.utils.ThemeManager;

public class UniversalSearchActivity extends AppCompatActivity {

    private static final String TAG = "UniversalSearchActivity";
    private ActivityUniversalSearchBinding binding;
    private UniversalSearchService searchService;
    private UniversalSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);

        binding = ActivityUniversalSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeServices();
        setupUI();
        setupSearch();
    }

    private void initializeServices() {
        searchService = new UniversalSearchService();
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search");
        }

        // Setup RecyclerView
        adapter = new UniversalSearchAdapter();
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(adapter);

        // Initially show empty state
        showEmptyState();
    }

    private void setupSearch() {
        // Focus on search input
        binding.etSearch.requestFocus();

        // Setup search functionality
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    performSearch(query);
                } else if (query.isEmpty()) {
                    showEmptyState();
                }
            }
        });

        // Setup clear button
        binding.btnClear.setOnClickListener(v -> {
            binding.etSearch.setText("");
            showEmptyState();
        });
    }

    private void performSearch(String query) {
        showLoading();

        searchService.searchAll(query, new UniversalSearchService.SearchCallback() {
            @Override
            public void onSuccess(UniversalSearchService.SearchResults results) {
                runOnUiThread(() -> {
                    hideLoading();
                    if (results.getTotalResults() > 0) {
                        showResults(results);
                    } else {
                        showNoResults();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Search failed", e);
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(UniversalSearchActivity.this,
                            "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvSearchResults.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.tvNoResults.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
    }

    private void showResults(UniversalSearchService.SearchResults results) {
        binding.rvSearchResults.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.tvNoResults.setVisibility(View.GONE);

        adapter.updateResults(results);
    }

    private void showEmptyState() {
        binding.rvSearchResults.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.VISIBLE);
        binding.tvNoResults.setVisibility(View.GONE);
    }

    private void showNoResults() {
        binding.rvSearchResults.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.tvNoResults.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
