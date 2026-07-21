package com.endfielders.erl.service;

import com.endfielders.erl.model.DayWeather;
import com.endfielders.erl.model.RouteStop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService();
    }

    @Test
    @DisplayName("Should mark days > 5 as forecast unavailable gracefully")
    void testBeyond5DaysForecast() {
        List<RouteStop> stops = List.of(
                new RouteStop(0, "Delhi", "110001"),
                new RouteStop(6, "Kochi", "682011")
        );

        Map<String, DayWeather> dedupCache = new HashMap<>();
        List<DayWeather> result = weatherService.batchFetchForecasts(stops, dedupCache);

        assertEquals(2, result.size());

        DayWeather day6Weather = result.get(1);
        assertEquals(6, day6Weather.getDay());
        assertFalse(day6Weather.isForecastAvailable());
        assertEquals("Forecast unavailable", day6Weather.getCondition());
        assertTrue(day6Weather.getAdvisory().contains("5 days ahead"));
    }

    @Test
    @DisplayName("Should reuse cached weather for duplicate (pincode, day) keys")
    void testWeatherDeduplication() {
        List<RouteStop> stops = List.of(
                new RouteStop(0, "Delhi", "110001"),
                new RouteStop(0, "Delhi Hub", "110001") // Duplicate pincode & day
        );

        Map<String, DayWeather> dedupCache = new HashMap<>();

        // Pre-fill cache for "110001:0"
        DayWeather prefilled = new DayWeather();
        prefilled.setDay(0);
        prefilled.setDate("2026-07-21");
        prefilled.setCity("Delhi");
        prefilled.setPincode("110001");
        prefilled.setCondition("clear sky");
        prefilled.setTemperature(30.0);
        prefilled.setForecastAvailable(true);
        prefilled.setAdvisory("Clear conditions");
        dedupCache.put("110001:0", prefilled);

        List<DayWeather> result = weatherService.batchFetchForecasts(stops, dedupCache);

        assertEquals(2, result.size());
        assertEquals("clear sky", result.get(0).getCondition());
        assertEquals("clear sky", result.get(1).getCondition());
        assertEquals(30.0, result.get(0).getTemperature());
    }
}
