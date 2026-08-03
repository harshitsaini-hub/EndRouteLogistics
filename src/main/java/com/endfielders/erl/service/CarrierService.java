package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.DayWeather;
import com.endfielders.erl.model.JourneyTimeline;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.model.RouteStop;
import com.endfielders.erl.repository.CarrierRepository;
import com.endfielders.erl.util.CategoryResolver;
import com.endfielders.erl.util.JsonUtil;
import com.endfielders.erl.util.ScoringEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * Main business service for ranking carriers and generating AI insights.
 * Implements Category-Aware Two-Phase Parallel Processing:
 *   Phase 0: Resolve cargo type → carrier category (instant or AI-based).
 *   Phase 1: Parallel route stop estimation & weather summary fetch.
 *   Phase 2: Deduplicated batch weather forecast & final AI enrichment.
 */
@Service
public class CarrierService {

    private static final String DEFAULT_AI_INSIGHT = "AI suggests this carrier based on cost, speed, and reliability.";
    private static final String DEFAULT_CARGO_TYPE = "General goods";

    private final GeminiService geminiService;
    private final WeatherService weatherService;
    private final RouteEstimationService routeEstimationService;
    private final CarrierRepository carrierRepository;
    private final CityDataService cityDataService;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String cachedRouteInsight;

    public CarrierService(GeminiService geminiService,
                          WeatherService weatherService,
                          RouteEstimationService routeEstimationService,
                          CarrierRepository carrierRepository,
                          CityDataService cityDataService) {
        this.geminiService = geminiService;
        this.weatherService = weatherService;
        this.routeEstimationService = routeEstimationService;
        this.carrierRepository = carrierRepository;
        this.cityDataService = cityDataService;
    }

