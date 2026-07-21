package com.endfielders.erl.model;

/**
 * Represents an estimated stop along a shipment route on a specific day.
 * Used by Gemini to predict where a shipment would be on each day of transit.
 */
public class RouteStop {
    private int day;
    private String city;
    private String pincode;

    public RouteStop() {}

    public RouteStop(int day, String city, String pincode) {
        this.day = day;
        this.city = city;
        this.pincode = pincode;
    }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}
