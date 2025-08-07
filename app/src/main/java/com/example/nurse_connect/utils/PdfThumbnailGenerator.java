package com.example.nurse_connect.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PdfThumbnailGenerator {
    private static final String TAG = "PdfThumbnailGenerator";
    private static final int THUMBNAIL_WIDTH = 4000;
    private static final int THUMBNAIL_HEIGHT = 6000;
    private static boolean isPdfBoxInitialized = false;

    public static void initializePdfBox(Context context) {
        if (!isPdfBoxInitialized) {
            Log.d(TAG, "Initializing PDFBox library...");
            PDFBoxResourceLoader.init(context);
            isPdfBoxInitialized = true;
            Log.d(TAG, "PDFBox library initialized successfully");
        } else {
            Log.d(TAG, "PDFBox library already initialized");
        }
    }

    public static int getPageCount(Context context, Uri pdfUri) {
        Log.d(TAG, "Getting page count for URI: " + pdfUri);
        if (!isPdfBoxInitialized) {
            Log.d(TAG, "PDFBox not initialized, initializing now...");
            initializePdfBox(context);
        }

        InputStream inputStream = null;
        PDDocument document = null;

        try {
            // Get input stream from URI
            inputStream = context.getContentResolver().openInputStream(pdfUri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for PDF");
                return 0;
            }
            Log.d(TAG, "Successfully opened input stream for PDF");

            // Load PDF document
            document = PDDocument.load(inputStream);
            int pageCount = document.getNumberOfPages();
            Log.d(TAG, "PDF document loaded successfully, pages: " + pageCount);
            return pageCount;

        } catch (IOException e) {
            Log.e(TAG, "Error getting page count: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting page count: " + e.getMessage(), e);
            return 0;
        } finally {
            // Clean up resources
            try {
                if (document != null) {
                    document.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up resources: " + e.getMessage(), e);
            }
        }
    }

    public static Bitmap generateThumbnail(Context context, Uri pdfUri) {
        Log.d(TAG, "Starting thumbnail generation for URI: " + pdfUri);
        if (!isPdfBoxInitialized) {
            Log.d(TAG, "PDFBox not initialized, initializing now...");
            initializePdfBox(context);
        }

        InputStream inputStream = null;
        PDDocument document = null;
        PDFRenderer renderer = null;
        Bitmap bitmap = null;

        try {
            // Get input stream from URI
            inputStream = context.getContentResolver().openInputStream(pdfUri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for PDF");
                return null;
            }
            Log.d(TAG, "Successfully opened input stream for PDF");

            // Load PDF document
            document = PDDocument.load(inputStream);
            Log.d(TAG, "PDF document loaded successfully, pages: " + document.getNumberOfPages());
            if (document.getNumberOfPages() == 0) {
                Log.e(TAG, "PDF has no pages");
                return null;
            }

            // Create PDF renderer
            renderer = new PDFRenderer(document);
            Log.d(TAG, "PDF renderer created successfully");

            // Render first page
            bitmap = renderer.renderImageWithDPI(0, 600); // 600 DPI for very high quality
            Log.d(TAG, "First page rendered successfully, original size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            // Scale bitmap to thumbnail size
            Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
            Log.d(TAG, "Thumbnail scaled successfully to: " + thumbnail.getWidth() + "x" + thumbnail.getHeight());

            return thumbnail;

        } catch (IOException e) {
            Log.e(TAG, "Error generating thumbnail: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error generating thumbnail: " + e.getMessage(), e);
            return null;
        } finally {
            // Clean up resources
            try {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                if (document != null) {
                    document.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up resources: " + e.getMessage(), e);
            }
        }
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, cannot convert to byte array");
            return null;
        }
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // Use PNG format for better text quality, especially for documents with text
        boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        if (success) {
            byte[] bytes = stream.toByteArray();
            Log.d(TAG, "Bitmap converted to byte array successfully, size: " + bytes.length + " bytes");
            return bytes;
        } else {
            Log.e(TAG, "Failed to compress bitmap to JPEG");
            return null;
        }
    }

    public static Bitmap byteArrayToBitmap(byte[] bytes) {
        if (bytes == null) return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
} 