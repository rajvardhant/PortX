package com.portx.routesystem.dto;

import com.portx.routesystem.entity.DriverStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverResponse {
    private Long driverId;
    private String name;
    private String phone;
    private String licenseNumber;
    private DriverStatus status;
    private String vehicleRegistrationNumber;
    private Long vehicleId;
}
