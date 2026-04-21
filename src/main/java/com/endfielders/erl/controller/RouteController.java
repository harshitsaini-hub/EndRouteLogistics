package com.endfielders.erl.controller;

import com.endfielders.erl.dto.RouteRequest;
import com.endfielders.erl.dto.RouteResponse;
import com.endfielders.erl.model.RankedCarrier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.endfielders.erl.service.CarrierService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Route API", description = "Analyze logistics routes")
@RestController
@RequestMapping("/api/route")
@CrossOrigin(origins = "*")
public class RouteController {

    @Autowired
    private CarrierService carrierService;
    @Operation(summary = "Analyze route and get ranked carriers")
    @PostMapping("/analyze")
    public RouteResponse analyzeRoute(@RequestBody RouteRequest request) {

        List<RankedCarrier> ranked = carrierService.getRankedCarriers(
                request.getOrigin(),
                request.getDestination(),
                request.getCargoType()
        );

        RouteResponse response = new RouteResponse();
        response.setOrigin(request.getOrigin());
        response.setDestination(request.getDestination());
        response.setCargoType(request.getCargoType());
        response.setStatus("SUCCESS");
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        response.setCarriers(ranked);

        return response;
    }
}