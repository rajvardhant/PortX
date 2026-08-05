package com.portx.routesystem.controller;

import com.portx.routesystem.dto.*;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminController - Backend REST API for Admin Module.
 * Responsible for system setup, user management, fleet config, orders, and reports.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final DeliveryService deliveryService;
    private final DashboardService dashboardService;

    // USER MANAGEMENT
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // VEHICLE MANAGEMENT
    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(@RequestBody VehicleRequest req) {
        return ResponseEntity.ok(vehicleService.createVehicle(req));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    // DELIVERY ORDERS MANAGEMENT
    @PostMapping("/orders")
    public ResponseEntity<DeliveryResponse> createOrder(@RequestBody DeliveryRequest req) {
        return ResponseEntity.ok(deliveryService.createDelivery(req));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<DeliveryResponse>> getAllOrders() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    // DASHBOARD & REPORTS
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getSystemReports() {
        Map<String, Object> report = new HashMap<>();
        report.put("stats", dashboardService.getStats());
        report.put("recentDeliveries", dashboardService.getRecentDeliveries());
        report.put("deliveryStatusBreakdown", dashboardService.getDeliveryStatusCounts());
        report.put("invoiceStatusBreakdown", dashboardService.getInvoiceStatusCounts());
        return ResponseEntity.ok(report);
    }
}
