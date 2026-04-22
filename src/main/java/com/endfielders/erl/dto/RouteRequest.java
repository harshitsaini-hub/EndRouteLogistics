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

    @Schema(example = "FASTEST")
    private String priority;

    @Schema(example = "true")
    private boolean fragile;

    @Schema(example = "false")
    private boolean perishable;

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

    public String getPriority() {
    return priority;
}

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isFragile() {
        return fragile;
    }

    public void setFragile(boolean fragile) {
        this.fragile = fragile;
    }

    public boolean isPerishable() {
        return perishable;
    }

    public void setPerishable(boolean perishable) {
        this.perishable = perishable;
}
}