package com.portx.routesystem.dto;

import com.portx.routesystem.entity.VehicleStatus;
import com.portx.routesystem.entity.VehicleType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class VehicleResponse {
    private Long vehicleId;
    private String registrationNumber;
    private VehicleType vehicleType;
    private Double capacity;
    private VehicleStatus status;
    private LocalDateTime createdAt;
}
