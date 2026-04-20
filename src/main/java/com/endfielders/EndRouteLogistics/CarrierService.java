package com.endfielders.EndRouteLogistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CarrierService {

    @Autowired
    private GeminiService geminiService;

    // Mock carrier database
    private List<Map<String, Object>> getMockCarriers() {
        List<Map<String, Object>> carriers = new ArrayList<>();

        Map<String, Object> bluedart = new HashMap<>();
        bluedart.put("name", "BlueDart");
        bluedart.put("mode", "Air");
        bluedart.put("estimatedDays", 2);
        bluedart.put("costPerKg", 120);
        bluedart.put("website", "https://www.bluedart.com");
        carriers.add(bluedart);

        Map<String, Object> delhivery = new HashMap<>();
        delhivery.put("name", "Delhivery");
        delhivery.put("mode", "Road");
        delhivery.put("estimatedDays", 4);
        delhivery.put("costPerKg", 45);
        delhivery.put("website", "https://www.delhivery.com");
        carriers.add(delhivery);

        Map<String, Object> dtdc = new HashMap<>();
        dtdc.put("name", "DTDC");
        dtdc.put("mode", "Road");
        dtdc.put("estimatedDays", 5);
        dtdc.put("costPerKg", 35);
        dtdc.put("website", "https://www.dtdc.in");
        carriers.add(dtdc);

        Map<String, Object> ecom = new HashMap<>();
        ecom.put("name", "Ecom Express");
        ecom.put("mode", "Road");
        ecom.put("estimatedDays", 3);
        ecom.put("costPerKg", 55);
        ecom.put("website", "https://www.ecomexpress.in");
        carriers.add(ecom);

        Map<String, Object> indiapost = new HashMap<>();
        indiapost.put("name", "India Post");
        indiapost.put("mode", "Rail");
        indiapost.put("estimatedDays", 7);
        indiapost.put("costPerKg", 20);
        indiapost.put("website", "https://www.indiapost.gov.in");
        carriers.add(indiapost);

        return carriers;
    }

    public List<Map<String, Object>> getRankedCarriers(
            String originPincode, String destinationPincode, String cargoType) {

        List<Map<String, Object>> carriers = getMockCarriers();

        String prompt = "You are a logistics AI. A user wants to ship " + cargoType +
            " from pincode " + originPincode + " to pincode " + destinationPincode + " in India.\n\n" +
            "Here are 5 carriers:\n" +
            "1. BlueDart - Air - 2 days - Rs.120/kg\n" +
            "2. Delhivery - Road - 4 days - Rs.45/kg\n" +
            "3. Ecom Express - Road - 3 days - Rs.55/kg\n" +
            "4. DTDC - Road - 5 days - Rs.35/kg\n" +
            "5. India Post - Rail - 7 days - Rs.20/kg\n\n" +
            "For each carrier respond ONLY in this exact format, one per line:\n" +
            "CARRIER_NAME|GRADE|RISK_SCORE|ONE_LINE_REASON\n\n" +
            "GRADE must be A, B, C, D, or F.\n" +
            "RISK_SCORE must be a number 0-100 (higher = more risky).\n" +
            "Order them from best to worst for this shipment.\n" +
            "After the 5 carriers, add one final line:\n" +
            "ALTERNATIVE|your alternative route or timing suggestion if overall risk is high\n\n" +
            "Example:\n" +
            "BlueDart|A|15|Best for electronics via climate controlled air transport.\n" +
            "ALTERNATIVE|Consider splitting shipment — weather clears in 2 days for safer road transit.";

        String geminiResponse = geminiService.callGeminiRaw(prompt);
        System.out.println("GEMINI RAW: " + geminiResponse);

        // Parse Gemini response and match to carriers
        List<Map<String, Object>> rankedCarriers = new ArrayList<>();
        String[] lines = geminiResponse.split("\n");

        String alternative = "";
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("ALTERNATIVE|")) {
                alternative = line.replace("ALTERNATIVE|", "").trim();
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length < 4) continue;

            String carrierName = parts[0].trim()
            .replaceAll("^\\d+[\\.\\)\\s]+", "")
            .trim();
            String grade = parts[1].trim();
            String riskScore = parts[2].trim();
            String reason = parts[3].trim();

            for (Map<String, Object> carrier : carriers) {
                String name = (String) carrier.get("name");
                if (name.toLowerCase().contains(carrierName.toLowerCase()) ||
                    carrierName.toLowerCase().contains(name.toLowerCase())) {
                    Map<String, Object> ranked = new HashMap<>(carrier);
                    ranked.put("safetyGrade", grade);
                    ranked.put("riskScore", riskScore);
                    ranked.put("aiReason", reason);
                    rankedCarriers.add(ranked);
                    break;
                }
            }
        }

// Add alternative suggestion to first carrier as metadata
        if (!alternative.isEmpty() && !rankedCarriers.isEmpty()) {
            rankedCarriers.get(0).put("alternativeSuggestion", alternative);
        }

        // Fallback if Gemini was busy or parsing failed
        if (rankedCarriers.isEmpty()) {
            String[] defaultGrades = {"A", "B", "B", "C", "D"};
            String[] defaultReasons = {
                "Fastest and most reliable option for this cargo.",
                "Good balance of speed and cost.",
                "Reliable road option with reasonable pricing.",
                "Budget-friendly option for non-urgent shipments.",
                "Most economical option, best for low-priority cargo."
            };
            String[] defaultRisks = {"15", "25", "35", "50", "70"};
            for (int i = 0; i < carriers.size(); i++) {
                Map<String, Object> ranked = new HashMap<>(carriers.get(i));
                ranked.put("safetyGrade", defaultGrades[i]);
                ranked.put("riskScore", defaultRisks[i]);
                ranked.put("aiReason", defaultReasons[i]);
                rankedCarriers.add(ranked);
            }
        }

        return rankedCarriers;
    }
}