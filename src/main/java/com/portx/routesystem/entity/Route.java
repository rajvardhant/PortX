package com.portx.routesystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routeId;

    @Column(nullable = false)
    private String startLocation;

    @Column(nullable = false)
    private String endLocation;

    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
    
    private Double distance;
    private String estimatedTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RouteStatus status = RouteStatus.ACTIVE;

    @Column(columnDefinition = "LONGTEXT")
    private String polyline;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
