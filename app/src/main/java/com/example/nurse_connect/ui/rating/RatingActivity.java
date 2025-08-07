package com.example.nurse_connect.ui.rating;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.nurse_connect.R;
import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.viewmodels.RatingViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RatingActivity extends AppCompatActivity {
    
    private RatingViewModel viewModel;
    private StudyMaterial studyMaterial;
    
    // UI Components
    private TextView tvDocumentTitle, tvDocumentAuthor, tvAverageRating, tvTotalRatings;
    private TextView tvRatingText, tvCount1, tvCount2, tvCount3, tvCount4, tvCount5;
    private RatingBar ratingBarAverage, ratingBarUser;
    private TextInputEditText etRatingComment;
    private MaterialButton btnSubmitRating;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);
        
        // Get study material from intent
        String materialId = getIntent().getStringExtra("material_id");
        if (materialId == null) {
            Toast.makeText(this, "Error: No document specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(RatingViewModel.class);
        
        // Setup UI
        setupToolbar();
        initializeViews();
        setupListeners();
        
        // Load rating data
        viewModel.loadRatingData(materialId);
        observeViewModel();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void initializeViews() {
        tvDocumentTitle = findViewById(R.id.tvDocumentTitle);
        tvDocumentAuthor = findViewById(R.id.tvDocumentAuthor);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalRatings = findViewById(R.id.tvTotalRatings);
        tvRatingText = findViewById(R.id.tvRatingText);
        tvCount1 = findViewById(R.id.tvCount1);
        tvCount2 = findViewById(R.id.tvCount2);
        tvCount3 = findViewById(R.id.tvCount3);
        tvCount4 = findViewById(R.id.tvCount4);
        tvCount5 = findViewById(R.id.tvCount5);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        ratingBarUser = findViewById(R.id.ratingBarUser);
        etRatingComment = findViewById(R.id.etRatingComment);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);
    }
    
    private void setupListeners() {
        ratingBarUser.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                updateRatingText(rating);
                btnSubmitRating.setEnabled(rating > 0);
            }
        });
        
        btnSubmitRating.setOnClickListener(v -> submitRating());
    }
    
    private void updateRatingText(float rating) {
        String[] ratingTexts = {
            "Tap to rate",
            "Poor",
            "Fair", 
            "Good",
            "Very Good",
            "Excellent"
        };
        
        int index = (int) rating;
        if (index >= 0 && index < ratingTexts.length) {
            tvRatingText.setText(ratingTexts[index]);
        }
    }
    
    private void submitRating() {
        float rating = ratingBarUser.getRating();
        String comment = etRatingComment.getText().toString().trim();
        
        if (rating <= 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        
        viewModel.submitRating((int) rating, comment);
    }
    
    private void observeViewModel() {
        // Observe study material
        viewModel.getStudyMaterial().observe(this, material -> {
            if (material != null) {
                studyMaterial = material;
                tvDocumentTitle.setText(material.getTitle());
                tvDocumentAuthor.setText("by " + material.getAuthorName());
            }
        });
        
        // Observe rating statistics
        viewModel.getRatingStats().observe(this, stats -> {
            if (stats != null) {
                // Update average rating
                float averageRating = stats.getAverageRating();
                tvAverageRating.setText(String.format("%.1f", averageRating));
                ratingBarAverage.setRating(averageRating);
                tvTotalRatings.setText("(" + stats.getTotalRatings() + " ratings)");
                
                // Update distribution
                tvCount5.setText(String.valueOf(stats.getCount5()));
                tvCount4.setText(String.valueOf(stats.getCount4()));
                tvCount3.setText(String.valueOf(stats.getCount3()));
                tvCount2.setText(String.valueOf(stats.getCount2()));
                tvCount1.setText(String.valueOf(stats.getCount1()));
            }
        });
        
        // Observe user's existing rating
        viewModel.getUserRating().observe(this, userRating -> {
            if (userRating != null) {
                ratingBarUser.setRating(userRating.getRating());
                etRatingComment.setText(userRating.getComment());
                updateRatingText(userRating.getRating());
                btnSubmitRating.setText("Update Rating");
            }
        });
        
        // Observe submission result
        viewModel.getSubmissionResult().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Rating submitted successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 