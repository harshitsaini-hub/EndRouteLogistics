package com.endfielders.erl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for resolving Indian 6-digit pincodes to true City/District names
 * and interpolating realistic transit hubs along Indian logistics corridors.
 */
public class PincodeResolver {

    private static final Map<String, String> PINCODE_TO_CITY = new HashMap<>();

    static {
        // Specific Major Cities
        PINCODE_TO_CITY.put("110001", "Delhi");
        PINCODE_TO_CITY.put("110002", "Delhi");
        PINCODE_TO_CITY.put("110020", "Delhi");
        PINCODE_TO_CITY.put("400001", "Mumbai");
        PINCODE_TO_CITY.put("400050", "Mumbai");
        PINCODE_TO_CITY.put("560001", "Bengaluru");
        PINCODE_TO_CITY.put("560002", "Bengaluru");
        PINCODE_TO_CITY.put("700001", "Kolkata");
        PINCODE_TO_CITY.put("600001", "Chennai");
        PINCODE_TO_CITY.put("500001", "Hyderabad");
        PINCODE_TO_CITY.put("380001", "Ahmedabad");
        PINCODE_TO_CITY.put("302001", "Jaipur");
        PINCODE_TO_CITY.put("452001", "Indore");
        PINCODE_TO_CITY.put("462001", "Bhopal");
        PINCODE_TO_CITY.put("465693", "Susner");
        PINCODE_TO_CITY.put("201301", "Noida");
        PINCODE_TO_CITY.put("122001", "Gurgaon");
        PINCODE_TO_CITY.put("411001", "Pune");
        PINCODE_TO_CITY.put("395001", "Surat");
        PINCODE_TO_CITY.put("141001", "Ludhiana");
        PINCODE_TO_CITY.put("160017", "Chandigarh");
        PINCODE_TO_CITY.put("226001", "Lucknow");
        PINCODE_TO_CITY.put("282001", "Agra");
        PINCODE_TO_CITY.put("474001", "Gwalior");
        PINCODE_TO_CITY.put("492001", "Raipur");
        PINCODE_TO_CITY.put("751001", "Bhubaneswar");
        PINCODE_TO_CITY.put("781001", "Guwahati");
        PINCODE_TO_CITY.put("800001", "Patna");
        PINCODE_TO_CITY.put("834001", "Ranchi");
        PINCODE_TO_CITY.put("682001", "Kochi");
        PINCODE_TO_CITY.put("695001", "Trivandrum");
        PINCODE_TO_CITY.put("641001", "Coimbatore");
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
     * Interpolates realistic intermediate cities along Indian transit corridors
     * when AI is slow or rate-limited.
     */
    public static List<String> getIntermediateHubs(String originPincode, String destPincode, int count) {
        String originCity = resolveCity(originPincode);
        String destCity = resolveCity(destPincode);

        // Major national freight corridors
        if ((originCity.equals("Delhi") && destCity.equals("Mumbai")) || (originCity.equals("Mumbai") && destCity.equals("Delhi"))) {
            return List.of("Jaipur", "Ahmedabad", "Vadodara", "Surat").subList(0, Math.min(count, 4));
        }
        if ((originCity.equals("Delhi") && destCity.equals("Bengaluru")) || (originCity.equals("Bengaluru") && destCity.equals("Delhi"))) {
            return List.of("Gwalior", "Bhopal", "Nagpur", "Hyderabad").subList(0, Math.min(count, 4));
        }
        if ((originCity.equals("Delhi") && destCity.equals("Kolkata")) || (originCity.equals("Kolkata") && destCity.equals("Delhi"))) {
            return List.of("Agra", "Kanpur", "Varanasi", "Dhanbad").subList(0, Math.min(count, 4));
        }

        // Default major logistics hubs
        List<String> defaults = List.of("Jaipur", "Bhopal", "Nagpur", "Hyderabad", "Pune");
        return defaults.subList(0, Math.min(count, defaults.size()));
    }
}
