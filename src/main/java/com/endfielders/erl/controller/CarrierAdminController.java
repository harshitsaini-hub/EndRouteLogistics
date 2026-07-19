package com.endfielders.erl.controller;

import com.endfielders.erl.model.Carrier;
import com.endfielders.erl.repository.CarrierRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Carrier Admin API", description = "Manage carriers (add, update, remove)")
@RestController
@RequestMapping("/api/admin/carriers")
@CrossOrigin(origins = "*")
public class CarrierAdminController {

    private final CarrierRepository carrierRepository;

    public CarrierAdminController(CarrierRepository carrierRepository) {
        this.carrierRepository = carrierRepository;
    }

    @Operation(summary = "List all carriers")
    @GetMapping
    public List<Carrier> getAllCarriers() {
        return carrierRepository.findAll();
    }

    @Operation(summary = "List active carriers only")
    @GetMapping("/active")
    public List<Carrier> getActiveCarriers() {
        return carrierRepository.findByActiveStatusTrue();
    }

    @Operation(summary = "Get a carrier by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Carrier> getCarrierById(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Add a new carrier")
    @PostMapping
    public ResponseEntity<Carrier> addCarrier(@RequestBody Carrier carrier) {
        carrier.setId(null);
        carrier.setActiveStatus(true);
        Carrier saved = carrierRepository.save(carrier);
        return ResponseEntity.status(201).body(saved);
    }

    @Operation(summary = "Update an existing carrier")
    @PutMapping("/{id}")
    public ResponseEntity<Carrier> updateCarrier(@PathVariable Long id, @RequestBody Carrier updated) {
        return carrierRepository.findById(id)
                .map(existing -> {
                    if (updated.getName() != null) existing.setName(updated.getName());
                    if (updated.getMode() != null) existing.setMode(updated.getMode());
                    if (updated.getEstimatedDays() > 0) existing.setEstimatedDays(updated.getEstimatedDays());
                    if (updated.getCostPerKg() > 0) existing.setCostPerKg(updated.getCostPerKg());
                    if (updated.getWebsite() != null) existing.setWebsite(updated.getWebsite());
                    if (updated.getReliabilityScore() > 0) existing.setReliabilityScore(updated.getReliabilityScore());
                    Carrier saved = carrierRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Soft-delete a carrier (sets activeStatus to false)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCarrier(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> {
                    carrier.setActiveStatus(false);
                    carrierRepository.save(carrier);
                    return ResponseEntity.ok(Map.of(
                            "message", "Carrier '" + carrier.getName() + "' has been deactivated.",
                            "id", id.toString()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Reactivate a soft-deleted carrier")
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Carrier> reactivateCarrier(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> {
                    carrier.setActiveStatus(true);
                    Carrier saved = carrierRepository.save(carrier);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
