package com.endfielders.erl.util;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.DayWeather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {

    private Carrier airCarrier;
    private Carrier roadCarrier;

    @BeforeEach
    void setUp() {
        airCarrier = new Carrier();
        airCarrier.setId(1L);
        airCarrier.setName("BlueDart Air");
        airCarrier.setMode("Air");
        airCarrier.setEstimatedDays(2);
        airCarrier.setCostPerKg(120.0);
        airCarrier.setReliabilityScore(90.0);

        roadCarrier = new Carrier();
        roadCarrier.setId(2L);
        roadCarrier.setName("Delhivery Road");
        roadCarrier.setMode("Road");
        roadCarrier.setEstimatedDays(4);
        roadCarrier.setCostPerKg(45.0);
        roadCarrier.setReliabilityScore(80.0);
    }

    @Test
    @DisplayName("Should assign grade A for high score")
    void testAssignGradeA() {
        assertEquals("A", ScoringEngine.assignGrade(85.0));
        assertEquals("A", ScoringEngine.assignGrade(80.0));
    }

    @Test
    @DisplayName("Should assign grade B, C, D, F correctly")
    void testAssignGrades() {
        assertEquals("B", ScoringEngine.assignGrade(70.0));
        assertEquals("C", ScoringEngine.assignGrade(55.0));
        assertEquals("D", ScoringEngine.assignGrade(40.0));
        assertEquals("F", ScoringEngine.assignGrade(20.0));
    }

    @Test
    @DisplayName("FASTEST priority should favor speed (fewer days)")
    void testFastestPriority() {
        double avgCost = (airCarrier.getCostPerKg() + roadCarrier.getCostPerKg()) / 2;
        double avgSpeed = (airCarrier.getEstimatedDays() + roadCarrier.getEstimatedDays()) / 2.0;
        Map<String, Integer> suitability = Map.of("Air", 90, "Road", 65, "Rail", 65);

        double airScore = ScoringEngine.calculateScoreWithTimeline(
                airCarrier, "Electronics", "FASTEST", true, false,
                new ArrayList<>(), avgCost, avgSpeed, suitability);
        double roadScore = ScoringEngine.calculateScoreWithTimeline(
                roadCarrier, "Electronics", "FASTEST", true, false,
                new ArrayList<>(), avgCost, avgSpeed, suitability);

        assertTrue(airScore > roadScore, "Air carrier should score higher than road carrier for FASTEST priority");
    }

    @Test
    @DisplayName("CHEAPEST priority should favor lower cost")
    void testCheapestPriority() {
        double avgCost = (airCarrier.getCostPerKg() + roadCarrier.getCostPerKg()) / 2;
        double avgSpeed = (airCarrier.getEstimatedDays() + roadCarrier.getEstimatedDays()) / 2.0;
        Map<String, Integer> suitability = Map.of("Air", 65, "Road", 65, "Rail", 65);

        double airScore = ScoringEngine.calculateScoreWithTimeline(
                airCarrier, "General", "CHEAPEST", false, false,
                new ArrayList<>(), avgCost, avgSpeed, suitability);
        double roadScore = ScoringEngine.calculateScoreWithTimeline(
                roadCarrier, "General", "CHEAPEST", false, false,
                new ArrayList<>(), avgCost, avgSpeed, suitability);

        assertTrue(roadScore > airScore, "Road carrier should score higher than air carrier for CHEAPEST priority");
    }

    @Test
    @DisplayName("Timeline weather penalties should reduce score when rain/storm occurs on road routes")
    void testTimelineWeatherPenalties() {
        List<DayWeather> badWeatherTimeline = new ArrayList<>();
        DayWeather day0 = new DayWeather();
        day0.setDay(0);
        day0.setForecastAvailable(true);
        day0.setCondition("heavy rain");
        day0.setTemperature(25.0);
        badWeatherTimeline.add(day0);

        DayWeather day1 = new DayWeather();
        day1.setDay(1);
        day1.setForecastAvailable(true);
        day1.setCondition("drizzle");
        day1.setTemperature(24.0);
        badWeatherTimeline.add(day1);

        double avgCost = roadCarrier.getCostPerKg();
        double avgSpeed = roadCarrier.getEstimatedDays();
        Map<String, Integer> suitability = Map.of("Air", 65, "Road", 65, "Rail", 65);

        double clearScore = ScoringEngine.calculateScoreWithTimeline(
                roadCarrier, "General", "BALANCED", false, false,
                new ArrayList<>(), avgCost, avgSpeed, suitability);
        double rainyScore = ScoringEngine.calculateScoreWithTimeline(
                roadCarrier, "General", "BALANCED", false, false,
                badWeatherTimeline, avgCost, avgSpeed, suitability);

        assertTrue(clearScore > rainyScore, "Score with rain penalty should be lower than clear weather score");
    }
}
