package com.portx.routesystem.controller;

import com.portx.routesystem.dto.DeliveryRequest;
import com.portx.routesystem.dto.DeliveryResponse;
import com.portx.routesystem.dto.VehicleResponse;
import com.portx.routesystem.entity.DeliveryStatus;
import com.portx.routesystem.service.DeliveryService;
import com.portx.routesystem.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DispatcherController — Backend REST API for Dispatcher Module.
 * Responsible for daily logistics operations: viewing orders, assigning drivers,
 * vehicle availability checks, route optimization, active tracking, and reassigning drivers.
 */
@RestController
@RequestMapping("/api/dispatcher")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class DispatcherController {

    private final DeliveryService deliveryService;
    private final VehicleService vehicleService;

    @GetMapping("/orders")
    public ResponseEntity<List<DeliveryResponse>> getOrders() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getAvailableVehicles() {
        return ResponseEntity.ok(vehicleService.getAvailableVehicles());
    }

    @PutMapping("/orders/{id}/assign-driver")
    public ResponseEntity<DeliveryResponse> assignDriver(@PathVariable Long id, @RequestParam Long driverId) {
        DeliveryResponse existing = deliveryService.getDeliveryById(id);
        DeliveryRequest req = new DeliveryRequest();
        req.setId(id);
        req.setCustomerName(existing.getCustomerName());
        req.setCustomerAddress(existing.getCustomerAddress());
        req.setPackageWeight(existing.getPackageWeight());
        req.setNotes(existing.getNotes());
        req.setDriverId(driverId);
        req.setVehicleId(existing.getVehicleId());
        req.setRouteId(existing.getRouteId());
        req.setStatus(DeliveryStatus.ASSIGNED);

        DeliveryResponse updated = deliveryService.updateDelivery(id, req);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/orders/{id}/route")
    public ResponseEntity<DeliveryResponse> assignRoute(@PathVariable Long id, @RequestParam Long routeId) {
        DeliveryResponse existing = deliveryService.getDeliveryById(id);
        DeliveryRequest req = new DeliveryRequest();
        req.setId(id);
        req.setCustomerName(existing.getCustomerName());
        req.setCustomerAddress(existing.getCustomerAddress());
        req.setPackageWeight(existing.getPackageWeight());
        req.setNotes(existing.getNotes());
        req.setDriverId(existing.getDriverId());
        req.setVehicleId(existing.getVehicleId());
        req.setRouteId(routeId);
        req.setStatus(existing.getStatus());

        DeliveryResponse updated = deliveryService.updateDelivery(id, req);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/active-deliveries")
    public ResponseEntity<List<DeliveryResponse>> getActiveDeliveries() {
        List<DeliveryResponse> active = deliveryService.getAllDeliveries().stream()
                .filter(d -> d.getStatus() == DeliveryStatus.ASSIGNED || d.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY)
                .collect(Collectors.toList());
        return ResponseEntity.ok(active);
    }

    @PutMapping("/orders/{id}/reassign")
    public ResponseEntity<DeliveryResponse> reassignDriver(@PathVariable Long id, @RequestParam Long driverId, @RequestParam(required = false) Long vehicleId) {
        DeliveryResponse existing = deliveryService.getDeliveryById(id);
        DeliveryRequest req = new DeliveryRequest();
        req.setId(id);
        req.setCustomerName(existing.getCustomerName());
        req.setCustomerAddress(existing.getCustomerAddress());
        req.setPackageWeight(existing.getPackageWeight());
        req.setNotes(existing.getNotes());
        req.setDriverId(driverId);
        req.setVehicleId(vehicleId != null ? vehicleId : existing.getVehicleId());
        req.setRouteId(existing.getRouteId());
        req.setStatus(DeliveryStatus.ASSIGNED);

        DeliveryResponse updated = deliveryService.updateDelivery(id, req);
        return ResponseEntity.ok(updated);
    }
}
