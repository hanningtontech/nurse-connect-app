package com.example.nurse_connect.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class for managing app themes (light/dark mode)
 */
public class ThemeManager {
    
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    // Theme mode constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;
    
    private static ThemeManager instance;
    private SharedPreferences prefs;
    
    private ThemeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Apply the saved theme or default to system theme
     */
    public void applyTheme() {
        int themeMode = getThemeMode();
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    /**
     * Set the theme mode and apply it
     */
    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme();
    }
    
    /**
     * Get the current theme mode
     */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }
    
    /**
     * Check if dark theme is currently active
     */
    public boolean isDarkTheme(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Get theme mode display name
     */
    public String getThemeModeDisplayName(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return "Light";
            case THEME_DARK:
                return "Dark";
            case THEME_SYSTEM:
            default:
                return "System Default";
        }
    }
    
    /**
     * Toggle between light and dark theme (ignoring system theme)
     */
    public void toggleTheme() {
        int currentMode = getThemeMode();
        if (currentMode == THEME_LIGHT) {
            setThemeMode(THEME_DARK);
        } else {
            setThemeMode(THEME_LIGHT);
        }
    }
}
