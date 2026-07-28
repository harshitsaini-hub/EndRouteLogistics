package com.endfielders.erl.util;

/**
 * Utility for cleaning AI-generated JSON responses.
 */
public class JsonUtil {

    private JsonUtil() {}

    /**
     * Strips markdown code fences and extracts the JSON object/array from raw AI output.
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String cleaned = raw.replace("```json", "").replace("```", "").trim();

        // Try extracting JSON object
        int objStart = cleaned.indexOf("{");
        int objEnd = cleaned.lastIndexOf("}");
        if (objStart != -1 && objEnd != -1 && objEnd > objStart) {
            return cleaned.substring(objStart, objEnd + 1);
        }

        // Try extracting JSON array
        int arrStart = cleaned.indexOf("[");
        int arrEnd = cleaned.lastIndexOf("]");
        if (arrStart != -1 && arrEnd != -1 && arrEnd > arrStart) {
            return cleaned.substring(arrStart, arrEnd + 1);
        }

        return cleaned;
    }
}
