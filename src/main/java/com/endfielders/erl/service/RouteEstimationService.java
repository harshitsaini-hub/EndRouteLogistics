package com.endfielders.erl.service;

import com.endfielders.erl.model.RouteStop;
import com.endfielders.erl.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Uses Gemini AI to predict intermediate day-wise locations (city & pincode)
 * based on origin, destination, transport mode, and estimated days.
 */
@Service
public class RouteEstimationService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteEstimationService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Estimates day-by-day route stops for a shipment.
     * Day 0 = Origin pincode/city
     * Day N = Destination pincode/city
     * Day 1..N-1 = Estimated intermediate cities & pincodes for the mode of transport.
     */
    public List<RouteStop> estimateRouteStops(String originPincode, String destPincode, String mode, int estimatedDays) {
        List<RouteStop> stops = new ArrayList<>();

        if (estimatedDays <= 0) {
            stops.add(new RouteStop(0, "Origin (" + originPincode + ")", originPincode));
            return stops;
        }

        // Prompt Gemini for realistic intermediate stops along the route for the transport mode
        String prompt = """
                You are an Indian logistics route predictor and geography expert.
                Estimate realistic day-by-day transit stops for a shipment.
                Origin Pincode: %s
                Destination Pincode: %s
                Mode of Transport: %s
                Total Transit Days: %d

                RULES:
                1. Return ONLY a valid JSON array of objects. No markdown, no commentary outside JSON.
                2. Each object must have:
                   - "day": integer (0 to %d)
                   - "city": string (Major Recognized City or District Name ONLY. E.g. "Bhopal", "Susner", "Indore", "Ujjain", "Delhi", "Mumbai").
                     CRITICAL: NEVER use micro-localities, villages, sub-colonies or police station names (e.g. NEVER use "S.I. Line", "Chhota Bangarda", "Paliya"). ALWAYS use the primary Major City/District name!
                   - "pincode": string (6-digit Indian pincode)
                3. Day 0 MUST be the origin pincode location with its true Major City/District name.
                4. Day %d MUST be the destination pincode location with its true Major City/District name.
                5. For intermediate days (Day 1 to %d), estimate major logistics hub cities & valid 6-digit pincodes along the major %s route.

                EXAMPLE FORMAT:
                [
                  {"day": 0, "city": "Susner", "pincode": "%s"},
                  {"day": %d, "city": "Bhopal", "pincode": "%s"}
                ]
                """.formatted(
                originPincode, destPincode, mode, estimatedDays,
                estimatedDays, estimatedDays, estimatedDays - 1, mode,
                originPincode, estimatedDays, destPincode
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
                        String pincode = item.get("pincode") != null ? item.get("pincode").toString() : originPincode;
                        stops.add(new RouteStop(day, city, pincode));
                    }
                }
            } catch (Exception e) {
                System.out.println("[WARN] Gemini route estimation JSON parse failed: " + e.getMessage());
            }
        }

        // Fallback: If AI fails or returns incomplete list, generate programmatic fallback
        if (stops.isEmpty() || stops.size() < estimatedDays + 1) {
            stops = generateFallbackStops(originPincode, destPincode, mode, estimatedDays);
        }

        stops.sort(Comparator.comparingInt(RouteStop::getDay));
        return stops;
    }

    private List<RouteStop> generateFallbackStops(String origin, String dest, String mode, int days) {
        List<RouteStop> fallback = new ArrayList<>();
        fallback.add(new RouteStop(0, "Origin Hub (" + origin + ")", origin));

        for (int d = 1; d < days; d++) {
            fallback.add(new RouteStop(d, mode + " Transit Hub - Day " + d, origin));
        }

        if (days > 0) {
            fallback.add(new RouteStop(days, "Destination Hub (" + dest + ")", dest));
        }
        return fallback;
    }
}
