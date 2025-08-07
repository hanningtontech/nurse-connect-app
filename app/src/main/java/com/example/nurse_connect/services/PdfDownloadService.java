package com.example.nurse_connect.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.nurse_connect.R;
import com.example.nurse_connect.data.StudyMaterialRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfDownloadService extends Service {
    private static final String TAG = "PdfDownloadService";
    private static final String CHANNEL_ID = "pdf_download_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String pdfUrl = intent.getStringExtra("pdf_url");
            String pdfTitle = intent.getStringExtra("pdf_title");
            String materialId = intent.getStringExtra("material_id");

            if (pdfUrl != null && pdfTitle != null) {
                downloadPdf(pdfUrl, pdfTitle, materialId);
            }
        }
        return START_NOT_STICKY;
    }

    private void downloadPdf(String pdfUrl, String pdfTitle, String materialId) {
        new Thread(() -> {
            try {
                // Create notification
                createNotificationChannel();
                startForeground(NOTIFICATION_ID, createNotification("Downloading PDF...", 0));

                // Try multiple directory locations for better compatibility
                File downloadDir = getDownloadDirectory();
                if (downloadDir == null) {
                    Log.e(TAG, "Could not create download directory");
                    updateNotification("Download failed: Cannot access storage", 0);
                    stopForeground(true);
                    stopSelf();
                    return;
                }

                // Create filename
                String fileName = sanitizeFileName(pdfTitle) + ".pdf";
                File pdfFile = new File(downloadDir, fileName);

                // Check if file already exists and create unique name if needed
                int counter = 1;
                while (pdfFile.exists()) {
                    String baseName = sanitizeFileName(pdfTitle);
                    fileName = baseName + "_" + counter + ".pdf";
                    pdfFile = new File(downloadDir, fileName);
                    counter++;
                }

                Log.d(TAG, "Downloading to: " + pdfFile.getAbsolutePath());

                // Download the file
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "NurseConnect/1.0");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();

                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(pdfFile);

                byte[] buffer = new byte[4096];
                long total = 0;
                int count;

                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    output.write(buffer, 0, count);

                    // Update progress
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        updateNotification("Downloading PDF...", progress);
                    }
                }

                output.flush();
                output.close();
                input.close();

                // Verify file was created and has content
                if (!pdfFile.exists() || pdfFile.length() == 0) {
                    throw new IOException("Downloaded file is empty or was not created");
                }

                Log.d(TAG, "Download completed successfully. File size: " + pdfFile.length() + " bytes");

                // Show completion notification
                updateNotification("PDF downloaded successfully!", 100);
                
                // Open file manager to the downloaded file location
                openFileManagerToDownload(pdfFile);
                
                // Stop service after a delay
                Thread.sleep(2000);
                stopForeground(true);
                stopSelf();

            } catch (Exception e) {
                Log.e(TAG, "Error downloading PDF", e);
                updateNotification("Download failed: " + e.getMessage(), 0);
                stopForeground(true);
                stopSelf();
            }
        }).start();
    }

    private File getDownloadDirectory() {
        // Try multiple locations for better compatibility
        File[] possibleDirs = {
            // Primary: External Downloads/NURSE_CONNECT
            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NURSE_CONNECT"),
            // Fallback: App's external files directory
            new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "NURSE_CONNECT"),
            // Fallback: App's cache directory
            new File(getCacheDir(), "downloads"),
            // Last resort: App's files directory
            new File(getFilesDir(), "downloads")
        };

        for (File dir : possibleDirs) {
            try {
                if (!dir.exists()) {
                    if (dir.mkdirs()) {
                        Log.d(TAG, "Created directory: " + dir.getAbsolutePath());
                        return dir;
                    } else {
                        Log.w(TAG, "Failed to create directory: " + dir.getAbsolutePath());
                    }
                } else {
                    Log.d(TAG, "Using existing directory: " + dir.getAbsolutePath());
                    return dir;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error accessing directory: " + dir.getAbsolutePath(), e);
            }
        }

        return null;
    }

    private String sanitizeFileName(String fileName) {
        // Remove invalid characters for file names
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "PDF Downloads",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows PDF download progress");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String message, int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nurse Connect")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        if (progress > 0) {
            builder.setProgress(100, progress, false);
        }

        return builder.build();
    }

    private void updateNotification(String message, int progress) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(message, progress));
        }
    }

    private void openFileManagerToDownload(File pdfFile) {
        try {
            Log.d(TAG, "Attempting to open file: " + pdfFile.getAbsolutePath());
            
            // Try to open the PDF file directly first
            Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
            openFileIntent.setDataAndType(android.net.Uri.fromFile(pdfFile), "application/pdf");
            openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            openFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (openFileIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(openFileIntent);
                Log.d(TAG, "Opened PDF file directly");
            } else {
                // Fallback: Open file manager to the directory
                Intent fileManagerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileManagerIntent.setType("*/*");
                fileManagerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fileManagerIntent);
                Log.d(TAG, "Opened file manager as fallback");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file manager: " + e.getMessage(), e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 