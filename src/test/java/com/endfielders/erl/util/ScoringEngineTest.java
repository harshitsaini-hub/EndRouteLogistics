package com.endfielders.erl.util;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.DayWeather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        double airScore = ScoringEngine.calculateScore(airCarrier, "Electronics", "FASTEST", true, false, "clear");
        double roadScore = ScoringEngine.calculateScore(roadCarrier, "Electronics", "FASTEST", true, false, "clear");

        assertTrue(airScore > roadScore, "Air carrier should score higher than road carrier for FASTEST priority");
    }

    @Test
    @DisplayName("CHEAPEST priority should favor lower cost")
    void testCheapestPriority() {
        double airScore = ScoringEngine.calculateScore(airCarrier, "General", "CHEAPEST", false, false, "clear");
        double roadScore = ScoringEngine.calculateScore(roadCarrier, "General", "CHEAPEST", false, false, "clear");

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

        double clearScore = ScoringEngine.calculateScoreWithTimeline(roadCarrier, "General", "BALANCED", false, false, new ArrayList<>());
        double rainyScore = ScoringEngine.calculateScoreWithTimeline(roadCarrier, "General", "BALANCED", false, false, badWeatherTimeline);

        assertTrue(clearScore > rainyScore, "Score with rain penalty should be lower than clear weather score");
    }
}
