package com.example.nurse_connect.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandleUtils {
    
    // Pattern to match @username in text
    private static final Pattern HANDLE_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");
    
    // Pattern to validate handle format
    private static final Pattern VALID_HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    /**
     * Extract handles from text
     * @param text The text to search for handles
     * @return Array of handles found (without @ symbol)
     */
    public static String[] extractHandles(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        
        Matcher matcher = HANDLE_PATTERN.matcher(text);
        java.util.List<String> handles = new java.util.ArrayList<>();
        
        while (matcher.find()) {
            handles.add(matcher.group(1));
        }
        
        return handles.toArray(new String[0]);
    }
    
    /**
     * Check if a handle is valid
     * @param handle The handle to validate (without @ symbol)
     * @return true if valid, false otherwise
     */
    public static boolean isValidHandle(String handle) {
        if (handle == null || handle.isEmpty()) {
            return false;
        }
        
        return VALID_HANDLE_PATTERN.matcher(handle).matches();
    }
    
    /**
     * Generate a handle from username
     * @param username The username to convert to handle
     * @return A valid handle
     */
    public static String generateHandle(String username) {
        if (username == null || username.isEmpty()) {
            return "";
        }
        
        // Remove special characters and convert to lowercase
        String handle = username.toLowerCase()
                .replaceAll("[^a-zA-Z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        // Ensure minimum length
        if (handle.length() < 3) {
            handle = handle + "user";
        }
        
        // Ensure maximum length
        if (handle.length() > 20) {
            handle = handle.substring(0, 20);
        }
        
        return handle;
    }
    
    /**
     * Generate a unique handle with number suffix
     * @param baseHandle The base handle
     * @param existingHandles Array of existing handles
     * @return A unique handle
     */
    public static String generateUniqueHandle(String baseHandle, String[] existingHandles) {
        String handle = baseHandle;
        int suffix = 1;
        
        while (containsHandle(handle, existingHandles)) {
            String suffixStr = String.valueOf(suffix);
            if (handle.length() + suffixStr.length() > 20) {
                handle = handle.substring(0, 20 - suffixStr.length()) + suffixStr;
            } else {
                handle = baseHandle + suffixStr;
            }
            suffix++;
        }
        
        return handle;
    }
    
    /**
     * Check if handle exists in array
     * @param handle The handle to check
     * @param existingHandles Array of existing handles
     * @return true if exists, false otherwise
     */
    private static boolean containsHandle(String handle, String[] existingHandles) {
        if (existingHandles == null) {
            return false;
        }
        
        for (String existingHandle : existingHandles) {
            if (handle.equalsIgnoreCase(existingHandle)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Format handle for display (add @ symbol)
     * @param handle The handle to format
     * @return Formatted handle with @ symbol
     */
    public static String formatHandle(String handle) {
        if (handle == null || handle.isEmpty()) {
            return "";
        }
        
        return "@" + handle;
    }
    
    /**
     * Remove @ symbol from handle
     * @param formattedHandle The handle with @ symbol
     * @return Handle without @ symbol
     */
    public static String unformatHandle(String formattedHandle) {
        if (formattedHandle == null || formattedHandle.isEmpty()) {
            return "";
        }
        
        if (formattedHandle.startsWith("@")) {
            return formattedHandle.substring(1);
        }
        
        return formattedHandle;
    }
} 