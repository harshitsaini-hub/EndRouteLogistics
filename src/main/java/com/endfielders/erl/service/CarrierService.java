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

        // STEP 1: Score carriers
        for (Carrier c : carriers) {

            RankedCarrier rc = new RankedCarrier();

            rc.setName(c.getName());
            rc.setMode(c.getMode());
            rc.setEstimatedDays(c.getEstimatedDays());
            rc.setCostPerKg(c.getCostPerKg());
            rc.setWebsite(c.getWebsite());

            double score = ScoringEngine.calculateScore(
                    c, cargoType, priority, fragile, perishable, weatherSummary);

            rc.setScore(score);
            rc.setRiskScore((int) (100 - score));
            rc.setGrade(ScoringEngine.assignGrade(score));

            rc.setAiInsight(DEFAULT_AI_INSIGHT);
            rc.setAiReasons(new ArrayList<>());

            rankedList.add(rc);
        }

        // STEP 2: Sort
        rankedList.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // STEP 3: Add AI only for top 3
        for (int i = 0; i < Math.min(3, rankedList.size()); i++) {

            RankedCarrier rc = rankedList.get(i);

            // ✅ AI Insight
            String insight = geminiService.callGeminiRaw(
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

            if (insight == null || insight.isBlank() || insight.startsWith("AI failed:")) {
                insight = DEFAULT_AI_INSIGHT;
            }

            rc.setAiInsight(insight.trim());

            // ✅ NEW: AI Reasons (list)
            String reasonsRaw = geminiService.callGeminiRaw(
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

            if (reasonsRaw != null && !reasonsRaw.isBlank() && !reasonsRaw.startsWith("AI failed")) {
                reasons = Arrays.stream(reasonsRaw.split("\\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(3)
                        .toList();
            }

            // fallback safety
            if (reasons.isEmpty()) {
                reasons = List.of(
                        "Balanced performance",
                        "Reliable delivery",
                        "Standard pricing"
                );
            }

            rc.setAiReasons(reasons);
        }

        return rankedList;
    }
}