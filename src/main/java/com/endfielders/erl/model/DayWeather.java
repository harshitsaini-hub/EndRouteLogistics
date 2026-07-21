package com.endfielders.erl.model;

/**
 * Weather forecast data for a specific day and location along a shipment journey.
 */
public class DayWeather {
    private int day;
    private String date;
    private String city;
    private String pincode;
    private String condition;
    private Double temperature;
    private Integer humidity;
    private String advisory;
    private boolean forecastAvailable;

    public DayWeather() {}

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }

    public String getAdvisory() { return advisory; }
    public void setAdvisory(String advisory) { this.advisory = advisory; }

    public boolean isForecastAvailable() { return forecastAvailable; }
    public void setForecastAvailable(boolean forecastAvailable) { this.forecastAvailable = forecastAvailable; }
}
