package com.example.nurse_connect.models;

public class OnboardingItem {
    private int imageRes;
    private String title;
    private String description;
    private String buttonText;
    
    public OnboardingItem(int imageRes, String title, String description, String buttonText) {
        this.imageRes = imageRes;
        this.title = title;
        this.description = description;
        this.buttonText = buttonText;
    }
    
    // Getters
    public int getImageRes() { return imageRes; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getButtonText() { return buttonText; }
} 