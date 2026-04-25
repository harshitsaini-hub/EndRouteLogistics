package com.endfielders.erl.controller;

import com.endfielders.erl.dto.RouteRequest;
import com.endfielders.erl.dto.RouteResponse;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.service.CarrierService;
import com.endfielders.erl.service.GeminiService;
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
    private final GeminiService geminiService;

    public RouteController(CarrierService carrierService, GeminiService geminiService) {
        this.carrierService = carrierService;
        this.geminiService = geminiService;
    }

    @Operation(summary = "Analyze route and get ranked carriers")
    @PostMapping("/analyze")
    public RouteResponse analyzeRoute(@Valid @RequestBody RouteRequest request) {

        List<RankedCarrier> ranked = carrierService.getRankedCarriers(
                request.getOrigin(),
                request.getDestination(),
                request.getCargoType(),
                request.getPriority(),
                Boolean.TRUE.equals(request.isFragile()),
                Boolean.TRUE.equals(request.isPerishable())
        );

        String routeInsight = geminiService.analyzeRoute(
                request.getOrigin(),
                request.getDestination(),
                request.getCargoType()
        );
        RouteResponse response = new RouteResponse();
        response.setOrigin(request.getOrigin());
        response.setDestination(request.getDestination());
        response.setCargoType(request.getCargoType());
        response.setRouteInsight(routeInsight);
        response.setStatus("SUCCESS");
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.setCarriers(ranked);

        return response;
    }
}