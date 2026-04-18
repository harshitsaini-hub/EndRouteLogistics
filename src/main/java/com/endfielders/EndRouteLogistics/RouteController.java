package com.endfielders.EndRouteLogistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RouteController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/analyze")
    public RouteResponse analyzeRoute(@RequestBody RouteRequest request) {
        String result = geminiService.analyzeRoute(
            request.getOriginPincode(),
            request.getDestinationPincode(),
            request.getCargoType()
        );

        return new RouteResponse(result);
    }

static class RouteRequest {
    private String originPincode;
    private String destinationPincode;
    private String cargoType;

    public String getOriginPincode() { return originPincode; }
    public void setOriginPincode(String originPincode) { this.originPincode = originPincode; }
    public String getDestinationPincode() { return destinationPincode; }
    public void setDestinationPincode(String destinationPincode) { this.destinationPincode = destinationPincode; }
    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }
}

    static class RouteResponse {
        private String recommendation;
        public RouteResponse(String r) { this.recommendation = r; }
        public String getRecommendation() { return recommendation; }
    }
}