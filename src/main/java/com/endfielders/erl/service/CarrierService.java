package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.repository.CarrierRepository;
import com.endfielders.erl.util.ScoringEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CarrierService {
    private static final String DEFAULT_AI_INSIGHT = "AI suggests this carrier based on cost, speed, and reliability.";
    private static final String DEFAULT_CARGO_TYPE = "General goods";

    private final GeminiService geminiService;
    private final CarrierRepository carrierRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public CarrierService(GeminiService geminiService, CarrierRepository carrierRepository) {
        this.geminiService = geminiService;
        this.carrierRepository = carrierRepository;
    }

    public List<RankedCarrier> getRankedCarriers(
            String origin, String destination, String cargoType,
            String priority, boolean fragile, boolean perishable) {

        String safeCargoType = (cargoType == null || cargoType.trim().isEmpty()) ? DEFAULT_CARGO_TYPE : cargoType.trim();
        String cargo = safeCargoType.toLowerCase();
        
        boolean fragileCargo = cargo.contains("glass") || cargo.contains("electronics") || fragile;
        boolean perishableCargo = cargo.contains("food") || cargo.contains("fish") || cargo.contains("meat") || perishable;

        // Fetch weather ONCE and cache it
        String weatherSummary = geminiService.buildWeatherSummary(origin, destination);

        List<Carrier> carriers = carrierRepository.findByActiveStatusTrue();
        List<RankedCarrier> rankedList = new ArrayList<>();

        for (Carrier c : carriers) {
            RankedCarrier rc = new RankedCarrier();
            rc.setName(c.getName());
            rc.setMode(c.getMode());
            rc.setEstimatedDays(c.getEstimatedDays());
            rc.setCostPerKg(c.getCostPerKg());
            rc.setWebsite(c.getWebsite());

            double score = ScoringEngine.calculateScore(
                    c, safeCargoType, priority, fragileCargo, perishableCargo, weatherSummary);
            
            boolean isLocal = origin != null && destination != null && origin.equals(destination);
            if (isLocal) {
                if (c.getMode().equalsIgnoreCase("Road")) {
                    score += 40;
                    rc.setEstimatedDays(1);
                } else if (c.getMode().equalsIgnoreCase("Air") || c.getMode().equalsIgnoreCase("Rail")) {
                    score -= 50;
                }
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

        // Run BOTH Gemini calls in PARALLEL using the cached weather
        CompletableFuture<String> routeInsightFuture = CompletableFuture.supplyAsync(() ->
            geminiService.analyzeRouteWithWeather(origin, destination, safeCargoType, weatherSummary)
        );

        CompletableFuture<Void> carrierInsightFuture = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < Math.min(1, rankedList.size()); i++) {
                RankedCarrier rc = rankedList.get(i);
                String safeWeather = (weatherSummary == null) ? "clear" : weatherSummary;

                String prompt = """
                You are a logistics AI.
                Your task is to analyze ONE carrier and return a JSON response.
                IMPORTANT RULES:
                - Return ONLY JSON
                - No markdown
                - No explanation outside JSON
                FORMAT:
                {
                "insight": "1 short sentence why this carrier is suitable",
                "reasons": ["reason 1", "reason 2", "reason 3"],
                "explanation": "1 short sentence why this carrier ranked high"
                }
                INPUT:
                Carrier: %s
                Mode: %s
                Delivery Time: %d days
                Cost: %.2f
                Cargo: %s
                Weather: %s
                """.formatted(rc.getName(), rc.getMode(), rc.getEstimatedDays(), rc.getCostPerKg(), safeCargoType, safeWeather);

                String aiResponseRaw = geminiService.callGemini(prompt);

                try {
                    if (aiResponseRaw != null && !aiResponseRaw.trim().isEmpty()) {
                        String cleanJson = aiResponseRaw.replace("```json", "").replace("```", "").trim();
                        int start = cleanJson.indexOf("{");
                        int end = cleanJson.lastIndexOf("}");

                        if (start != -1 && end != -1) {
                            cleanJson = cleanJson.substring(start, end + 1);
                        }
                        Map<?, ?> map = mapper.readValue(cleanJson, Map.class);

                        if (map.containsKey("insight")) rc.setAiInsight(map.get("insight").toString());
                        if (map.containsKey("explanation")) rc.setExplanation(map.get("explanation").toString());
                        if (map.containsKey("reasons") && map.get("reasons") instanceof List<?>) {
                            List<String> parsedReasons = ((List<?>) map.get("reasons")).stream()
                                    .map(Object::toString).limit(3).collect(Collectors.toList());
                            if (!parsedReasons.isEmpty()) rc.setAiReasons(parsedReasons);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[WARN] AI Parse Issue, falling back to defaults.");
                }
            }
        });

        // Wait for both to complete
        CompletableFuture.allOf(routeInsightFuture, carrierInsightFuture).join();

        // Store the route insight on the first carrier (or it can be retrieved separately)
        cachedRouteInsight = routeInsightFuture.join();

        return rankedList;
    }

    // Cached route insight from the last analysis (avoids re-calling from controller)
    private volatile String cachedRouteInsight;

    public String getLastRouteInsight() {
        return cachedRouteInsight;
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