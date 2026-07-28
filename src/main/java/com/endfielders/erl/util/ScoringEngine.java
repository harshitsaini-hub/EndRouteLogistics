package com.endfielders.erl.util;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.DayWeather;

import java.util.List;
import java.util.Map;

public class ScoringEngine {

    /**
     * Overloaded calculateScore supporting day-wise forecast timeline penalties.
     */
    public static double calculateScoreWithTimeline(Carrier c,
                                                    String cargoType,
                                                    String priority,
                                                    boolean fragile,
                                                    boolean perishable,
                                                    List<DayWeather> timeline,
                                                    double avgCost,
                                                    double avgSpeed,
                                                    Map<String, Integer> cargoSuitability) {

        double costScore = normalizeRelative(c.getCostPerKg(), avgCost);
        double speedScore = normalizeRelative(c.getEstimatedDays(), avgSpeed);
        double modeScore = cargoSuitability != null ? cargoSuitability.getOrDefault(c.getMode(), 65) : 65;
        double reliabilityScore = c.getReliabilityScore();

        double riskPenalty = calculateTimelineRiskPenalty(c, fragile, perishable, timeline);

        double costWeight = 0.3;
        double speedWeight = 0.3;
        double reliabilityWeight = 0.2;
        double modeWeight = 0.2;

        if ("FASTEST".equalsIgnoreCase(priority)) {
            speedWeight = 0.5;
            costWeight = 0.1;
        } else if ("CHEAPEST".equalsIgnoreCase(priority)) {
            costWeight = 0.5;
            speedWeight = 0.1;
        }

        double finalScore = (costScore * costWeight)
                + (speedScore * speedWeight)
                + (reliabilityScore * reliabilityWeight)
                + (modeScore * modeWeight)
                - riskPenalty;

        return Math.max(0, Math.round(finalScore * 100.0) / 100.0);
    }

    private static double normalizeRelative(double value, double average) {
        if (average == 0) return 50;
        double ratio = value / average;
        double score = 100 - ((ratio - 0.5) * 50);
        return Math.max(0, Math.min(100, score));
    }

    private static double calculateTimelineRiskPenalty(Carrier c,
                                                       boolean fragile,
                                                       boolean perishable,
                                                       List<DayWeather> timeline) {
        double penalty = 0;

        if (fragile && !"Air".equalsIgnoreCase(c.getMode())) {
            penalty += 10;
        }

        if (perishable && c.getEstimatedDays() > 3) {
            penalty += 15;
        }

        if (timeline != null) {
            for (DayWeather dw : timeline) {
                if (!dw.isForecastAvailable()) continue;
                String cond = dw.getCondition() != null ? dw.getCondition().toLowerCase() : "";
                Double temp = dw.getTemperature();

                // Road transport rain penalty
                if ("Road".equalsIgnoreCase(c.getMode()) && (cond.contains("rain") || cond.contains("drizzle"))) {
                    penalty += 4; // Max ~12-16 across multiple days
                }

                // Air transport storm penalty
                if ("Air".equalsIgnoreCase(c.getMode()) && (cond.contains("thunderstorm") || cond.contains("squall"))) {
                    penalty += 8;
                }

                // Heat sensitive perishable penalty
                if (perishable && temp != null && temp > 35) {
                    penalty += 5;
                }
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