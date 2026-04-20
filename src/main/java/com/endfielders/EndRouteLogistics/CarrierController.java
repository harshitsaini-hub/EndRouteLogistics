package com.endfielders.EndRouteLogistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CarrierController {

    @Autowired
    private CarrierService carrierService;

    @PostMapping("/carriers")
    public ResponseEntity<?> getCarriers(@RequestBody CarrierRequest request) {
        // Validate pincodes
        if (request.getOriginPincode() == null || 
            !request.getOriginPincode().matches("[0-9]{6}")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid origin pincode"));
        }
        if (request.getDestinationPincode() == null || 
            !request.getDestinationPincode().matches("[0-9]{6}")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid destination pincode"));
        }
        return ResponseEntity.ok(carrierService.getRankedCarriers(
            request.getOriginPincode(),
            request.getDestinationPincode(),
            request.getCargoType()
        ));
    }

    static class CarrierRequest {
        private String originPincode;
        private String destinationPincode;
        private String cargoType;

        public String getOriginPincode() { return originPincode; }
        public void setOriginPincode(String o) { this.originPincode = o; }
        public String getDestinationPincode() { return destinationPincode; }
        public void setDestinationPincode(String d) { this.destinationPincode = d; }
        public String getCargoType() { return cargoType; }
        public void setCargoType(String c) { this.cargoType = c; }
    }
}