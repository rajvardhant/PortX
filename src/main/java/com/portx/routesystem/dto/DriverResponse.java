package com.portx.routesystem.dto;

import com.portx.routesystem.entity.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private Long driverId;
    private String name;
    private String phone;
    private String licenseNumber;
    private DriverStatus status;
    private String vehicleRegistrationNumber;
    private Long vehicleId;

    // Linked username for driver login
    private String userUsername;
}
