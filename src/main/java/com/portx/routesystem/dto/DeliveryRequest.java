package com.portx.routesystem.dto;

import com.portx.routesystem.entity.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DeliveryRequest - DTO used for creating and updating Deliveries.
 */
@Data
public class DeliveryRequest {
    // Optional ID for form binding during edit operations
    private Long id;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerAddress;
    private Double packageWeight;
    private String notes;
    private DeliveryStatus status;
    private Long driverId;
    private Long vehicleId;
    private Long routeId;
}
