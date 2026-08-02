package com.endfielders.erl.service;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.model.RankedCarrier;
import com.endfielders.erl.repository.CarrierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CarrierServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private RouteEstimationService routeEstimationService;

    @Mock
    private CarrierRepository carrierRepository;

    private CarrierService carrierService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CityDataService cityDataService = new CityDataService();
        cityDataService.init();
        carrierService = new CarrierService(geminiService, weatherService, routeEstimationService, carrierRepository, cityDataService);

        Carrier c1 = new Carrier();
        c1.setId(1L);
        c1.setName("BlueDart");
        c1.setMode("Air");
        c1.setEstimatedDays(2);
        c1.setCostPerKg(120.0);
        c1.setActiveStatus(true);

        Carrier c2 = new Carrier();
        c2.setId(2L);
        c2.setName("Delhivery");
        c2.setMode("Road");
        c2.setEstimatedDays(4);
        c2.setCostPerKg(45.0);
        c2.setActiveStatus(true);

        when(carrierRepository.findByActiveStatusTrue()).thenReturn(List.of(c1, c2));
        when(carrierRepository.findByCategoryInAndActiveStatusTrue(anyList())).thenReturn(List.of(c1, c2));
        when(weatherService.buildWeatherSummary(anyString(), anyString())).thenReturn("Clear weather");
        when(routeEstimationService.estimateRouteStops(anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of());
        when(weatherService.batchFetchForecasts(anyList(), anyMap())).thenReturn(List.of());
        when(geminiService.analyzeRouteWithWeather(anyString(), anyString(), anyString(), anyString())).thenReturn("Route looks clear");
    }

    @Test
    @DisplayName("Should rank carriers and attach timelines")
    void testGetRankedCarriers() {
        List<RankedCarrier> ranked = carrierService.getRankedCarriers(
                "110001", "400001", "Electronics", "FASTEST", true, false
        );

        assertNotNull(ranked);
        assertEquals(2, ranked.size());
        assertEquals("BlueDart", ranked.get(0).getName(), "BlueDart Air should rank first for FASTEST priority");
        assertNotNull(ranked.get(0).getTimeline());
        assertEquals("Route looks clear", carrierService.getLastRouteInsight());
    }
}
