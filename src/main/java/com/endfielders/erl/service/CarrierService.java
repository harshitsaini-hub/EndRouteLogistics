package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.util.ScoringEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CarrierService {

    @Autowired
    private GeminiService geminiService;

    private List<Carrier> getCarriers() {
        List<Carrier> list = new ArrayList<>();

        list.add(create("BlueDart", "Air", 2, 120, "https://www.bluedart.com"));
        list.add(create("Delhivery", "Road", 4, 45, "https://www.delhivery.com"));
        list.add(create("DTDC", "Road", 5, 35, "https://www.dtdc.in"));
        list.add(create("Ecom Express", "Road", 3, 55, "https://www.ecomexpress.in"));
        list.add(create("India Post", "Rail", 7, 20, "https://www.indiapost.gov.in"));

        return list;
    }

    private Carrier create(String name, String mode, int days, double cost, String website) {
        Carrier c = new Carrier();
        c.setName(name);
        c.setMode(mode);
        c.setEstimatedDays(days);
        c.setCostPerKg(cost);
        c.setWebsite(website);
        return c;
    }

    public List<RankedCarrier> getRankedCarriers(
            String origin, String destination, String cargoType) {

        List<Carrier> carriers = getCarriers();
        List<RankedCarrier> rankedList = new ArrayList<>();

        for (Carrier c : carriers) {

            RankedCarrier rc = new RankedCarrier();

            rc.setName(c.getName());
            rc.setMode(c.getMode());
            rc.setEstimatedDays(c.getEstimatedDays());
            rc.setCostPerKg(c.getCostPerKg());
            rc.setWebsite(c.getWebsite());

            double score = ScoringEngine.calculateScore(c, cargoType);

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
                "Give a 1-line reason (no formatting, no symbols) why " 
                + rc.getName() +
                " is suitable for transporting " + cargoType +
                " from " + origin + " to " + destination
            );

        if (insight == null || insight.isBlank()) {
            insight = "No AI insight available.";
            }
        rc.setAiInsight(insight);
        }
        
        return rankedList;
    }
}