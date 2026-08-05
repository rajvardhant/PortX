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
 * DashboardService - Business logic layer providing aggregated stats & chart metrics.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;

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

    public List<DeliveryResponse> getRecentDeliveries() {
        return deliveryRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(this::mapDelivery)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getDeliveryStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (DeliveryStatus status : DeliveryStatus.values()) {
            counts.put(status.name(), deliveryRepository.countByStatus(status));
        }
        return counts;
    }

    public Map<String, Long> getInvoiceStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            counts.put(status.name(), invoiceRepository.countByStatus(status));
        }
        return counts;
    }

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
