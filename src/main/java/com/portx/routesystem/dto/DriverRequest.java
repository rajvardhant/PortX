package com.portx.routesystem.dto;

import com.portx.routesystem.entity.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DriverRequest - DTO used for creating and updating Drivers via REST API or Form submission.
 */
@Data
public class DriverRequest {
    // Optional ID for form binding during edit operations
    private Long id;

    @NotBlank(message = "Driver name is required")
    private String name;
    
    private String phone;
    
    @NotBlank(message = "License number is required")
    private String licenseNumber;
    
    private DriverStatus status;
    private Long vehicleId;
}
