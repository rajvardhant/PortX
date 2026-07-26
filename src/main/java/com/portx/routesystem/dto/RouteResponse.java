package com.portx.routesystem.dto;

import com.portx.routesystem.entity.RouteStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RouteResponse {
    private Long routeId;
    private String startLocation;
    private String endLocation;
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
    private Double distance;
    private String estimatedTime;
    private RouteStatus status;
    private String polyline;
    private LocalDateTime createdAt;
}