    @SuppressWarnings("null")
    public List<RankedCarrier> getRankedCarriers(
            String origin, String destination, String cargoType,
            String priority, boolean fragile, boolean perishable) {

        String safeCargoType = (cargoType == null || cargoType.trim().isEmpty()) ? DEFAULT_CARGO_TYPE : cargoType.trim();
        String cargo = safeCargoType.toLowerCase();

        boolean fragileCargo = cargo.contains("glass") || cargo.contains("electronics") || fragile;
        boolean perishableCargo = cargo.contains("food") || cargo.contains("fish") || cargo.contains("meat") || perishable;

        // -------------------------------------------------------------
        // PHASE 0: Resolve cargo type → carrier category
        // -------------------------------------------------------------
        String resolvedCategory = CategoryResolver.resolve(safeCargoType);
        if (resolvedCategory == null) {
            // Unknown/custom cargo — ask Gemini to classify it
            resolvedCategory = geminiService.resolveCargoCategory(safeCargoType);
            System.out.println("[INFO] AI resolved '" + safeCargoType + "' → " + resolvedCategory);
        } else {
            System.out.println("[INFO] Direct resolved '" + safeCargoType + "' → " + resolvedCategory);
        }

        // Fetch carriers for the resolved category + GENERAL all-rounders
        List<String> categories = new ArrayList<>();
        categories.add(resolvedCategory);
        if (!"GENERAL".equals(resolvedCategory)) {
            categories.add("GENERAL");
        }
        List<Carrier> carriers = carrierRepository.findByCategoryInAndActiveStatusTrue(categories);

        System.out.println("[INFO] Fetched " + carriers.size() + " carriers for categories: " + categories);

        // -------------------------------------------------------------
        // PHASE 1: Parallel Route Estimation & Weather Summary Fetch
        // -------------------------------------------------------------

        // 1. Fetch basic weather summary in parallel (for route insight prompt)
        CompletableFuture<String> weatherSummaryFuture = CompletableFuture.supplyAsync(() ->
                weatherService.buildWeatherSummary(origin, destination)
        );

        // 1b. Deterministic cargo mode suitability engine (0ms local lookup)
        Map<String, Integer> cargoSuitability = cityDataService.getCargoModeSuitability(safeCargoType);

        // 2. Estimate route stops for ALL carriers in parallel
        Map<Long, CompletableFuture<List<RouteStop>>> stopFuturesMap = new HashMap<>();
        for (Carrier c : carriers) {
            stopFuturesMap.put(c.getId(), CompletableFuture.supplyAsync(() ->
                    routeEstimationService.estimateRouteStops(origin, destination, c.getMode(), c.getEstimatedDays())
            ));
        }

        // Wait for all route estimations to complete
        CompletableFuture.allOf(stopFuturesMap.values().toArray(new CompletableFuture[0])).join();
        String weatherSummary = weatherSummaryFuture.join();

        // Collect estimated stops per carrier
        Map<Long, List<RouteStop>> carrierStopsMap = new HashMap<>();
        for (Map.Entry<Long, CompletableFuture<List<RouteStop>>> entry : stopFuturesMap.entrySet()) {
            carrierStopsMap.put(entry.getKey(), entry.getValue().join());
        }

        // -------------------------------------------------------------
        // PHASE 2: Deduplicated Weather Forecast Fetch & Timeline Attachment
        // -------------------------------------------------------------
        Map<String, DayWeather> sharedDedupCache = new ConcurrentHashMap<>();
        List<RankedCarrier> rankedList = new ArrayList<>();

        // Calculate averages for benchmarking
        double totalCost = 0;
        double totalSpeed = 0;
        for (Carrier c : carriers) {
            totalCost += c.getCostPerKg();
            totalSpeed += c.getEstimatedDays();
        }
        double avgCost = carriers.isEmpty() ? 50 : totalCost / carriers.size();
        double avgSpeed = carriers.isEmpty() ? 4 : totalSpeed / carriers.size();

        for (Carrier c : carriers) {
            RankedCarrier rc = new RankedCarrier();
            rc.setId(c.getId());
            rc.setName(c.getName());
            rc.setMode(c.getMode());
            rc.setEstimatedDays(c.getEstimatedDays());
            rc.setCostPerKg(c.getCostPerKg());
            rc.setWebsite(c.getWebsite());
            rc.setReliabilityScore(c.getReliabilityScore());
            rc.setActiveStatus(c.isActiveStatus());
            rc.setCategory(c.getCategory());

            // Fetch deduplicated weather forecasts for this carrier's route
            List<RouteStop> stops = carrierStopsMap.get(c.getId());
            List<DayWeather> dayWeathers = weatherService.batchFetchForecasts(stops, sharedDedupCache);
            rc.setTimeline(new JourneyTimeline(dayWeathers));

            // Score carrier taking into account full forecast timeline
            double score = ScoringEngine.calculateScoreWithTimeline(
                    c, safeCargoType, priority, fragileCargo, perishableCargo, dayWeathers, avgCost, avgSpeed, cargoSuitability);

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

        // -------------------------------------------------------------
        // Sort carriers according to user's selected priority
        // -------------------------------------------------------------
        if ("CHEAPEST".equalsIgnoreCase(priority)) {
            rankedList.sort((a, b) -> {
                int costComp = Double.compare(a.getCostPerKg(), b.getCostPerKg());
                if (costComp != 0) return costComp;
                return Double.compare(b.getScore(), a.getScore());
            });
        } else if ("FASTEST".equalsIgnoreCase(priority)) {
            rankedList.sort((a, b) -> {
                int daysComp = Integer.compare(a.getEstimatedDays(), b.getEstimatedDays());
                if (daysComp != 0) return daysComp;
                return Double.compare(b.getScore(), a.getScore());
            });
        } else {
            rankedList.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        }

        // -------------------------------------------------------------
        // PHASE 3: Parallel AI Insights & Carrier Analysis Generation
        // -------------------------------------------------------------

        // 1. Generate Overall Route Insight
        CompletableFuture<String> routeInsightFuture = CompletableFuture.supplyAsync(() ->
                geminiService.analyzeRouteWithWeather(origin, destination, safeCargoType, weatherSummary)
        );

        // 2. Generate Top Carrier Detailed AI Insight
        CompletableFuture<Void> topCarrierInsightFuture = CompletableFuture.runAsync(() -> {
            if (!rankedList.isEmpty()) {
                RankedCarrier rc = rankedList.get(0);
                String safeWeather = (weatherSummary == null) ? "clear" : weatherSummary;

                String prompt = """
                You are a logistics AI.
                Your task is to analyze ONE top carrier and return a JSON response.
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
                        String cleanJson = JsonUtil.extractJson(aiResponseRaw);
                        Map<?, ?> map = mapper.readValue(cleanJson, Map.class);

                        if (map.containsKey("insight")) rc.setAiInsight(map.get("insight").toString());
                        if (map.containsKey("explanation")) rc.setExplanation(map.get("explanation").toString());
                        if (map.containsKey("reasons") && map.get("reasons") instanceof List<?>) {
                            List<String> parsedReasons = ((List<?>) map.get("reasons")).stream()
                                    .filter(Objects::nonNull).map(Object::toString).limit(3).collect(Collectors.toList());
                            if (!parsedReasons.isEmpty()) rc.setAiReasons(parsedReasons);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[WARN] AI Parse Issue, falling back to defaults.");
                }
            }
        });

        CompletableFuture.allOf(routeInsightFuture, topCarrierInsightFuture).join();
        cachedRouteInsight = routeInsightFuture.join();

        return rankedList;
    }

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