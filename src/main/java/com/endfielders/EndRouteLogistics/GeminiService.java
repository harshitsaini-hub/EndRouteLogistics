package com.endfielders.EndRouteLogistics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Value("${WEATHER_API_KEY}")
    private String weatherApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeRoute(String origin, String destination, String cargoType) {
        String originWeather = getWeather(origin);
        String destWeather = getWeather(destination);

        String prompt = "You are a logistics AI assistant. Analyze this shipment:\n" +
                "Origin Pincode: " + origin + " (Weather: " + originWeather + ")\n" +
                "Destination Pincode: " + destination + " (Weather: " + destWeather + ")\n" +
                "Cargo Type: " + cargoType + "\n\n" +
                "Provide: 1) Risk level (Low/Medium/High) 2) Main risks 3) Your recommendation. Keep it concise.";

        return callGemini(prompt);
    }

    private String getWeather(String pincode) {
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?zip="
                    + pincode + ",IN&appid=" + weatherApiKey + "&units=metric";
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            Map<?, ?> main = (Map<?, ?>) response.get("main");
            List<?> weatherList = (List<?>) response.get("weather");
            Map<?, ?> weatherDesc = (Map<?, ?>) weatherList.get(0);

            return weatherDesc.get("description") + ", " + main.get("temp") + "°C";
        } catch (Exception e) {
            return "Weather data unavailable";
        }
    }

    private String callGemini(String prompt) {
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

            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            Map<?, ?> responseContent = (Map<?, ?>) first.get("content");
            List<?> parts = (List<?>) responseContent.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);

            return (String) part.get("text");

        } catch (Exception e) {
            if (e.getMessage().contains("503")) {
                return "Service temporarily busy. Please try again in a moment.";
            }
            return "AI analysis failed: " + e.getMessage();
        }
    }
    public String callGeminiRaw(String prompt) {
    return callGemini(prompt);
    }
}