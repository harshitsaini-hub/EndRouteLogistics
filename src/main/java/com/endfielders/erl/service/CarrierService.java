package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.util.ScoringEngine;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CarrierService {
    private static final String DEFAULT_AI_INSIGHT = "No AI insight available.";
    private static final String DEFAULT_CARGO_TYPE = "General goods";

    private final GeminiService geminiService;

    private static final List<Carrier> CARRIERS = List.of(
            create("BlueDart", "Air", 2, 120, "https://www.bluedart.com"),
            create("Delhivery", "Road", 4, 45, "https://www.delhivery.com"),
            create("DTDC", "Road", 5, 35, "https://www.dtdc.in"),
            create("Ecom Express", "Road", 3, 55, "https://www.ecomexpress.in"),
            create("India Post", "Rail", 7, 20, "https://www.indiapost.gov.in")
    );

    public CarrierService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    private List<Carrier> getCarriers() {
        return CARRIERS;
    }

    private static Carrier create(String name, String mode, int days, double cost, String website) {
        Carrier c = new Carrier();
        c.setName(name);
        c.setMode(mode);
        c.setEstimatedDays(days);
        c.setCostPerKg(cost);
        c.setWebsite(website);
        return c;
    }

public List<RankedCarrier> getRankedCarriers(
        String origin,
        String destination,
        String cargoType,
        String priority,
        boolean fragile,
        boolean perishable) {

    String safeCargoType = (cargoType == null || cargoType.isBlank())
            ? DEFAULT_CARGO_TYPE
            : cargoType.trim();

    List<Carrier> carriers = getCarriers();
    List<RankedCarrier> rankedList = new ArrayList<>();
    String weatherSummary = geminiService.buildWeatherSummary(origin, destination);

    for (Carrier c : carriers) {

        RankedCarrier rc = new RankedCarrier();

        rc.setName(c.getName());
        rc.setMode(c.getMode());
        rc.setEstimatedDays(c.getEstimatedDays());
        rc.setCostPerKg(c.getCostPerKg());
        rc.setWebsite(c.getWebsite());

        double score = ScoringEngine.calculateScore(
                c, safeCargoType, priority, fragile, perishable, weatherSummary);

        rc.setScore(score);
        rc.setRiskScore((int) (100 - score));
        rc.setGrade(ScoringEngine.assignGrade(score));

        rc.setAiInsight(DEFAULT_AI_INSIGHT);
        rc.setAiReasons(new ArrayList<>());

        rankedList.add(rc);
    }

    rankedList.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

    for (int i = 0; i < Math.min(3, rankedList.size()); i++) {

        RankedCarrier rc = rankedList.get(i);

        String insight = geminiService.callGemini(
                "You are a logistics ranking engine.\n" +
                "Explain why this carrier is a strong choice.\n\n" +

                "Carrier: " + rc.getName() + "\n" +
                "Mode: " + rc.getMode() + "\n" +
                "Delivery Time: " + rc.getEstimatedDays() + " days\n" +
                "Cargo: " + safeCargoType + "\n\n" +

                "Rules:\n" +
                "- 1 short sentence only\n" +
                "- No formatting, no symbols\n" +
                "- Be specific (speed, cost, reliability)\n\n" +

                "Output:"
        );

        if (insight == null || insight.isBlank() || insight.toLowerCase().contains("unavailable")) {
            insight = "Reliable option based on delivery time and cost balance.";
        }

        rc.setAiInsight(insight.trim());

        String reasonsRaw = geminiService.callGemini(
                "You are a logistics decision engine.\n" +
                "Give exactly 3 short reasons why this carrier is a good choice.\n\n" +

                "Carrier: " + rc.getName() + "\n" +
                "Mode: " + rc.getMode() + "\n" +
                "Delivery Time: " + rc.getEstimatedDays() + " days\n" +
                "Cost per Kg: " + rc.getCostPerKg() + "\n" +
                "Cargo: " + safeCargoType + "\n\n" +

                "Rules:\n" +
                "- Exactly 3 lines\n" +
                "- Each line max 5 words\n" +
                "- No symbols, no numbering\n" +
                "- Plain text only\n\n" +

                "Output:"
        );

        

        List<String> reasons = new ArrayList<>();

        if (reasonsRaw != null && !reasonsRaw.isBlank() && !reasonsRaw.toLowerCase().contains("unavailable")) {
            reasons = Arrays.stream(reasonsRaw.split("\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(3)
                    .collect(java.util.stream.Collectors.toList());
        }

        if (reasons.isEmpty()) {
            reasons = List.of(
                    "Balanced performance",
                    "Reliable delivery",
                    "Standard pricing"
            );
        }

        rc.setAiReasons(reasons);


        int confidence = calculateConfidence(rc.getScore(), rc.getRiskScore(), weatherSummary);
        rc.setConfidenceScore(confidence);

        String explanation = geminiService.callGemini(
                "You are a logistics intelligence system.\n" +
                "Explain WHY this carrier received this ranking.\n\n" +

                "Carrier: " + rc.getName() + "\n" +
                "Score: " + rc.getScore() + "\n" +
                "Risk Score: " + rc.getRiskScore() + "\n" +
                "Delivery Time: " + rc.getEstimatedDays() + "\n" +
                "Weather: " + weatherSummary + "\n\n" +

                "Rules:\n" +
                "- 1 sentence only\n" +
                "- No formatting\n" +
                "- Focus on decision reasoning\n\n" +

                "Output:"
        );

        if (explanation == null || explanation.isBlank() || explanation.toLowerCase().contains("unavailable")) {
            explanation = "Ranked based on optimal balance of cost, delivery speed, and risk factors.";
        }

        rc.setExplanation(explanation);
            }

    return rankedList;
}
    private int calculateConfidence(double score, int riskScore, String weatherSummary) {

        int base = (int) score;

        if (riskScore > 50) base -= 10;
        if (riskScore > 70) base -= 15;

        if (weatherSummary != null) {
            String weather = weatherSummary.toLowerCase();

            if (weather.contains("rain") || weather.contains("storm")) {
                base -= 10;
            } else if (weather.contains("clear") || weather.contains("sun")) {
                base += 5;
            }
        }

        return Math.max(0, Math.min(100, base));
    }
}