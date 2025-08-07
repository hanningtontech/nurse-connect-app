package com.example.nurse_connect.ui.pdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.nurse_connect.R;
import com.example.nurse_connect.databinding.ActivityCustomPdfViewerBinding;
import com.example.nurse_connect.services.PdfDownloadService;
import com.google.android.material.snackbar.Snackbar;

public class CustomPdfViewerActivity extends AppCompatActivity {
    private ActivityCustomPdfViewerBinding binding;
    private String pdfUrl;
    private String pdfTitle;
    private String materialId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            pdfUrl = intent.getStringExtra("pdf_url");
            pdfTitle = intent.getStringExtra("pdf_title");
            materialId = intent.getStringExtra("material_id");
        }

        if (pdfUrl == null || pdfTitle == null) {
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupWebView();
        loadPdfFromUrl();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(pdfTitle);
        }
    }

    private void setupWebView() {
        // Configure WebView
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);
        binding.webView.getSettings().setLoadWithOverviewMode(true);
        binding.webView.getSettings().setUseWideViewPort(true);
        binding.webView.getSettings().setBuiltInZoomControls(true);
        binding.webView.getSettings().setDisplayZoomControls(false);
        binding.webView.getSettings().setSupportZoom(true);
        binding.webView.getSettings().setDefaultTextEncodingName("utf-8");

        // Set WebView client
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.tvStatus.setVisibility(View.VISIBLE);
                binding.tvStatus.setText("Loading PDF...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvStatus.setVisibility(View.GONE);
                binding.webView.setVisibility(View.VISIBLE);
                Snackbar.make(binding.getRoot(), "PDF loaded successfully", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvStatus.setText("Failed to load PDF. Please try downloading it instead.");
                binding.btnDownloadAndOpen.setVisibility(View.VISIBLE);
                binding.btnRetry.setVisibility(View.VISIBLE);
            }
        });

        setupButtons();
    }

    private void loadPdfFromUrl() {
        try {
            // Create a Google Docs viewer URL for the PDF
            String googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=" + Uri.encode(pdfUrl);
            binding.webView.loadUrl(googleDocsUrl);
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvStatus.setText("Unable to load PDF directly. Please download it to view.");
            binding.btnDownloadAndOpen.setVisibility(View.VISIBLE);
            binding.btnRetry.setVisibility(View.VISIBLE);
        }
    }

    private void setupButtons() {
        binding.btnDownloadAndOpen.setOnClickListener(v -> downloadAndOpenPdf());
        binding.btnRetry.setOnClickListener(v -> loadPdfFromUrl());
    }

    private void downloadAndOpenPdf() {
        // Increment download count first
        if (materialId != null) {
            com.example.nurse_connect.data.StudyMaterialRepository repository = new com.example.nurse_connect.data.StudyMaterialRepository();
            repository.incrementDownloadCount(materialId, new com.example.nurse_connect.data.StudyMaterialRepository.IncrementCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("CustomPdfViewerActivity", "Download count incremented successfully for: " + materialId);
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("CustomPdfViewerActivity", "Failed to increment download count for: " + materialId, e);
                }
            });
        }
        
        // Launch download service
        Intent downloadIntent = new Intent(this, PdfDownloadService.class);
        downloadIntent.putExtra("pdf_url", pdfUrl);
        downloadIntent.putExtra("pdf_title", pdfTitle);
        downloadIntent.putExtra("material_id", materialId);
        startService(downloadIntent);
        
        Toast.makeText(this, "Downloading PDF... Will open file after download completes!", Toast.LENGTH_LONG).show();
        finish();
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
        if (pdfUrl != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this document: " + pdfTitle);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "I found this great study material: " + pdfTitle + "\n\n" + pdfUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }

    private void downloadPdf() {
        downloadAndOpenPdf();
    }

    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding.webView != null) {
            binding.webView.destroy();
        }
    }
} 