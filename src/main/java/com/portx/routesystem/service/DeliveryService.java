package com.portx.routesystem.service;

import com.portx.routesystem.dto.DeliveryRequest;
import com.portx.routesystem.dto.DeliveryResponse;
import com.portx.routesystem.entity.*;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DeliveryService — Business logic layer for managing shipments and deliveries.
 *
 * PURPOSE:
 * 1. Manages delivery assignments (driver, vehicle, route).
 * 2. Synchronizes Vehicle status (IN_TRANSIT vs AVAILABLE) and Driver status (ON_DUTY vs AVAILABLE)
 *    in real-time according to delivery progress.
 * 3. Auto-generates per-km Rupee (₹) GST Invoices upon creation.
 * 4. Automatically updates invoice status to PAID when a delivery is marked DELIVERED.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    // Repositories injected for entity persistence and relational mapping
    private final DeliveryRepository deliveryRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Retrieves all deliveries in the system.
     */
    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves assigned deliveries for a specific driver ID.
     */
    public List<DeliveryResponse> getDeliveriesByDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        return deliveryRepository.findByDriver(driver).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a delivery by ID or throws ResourceNotFoundException.
     */
    public DeliveryResponse getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
        return mapToResponse(delivery);
    }

    /**
     * Creates a new Delivery record, syncs fleet status, and auto-generates a billing invoice.
     */
    @Transactional
    public DeliveryResponse createDelivery(DeliveryRequest req) {
        Driver driver = null;
        if (req.getDriverId() != null) {
            driver = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", req.getDriverId()));
        }

        Vehicle vehicle = null;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.getVehicleId()));
        }

        Route route = null;
        if (req.getRouteId() != null) {
            route = routeRepository.findById(req.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route", req.getRouteId()));
        }

        DeliveryStatus initialStatus = req.getStatus() != null ? req.getStatus() : DeliveryStatus.PENDING;

        Delivery delivery = Delivery.builder()
                .customerName(req.getCustomerName())
                .customerAddress(req.getCustomerAddress())
                .packageWeight(req.getPackageWeight())
                .notes(req.getNotes())
                .driver(driver)
                .vehicle(vehicle)
                .route(route)
                .status(initialStatus)
                .build();

        Delivery saved = deliveryRepository.save(delivery);

        // Synchronize vehicle and driver status according to delivery assignment
        updateFleetStatusForDelivery(saved, initialStatus);

        // Auto-generate Rupee (₹) invoice for the created delivery
        createInvoiceForDelivery(saved);

        return mapToResponse(saved);
    }

    /**
     * Updates delivery status (PENDING -> ASSIGNED -> OUT_FOR_DELIVERY -> DELIVERED).
     * Automatically syncs Vehicle (IN_TRANSIT vs AVAILABLE) and Driver (ON_DUTY vs AVAILABLE) status.
     * If status changes to DELIVERED, automatically marks invoice status to PAID.
     */
    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long id, String status) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
        
        DeliveryStatus newStatus = DeliveryStatus.valueOf(status.toUpperCase());
        delivery.setStatus(newStatus);
        delivery.setUpdatedAt(LocalDateTime.now());

        // Update assigned Vehicle and Driver status in real-time according to delivery progress
        updateFleetStatusForDelivery(delivery, newStatus);
        
        Delivery saved = deliveryRepository.save(delivery);
        
        // Auto-update linked invoice to PAID when delivery completes
        if (newStatus == DeliveryStatus.DELIVERED) {
            invoiceRepository.findByDelivery_DeliveryId(id).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.PAID);
                invoiceRepository.save(invoice);
            });
        }
        
        return mapToResponse(saved);
    }

    /**
     * Updates delivery details (customer, package weight, driver/vehicle assignments) and syncs fleet status.
     */
    @Transactional
    public DeliveryResponse updateDelivery(Long id, DeliveryRequest req) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));

        Driver driver = null;
        if (req.getDriverId() != null) {
            driver = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", req.getDriverId()));
        }

        Vehicle vehicle = null;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.getVehicleId()));
        }

        Route route = null;
        if (req.getRouteId() != null) {
            route = routeRepository.findById(req.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route", req.getRouteId()));
        }

        delivery.setCustomerName(req.getCustomerName());
        delivery.setCustomerAddress(req.getCustomerAddress());
        delivery.setPackageWeight(req.getPackageWeight());
        delivery.setNotes(req.getNotes());
        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setRoute(route);
        if (req.getStatus() != null) {
            delivery.setStatus(req.getStatus());
        }
        delivery.setUpdatedAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);

        // Update assigned Vehicle and Driver status according to current delivery progress
        updateFleetStatusForDelivery(saved, saved.getStatus());

        return mapToResponse(saved);
    }

    /**
     * Deletes a delivery, frees its assigned vehicle/driver, and unlinks its associated invoice safely.
     */
    @Transactional
    public void deleteDelivery(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));

        // Revert assigned vehicle and driver back to AVAILABLE upon delivery deletion
        updateFleetStatusForDelivery(delivery, DeliveryStatus.DELIVERED);

        invoiceRepository.findByDelivery_DeliveryId(id).ifPresent(invoiceRepository::delete);
        deliveryRepository.delete(delivery);
    }

    /**
     * Helper method to synchronize Vehicle and Driver status in real-time based on delivery progress.
     */
    private void updateFleetStatusForDelivery(Delivery delivery, DeliveryStatus delStatus) {
        Vehicle vehicle = delivery.getVehicle();
        Driver driver = delivery.getDriver();

        if (delStatus == DeliveryStatus.ASSIGNED || delStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
            if (vehicle != null) {
                vehicle.setStatus(VehicleStatus.IN_TRANSIT);
                vehicleRepository.save(vehicle);
            }
            if (driver != null) {
                driver.setStatus(DriverStatus.ON_DUTY);
                driverRepository.save(driver);
            }
        } else if (delStatus == DeliveryStatus.DELIVERED) {
            if (vehicle != null) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
            }
            if (driver != null) {
                driver.setStatus(DriverStatus.AVAILABLE);
                driverRepository.save(driver);
            }
        }
    }

    /**
     * Helper method to generate an invoice based on base fare ₹100 + ₹10/km.
     */
    private void createInvoiceForDelivery(Delivery delivery) {
        double distance = 0.0;
        if (delivery.getRoute() != null && delivery.getRoute().getDistance() != null) {
            distance = delivery.getRoute().getDistance();
        }
        
        // Per-km price calculation formula
        double amount = 100.0 + (distance * 10.0);
        
        Invoice invoice = Invoice.builder()
                .delivery(delivery)
                .customerName(delivery.getCustomerName())
                .invoiceDate(LocalDate.now())
                .amount(amount)
                .status(InvoiceStatus.PENDING)
                .remarks("Auto-generated invoice for Delivery #" + delivery.getDeliveryId())
                .build();
                
        invoiceRepository.save(invoice);
    }

    /**
     * Maps Delivery entity to DeliveryResponse DTO.
     */
    private DeliveryResponse mapToResponse(Delivery d) {
        DeliveryResponse.DeliveryResponseBuilder builder = DeliveryResponse.builder()
                .deliveryId(d.getDeliveryId())
                .customerName(d.getCustomerName())
                .customerAddress(d.getCustomerAddress())
                .packageWeight(d.getPackageWeight())
                .notes(d.getNotes())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt());

        if (d.getDriver() != null) {
            builder.driverName(d.getDriver().getName())
                   .driverId(d.getDriver().getDriverId());
        }
        if (d.getVehicle() != null) {
            builder.vehicleRegistrationNumber(d.getVehicle().getRegistrationNumber())
                   .vehicleId(d.getVehicle().getVehicleId());
        }
        if (d.getRoute() != null) {
            builder.routeStartLocation(d.getRoute().getStartLocation())
                   .routeEndLocation(d.getRoute().getEndLocation())
                   .routeEstimatedTime(d.getRoute().getEstimatedTime())
                   .routeDistance(d.getRoute().getDistance())
                   .routeId(d.getRoute().getRouteId());
        }

        return builder.build();
    }
}
