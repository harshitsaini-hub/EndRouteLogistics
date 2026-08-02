package com.endfielders.erl.service;

import com.endfielders.erl.util.CategoryResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service loading bundled cities.json dataset using modern Java record,
 * deterministic mode suitability rules, and spatial bounding box pathfinding.
 */
@Service
public class CityDataService {

    public record City(String city, String pincode, String state, double lat, double lng) {}

    private Map<String, City> cityDb = new HashMap<>();
    private Map<String, City> pincodeDb = new HashMap<>();
    private List<City> allCities = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/cities.json");
            if (is == null) {
                ClassPathResource resource = new ClassPathResource("cities.json");
                is = resource.getInputStream();
            }
            List<City> cities = mapper.readValue(is, new TypeReference<List<City>>() {});
            this.allCities = cities;
            this.cityDb = cities.stream()
                    .collect(Collectors.toMap(c -> c.city().toLowerCase(), Function.identity(), (a, b) -> a));
            this.pincodeDb = cities.stream()
                    .collect(Collectors.toMap(City::pincode, Function.identity(), (a, b) -> a));
            System.out.println("✅ Loaded " + cities.size() + " cities into CityDataService");
        } catch (Exception e) {
            System.err.println("❌ Failed to load cities.json: " + e.getMessage());
        }
    }

    public City findByCityName(String cityName) {
        if (cityName == null || cityName.isBlank()) return null;
        String clean = cityName.replaceAll("\\(.*?\\)", "").trim().toLowerCase();
        if (cityDb.containsKey(clean)) return cityDb.get(clean);
        for (Map.Entry<String, City> entry : cityDb.entrySet()) {
            if (entry.getKey().contains(clean) || clean.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public City findByPincode(String pincode) {
        if (pincode == null || pincode.isBlank()) return null;
        String clean = pincode.trim();
        if (pincodeDb.containsKey(clean)) return pincodeDb.get(clean);
        if (clean.length() >= 2) {
            String prefix = clean.substring(0, 2);
            for (City c : allCities) {
                if (c.pincode().startsWith(prefix)) return c;
            }
        }
        return pincodeDb.getOrDefault("110001", new City("Delhi", "110001", "Delhi", 28.6139, 77.2090));
    }

    public String getPincode(String cityName) {
        City c = findByCityName(cityName);
        return c != null ? c.pincode() : "110001";
    }

    /**
     * Deterministic Rule-Based Cargo Mode Suitability Engine (0ms, 100% offline).
     */
    public Map<String, Integer> getCargoModeSuitability(String cargoType) {
        String category = CategoryResolver.resolve(cargoType);
        if (category == null) category = "GENERAL";

        return switch (category) {
            case "E_COMMERCE"  -> Map.of("Air", 90, "Road", 75, "Rail", 55);
            case "COLD_CHAIN"  -> Map.of("Air", 85, "Road", 90, "Rail", 45);
            case "B2B_FREIGHT" -> Map.of("Air", 30, "Road", 90, "Rail", 85);
            case "HOUSEHOLD"   -> Map.of("Air", 20, "Road", 95, "Rail", 50);
            default            -> Map.of("Air", 70, "Road", 80, "Rail", 75);
        };
    }

    public double calculateDistance(City c1, City c2) {
        if (c1 == null || c2 == null) return -1.0;

        double lat1 = Math.toRadians(c1.lat());
        double lon1 = Math.toRadians(c1.lng());
        double lat2 = Math.toRadians(c2.lat());
        double lon2 = Math.toRadians(c2.lng());

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        // Haversine formula
        double a = Math.pow(Math.sin(dlat / 2), 2)
                 + Math.cos(lat1) * Math.cos(lat2)
                 * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 6371; // Radius of Earth in KM
        return c * r;
    }

    /**
     * Spatial Bounding Box Filter:
     * Narrows down the dataset to 3-5 candidate hubs geographically located
     * inside the rectangular corridor between origin and destination.
     */
    public List<City> findCandidateHubs(String originPincode, String destPincode, int maxCandidates) {
        City origin = findByPincode(originPincode);
        City dest = findByPincode(destPincode);

        if (origin == null) origin = findByPincode("110001");
        if (dest == null) dest = findByPincode("400001");

        double minLat = Math.min(origin.lat(), dest.lat()) - 0.6;
        double maxLat = Math.max(origin.lat(), dest.lat()) + 0.6;
        double minLng = Math.min(origin.lng(), dest.lng()) - 0.6;
        double maxLng = Math.max(origin.lng(), dest.lng()) + 0.6;

        Set<String> exclude = Set.of(origin.city().toLowerCase(), dest.city().toLowerCase());

        List<City> candidates = new ArrayList<>();
        for (City c : allCities) {
            if (exclude.contains(c.city().toLowerCase())) continue;
            if (c.lat() >= minLat && c.lat() <= maxLat && c.lng() >= minLng && c.lng() <= maxLng) {
                candidates.add(c);
            }
        }

        // Sort candidates by progression projection along the vector
        double startLat = origin.lat(), startLng = origin.lng();
        double deltaLat = dest.lat() - startLat, deltaLng = dest.lng() - startLng;
        double lineLenSq = deltaLat * deltaLat + deltaLng * deltaLng;

        candidates.sort((c1, c2) -> {
            double proj1 = lineLenSq == 0 ? 0 : ((c1.lat() - startLat) * deltaLat + (c1.lng() - startLng) * deltaLng) / lineLenSq;
            double proj2 = lineLenSq == 0 ? 0 : ((c2.lat() - startLat) * deltaLat + (c2.lng() - startLng) * deltaLng) / lineLenSq;
            return Double.compare(proj1, proj2);
        });

        if (candidates.size() > maxCandidates) {
            return candidates.subList(0, maxCandidates);
        }
        return candidates;
    }

    public List<City> calculateVectorPath(String originPincode, String destPincode, int totalDays) {
        List<City> path = new ArrayList<>();
        City originCity = findByPincode(originPincode);
        City destCity = findByPincode(destPincode);

        if (originCity == null) originCity = findByPincode("110001");
        if (destCity == null) destCity = findByPincode("400001");

        path.add(originCity);

        if (totalDays <= 1) {
            path.add(destCity);
            return path;
        }

        int intermediateSteps = totalDays - 1;
        Set<String> chosenCities = new HashSet<>();
        chosenCities.add(originCity.city().toLowerCase());
        chosenCities.add(destCity.city().toLowerCase());

        double startLat = originCity.lat();
        double startLng = originCity.lng();
        double endLat = destCity.lat();
        double endLng = destCity.lng();

        for (int step = 1; step <= intermediateSteps; step++) {
            double fraction = (double) step / (double) totalDays;
            double targetLat = startLat + fraction * (endLat - startLat);
            double targetLng = startLng + fraction * (endLng - startLng);

            City targetPoint = new City("Target", "000000", "State", targetLat, targetLng);
            City bestMatch = null;
            double minDistance = Double.MAX_VALUE;

            for (City candidate : allCities) {
                if (chosenCities.contains(candidate.city().toLowerCase())) continue;

                double dist = calculateDistance(targetPoint, candidate);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestMatch = candidate;
                }
            }

            if (bestMatch != null) {
                chosenCities.add(bestMatch.city().toLowerCase());
                path.add(bestMatch);
            } else {
                path.add(originCity);
            }
        }

        path.add(destCity);
        return path;
    }
}
