package com.portx.routesystem.dto;

import com.portx.routesystem.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryResponse {
    private Long deliveryId;
    private String customerName;
    private String customerAddress;
    private Double packageWeight;
    private String notes;
    private DeliveryStatus status;
    private String driverName;
    private Long driverId;
    private String vehicleRegistrationNumber;
    private Long vehicleId;
    private String routeStartLocation;
    private String routeEndLocation;
    private String routeEstimatedTime;
    private Double routeDistance;
    private Long routeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
