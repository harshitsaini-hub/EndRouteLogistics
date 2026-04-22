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
    private static final String GENERAL_GOODS = "General goods";

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${WEATHER_API_KEY:}")
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

    // ================= WEATHER =================

    public String buildWeatherSummary(String origin, String destination) {
        String originWeather = getWeather(sanitizeLocation(origin));
        String destWeather = getWeather(sanitizeLocation(destination));
        return "Origin weather: " + originWeather + "; Destination weather: " + destWeather;
    }

    private String sanitizeLocation(String value) {
        return value == null ? "" : value.trim();
    }

    @SuppressWarnings("null")
    private String getWeather(String locationCode) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            return WEATHER_UNAVAILABLE;
        }
                if (locationCode.isBlank()) {
            return WEATHER_UNAVAILABLE;
        }

        try {
            String countryCode = weatherCountryCode == null || weatherCountryCode.isBlank()
                    ? "IN"
                    : weatherCountryCode.trim().toUpperCase(Locale.ROOT);
            String url = UriComponentsBuilder
                    .fromUriString("https://api.openweathermap.org/data/2.5/weather")
                    .queryParam("zip", locationCode + "," + countryCode)
                    .queryParam("appid", weatherApiKey)
                    .queryParam("units", "metric")
                    .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map<String, Object>> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new org.springframework.core.ParameterizedTypeReference<>() {
                        }
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

        String safeCargoType = (cargoType == null || cargoType.isBlank()) ? GENERAL_GOODS : cargoType.trim();

        return "You are an intelligent logistics decision engine.\n" +
                "Analyze the shipment and generate a professional risk insight.\n\n" +
                "Shipment Details:\n" +
                "- Origin: " + origin + "\n" +
                "- Destination: " + destination + "\n" +
                "- Cargo: " + safeCargoType + "\n" +
                "- Conditions: " + weatherSummary + "\n\n" +

            "Rules:\n" +
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
                if (prompt == null || prompt.isBlank()) {
            return "Prompt is empty";
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

            List<?> candidates = asRawList(response.get("candidates"));
            if (candidates == null || candidates.isEmpty()) return "No candidates returned";

            Map<?, ?> first = asRawMap(candidates.get(0));
            if (first == null) return "Malformed AI response";

            Map<?, ?> contentResp = asRawMap(first.get("content"));
            if (contentResp == null) return "Malformed AI response";

            List<?> parts = asRawList(contentResp.get("parts"));
            if (parts == null || parts.isEmpty()) return "Empty AI response";

            Map<?, ?> part = asRawMap(parts.get(0));
            if (part == null) return "Malformed AI response";

            String text = part.get("text") == null ? "" : part.get("text").toString().trim();


            return text.isEmpty() ? "Empty AI response" : text;

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

    private Map<?, ?> asRawMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return null;
    }

    private List<?> asRawList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return null;
    }
}