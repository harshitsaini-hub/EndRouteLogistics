package com.endfielders.erl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

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

    public String analyzeRoute(String origin, String destination, String cargoType) {
        String originWeather = getWeather(origin);
        String destWeather = getWeather(destination);

        String prompt = "You are a logistics AI assistant. Analyze this shipment:\n" +
                "Origin Pincode: " + origin + " (Weather: " + originWeather + ")\n" +
                "Destination Pincode: " + destination + " (Weather: " + destWeather + ")\n" +
                "Cargo Type: " + cargoType + "\n\n" +
                "Give a short 1-2 line reason why this carrier is suitable. No formatting, no bold text.";

        return callGemini(prompt);
    }

    private String getWeather(String pincode) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            return "Weather data unavailable";
        }

        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?zip="
                    + pincode + ",IN&appid=" + weatherApiKey + "&units=metric";

            Map<?, ?> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("main") || !response.containsKey("weather")) {
                return "Weather data unavailable";
            }

            Map<?, ?> main = (Map<?, ?>) response.get("main");
            List<?> weatherList = (List<?>) response.get("weather");

            if (weatherList == null || weatherList.isEmpty()) {
                return "Weather data unavailable";
            }

            Map<?, ?> weatherDesc = (Map<?, ?>) weatherList.get(0);

            return weatherDesc.get("description") + ", " + main.get("temp") + "°C";

        } catch (Exception e) {
            return "Weather data unavailable";
        }
    }

    private String callGemini(String prompt) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            return "Weather data unavailable";
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
            if (candidates == null || candidates.isEmpty()) {
                return "No candidates returned";
            }

            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            if (first == null || !first.containsKey("content")) {
                return "Invalid AI response";
            }

            Map<?, ?> responseContent = (Map<?, ?>) first.get("content");
            if (responseContent == null || !responseContent.containsKey("parts")) {
                return "Invalid AI response structure";
            }

            List<?> parts = (List<?>) responseContent.get("parts");
            if (parts == null || parts.isEmpty()) {
                return "No content generated";
            }

            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            return part.get("text") != null ? part.get("text").toString() : "Empty AI response";
            }
            catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("503")) {
                return "Service temporarily busy. Please try again.";
            }
            return "AI analysis failed: " + e.getMessage();
        }
    }

    public String callGeminiRaw(String prompt) {
        return callGemini(prompt);
    }
}