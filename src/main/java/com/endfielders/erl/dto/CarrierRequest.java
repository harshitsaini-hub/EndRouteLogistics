package com.endfielders.erl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CarrierRequest {

    @NotBlank(message = "Origin pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid origin pincode")
    private String originPincode;

    @NotBlank(message = "Destination pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid destination pincode")
    private String destinationPincode;

    @NotBlank(message = "Cargo type is required")
    private String cargoType;

    public String getOriginPincode() { return originPincode; }
    public void setOriginPincode(String originPincode) { this.originPincode = originPincode; }

    public String getDestinationPincode() { return destinationPincode; }
    public void setDestinationPincode(String destinationPincode) { this.destinationPincode = destinationPincode; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }
}