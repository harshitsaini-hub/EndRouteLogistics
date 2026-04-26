package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.util.ScoringEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarrierService {
    private static final String DEFAULT_AI_INSIGHT = "No AI insight available.";
    private static final String DEFAULT_CARGO_TYPE = "General goods";

    private final GeminiService geminiService;
    private final ObjectMapper mapper = new ObjectMapper(); // Reused for performance

    private static final List<Carrier> CARRIERS = Arrays.asList(
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

        String safeCargoType = (cargoType == null || cargoType.trim().isEmpty())
                ? DEFAULT_CARGO_TYPE
                : cargoType.trim();
        
        String cargo = safeCargoType.toLowerCase();
        boolean heavy = cargo.contains("vehicle") || cargo.contains("machinery");
        boolean fragileCargo = cargo.contains("glass") || cargo.contains("electronics") || fragile;
        boolean perishableCargo = cargo.contains("food") || cargo.contains("fish") || cargo.contains("meat") || perishable;

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
                    c, safeCargoType, priority, fragileCargo, perishableCargo, weatherSummary);

            if (weatherSummary != null) {
                String weather = weatherSummary.toLowerCase();
                if (weather.contains("rain") || weather.contains("storm")) {
                    if (c.getMode().equalsIgnoreCase("Road")) score -= 10;
                    if (c.getMode().equalsIgnoreCase("Air")) score -= 5;
                }
                if (weather.contains("fog")) {
                    if (c.getMode().equalsIgnoreCase("Air")) score -= 15;
                }
                if (weather.contains("heat")) {
                    if (perishableCargo) score -= 10;
                }
            }
            
            if (heavy) {
                if (c.getMode().equalsIgnoreCase("Road")) score += 15;
                if (c.getMode().equalsIgnoreCase("Air")) score += 5;
            }
            if (fragileCargo) {
                if (c.getMode().equalsIgnoreCase("Air")) score += 15;
            }
            if (perishableCargo) {
                if (c.getMode().equalsIgnoreCase("Air")) score += 20;
                if (c.getMode().equalsIgnoreCase("Rail")) score -= 10;
            }

            score = Math.max(0, Math.min(100, score));
            rc.setScore(score);
            rc.setRiskScore((int) Math.max(0, 100 - score));
            rc.setGrade(ScoringEngine.assignGrade(score));
            
            rc.setAiInsight(DEFAULT_AI_INSIGHT);
            rc.setAiReasons(Arrays.asList("Balanced performance", "Reliable delivery", "Standard pricing"));
            rc.setExplanation("Ranked based on optimal balance of cost, delivery speed, and risk factors.");
            
            int confidence = calculateConfidence(rc.getScore(), rc.getRiskScore(), weatherSummary, rc);
            rc.setConfidenceScore(confidence);

            rankedList.add(rc);
        }

        rankedList.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        for (int i = 0; i < Math.min(1, rankedList.size()); i++) {
            RankedCarrier rc = rankedList.get(i);

            String aiResponseRaw = geminiService.callGemini(
                "You are a logistics AI.\n" +
                "Return STRICT JSON only. Do not use markdown code blocks.\n\n" +
                "{\n" +
                "  \"insight\": \"1 sentence explanation why this is suitable for " + safeCargoType + "\",\n" +
                "  \"reasons\": [\"short reason 1\", \"short reason 2\", \"short reason 3\"],\n" +
                "  \"explanation\": \"1 sentence explaining why this got the top rank\"\n" +
                "}\n\n" +
                "Carrier: " + rc.getName() + "\n" +
                "Mode: " + rc.getMode() + "\n" +
                "Delivery Time: " + rc.getEstimatedDays() + " days\n" +
                "Cost: " + rc.getCostPerKg() + "\n" +
                "Cargo: " + safeCargoType + "\n" +
                "Weather: " + weatherSummary
            );

            try {
                if (aiResponseRaw != null && !aiResponseRaw.trim().isEmpty()) {

                    String cleanJson = aiResponseRaw.replace("```json", "").replace("```", "").trim();
                    Map<?, ?> map = mapper.readValue(cleanJson, Map.class);

                    if (map.containsKey("insight")) {
                        rc.setAiInsight(map.get("insight").toString());
                    }
                    if (map.containsKey("reasons") && map.get("reasons") instanceof List<?>) {
                        List<String> parsedReasons = ((List<?>) map.get("reasons")).stream()
                                .map(Object::toString)
                                .limit(3)
                                .collect(Collectors.toList());
                        if (!parsedReasons.isEmpty()) {
                            rc.setAiReasons(parsedReasons);
                        }
                    }
                    if (map.containsKey("explanation")) {
                        rc.setExplanation(map.get("explanation").toString());
                    }
                }
            } catch (Exception ignored) {

            }
        }

        return rankedList;
    }

    private int calculateConfidence(double score, int riskScore, String weatherSummary, RankedCarrier rc) {
        int base = (int) score;

        if (riskScore > 50) base -= 10;
        if (riskScore > 70) base -= 15;

        if (rc.getEstimatedDays() <= 2) base += 5;
        if (rc.getCostPerKg() > 100) base -= 5;

        if (weatherSummary != null) {
            String weather = weatherSummary.toLowerCase();
            if (weather.contains("storm") || weather.contains("rain")) base -= 10;
            if (weather.contains("clear")) base += 5;
        }

        return Math.max(0, Math.min(100, base));
    }
}