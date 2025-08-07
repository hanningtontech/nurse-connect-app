package com.example.nurse_connect;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.nurse_connect.utils.ThemeManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ThemeManager
 */
@RunWith(RobolectricTestRunner.class)
public class ThemeManagerTest {

    private ThemeManager themeManager;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        themeManager = ThemeManager.getInstance(context);
    }

    @Test
    public void testGetInstance_ReturnsSameInstance() {
        ThemeManager instance1 = ThemeManager.getInstance(context);
        ThemeManager instance2 = ThemeManager.getInstance(context);
        
        assertSame("getInstance should return the same instance", instance1, instance2);
    }

    @Test
    public void testDefaultThemeMode() {
        // Clear any existing preferences
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .edit().clear().commit();
        
        ThemeManager freshInstance = ThemeManager.getInstance(context);
        assertEquals("Default theme should be THEME_SYSTEM", 
                ThemeManager.THEME_SYSTEM, freshInstance.getThemeMode());
    }

    @Test
    public void testSetThemeMode() {
        themeManager.setThemeMode(ThemeManager.THEME_DARK);
        assertEquals("Theme mode should be set to THEME_DARK", 
                ThemeManager.THEME_DARK, themeManager.getThemeMode());
        
        themeManager.setThemeMode(ThemeManager.THEME_LIGHT);
        assertEquals("Theme mode should be set to THEME_LIGHT", 
                ThemeManager.THEME_LIGHT, themeManager.getThemeMode());
    }

    @Test
    public void testGetThemeModeDisplayName() {
        assertEquals("Light", themeManager.getThemeModeDisplayName(ThemeManager.THEME_LIGHT));
        assertEquals("Dark", themeManager.getThemeModeDisplayName(ThemeManager.THEME_DARK));
        assertEquals("System Default", themeManager.getThemeModeDisplayName(ThemeManager.THEME_SYSTEM));
    }

    @Test
    public void testToggleTheme() {
        // Start with light theme
        themeManager.setThemeMode(ThemeManager.THEME_LIGHT);
        themeManager.toggleTheme();
        assertEquals("Toggle from light should go to dark", 
                ThemeManager.THEME_DARK, themeManager.getThemeMode());
        
        // Toggle again
        themeManager.toggleTheme();
        assertEquals("Toggle from dark should go to light", 
                ThemeManager.THEME_LIGHT, themeManager.getThemeMode());
        
        // Test from system theme
        themeManager.setThemeMode(ThemeManager.THEME_SYSTEM);
        themeManager.toggleTheme();
        assertEquals("Toggle from system should go to light", 
                ThemeManager.THEME_LIGHT, themeManager.getThemeMode());
    }
}
