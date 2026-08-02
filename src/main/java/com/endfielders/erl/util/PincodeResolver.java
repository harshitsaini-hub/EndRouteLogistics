package com.endfielders.erl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for resolving Indian 6-digit pincodes to true City/District names,
 * assigning distinct PIN codes per city, and interpolating geographically accurate
 * directional transit corridors across India.
 */
public class PincodeResolver {

    private static final Map<String, String> PINCODE_TO_CITY = new HashMap<>();
    private static final Map<String, String> CITY_TO_PINCODE = new HashMap<>();

    static {
        // Specific Major Cities
        addMapping("110001", "Delhi");
        addMapping("110002", "Delhi");
        addMapping("110020", "Delhi");
        addMapping("400001", "Mumbai");
        addMapping("400050", "Mumbai");
        addMapping("560001", "Bengaluru");
        addMapping("560002", "Bengaluru");
        addMapping("700001", "Kolkata");
        addMapping("600001", "Chennai");
        addMapping("500001", "Hyderabad");
        addMapping("380001", "Ahmedabad");
        addMapping("302001", "Jaipur");
        addMapping("452001", "Indore");
        addMapping("462001", "Bhopal");
        addMapping("465693", "Susner");
        addMapping("201301", "Noida");
        addMapping("122001", "Gurgaon");
        addMapping("411001", "Pune");
        addMapping("395001", "Surat");
        addMapping("390001", "Vadodara");
        addMapping("141001", "Ludhiana");
        addMapping("160017", "Chandigarh");
        addMapping("226001", "Lucknow");
        addMapping("208001", "Kanpur");
        addMapping("221001", "Varanasi");
        addMapping("282001", "Agra");
        addMapping("474001", "Gwalior");
        addMapping("482001", "Jabalpur");
        addMapping("492001", "Raipur");
        addMapping("751001", "Bhubaneswar");
        addMapping("781001", "Guwahati");
        addMapping("800001", "Patna");
        addMapping("834001", "Ranchi");
        addMapping("826001", "Dhanbad");
        addMapping("682001", "Kochi");
        addMapping("695001", "Trivandrum");
        addMapping("641001", "Coimbatore");
        addMapping("440001", "Nagpur");
        addMapping("342001", "Jodhpur");
        addMapping("422001", "Nashik");
    }

    private static void addMapping(String pincode, String city) {
        PINCODE_TO_CITY.put(pincode, city);
        CITY_TO_PINCODE.putIfAbsent(city.toLowerCase(), pincode);
    }

    public static String resolveCity(String pincode) {
        if (pincode == null || pincode.trim().length() < 2) {
            return "Transit Hub";
        }
        String p = pincode.trim();

        // 1. Direct match
        if (PINCODE_TO_CITY.containsKey(p)) {
            return PINCODE_TO_CITY.get(p);
        }

        // 2. Prefix match (First 2 digits)
        String prefix2 = p.substring(0, 2);
        return switch (prefix2) {
            case "11" -> "Delhi";
            case "12", "13" -> "Gurgaon";
            case "14", "15" -> "Ludhiana";
            case "16" -> "Chandigarh";
            case "17" -> "Shimla";
            case "18", "19" -> "Jammu";
            case "20" -> "Noida";
            case "21", "22", "23" -> "Lucknow";
            case "24", "25", "26" -> "Dehradun";
            case "27", "28" -> "Agra";
            case "30", "31" -> "Jaipur";
            case "32", "33", "34" -> "Jodhpur";
            case "36", "37", "38" -> "Ahmedabad";
            case "39" -> "Surat";
            case "40" -> "Mumbai";
            case "41" -> "Pune";
            case "42" -> "Nashik";
            case "43", "44" -> "Nagpur";
            case "45" -> "Indore";
            case "46" -> "Bhopal";
            case "47", "48" -> "Gwalior";
            case "49" -> "Raipur";
            case "50", "51", "52", "53" -> "Hyderabad";
            case "56", "57", "58", "59" -> "Bengaluru";
            case "60", "61", "62", "63", "64" -> "Chennai";
            case "67", "68", "69" -> "Kochi";
            case "70", "71", "72", "73", "74" -> "Kolkata";
            case "75", "76", "77" -> "Bhubaneswar";
            case "78", "79" -> "Guwahati";
            case "80", "81", "82" -> "Patna";
            case "83", "84", "85" -> "Ranchi";
            default -> "India Hub";
        };
    }

