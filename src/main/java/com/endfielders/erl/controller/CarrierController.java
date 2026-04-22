package com.endfielders.erl.controller;

import com.endfielders.erl.dto.CarrierRequest;
import com.endfielders.erl.service.CarrierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CarrierController {

    @Autowired
    private CarrierService carrierService;

    @PostMapping("/carriers")
    public ResponseEntity<?> getCarriers(@Valid @RequestBody CarrierRequest request) {
        return ResponseEntity.ok(carrierService.getRankedCarriers(
                request.getOriginPincode(),
                request.getDestinationPincode(),
                request.getCargoType()
        ));
    }
}