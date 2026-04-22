package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.util.ScoringEngine;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CarrierService {

    private final GeminiService geminiService;

    private static final List<Carrier> CARRIERS = List.of(
            create("BlueDart", "Air", 2, 120, "https://www.bluedart.com"),
            create("Delhivery", "Road", 4, 45, "https://www.delhivery.com"),
            create("DTDC", "Road", 5, 35, "https://www.dtdc.in"),
            create("Ecom Express", "Road", 3, 55, "https://www.ecomexpress.in"),
            create("India Post", "Rail", 7, 20, "https://www.indiapost.gov.in")
    );
    public CarrierService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    private List<Carrier> getCarriers() {
        return CARRIERS;
    }

    private static Carrier create(String name, String mode, int days, double cost, String website) {
        Carrier c = new Carrier();
        c.setName(name);
        c.setMode(mode);
        c.setEstimatedDays(days);
        c.setCostPerKg(cost);
        c.setWebsite(website);
        return c;
    }

    public List<RankedCarrier> getRankedCarriers(
            String origin,
            String destination,
            String cargoType,
            String priority,
            boolean fragile,
            boolean perishable){

        List<Carrier> carriers = getCarriers();
        List<RankedCarrier> rankedList = new ArrayList<>();
        String weatherSummary = geminiService.buildWeatherSummary(origin, destination);

        for (Carrier c : carriers) {

            RankedCarrier rc = new RankedCarrier();

            rc.setName(c.getName());
            rc.setMode(c.getMode());
            rc.setEstimatedDays(c.getEstimatedDays());
            rc.setCostPerKg(c.getCostPerKg());
            rc.setWebsite(c.getWebsite());
            double score = ScoringEngine.calculateScore(
                    c, cargoType, priority, fragile, perishable, weatherSummary);

            rc.setScore(score);
            rc.setRiskScore((int) (100 - score));
            rc.setGrade(ScoringEngine.assignGrade(score));
            rc.setAiInsight("Not analyzed by AI");
            rankedList.add(rc);
        }

        rankedList.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        for (int i = 0; i < Math.min(3, rankedList.size()); i++) {

            RankedCarrier rc = rankedList.get(i);

        String insight = geminiService.callGeminiRaw(
            "You are a logistics ranking engine.\n" +
            "Explain why this carrier is a strong choice.\n\n" +

            "Carrier: " + rc.getName() + "\n" +
            "Mode: " + rc.getMode() + "\n" +
            "Delivery Time: " + rc.getEstimatedDays() + " days\n" +
            "Cargo: " + cargoType + "\n\n" +

            "Rules:\n" +
            "- 1 short sentence only\n" +
            "- No formatting, no symbols\n" +
            "- Be specific (speed, cost, reliability)\n" +
            "- Sound like a product recommendation\n\n" +

            "Output:"
        );
        if (insight == null || insight.isBlank()) {
            insight = "No AI insight available.";
            }
        rc.setAiInsight(insight);
        }
        
        return rankedList;
    }

}