    /**
     * Returns a distinct 6-digit Indian PIN code for a given city name.
     */
    public static String getCityPincode(String cityName) {
        if (cityName == null || cityName.isBlank()) return "110001";
        String clean = cityName.replaceAll("\\(.*?\\)", "").trim().toLowerCase();
        if (CITY_TO_PINCODE.containsKey(clean)) {
            return CITY_TO_PINCODE.get(clean);
        }
        // General defaults by key city patterns
        if (clean.contains("delhi")) return "110001";
        if (clean.contains("mumbai")) return "400001";
        if (clean.contains("bhopal")) return "462001";
        if (clean.contains("gwalior")) return "474001";
        if (clean.contains("agra")) return "282001";
        if (clean.contains("jaipur")) return "302001";
        if (clean.contains("ahmedabad")) return "380001";
        if (clean.contains("surat")) return "395001";
        if (clean.contains("indore")) return "452001";
        if (clean.contains("nagpur")) return "440001";
        if (clean.contains("hyderabad")) return "500001";
        if (clean.contains("bengaluru") || clean.contains("bangalore")) return "560001";
        if (clean.contains("chennai")) return "600001";
        if (clean.contains("kolkata")) return "700001";
        if (clean.contains("kanpur")) return "208001";
        if (clean.contains("varanasi")) return "221001";
        if (clean.contains("patna")) return "800001";
        if (clean.contains("ranchi")) return "834001";
        if (clean.contains("raipur")) return "492001";
        if (clean.contains("pune")) return "411001";

        return "110001";
    }

    /**
     * Interpolates geographically accurate, directional intermediate cities along Indian transit corridors.
     */
    public static List<String> getIntermediateHubs(String originPincode, String destPincode, int count) {
        String originCity = resolveCity(originPincode);
        String destCity = resolveCity(destPincode);

        // 1. Bhopal <-> Delhi Corridor (Northward / Southward)
        if (originCity.equals("Bhopal") && destCity.equals("Delhi")) {
            return selectSubset(List.of("Gwalior", "Agra"), count);
        }
        if (originCity.equals("Delhi") && destCity.equals("Bhopal")) {
            return selectSubset(List.of("Agra", "Gwalior"), count);
        }

        // 2. Delhi <-> Mumbai Corridor (South-West / North-East)
        if (originCity.equals("Delhi") && destCity.equals("Mumbai")) {
            return selectSubset(List.of("Jaipur", "Ahmedabad", "Vadodara", "Surat"), count);
        }
        if (originCity.equals("Mumbai") && destCity.equals("Delhi")) {
            return selectSubset(List.of("Surat", "Vadodara", "Ahmedabad", "Jaipur"), count);
        }

        // 3. Bhopal <-> Mumbai Corridor
        if (originCity.equals("Bhopal") && destCity.equals("Mumbai")) {
            return selectSubset(List.of("Indore", "Nashik"), count);
        }
        if (originCity.equals("Mumbai") && destCity.equals("Bhopal")) {
            return selectSubset(List.of("Nashik", "Indore"), count);
        }

        // 4. Delhi <-> Bengaluru Corridor
        if (originCity.equals("Delhi") && destCity.equals("Bengaluru")) {
            return selectSubset(List.of("Agra", "Gwalior", "Bhopal", "Nagpur", "Hyderabad"), count);
        }
        if (originCity.equals("Bengaluru") && destCity.equals("Delhi")) {
            return selectSubset(List.of("Hyderabad", "Nagpur", "Bhopal", "Gwalior", "Agra"), count);
        }

        // 5. Delhi <-> Kolkata Corridor
        if (originCity.equals("Delhi") && destCity.equals("Kolkata")) {
            return selectSubset(List.of("Kanpur", "Varanasi", "Patna"), count);
        }
        if (originCity.equals("Kolkata") && destCity.equals("Delhi")) {
            return selectSubset(List.of("Patna", "Varanasi", "Kanpur"), count);
        }

        // 6. Mumbai <-> Kolkata Corridor
        if (originCity.equals("Mumbai") && destCity.equals("Kolkata")) {
            return selectSubset(List.of("Nagpur", "Raipur", "Ranchi"), count);
        }
        if (originCity.equals("Kolkata") && destCity.equals("Mumbai")) {
            return selectSubset(List.of("Ranchi", "Raipur", "Nagpur"), count);
        }

        // Default directional hubs
        List<String> defaults = List.of("Gwalior", "Agra", "Jaipur", "Ahmedabad", "Nagpur");
        return selectSubset(defaults, count);
    }

    private static List<String> selectSubset(List<String> list, int count) {
        if (count <= 0) return List.of();
        if (count >= list.size()) return list;
        return list.subList(0, count);
    }
}
