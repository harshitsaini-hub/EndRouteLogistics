package com.endfielders.erl.util;

import com.endfielders.erl.model.Carrier;

public class ScoringEngine {

    public static double calculateScore(Carrier c,
                                        String cargoType,
                                        String priority,
                                        boolean fragile,
                                        boolean perishable,
                                        String weather) {

        double costScore = normalizeInverse(c.getCostPerKg(), 20, 120);
        double speedScore = normalizeInverse(c.getEstimatedDays(), 2, 7);
        double modeScore = getModeScore(c, cargoType);

        double riskPenalty = calculateRiskPenalty(c, fragile, perishable, weather);

        double costWeight = 0.4;
        double speedWeight = 0.4;

        // 🎯 Dynamic weighting (THIS is what makes you stand out)
        if ("FASTEST".equalsIgnoreCase(priority)) {
            speedWeight = 0.6;
            costWeight = 0.2;
        } else if ("CHEAPEST".equalsIgnoreCase(priority)) {
            costWeight = 0.6;
            speedWeight = 0.2;
        }

        double finalScore = (costScore * costWeight)
                + (speedScore * speedWeight)
                + (modeScore * 0.2)
                - riskPenalty;

        return Math.max(0, Math.round(finalScore * 100.0) / 100.0);
    }

    private static double normalizeInverse(double value, double min, double max) {
        if (value <= min) return 100;
        if (value >= max) return 0;
        return ((max - value) / (max - min)) * 100;
    }

    private static double getModeScore(Carrier c, String cargoType) {
        if (cargoType == null) return 60;

        if ("Electronics".equalsIgnoreCase(cargoType)) {
            return "Air".equalsIgnoreCase(c.getMode()) ? 90 : 65;
        }

        if ("Pharma".equalsIgnoreCase(cargoType) || "Documents".equalsIgnoreCase(cargoType)) {
            return "Air".equalsIgnoreCase(c.getMode()) ? 85 : 70;
        }

        if ("Bulk".equalsIgnoreCase(cargoType) || "Heavy".equalsIgnoreCase(cargoType)) {
            return "Road".equalsIgnoreCase(c.getMode()) ? 85 : 60;
        }

        return 65;
    }

    private static double calculateRiskPenalty(Carrier c,
                                               boolean fragile,
                                               boolean perishable,
                                               String weather) {

        double penalty = 0;

        if (fragile && !"Air".equalsIgnoreCase(c.getMode())) {
            penalty += 10;
        }

        if (perishable && c.getEstimatedDays() > 3) {
            penalty += 15;
        }

        if (weather != null && weather.toLowerCase().contains("rain")) {
            if ("Road".equalsIgnoreCase(c.getMode())) {
                penalty += 10;
            }
        }

        return penalty;
    }

    public static String assignGrade(double score) {
        if (score >= 80) return "A";
        if (score >= 65) return "B";
        if (score >= 50) return "C";
        if (score >= 35) return "D";
        return "F";
    }
}