package com.endfielders.erl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service loading bundled cities.json dataset for zero-latency, offline geographic lookups
 * and mathematical vector pathfinding across India.
 */
@Service
public class CityDataService {

    public static class CityInfo {
        private String city;
        private String pincode;
        private String state;
        private double lat;
        private double lng;

        public CityInfo() {}

        public CityInfo(String city, String pincode, String state, double lat, double lng) {
            this.city = city;
            this.pincode = pincode;
            this.state = state;
            this.lat = lat;
            this.lng = lng;
        }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
    }

    private final List<CityInfo> allCities = new ArrayList<>();
    private final Map<String, CityInfo> pincodeMap = new ConcurrentHashMap<>();
    private final Map<String, CityInfo> cityMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("cities.json");
            if (resource.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                try (InputStream is = resource.getInputStream()) {
                    List<CityInfo> list = mapper.readValue(is, new TypeReference<>() {});
                    for (CityInfo c : list) {
                        allCities.add(c);
                        if (c.getPincode() != null) {
                            pincodeMap.put(c.getPincode().trim(), c);
                        }
                        if (c.getCity() != null) {
                            cityMap.put(c.getCity().trim().toLowerCase(), c);
                        }
                    }
                    System.out.println("✅ Loaded " + allCities.size() + " cities from bundled cities.json");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load cities.json: " + e.getMessage());
        }
    }

    public CityInfo findByPincode(String pincode) {
        if (pincode == null || pincode.isBlank()) return null;
        String clean = pincode.trim();

        // Direct pincode match
        if (pincodeMap.containsKey(clean)) {
            return pincodeMap.get(clean);
        }

        // Prefix match (first 2 digits)
        if (clean.length() >= 2) {
            String prefix = clean.substring(0, 2);
            for (CityInfo c : allCities) {
                if (c.getPincode() != null && c.getPincode().startsWith(prefix)) {
                    return c;
                }
            }
        }

        return new CityInfo("Delhi Hub", "110001", "Delhi", 28.6139, 77.2090);
    }

    public CityInfo findByCityName(String cityName) {
        if (cityName == null || cityName.isBlank()) return null;
        String clean = cityName.replaceAll("\\(.*?\\)", "").trim().toLowerCase();
        if (cityMap.containsKey(clean)) {
            return cityMap.get(clean);
        }
        for (Map.Entry<String, CityInfo> entry : cityMap.entrySet()) {
            if (entry.getKey().contains(clean) || clean.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Calculates mathematical vector trajectory between origin and destination,
     * returning the closest real Indian hub cities along the shortest geographical line.
     */
    public List<CityInfo> calculateVectorPath(String originPincode, String destPincode, int totalDays) {
        List<CityInfo> path = new ArrayList<>();
        CityInfo originCity = findByPincode(originPincode);
        CityInfo destCity = findByPincode(destPincode);

        if (originCity == null) originCity = new CityInfo("Delhi", "110001", "Delhi", 28.6139, 77.2090);
        if (destCity == null) destCity = new CityInfo("Mumbai", "400001", "Maharashtra", 18.9667, 72.8333);

        path.add(originCity);

        if (totalDays <= 1) {
            path.add(destCity);
            return path;
        }

        int intermediateSteps = totalDays - 1;
        Set<String> chosenCities = new HashSet<>();
        chosenCities.add(originCity.getCity().toLowerCase());
        chosenCities.add(destCity.getCity().toLowerCase());

        double startLat = originCity.getLat();
        double startLng = originCity.getLng();
        double endLat = destCity.getLat();
        double endLng = destCity.getLng();

        for (int step = 1; step <= intermediateSteps; step++) {
            double fraction = (double) step / (double) totalDays;
            double targetLat = startLat + fraction * (endLat - startLat);
            double targetLng = startLng + fraction * (endLng - startLng);

            CityInfo bestMatch = null;
            double minDistance = Double.MAX_VALUE;

            for (CityInfo candidate : allCities) {
                if (chosenCities.contains(candidate.getCity().toLowerCase())) continue;

                double dist = distance(targetLat, targetLng, candidate.getLat(), candidate.getLng());
                if (dist < minDistance) {
                    minDistance = dist;
                    bestMatch = candidate;
                }
            }

            if (bestMatch != null) {
                chosenCities.add(bestMatch.getCity().toLowerCase());
                path.add(bestMatch);
            } else {
                path.add(originCity);
            }
        }

        path.add(destCity);
        return path;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c; // Distance in KM
    }
}
