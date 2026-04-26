package com.endfielders.erl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class GeminiService {

    private static final String WEATHER_UNAVAILABLE = "Weather data unavailable";
    private static final String GENERAL_GOODS = "General goods";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${weather.api.key:}")
    private String weatherApiKey;

    @Value("${WEATHER_COUNTRY_CODE:IN}")
    private String weatherCountryCode;

    private final RestTemplate restTemplate = createRestTemplate();

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
    public String buildWeatherSummary(String origin, String destination) {
        String originWeather = getWeather(sanitizeLocation(origin));
        String destWeather = getWeather(sanitizeLocation(destination));
        return "Origin weather: " + originWeather + "; Destination weather: " + destWeather;
    }

    private String sanitizeLocation(String value) { return value == null ? "" : value.trim(); }

    @SuppressWarnings("null")
    private String getWeather(String locationCode) {
        if (weatherApiKey == null || weatherApiKey.isBlank() || locationCode.isBlank()) return WEATHER_UNAVAILABLE;
        try {
            String countryCode = weatherCountryCode == null || weatherCountryCode.isBlank() ? "IN" : weatherCountryCode.trim().toUpperCase(Locale.ROOT);
            String url = UriComponentsBuilder.fromUriString("https://api.openweathermap.org/data/2.5/weather")
                    .queryParam("zip", locationCode + "," + countryCode)
                    .queryParam("appid", weatherApiKey)
                    .queryParam("units", "metric").toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), new org.springframework.core.ParameterizedTypeReference<>() {});

            Map<String, Object> response = responseEntity.getBody();
            if (response == null) return WEATHER_UNAVAILABLE;

            Map<String, Object> main = asMap(response.get("main"));
            List<Object> weatherList = asList(response.get("weather"));

            if (main == null || weatherList == null || weatherList.isEmpty()) return WEATHER_UNAVAILABLE;
            Map<String, Object> weatherDesc = asMap(weatherList.get(0));

            if (weatherDesc == null || !weatherDesc.containsKey("description") || !main.containsKey("temp")) return WEATHER_UNAVAILABLE;
            return weatherDesc.get("description") + ", " + main.get("temp") + "°C";
        } catch (Exception e) {
            return WEATHER_UNAVAILABLE;
        }
    }

    public String analyzeRoute(String origin, String destination, String cargoType) {
        String weatherSummary = buildWeatherSummary(origin, destination);
        String prompt = buildRouteRiskPrompt(origin, destination, cargoType, weatherSummary);
        String aiResponse = callGemini(prompt);
        if (aiResponse == null || aiResponse.isBlank()) {
            return generateFallbackInsight(origin, destination, cargoType);
        }
        return aiResponse;
    }

    private String buildRouteRiskPrompt(String origin, String destination, String cargoType, String weatherSummary) {
        String safeCargoType = (cargoType == null || cargoType.isBlank()) ? GENERAL_GOODS : cargoType.trim();
        return "You are an intelligent logistics decision engine.\nAnalyze the shipment and generate a professional risk insight.\n\nShipment Details:\n- Origin: " + origin + "\n- Destination: " + destination + "\n- Cargo: " + safeCargoType + "\n- Conditions: " + weatherSummary + "\n\nRules:\n1. Output only 1 short sentence\n2. No emojis, no symbols, no formatting\n3. Sound like a logistics platform, not a chatbot\n4. Focus on risk, speed, and reliability\n5. Avoid generic phrases\n\nNow generate the insight:";
    }

    public String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            System.out.println("❌ GEMINI API KEY MISSING");
            return null;
        }
        if (prompt == null || prompt.isBlank()) return "Prompt is empty";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + geminiApiKey;
        Map<String, Object> textPart = new HashMap<>(); textPart.put("text", prompt);
        Map<String, Object> content = new HashMap<>(); content.put("parts", List.of(textPart));
        Map<String, Object> requestBody = new HashMap<>(); requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 3;
        long waitTimeMs = 7000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);

                if (response == null || !response.containsKey("candidates")) return null;
                List<?> candidates = asRawList(response.get("candidates"));
                if (candidates == null || candidates.isEmpty()) return null;

                Map<?, ?> first = asRawMap(candidates.get(0));
                if (first == null) return null;

                Map<?, ?> contentResp = asRawMap(first.get("content"));
                if (contentResp == null) return null;

                List<?> parts = asRawList(contentResp.get("parts"));
                if (parts == null || parts.isEmpty()) return null;

                Map<?, ?> part = asRawMap(parts.get(0));
                if (part == null || part.get("text") == null) return null;

                return part.get("text").toString().trim();

            } catch (HttpClientErrorException.TooManyRequests e) {
                System.out.println("⏳ Gemini API Rate Limit (429) hit. Waiting 7 seconds... (Attempt " + attempt + " of " + maxRetries + ")");
                if (attempt == maxRetries) {
                    System.out.println("❌ Max retries reached for Gemini API.");
                    return null;
                }
                try {
                    Thread.sleep(waitTimeMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                System.out.println("❌ GEMINI ERROR: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    private String generateFallbackInsight(String origin, String destination, String cargoType) {
        String cargo = (cargoType == null || cargoType.isBlank()) ? "general goods" : cargoType.toLowerCase();
        StringBuilder insight = new StringBuilder();
        insight.append("Optimized route from ").append(origin).append(" to ").append(destination).append(" suitable for ");
        
        if (cargo.contains("electronic") || cargo.contains("fragile")) {
            insight.append("fragile shipments with reduced handling risk ");
        } else if (cargo.contains("food") || cargo.contains("perishable")) {
            insight.append("perishable goods requiring faster transit ");
        } else {
            insight.append("general cargo movement ");
        }
        
        insight.append("with balanced delivery speed and reliability ");
        if (origin.equalsIgnoreCase(destination)) {
            insight.append("within local distribution network ");
        } else {
            insight.append("across intercity logistics network ");
        }
        insight.append("making it a dependable shipping choice.");
        return insight.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) { return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null; }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) { return value instanceof List<?> list ? (List<Object>) list : null; }

    private Map<?, ?> asRawMap(Object value) { return value instanceof Map<?, ?> map ? map : null; }

    private List<?> asRawList(Object value) { return value instanceof List<?> list ? list : null; }
}