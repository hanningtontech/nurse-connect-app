package com.example.nurse_connect;

import android.app.Application;
import com.example.nurse_connect.utils.ThemeManager;

/**
 * Application class for global initialization
 */
public class NurseConnectApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize theme system globally
        ThemeManager.getInstance(this).applyTheme();
    }
}
