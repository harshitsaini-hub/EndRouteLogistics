package com.endfielders.erl.model;

import jakarta.persistence.*;

@Entity
@Table(name = "carriers")
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private int estimatedDays;

    @Column(nullable = false)
    private double costPerKg;

    private String website;

    @Column(nullable = false)
    private String category = "GENERAL";

    @Column(nullable = false)
    private double reliabilityScore = 75.0;

    @Column(nullable = false)
    private boolean activeStatus = true;

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getReliabilityScore() { return reliabilityScore; }
    public void setReliabilityScore(double reliabilityScore) { this.reliabilityScore = reliabilityScore; }

    public boolean isActiveStatus() { return activeStatus; }
    public void setActiveStatus(boolean activeStatus) { this.activeStatus = activeStatus; }
}