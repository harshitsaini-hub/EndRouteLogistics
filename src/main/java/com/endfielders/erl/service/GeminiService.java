package com.endfielders.erl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class GeminiService {

    private static final String WEATHER_UNAVAILABLE = "Weather data unavailable";
    private static final String AI_UNAVAILABLE = "AI insight unavailable";

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${WEATHER_API_KEY:}")
    private String weatherApiKey;

    private final RestTemplate restTemplate = createRestTemplate();

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    // ================= WEATHER =================

    public String buildWeatherSummary(String origin, String destination) {
        String originWeather = getWeather(origin);
        String destWeather = getWeather(destination);
        return "Origin weather: " + originWeather + "; Destination weather: " + destWeather;
    }

    private String getWeather(String pincode) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            return WEATHER_UNAVAILABLE;
        }

        try {
        String url = UriComponentsBuilder
                .fromUriString("https://api.openweathermap.org/data/2.5/weather")
                .queryParam("zip", pincode + ",IN")
                .queryParam("appid", weatherApiKey)
                .queryParam("units", "metric")
                .toUriString();

            ResponseEntity<Map<String, Object>> responseEntity =
                restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

Map<String, Object> response = responseEntity.getBody();

            if (response == null) return WEATHER_UNAVAILABLE;

            Map<String, Object> main = asMap(response.get("main"));
            List<Object> weatherList = asList(response.get("weather"));

            if (main == null || weatherList == null || weatherList.isEmpty()) {
                return WEATHER_UNAVAILABLE;
            }

            Map<String, Object> weatherDesc = asMap(weatherList.get(0));

            if (weatherDesc == null || !weatherDesc.containsKey("description") || !main.containsKey("temp")) {
                return WEATHER_UNAVAILABLE;
            }

            return weatherDesc.get("description") + ", " + main.get("temp") + "°C";

        } catch (Exception e) {
            return WEATHER_UNAVAILABLE;
        }
    }

    // ================= GEMINI =================

    public String analyzeRoute(String origin, String destination, String cargoType) {

        String weatherSummary = buildWeatherSummary(origin, destination);

        String prompt = buildRouteRiskPrompt(
                origin,
                destination,
                cargoType,
                weatherSummary
        );

        return callGemini(prompt);
    }

private String buildRouteRiskPrompt(String origin, String destination, String cargoType, String weatherSummary) {

    String safeCargoType = (cargoType == null || cargoType.isBlank()) ? "General goods" : cargoType;

    return "You are an intelligent logistics decision engine.\n" +
            "Analyze the shipment and generate a professional risk insight.\n\n" +

            "Shipment Details:\n" +
            "- Origin: " + origin + "\n" +
            "- Destination: " + destination + "\n" +
            "- Cargo: " + safeCargoType + "\n" +
            "- Conditions: " + weatherSummary + "\n\n" +

            "Rules:\n" +
            "1. Output only 1 short sentence\n" +
            "2. No emojis, no symbols, no formatting\n" +
            "3. Sound like a logistics platform, not a chatbot\n" +
            "4. Focus on risk, speed, and reliability\n" +
            "5. Avoid generic phrases\n\n" +

            "Example style:\n" +
            "Moderate delay risk due to rain at destination, suitable for non-perishable goods.\n\n" +

            "Now generate the insight:";
}

    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return AI_UNAVAILABLE;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null || !response.containsKey("candidates")) {
                return "No response from AI";
            }

            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates.isEmpty()) return "No candidates returned";

            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            Map<?, ?> contentResp = (Map<?, ?>) first.get("content");
            List<?> parts = (List<?>) contentResp.get("parts");

            Map<?, ?> part = (Map<?, ?>) parts.get(0);

            return part.get("text") != null ? part.get("text").toString() : "Empty AI response";

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("503")) {
                return "Service temporarily busy. Try again.";
            }
            return "AI failed: " + e.getMessage();
        }
    }

    public String callGeminiRaw(String prompt) {
        return callGemini(prompt);
    }

    // ================= HELPERS =================

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return null;
    }
}