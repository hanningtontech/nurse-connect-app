package com.example.nurse_connect.ui.upload;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nurse_connect.data.StudyMaterialRepository;
import com.example.nurse_connect.databinding.ActivityEditDocumentBinding;
import com.example.nurse_connect.models.MaterialType;
import com.example.nurse_connect.models.StudyCategory;
import com.example.nurse_connect.models.StudyMaterial;

import java.util.Arrays;
import java.util.List;

public class EditDocumentActivity extends AppCompatActivity {

    private ActivityEditDocumentBinding binding;
    private StudyMaterialRepository repository;
    private StudyMaterial material;
    private String materialId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new StudyMaterialRepository();
        materialId = getIntent().getStringExtra("material_id");
        material = (StudyMaterial) getIntent().getSerializableExtra("material");

        if (material == null || materialId == null) {
            Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupSpinners();
        populateFields();
        setupButtons();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Document");
        }
    }

    private void setupSpinners() {
        // Setup category spinner
        List<String> categories = Arrays.asList(
            "Anatomy & Physiology",
            "Medical-Surgical Nursing",
            "Pediatric Nursing",
            "Obstetric Nursing",
            "Psychiatric Nursing",
            "Community Health",
            "Nursing Research",
            "Nursing Ethics",
            "Pharmacology",
            "Pathophysiology",
            "Health Assessment",
            "Critical Care",
            "Emergency Nursing",
            "Geriatric Nursing",
            "Oncology Nursing",
            "Other"
        );
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);

        // Setup type spinner
        List<String> types = Arrays.asList("PDF", "DOC", "PPT", "VIDEO", "AUDIO", "IMAGE");
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerType.setAdapter(typeAdapter);

        // Setup privacy spinner
        List<String> privacyOptions = Arrays.asList("Public", "Private");
        ArrayAdapter<String> privacyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, privacyOptions);
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPrivacy.setAdapter(privacyAdapter);
    }

    private void populateFields() {
        binding.etTitle.setText(material.getTitle());
        binding.etDescription.setText(material.getDescription());
        binding.etPrice.setText(material.getPrice());

        // Set category
        String category = material.getCategory();
        if (category != null && !category.isEmpty()) {
            binding.spinnerCategory.setText(category);
        }

        // Set type
        String type = material.getType();
        if (type != null && !type.isEmpty()) {
            binding.spinnerType.setText(type);
        }

        // Set privacy (assuming material has privacy field, if not we'll default to public)
        String privacy = material.getPrivacy();
        if (privacy != null && !privacy.isEmpty()) {
            binding.spinnerPrivacy.setText(privacy);
        } else {
            binding.spinnerPrivacy.setText("Public");
        }
    }

    private void setupButtons() {
        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void saveChanges() {
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String price = binding.etPrice.getText().toString().trim();
        String category = binding.spinnerCategory.getText().toString();
        String type = binding.spinnerType.getText().toString();
        String privacy = binding.spinnerPrivacy.getText().toString();

        // Validation
        if (title.isEmpty()) {
            binding.etTitle.setError("Title is required");
            return;
        }

        if (description.isEmpty()) {
            binding.etDescription.setError("Description is required");
            return;
        }

        if (price.isEmpty()) {
            binding.etPrice.setError("Price is required");
            return;
        }

        if (category.isEmpty()) {
            binding.spinnerCategory.setError("Category is required");
            return;
        }

        if (type.isEmpty()) {
            binding.spinnerType.setError("Type is required");
            return;
        }

        if (privacy.isEmpty()) {
            binding.spinnerPrivacy.setError("Privacy is required");
            return;
        }

        // Show loading
        binding.btnSave.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Update the material object
        material.setTitle(title);
        material.setDescription(description);
        material.setPrice(price);
        material.setCategory(category);
        material.setType(type);
        material.setPrivacy(privacy);

        // Save to Firestore
        repository.updateStudyMaterial(materialId, material, new StudyMaterialRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    binding.btnSave.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditDocumentActivity.this, "Document updated successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Return to previous activity with updated data
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_material", material);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    binding.btnSave.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditDocumentActivity.this, "Failed to update document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete \"" + material.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDocument() {
        binding.btnDelete.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Get the file name from the file URL
        String fileName = material.getFileUrl();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }

        repository.deleteStudyMaterial(materialId, fileName, () -> {
            runOnUiThread(() -> {
                binding.btnDelete.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(EditDocumentActivity.this, "Document deleted successfully!", Toast.LENGTH_SHORT).show();
                
                // Return to previous activity with delete result
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deleted_material_id", materialId);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        });
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