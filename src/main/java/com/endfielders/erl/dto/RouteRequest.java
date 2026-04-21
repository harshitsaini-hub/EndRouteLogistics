package com.endfielders.erl.dto;
import io.swagger.v3.oas.annotations.media.Schema;

public class RouteRequest {
    
    @Schema(example = "110001")
    private String origin;
    @Schema(example = "400001")
    private String destination;
    @Schema(example = "electronics")
    private String cargoType;
    private String timestamp;

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    
        public String getTimestamp() {
        return timestamp;}

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;}

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }
}