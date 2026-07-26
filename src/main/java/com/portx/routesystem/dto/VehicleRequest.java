package com.portx.routesystem.dto;

import com.portx.routesystem.entity.VehicleStatus;
import com.portx.routesystem.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * VehicleRequest - DTO used for creating and updating Vehicles.
 */
@Data
public class VehicleRequest {
    // Optional ID for form binding during edit operations
    private Long id;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Capacity is required")
    private Double capacity;

    private VehicleStatus status;
}
