package com.endfielders.erl.util;

import com.endfielders.erl.model.Carrier;

public class ScoringEngine {

    public static double calculateScore(Carrier c, String cargoType) {
        double normalizedCostScore = normalizeInverse(c.getCostPerKg(), 20, 120);
        double normalizedSpeedScore = normalizeInverse(c.getEstimatedDays(), 2, 7);
        double modeScore = getModeScore(c, cargoType);

        double costWeight = 0.40;
        double speedWeight = 0.40;
        double modeWeight = 0.20;

        double finalScore = (normalizedCostScore * costWeight)
                + (normalizedSpeedScore * speedWeight)
                + (modeScore * modeWeight);

        return Math.round(finalScore * 100.0) / 100.0;
    }

    private static double normalizeInverse(double value, double min, double max) {
        if (value <= min) {
            return 100;
        }
        if (value >= max) {
            return 0;
        }
        return ((max - value) / (max - min)) * 100;
    }

    private static double getModeScore(Carrier c, String cargoType) {
        if (cargoType == null) {
            return 60;
        }

        if ("Electronics".equalsIgnoreCase(cargoType)) {
            if ("Air".equalsIgnoreCase(c.getMode())) {
                return 90;
            }
            if ("Road".equalsIgnoreCase(c.getMode())) {
                return 70;
            }
            return 55;
        }

        if ("Documents".equalsIgnoreCase(cargoType) || "Pharma".equalsIgnoreCase(cargoType)) {
            return "Air".equalsIgnoreCase(c.getMode()) ? 85 : 65;
        }

        if ("Bulk".equalsIgnoreCase(cargoType) || "Heavy".equalsIgnoreCase(cargoType)) {
   
        }

        return 65;
    }

    public static String assignGrade(double score) {
        if (score >= 80) return "A";
        if (score >= 65) return "B";
        if (score >= 50) return "C";
        if (score >= 35) return "D";
        return "F";
        }

}
