package com.endfielders.EndRouteLogistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CarrierController {

    @Autowired
    private CarrierService carrierService;

    @PostMapping("/carriers")
    public List<Map<String, Object>> getCarriers(@RequestBody CarrierRequest request) {
        return carrierService.getRankedCarriers(
            request.getOriginPincode(),
            request.getDestinationPincode(),
            request.getCargoType()
        );
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