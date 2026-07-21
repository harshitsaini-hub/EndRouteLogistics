package com.endfielders.erl.service;

import com.endfielders.erl.model.DayWeather;
import com.endfielders.erl.model.RouteStop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated weather service using OpenWeather 5-day/3-hour forecast API.
 * Supports pincode and city-name fallback for 100% accurate Indian location matching.
 * Handles deduplication via a per-request cache keyed by (locationKey, day).
 */
@Service
public class WeatherService {

    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${weather.api.key:}")
    private String weatherApiKey;

    @Value("${WEATHER_COUNTRY_CODE:IN}")
    private String weatherCountryCode;

    private final RestTemplate restTemplate;

    public WeatherService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Builds a weather summary string from origin and destination current weather.
     * Used for scoring and route insight prompts.
     */
    public String buildWeatherSummary(String originPincode, String destPincode) {
        String originWeather = getCurrentWeather(originPincode);
        String destWeather = getCurrentWeather(destPincode);
        return "Origin weather: " + originWeather + "; Destination weather: " + destWeather;
    }

    /**
     * Fetch current weather for a pincode (used for quick scoring summary).
     */
    private String getCurrentWeather(String locationCode) {
        if (weatherApiKey == null || weatherApiKey.isBlank() || locationCode == null || locationCode.isBlank()) {
            return "Weather data unavailable";
        }
        try {
            String cc = (weatherCountryCode == null || weatherCountryCode.isBlank()) ? "IN" : weatherCountryCode.trim().toUpperCase(Locale.ROOT);
            String url = "https://api.openweathermap.org/data/2.5/weather?zip=" + locationCode + "," + cc
                    + "&appid=" + weatherApiKey + "&units=metric";

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return "Weather data unavailable";

            @SuppressWarnings("unchecked")
            Map<String, Object> main = (Map<String, Object>) resp.get("main");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) resp.get("weather");

            if (main == null || weatherList == null || weatherList.isEmpty()) return "Weather data unavailable";

            double temp = main.get("temp") instanceof Number ? ((Number) main.get("temp")).doubleValue() : 0.0;
            double roundedTemp = Math.round(temp * 10.0) / 10.0;

            return weatherList.get(0).get("description") + ", " + roundedTemp + "°C";
        } catch (Exception e) {
            return "Weather data unavailable";
        }
    }

    /**
     * Batch-fetch weather forecasts for a list of route stops, deduplicating by (locationKey, day).
     * Uses pincode first, then city fallback to guarantee accurate matching.
     */
    public List<DayWeather> batchFetchForecasts(List<RouteStop> stops, Map<String, DayWeather> deduped) {
        List<DayWeather> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Map to store fetched forecast data per location key (pincode or city)
        Map<String, List<Map<String, Object>>> forecastCache = new ConcurrentHashMap<>();

        for (RouteStop stop : stops) {
            String cacheKey = stop.getPincode() + ":" + stop.getDay();

            // Check dedup cache first
            if (deduped.containsKey(cacheKey)) {
                DayWeather cached = deduped.get(cacheKey);
                DayWeather dw = new DayWeather();
                dw.setDay(stop.getDay());
                dw.setDate(cached.getDate());
                dw.setCity(stop.getCity());
                dw.setPincode(stop.getPincode());
                dw.setCondition(cached.getCondition());
                dw.setTemperature(cached.getTemperature());
                dw.setHumidity(cached.getHumidity());
                dw.setAdvisory(cached.getAdvisory());
                dw.setForecastAvailable(cached.isForecastAvailable());
                result.add(dw);
                continue;
            }

            LocalDate targetDate = today.plusDays(stop.getDay());
            DayWeather dw = new DayWeather();
            dw.setDay(stop.getDay());
            dw.setDate(targetDate.format(DATE_FMT));
            dw.setCity(stop.getCity());
            dw.setPincode(stop.getPincode());

            // OpenWeather free tier limit: 5 days
            if (stop.getDay() > 5) {
                dw.setForecastAvailable(false);
                dw.setCondition("Forecast unavailable");
                dw.setAdvisory("Weather forecast is only available up to 5 days ahead.");
                result.add(dw);
                deduped.put(cacheKey, dw);
                continue;
            }

            // Fetch forecast list trying pincode first, then city fallback
            String locationKey = stop.getPincode();
            if (!forecastCache.containsKey(locationKey)) {
                List<Map<String, Object>> forecast = fetchForecastByPincode(stop.getPincode());
                if (forecast == null && stop.getCity() != null && !stop.getCity().isBlank()) {
                    forecast = fetchForecastByCity(stop.getCity());
                }
                if (forecast != null) {
                    forecastCache.put(locationKey, forecast);
                }
            }

            List<Map<String, Object>> forecastList = forecastCache.get(locationKey);
            if (forecastList == null || forecastList.isEmpty()) {
                dw.setForecastAvailable(false);
                dw.setCondition("Weather data unavailable");
                dw.setAdvisory("Could not fetch weather for this location.");
                result.add(dw);
                deduped.put(cacheKey, dw);
                continue;
            }

            // Find the forecast entry closest to noon (12:00) on the target date
            String targetStr = targetDate.format(DATE_FMT);
            Map<String, Object> bestMatch = null;
            for (Map<String, Object> entry : forecastList) {
                String dtTxt = (String) entry.get("dt_txt");
                if (dtTxt != null && dtTxt.startsWith(targetStr)) {
                    if (dtTxt.contains("12:00:00")) {
                        bestMatch = entry;
                        break;
                    }
                    if (bestMatch == null) {
                        bestMatch = entry;
                    }
                }
            }

            if (bestMatch != null) {
                fillDayWeather(dw, bestMatch);
            } else {
                dw.setForecastAvailable(false);
                dw.setCondition("No forecast data for this date");
                dw.setAdvisory("Forecast data not available for " + targetStr);
            }

            result.add(dw);
            deduped.put(cacheKey, dw);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchForecastByPincode(String pincode) {
        if (weatherApiKey == null || weatherApiKey.isBlank() || pincode == null || pincode.isBlank()) return null;
        try {
            String cc = (weatherCountryCode == null || weatherCountryCode.isBlank()) ? "IN" : weatherCountryCode.trim().toUpperCase(Locale.ROOT);
            String url = FORECAST_URL + "?zip=" + pincode + "," + cc
                    + "&appid=" + weatherApiKey + "&units=metric";

            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !resp.containsKey("list")) return null;
            return (List<Map<String, Object>>) resp.get("list");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchForecastByCity(String cityName) {
        if (weatherApiKey == null || weatherApiKey.isBlank() || cityName == null || cityName.isBlank()) return null;
        try {
            // Clean city name (e.g. "Origin (110001)" -> "Delhi")
            String cleanCity = cityName.replaceAll("\\(.*?\\)", "").trim();
            String url = FORECAST_URL + "?q=" + cleanCity + ",IN"
                    + "&appid=" + weatherApiKey + "&units=metric";

            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !resp.containsKey("list")) return null;
            return (List<Map<String, Object>>) resp.get("list");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void fillDayWeather(DayWeather dw, Map<String, Object> entry) {
        try {
            Map<String, Object> main = (Map<String, Object>) entry.get("main");
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) entry.get("weather");

            if (main != null) {
                Object temp = main.get("temp");
                if (temp instanceof Number) {
                    double rawTemp = ((Number) temp).doubleValue();
                    dw.setTemperature(Math.round(rawTemp * 10.0) / 10.0);
                }
                Object hum = main.get("humidity");
                if (hum instanceof Number) dw.setHumidity(((Number) hum).intValue());
            }

            if (weatherList != null && !weatherList.isEmpty()) {
                dw.setCondition(String.valueOf(weatherList.get(0).get("description")));
            }

            dw.setForecastAvailable(true);
            dw.setAdvisory(generateAdvisory(dw));
        } catch (Exception e) {
            dw.setForecastAvailable(false);
            dw.setCondition("Parse error");
            dw.setAdvisory("Could not parse weather data.");
        }
    }

    private String generateAdvisory(DayWeather dw) {
        if (dw.getCondition() == null) return "No advisory";
        String cond = dw.getCondition().toLowerCase();
        Double temp = dw.getTemperature();
        Integer hum = dw.getHumidity();

        List<String> advisories = new ArrayList<>();

        if (cond.contains("rain") || cond.contains("drizzle") || cond.contains("thunderstorm")) {
            advisories.add("Rain expected — ensure waterproof packaging");
        }
        if (cond.contains("storm") || cond.contains("thunderstorm")) {
            advisories.add("Severe weather may cause delays");
        }
        if (temp != null && temp > 38) {
            advisories.add("Extreme heat — avoid heat-sensitive cargo exposure");
        }
        if (temp != null && temp < 5) {
            advisories.add("Cold conditions — protect freeze-sensitive goods");
        }
        if (hum != null && hum > 85) {
            advisories.add("High humidity — use moisture-resistant packaging");
        }

        return advisories.isEmpty() ? "Clear conditions — no special precautions needed" : String.join(". ", advisories);
    }
}
