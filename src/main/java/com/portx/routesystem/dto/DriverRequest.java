package com.portx.routesystem.dto;

import com.portx.routesystem.entity.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DriverRequest - DTO used for creating and updating Drivers via REST API or Form submission.
 * Includes optional credentials (username & password) for creating linked driver user accounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // Optional user account credentials set by Admin
    private String username;
    private String password;
}
