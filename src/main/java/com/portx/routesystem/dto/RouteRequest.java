package com.portx.routesystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RouteRequest {
    @NotBlank
    private String startLocation;
    
    @NotBlank
    private String endLocation;
}
