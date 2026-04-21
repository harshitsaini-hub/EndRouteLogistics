package com.endfielders.erl.model;

public class Carrier {
    private String name;
    private String mode;
    private int estimatedDays;
    private double costPerKg;
    private String website;

    // Proper Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(int estimatedDays) { this.estimatedDays = estimatedDays; }

    public double getCostPerKg() { return costPerKg; }
    public void setCostPerKg(double costPerKg) { this.costPerKg = costPerKg; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}