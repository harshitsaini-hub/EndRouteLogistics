package com.endfielders.erl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RouteRequest {
    
    @NotBlank(message = "Origin pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid origin pincode")
    @Schema(example = "110001")
    private String origin;

    @NotBlank(message = "Destination pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid destination pincode")
    @Schema(example = "400001")
    private String destination;

    @NotBlank(message = "Cargo type is required")
    @Size(max = 40, message = "Cargo type must be at most 40 characters")
    @Schema(example = "electronics")
    private String cargoType;

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(FASTEST|CHEAPEST|BALANCED)$", message = "Priority must be FASTEST, CHEAPEST, or BALANCED")
    @Schema(example = "FASTEST")
    private String priority;

    @NotNull(message = "Fragile flag is required")
    @Schema(example = "true")
    private Boolean fragile;

    @NotNull(message = "Perishable flag is required")
    @Schema(example = "false")
    private Boolean perishable;

    private String timestamp;

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Boolean isFragile() { return fragile; }
    public void setFragile(Boolean fragile) { this.fragile = fragile; }

    public Boolean isPerishable() { return perishable; }
    public void setPerishable(Boolean perishable) { this.perishable = perishable; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}