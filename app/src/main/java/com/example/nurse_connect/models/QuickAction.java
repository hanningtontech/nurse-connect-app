package com.example.nurse_connect.models;

public class QuickAction {
    private String id;
    private String title;
    private int iconResource;
    private String actionType;
    private Class<?> targetActivity;
    private boolean requiresAuth;

    public QuickAction() {}

    public QuickAction(String id, String title, int iconResource, String actionType, 
                      Class<?> targetActivity, boolean requiresAuth) {
        this.id = id;
        this.title = title;
        this.iconResource = iconResource;
        this.actionType = actionType;
        this.targetActivity = targetActivity;
        this.requiresAuth = requiresAuth;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getIconResource() { return iconResource; }
    public void setIconResource(int iconResource) { this.iconResource = iconResource; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Class<?> getTargetActivity() { return targetActivity; }
    public void setTargetActivity(Class<?> targetActivity) { this.targetActivity = targetActivity; }

    public boolean isRequiresAuth() { return requiresAuth; }
    public void setRequiresAuth(boolean requiresAuth) { this.requiresAuth = requiresAuth; }
}
