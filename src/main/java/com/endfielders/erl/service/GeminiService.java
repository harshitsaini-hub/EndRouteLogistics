package com.endfielders.erl.service;

import com.endfielders.erl.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multi-Provider AI Service supporting Groq API (Primary - OpenAI-compatible)
 * and Gemini API (Secondary fallback) for high-speed logistics intelligence.
 */
@Service
public class GeminiService {

    private static final String GENERAL_GOODS = "General goods";

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = createRestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    /**
     * Main AI invocation method:
     * Attempts Groq API first; if unconfigured or failing, falls back to Gemini API.
     */
    public String callGemini(String prompt) {
        if (prompt == null || prompt.isBlank()) return "Prompt is empty";

        // 1. Try Groq API first if key is present
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            String groqResponse = callGroq(prompt);
            if (groqResponse != null && !groqResponse.isBlank()) {
                return groqResponse;
            }
            System.out.println("[WARN] Groq API returned null, trying Gemini API fallback...");
        }

        // 2. Try Gemini API fallback
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return callGeminiDirect(prompt);
        }

        System.out.println("❌ NO AI API KEY CONFIGURED (GROQ_API_KEY / GEMINI_API_KEY MISSING)");
        return null;
    }

    /**
     * Execute chat completion call using Groq API (OpenAI compatible).
     */
    public String callGroq(String prompt) {
        if (groqApiKey == null || groqApiKey.isBlank()) return null;

        String url = "https://api.groq.com/openai/v1/chat/completions";
        String modelName = (groqModel != null && !groqModel.isBlank()) ? groqModel.trim() : "llama-3.3-70b-versatile";

        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(userMessage),
                "temperature", 0.2
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 2;
        long waitTimeMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
                if (response == null || !response.containsKey("choices")) return null;

                List<?> choices = asRawList(response.get("choices"));
                if (choices == null || choices.isEmpty()) return null;

                Map<?, ?> firstChoice = asRawMap(choices.get(0));
                if (firstChoice == null) return null;

                Map<?, ?> message = asRawMap(firstChoice.get("message"));
                if (message == null || message.get("content") == null) return null;

                return message.get("content").toString().trim();

            } catch (HttpClientErrorException.TooManyRequests e) {
                System.out.println("⏳ Groq API Rate Limit (429) hit. Waiting 1s... (Attempt " + attempt + " of " + maxRetries + ")");
                if (attempt == maxRetries) return null;
                try {
                    Thread.sleep(waitTimeMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                System.out.println("❌ GROQ API ERROR: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Direct Gemini API invocation fallback.
     */
    private String callGeminiDirect(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + geminiApiKey;
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

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

        } catch (Exception e) {
            System.out.println("❌ GEMINI API ERROR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Analyze route using pre-fetched weather summary.
     */
    public String analyzeRouteWithWeather(String origin, String destination, String cargoType, String weatherSummary) {
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

    /**
     * Ask AI to rate suitability of a cargo type for Air, Road, and Rail modes.
     */
    public Map<String, Integer> getCargoModeSuitability(String cargoType) {
        String safeCargoType = (cargoType == null || cargoType.isBlank()) ? GENERAL_GOODS : cargoType.trim();

        String prompt = """
        You are a logistics engine. Rate how suitable "%s" is for different transport modes.
        IMPORTANT RULES:
        - Return ONLY valid JSON, no markdown, no explanation.
        - The keys must be EXACTLY "Air", "Road", and "Rail".
        - The values must be integers from 1 to 100 representing suitability.
        Example: {"Air": 20, "Road": 80, "Rail": 90}
        """.formatted(safeCargoType);

        String response = callGemini(prompt);
        Map<String, Integer> defaultScores = Map.of("Air", 65, "Road", 65, "Rail", 65);

        if (response == null || response.isBlank()) {
            return defaultScores;
        }

        try {
            String cleanJson = JsonUtil.extractJson(response);
            Map<String, Integer> map = mapper.readValue(cleanJson, mapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
            return map.isEmpty() ? defaultScores : map;
        } catch (Exception e) {
            System.out.println("[WARN] Failed to parse cargo suitability JSON: " + response);
            return defaultScores;
        }
    }

    /**
     * Ask AI to classify custom cargo into logistics category.
     */
    public String resolveCargoCategory(String cargoType) {
        String safeCargoType = (cargoType == null || cargoType.isBlank()) ? GENERAL_GOODS : cargoType.trim();

        String prompt = """
        You are a logistics classification engine.
        Classify the following cargo type into EXACTLY ONE of these categories:
        - B2B_FREIGHT (heavy industrial, wholesale, raw materials, bulk commercial goods)
        - E_COMMERCE (electronics, retail parcels, documents, clothing, gadgets, small items)
        - HOUSEHOLD (furniture, home appliances, personal belongings, home relocation)
        - COLD_CHAIN (food, pharma, perishable, frozen, dairy, chemicals, medical supplies)
        - GENERAL (if none of the above fit)

        RULES:
        - Return ONLY the category name, nothing else.
        - No explanation, no punctuation, no quotes.

        Cargo: "%s"
        """.formatted(safeCargoType);

        String response = callGemini(prompt);

        if (response == null || response.isBlank()) {
            return "GENERAL";
        }

        String cleaned = response.trim().toUpperCase().replace(" ", "_");

        if (Set.of("B2B_FREIGHT", "E_COMMERCE", "HOUSEHOLD", "COLD_CHAIN", "GENERAL").contains(cleaned)) {
            return cleaned;
        }

        System.out.println("[WARN] AI returned unknown category: " + response + ", falling back to GENERAL");
        return "GENERAL";
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

    private Map<?, ?> asRawMap(Object value) { return value instanceof Map<?, ?> map ? map : null; }
    private List<?> asRawList(Object value) { return value instanceof List<?> list ? list : null; }
}