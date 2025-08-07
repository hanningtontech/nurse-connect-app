package com.example.nurse_connect.ui.pdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityPdfViewerBinding;
import com.example.nurse_connect.models.StudyMaterial;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {
    private ActivityPdfViewerBinding binding;
    private String pdfUrl;
    private String pdfTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            pdfUrl = intent.getStringExtra("pdf_url");
            pdfTitle = intent.getStringExtra("pdf_title");
        }

        if (pdfUrl == null || pdfTitle == null) {
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        loadPdf();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(pdfTitle);
        }
    }

    private void loadPdf() {
        // Try to open PDF with available apps
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(pdfUrl), "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                finish();
            } else {
                showDownloadOption();
            }
        } catch (Exception e) {
            showDownloadOption();
        }
    }

    private void showDownloadOption() {
        binding.progressBar.setVisibility(android.view.View.GONE);
        binding.tvStatus.setText("No PDF viewer app found on your device.\nYou can download the PDF and open it manually from the Downloads folder.");
        binding.btnDownloadAndOpen.setVisibility(android.view.View.VISIBLE);
        
        binding.btnDownloadAndOpen.setOnClickListener(v -> {
            // Launch download service
            Intent downloadIntent = new Intent(this, com.example.nurse_connect.services.PdfDownloadService.class);
            downloadIntent.putExtra("pdf_url", pdfUrl);
            downloadIntent.putExtra("pdf_title", pdfTitle);
            downloadIntent.putExtra("material_id", getIntent().getStringExtra("material_id"));
            startService(downloadIntent);
            
            Toast.makeText(this, "Downloading PDF... Will open file after download completes!", Toast.LENGTH_LONG).show();
            finish(); // Close this activity
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_share) {
            sharePdf();
            return true;
        } else if (id == R.id.action_download) {
            downloadPdf();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sharePdf() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, pdfTitle);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this study material: " + pdfTitle);
            
            // Add the PDF URI
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pdfUrl));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadPdf() {
        // Launch download service
        Intent downloadIntent = new Intent(this, com.example.nurse_connect.services.PdfDownloadService.class);
        downloadIntent.putExtra("pdf_url", pdfUrl);
        downloadIntent.putExtra("pdf_title", pdfTitle);
        downloadIntent.putExtra("material_id", getIntent().getStringExtra("material_id"));
        startService(downloadIntent);
        
        Toast.makeText(this, "Downloading PDF to NURSE_CONNECT folder...", Toast.LENGTH_LONG).show();
    }
} 