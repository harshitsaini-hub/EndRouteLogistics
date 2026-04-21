package com.endfielders.erl.util;

import com.endfielders.erl.model.Carrier;

public class ScoringEngine {

    public static double calculateScore(Carrier c, String cargoType) {
        double costWeight = 0.4;
        double speedWeight = 0.4;
        double modeWeight = 0.2;

        double costScore = 1000 / (c.getCostPerKg() > 0 ? c.getCostPerKg() : 1);
        double speedScore = 100 / (c.getEstimatedDays() > 0 ? c.getEstimatedDays() : 1);
        
        double modeScore = 50;
        if ("Electronics".equalsIgnoreCase(cargoType) && "Air".equalsIgnoreCase(c.getMode())) {
            modeScore = 90; // High score for sensitive goods traveling fast and safe
        }

        return (costScore * costWeight) + (speedScore * speedWeight) + (modeScore * modeWeight);
    }

    public static String assignGrade(double score) {
        if (score >= 80) return "A";
        if (score >= 65) return "B";
        if (score >= 50) return "C";
        if (score >= 35) return "D";
        return "F";
    }
}