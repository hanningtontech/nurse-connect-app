package com.example.nurse_connect.models;

public class RatingStats {
    private float averageRating;
    private int totalRatings;
    private int count1, count2, count3, count4, count5;
    
    public RatingStats() {
        this.averageRating = 0.0f;
        this.totalRatings = 0;
        this.count1 = 0;
        this.count2 = 0;
        this.count3 = 0;
        this.count4 = 0;
        this.count5 = 0;
    }
    
    public RatingStats(float averageRating, int totalRatings, int count1, int count2, int count3, int count4, int count5) {
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
        this.count1 = count1;
        this.count2 = count2;
        this.count3 = count3;
        this.count4 = count4;
        this.count5 = count5;
    }
    
    // Getters and Setters
    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }
    
    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
    
    public int getCount1() { return count1; }
    public void setCount1(int count1) { this.count1 = count1; }
    
    public int getCount2() { return count2; }
    public void setCount2(int count2) { this.count2 = count2; }
    
    public int getCount3() { return count3; }
    public void setCount3(int count3) { this.count3 = count3; }
    
    public int getCount4() { return count4; }
    public void setCount4(int count4) { this.count4 = count4; }
    
    public int getCount5() { return count5; }
    public void setCount5(int count5) { this.count5 = count5; }
    
    // Helper methods
    public void calculateAverage() {
        if (totalRatings > 0) {
            int totalStars = (count1 * 1) + (count2 * 2) + (count3 * 3) + (count4 * 4) + (count5 * 5);
            averageRating = (float) totalStars / totalRatings;
        } else {
            averageRating = 0.0f;
        }
    }
    
    public void addRating(int rating) {
        switch (rating) {
            case 1: count1++; break;
            case 2: count2++; break;
            case 3: count3++; break;
            case 4: count4++; break;
            case 5: count5++; break;
        }
        totalRatings++;
        calculateAverage();
    }
    
    public void removeRating(int rating) {
        switch (rating) {
            case 1: if (count1 > 0) count1--; break;
            case 2: if (count2 > 0) count2--; break;
            case 3: if (count3 > 0) count3--; break;
            case 4: if (count4 > 0) count4--; break;
            case 5: if (count5 > 0) count5--; break;
        }
        if (totalRatings > 0) totalRatings--;
        calculateAverage();
    }
} 