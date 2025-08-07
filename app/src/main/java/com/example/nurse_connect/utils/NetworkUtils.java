package com.example.nurse_connect.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkUtils {
    
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    public static void showNetworkError(Context context) {
        Toast.makeText(context, 
            "No internet connection. Please check your network settings.", 
            Toast.LENGTH_LONG).show();
    }
    
    public static void showFirebaseError(Context context) {
        Toast.makeText(context, 
            "Unable to connect to Firebase. Please check your internet connection.", 
            Toast.LENGTH_LONG).show();
    }
} 