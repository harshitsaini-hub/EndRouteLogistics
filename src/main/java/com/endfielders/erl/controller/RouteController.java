package com.endfielders.erl.controller;

import com.endfielders.erl.dto.RouteRequest;
import com.endfielders.erl.dto.RouteResponse;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.service.CarrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Route API", description = "Analyze logistics routes")
@RestController
@RequestMapping("/api/route")
@CrossOrigin(origins = "*")
public class RouteController {

    private final CarrierService carrierService;

    public RouteController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    @Operation(summary = "Analyze route and get ranked carriers")
    @PostMapping("/analyze")
    public RouteResponse analyzeRoute(@Valid @RequestBody RouteRequest request) {

        // Resolve effective cargo type (uses customCargoType when cargoType is "Other")
        String effectiveCargoType = request.getEffectiveCargoType();

        // getRankedCarriers now runs weather + both Gemini calls in parallel
        List<RankedCarrier> ranked = carrierService.getRankedCarriers(
                request.getOrigin(),
                request.getDestination(),
                effectiveCargoType,
                request.getPriority(),
                request.isFragile() != null && request.isFragile(),
                request.isPerishable() != null && request.isPerishable()
        );

        // Route insight was already computed in parallel — no extra API call needed
        String routeInsight = carrierService.getLastRouteInsight();

        RouteResponse response = new RouteResponse();
        response.setOrigin(request.getOrigin());
        response.setDestination(request.getDestination());
        response.setCargoType(effectiveCargoType);
        response.setRouteInsight(routeInsight);
        response.setStatus("SUCCESS");
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.setCarriers(ranked);

        return response;
    }
}