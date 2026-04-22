package com.endfielders.erl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RouteRequest {
    
    @NotBlank(message = "Origin pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid origin pincode")
    @Schema(example = "110001")
    private String origin;

    @NotBlank(message = "Destination pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid destination pincode")
    @Schema(example = "400001")
    private String destination;

    @NotBlank(message = "Destination pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid destination pincode")
    @Schema(example = "electronics")
    private String cargoType;

    private String timestamp;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCargoType() {
        return cargoType;
    }

    public void setCargoType(String cargoType) {
        this.cargoType = cargoType;
    }
}