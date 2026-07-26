package com.portx.routesystem.service;

import com.portx.routesystem.dto.DashboardStats;
import com.portx.routesystem.dto.DeliveryResponse;
import com.portx.routesystem.entity.Delivery;
import com.portx.routesystem.entity.DeliveryStatus;
import com.portx.routesystem.entity.InvoiceStatus;
import com.portx.routesystem.repository.DeliveryRepository;
import com.portx.routesystem.repository.DriverRepository;
import com.portx.routesystem.repository.InvoiceRepository;
import com.portx.routesystem.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DashboardService — Business logic layer providing aggregated stats & chart metrics.
 *
 * PURPOSE:
 * 1. Computes total count metrics for Drivers, Vehicles, Deliveries, and Invoices.
 * 2. Provides status breakdown counts for Chart.js pie and bar charts.
 * 3. Retrieves recent 5 deliveries for dashboard activity feed.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    // Repositories for counting system entities
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Aggregates total system counts for dashboard metric cards.
     */
    public DashboardStats getStats() {
        return DashboardStats.builder()
                .totalDrivers(driverRepository.count())
                .totalVehicles(vehicleRepository.count())
                .totalDeliveries(deliveryRepository.count())
                .pendingDeliveries(deliveryRepository.countByStatus(DeliveryStatus.PENDING))
                .ongoingDeliveries(deliveryRepository.countByStatus(DeliveryStatus.OUT_FOR_DELIVERY))
                .completedDeliveries(deliveryRepository.countByStatus(DeliveryStatus.DELIVERED))
                .pendingInvoices(invoiceRepository.countByStatus(InvoiceStatus.PENDING))
                .completedInvoices(invoiceRepository.countByStatus(InvoiceStatus.PAID))
                .build();
    }

    /**
     * Retrieves top 5 most recent deliveries for dashboard activity overview.
     */
    public List<DeliveryResponse> getRecentDeliveries() {
        return deliveryRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(this::mapDelivery)
                .collect(Collectors.toList());
    }

    /**
     * Builds key-value status breakdown map for Delivery Chart.js rendering.
     */
    public Map<String, Long> getDeliveryStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (DeliveryStatus status : DeliveryStatus.values()) {
            counts.put(status.name(), deliveryRepository.countByStatus(status));
        }
        return counts;
    }

    /**
     * Builds key-value status breakdown map for Invoice Chart.js rendering.
     */
    public Map<String, Long> getInvoiceStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            counts.put(status.name(), invoiceRepository.countByStatus(status));
        }
        return counts;
    }

    /**
     * Helper mapper for recent delivery DTO output.
     */
    private DeliveryResponse mapDelivery(Delivery d) {
        DeliveryResponse.DeliveryResponseBuilder builder = DeliveryResponse.builder()
                .deliveryId(d.getDeliveryId())
                .customerName(d.getCustomerName())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt());
        
        if (d.getDriver() != null) {
            builder.driverName(d.getDriver().getName());
        }
        return builder.build();
    }
}
