package com.portx.routesystem.dto;

import com.portx.routesystem.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DispatcherResponse — DTO for sending Dispatcher account information to views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatcherResponse {
    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;
}
