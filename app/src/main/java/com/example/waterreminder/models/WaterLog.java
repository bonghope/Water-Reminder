package com.example.waterreminder.models;

public class WaterLog {
    private int id;
    private int categoryId; // 1: Nước, 2: Trà/Cafe, 3: Nước ngọt, 4: Sữa
    private int rawAmount;
    private int hydrationAmount;
    private long timestamp;

    public WaterLog(int categoryId, int rawAmount, int hydrationAmount, long timestamp) {
        this.categoryId = categoryId;
        this.rawAmount = rawAmount;
        this.hydrationAmount = hydrationAmount;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public int getRawAmount() { return rawAmount; }
    public int getHydrationAmount() { return hydrationAmount; }
    public long getTimestamp() { return timestamp; }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public void setHydrationAmount(int hydrationAmount) {
        this.hydrationAmount = hydrationAmount;
    }
    public void setRawAmount(int rawAmount) {
        this.rawAmount = rawAmount;
    }
}