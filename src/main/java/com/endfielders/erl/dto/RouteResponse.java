package com.endfielders.erl.dto;

import com.endfielders.erl.model.RankedCarrier;
import java.util.List;

public class RouteResponse {

    private String origin;
    private String destination;
    private String cargoType;
    private String routeInsight;
    private String timestamp;
    private String status;

    private List<RankedCarrier> carriers;

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }

    public String getRouteInsight() { return routeInsight; }
    public void setRouteInsight(String routeInsight) { this.routeInsight = routeInsight; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<RankedCarrier> getCarriers() { return carriers; }
    public void setCarriers(List<RankedCarrier> carriers) { this.carriers = carriers; }
    }