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
import java.util.List;
import java.util.stream.Collectors;

/**
 * DeliveryService - Business logic layer for managing shipments and deliveries.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final InvoiceRepository invoiceRepository;

    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DeliveryResponse> getDeliveriesByDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        return deliveryRepository.findByDriver(driver).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DeliveryResponse getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
        return mapToResponse(delivery);
    }

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

        updateFleetStatusForDelivery(saved, initialStatus);
        createInvoiceForDelivery(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public DeliveryResponse updateDelivery(Long id, DeliveryRequest req) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));

        delivery.setCustomerName(req.getCustomerName());
        delivery.setCustomerAddress(req.getCustomerAddress());
        delivery.setPackageWeight(req.getPackageWeight());
        delivery.setNotes(req.getNotes());

        if (req.getDriverId() != null) {
            Driver driver = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", req.getDriverId()));
            delivery.setDriver(driver);
        } else {
            delivery.setDriver(null);
        }

        if (req.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.getVehicleId()));
            delivery.setVehicle(vehicle);
        } else {
            delivery.setVehicle(null);
        }

        if (req.getRouteId() != null) {
            Route route = routeRepository.findById(req.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route", req.getRouteId()));
            delivery.setRoute(route);
        } else {
            delivery.setRoute(null);
        }

        if (req.getStatus() != null) {
            delivery.setStatus(req.getStatus());
            updateFleetStatusForDelivery(delivery, req.getStatus());
        }

        Delivery updated = deliveryRepository.save(delivery);
        updateInvoiceAmountAndStatus(updated);

        return mapToResponse(updated);
    }

    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long id, String statusStr) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));

        DeliveryStatus newStatus = DeliveryStatus.valueOf(statusStr.toUpperCase());
        delivery.setStatus(newStatus);

        updateFleetStatusForDelivery(delivery, newStatus);

        Delivery updated = deliveryRepository.save(delivery);
        updateInvoiceAmountAndStatus(updated);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteDelivery(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));

        if (delivery.getVehicle() != null) {
            delivery.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(delivery.getVehicle());
        }

        if (delivery.getDriver() != null) {
            delivery.getDriver().setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(delivery.getDriver());
        }

        Invoice invoice = invoiceRepository.findByDelivery(delivery).orElse(null);
        if (invoice != null) {
            invoiceRepository.delete(invoice);
        }

        deliveryRepository.delete(delivery);
    }

    private void updateFleetStatusForDelivery(Delivery d, DeliveryStatus status) {
        Vehicle v = d.getVehicle();
        Driver dr = d.getDriver();

        if (status == DeliveryStatus.ASSIGNED || status == DeliveryStatus.OUT_FOR_DELIVERY) {
            if (v != null) {
                v.setStatus(VehicleStatus.IN_TRANSIT);
                vehicleRepository.save(v);
            }
            if (dr != null) {
                dr.setStatus(DriverStatus.ON_DUTY);
                driverRepository.save(dr);
            }
        } else if (status == DeliveryStatus.DELIVERED) {
            if (v != null) {
                v.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(v);
            }
            if (dr != null) {
                dr.setStatus(DriverStatus.AVAILABLE);
                driverRepository.save(dr);
            }
        }
    }

    private void createInvoiceForDelivery(Delivery delivery) {
        double dist = (delivery.getRoute() != null && delivery.getRoute().getDistance() != null) 
                ? delivery.getRoute().getDistance() : 10.0;
        double amount = 100.0 + (dist * 10.0);

        InvoiceStatus status = (delivery.getStatus() == DeliveryStatus.DELIVERED) 
                ? InvoiceStatus.PAID : InvoiceStatus.PENDING;

        Invoice invoice = Invoice.builder()
                .delivery(delivery)
                .customerName(delivery.getCustomerName())
                .invoiceDate(LocalDate.now())
                .amount(amount)
                .status(status)
                .remarks("Auto-generated invoice for Delivery #" + delivery.getDeliveryId())
                .build();

        invoiceRepository.save(invoice);
    }

    private void updateInvoiceAmountAndStatus(Delivery delivery) {
        Invoice invoice = invoiceRepository.findByDelivery(delivery).orElse(null);
        if (invoice != null) {
            double dist = (delivery.getRoute() != null && delivery.getRoute().getDistance() != null) 
                    ? delivery.getRoute().getDistance() : 10.0;
            double amount = 100.0 + (dist * 10.0);
            invoice.setAmount(amount);
            invoice.setCustomerName(delivery.getCustomerName());

            if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
                invoice.setStatus(InvoiceStatus.PAID);
            }
            invoiceRepository.save(invoice);
        }
    }

    private DeliveryResponse mapToResponse(Delivery d) {
        DeliveryResponse.DeliveryResponseBuilder builder = DeliveryResponse.builder()
                .deliveryId(d.getDeliveryId())
                .customerName(d.getCustomerName())
                .customerAddress(d.getCustomerAddress())
                .packageWeight(d.getPackageWeight())
                .status(d.getStatus())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt());

        if (d.getDriver() != null) {
            builder.driverId(d.getDriver().getDriverId())
                   .driverName(d.getDriver().getName());
        }

        if (d.getVehicle() != null) {
            builder.vehicleId(d.getVehicle().getVehicleId())
                   .vehicleRegistrationNumber(d.getVehicle().getRegistrationNumber());
        }

        if (d.getRoute() != null) {
            builder.routeId(d.getRoute().getRouteId())
                   .routeStartLocation(d.getRoute().getStartLocation())
                   .routeEndLocation(d.getRoute().getEndLocation())
                   .routeDistance(d.getRoute().getDistance())
                   .routeEstimatedTime(d.getRoute().getEstimatedTime());
        }

        return builder.build();
    }
}
