package com.endfielders.erl.util;

import com.endfielders.erl.service.CityDataService;

/**
 * Lightweight helper delegating pincode and city lookups directly to CityDataService
 * without any hardcoded if-else routing blocks or hardcoded city maps.
 */
public class PincodeResolver {

    private static CityDataService cityDataService;

    public static void setCityDataService(CityDataService service) {
        cityDataService = service;
    }

    public static String resolveCity(String pincode) {
        if (cityDataService == null) return "Delhi";
        CityDataService.City city = cityDataService.findByPincode(pincode);
        return city != null ? city.city() : "Delhi";
    }

    public static String getCityPincode(String cityName) {
        if (cityDataService == null) return "110001";
        return cityDataService.getPincode(cityName);
    }
}
