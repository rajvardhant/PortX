package com.portx.routesystem.controller;

import com.portx.routesystem.dto.DeliveryResponse;
import com.portx.routesystem.entity.DeliveryStatus;
import com.portx.routesystem.entity.Driver;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.repository.DriverRepository;
import com.portx.routesystem.repository.UserRepository;
import com.portx.routesystem.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DriverController — Backend REST API for Driver Module.
 * Responsible for driver workflow: viewing assigned orders, updating status
 * (Pickup, In-Transit, Delivered), and viewing completed delivery history.
 */
@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
@RequiredArgsConstructor
public class DriverController {

    private final DeliveryService deliveryService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    private Driver getCurrentDriver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;
        return driverRepository.findAll().stream()
                .filter(d -> d.getUser() != null && d.getUser().getUserId().equals(user.getUserId()))
                .findFirst()
                .orElse(null);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<DeliveryResponse>> getAssignedOrders() {
        Driver driver = getCurrentDriver();
        if (driver != null) {
            return ResponseEntity.ok(deliveryService.getDeliveriesByDriver(driver.getDriverId()));
        }
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<DeliveryResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(id));
    }

    @PutMapping("/orders/{id}/pickup")
    public ResponseEntity<DeliveryResponse> pickupPackage(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(id, "OUT_FOR_DELIVERY"));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(id, status));
    }

    @PutMapping("/orders/{id}/deliver")
    public ResponseEntity<DeliveryResponse> markDelivered(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(id, "DELIVERED"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<DeliveryResponse>> getDeliveryHistory() {
        Driver driver = getCurrentDriver();
        List<DeliveryResponse> all = (driver != null) ?
                deliveryService.getDeliveriesByDriver(driver.getDriverId()) :
                deliveryService.getAllDeliveries();

        List<DeliveryResponse> completed = all.stream()
                .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                .collect(Collectors.toList());

        return ResponseEntity.ok(completed);
    }
}
