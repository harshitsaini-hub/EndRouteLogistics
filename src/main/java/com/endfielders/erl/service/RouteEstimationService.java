package com.endfielders.erl.service;

import com.endfielders.erl.model.RouteStop;
import com.endfielders.erl.util.JsonUtil;
import com.endfielders.erl.util.PincodeResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses Gemini AI with in-memory caching and PincodeResolver fallbacks to predict
 * realistic intermediate day-wise locations (city & pincode).
 */
@Service
public class RouteEstimationService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<RouteStop>> estimationCache = new ConcurrentHashMap<>();

    public RouteEstimationService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Estimates day-by-day route stops for a shipment.
     * Caches results per (origin, dest, mode, days) combination to eliminate duplicate AI calls.
     */
    public List<RouteStop> estimateRouteStops(String originPincode, String destPincode, String mode, int estimatedDays) {
        String safeOrigin = (originPincode == null) ? "110001" : originPincode.trim();
        String safeDest = (destPincode == null) ? "400001" : destPincode.trim();
        String safeMode = (mode == null) ? "Road" : mode.trim();

        if (estimatedDays <= 0) {
            String city = PincodeResolver.resolveCity(safeOrigin);
            return List.of(new RouteStop(0, city, safeOrigin));
        }

        String cacheKey = safeOrigin + ":" + safeDest + ":" + safeMode + ":" + estimatedDays;
        if (estimationCache.containsKey(cacheKey)) {
            return estimationCache.get(cacheKey);
        }

        List<RouteStop> stops = new ArrayList<>();

        // Fast path for local / same pincode shipping
        if (safeOrigin.equals(safeDest)) {
            String localCity = PincodeResolver.resolveCity(safeOrigin);
            stops.add(new RouteStop(0, localCity, safeOrigin));
            stops.add(new RouteStop(1, localCity, safeOrigin));
            estimationCache.put(cacheKey, stops);
            return stops;
        }

        // Prompt Gemini for realistic intermediate stops along the route
        String originCityName = PincodeResolver.resolveCity(safeOrigin);
        String destCityName = PincodeResolver.resolveCity(safeDest);

        String prompt = """
                You are an Indian logistics route predictor and geography expert.
                Estimate realistic day-by-day transit stops for a shipment.
                Origin: %s (Pincode: %s)
                Destination: %s (Pincode: %s)
                Mode of Transport: %s
                Total Transit Days: %d

                RULES:
                1. Return ONLY a valid JSON array of objects. No markdown, no commentary outside JSON.
                2. Each object must have:
                   - "day": integer (0 to %d)
                   - "city": string (Major Recognized City Name ONLY. E.g. "Delhi", "Jaipur", "Ahmedabad", "Mumbai").
                   - "pincode": string (6-digit Indian pincode)
                3. Day 0 MUST be %s (Pincode: %s).
                4. Day %d MUST be %s (Pincode: %s).
                5. For intermediate days (Day 1 to %d), estimate major logistics hub cities & valid 6-digit pincodes along the major %s route.

                EXAMPLE FORMAT:
                [
                  {"day": 0, "city": "%s", "pincode": "%s"},
                  {"day": %d, "city": "%s", "pincode": "%s"}
                ]
                """.formatted(
                originCityName, safeOrigin, destCityName, safeDest, safeMode, estimatedDays,
                estimatedDays,
                originCityName, safeOrigin,
                estimatedDays, destCityName, safeDest,
                estimatedDays - 1, safeMode,
                originCityName, safeOrigin, estimatedDays, destCityName, safeDest
        );

        String aiResponseRaw = geminiService.callGemini(prompt);

        if (aiResponseRaw != null && !aiResponseRaw.isBlank()) {
            try {
                String cleanJson = JsonUtil.extractJson(aiResponseRaw);
                if (cleanJson != null && !cleanJson.isBlank()) {
                    List<Map<String, Object>> parsed = objectMapper.readValue(cleanJson, new TypeReference<>() {});
                    for (Map<String, Object> item : parsed) {
                        int day = item.get("day") instanceof Number ? ((Number) item.get("day")).intValue() : 0;
                        String city = item.get("city") != null ? item.get("city").toString() : "Transit Hub";
                        String pincode = item.get("pincode") != null ? item.get("pincode").toString() : safeOrigin;
                        stops.add(new RouteStop(day, city, pincode));
                    }
                }
            } catch (Exception e) {
                System.out.println("[WARN] Gemini route estimation JSON parse failed: " + e.getMessage());
            }
        }

        // Fallback: If AI fails or returns incomplete list, generate realistic programmatic fallback
        if (stops.isEmpty() || stops.size() < estimatedDays + 1) {
            stops = generateFallbackStops(safeOrigin, safeDest, safeMode, estimatedDays);
        }

        stops.sort(Comparator.comparingInt(RouteStop::getDay));
        estimationCache.put(cacheKey, stops);
        return stops;
    }

    private List<RouteStop> generateFallbackStops(String origin, String dest, String mode, int days) {
        List<RouteStop> fallback = new ArrayList<>();
        String originCity = PincodeResolver.resolveCity(origin);
        String destCity = PincodeResolver.resolveCity(dest);

        fallback.add(new RouteStop(0, originCity, origin));

        int intermediateCount = Math.max(0, days - 1);
        List<String> hubCities = PincodeResolver.getIntermediateHubs(origin, dest, intermediateCount);

        for (int d = 1; d < days; d++) {
            String hubName = (d - 1 < hubCities.size()) ? hubCities.get(d - 1) : originCity + " Corridor " + d;
            fallback.add(new RouteStop(d, hubName, origin));
        }

        if (days > 0) {
            fallback.add(new RouteStop(days, destCity, dest));
        }
        return fallback;
    }
}
