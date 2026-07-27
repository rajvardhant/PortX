package com.portx.routesystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DispatcherRequest — DTO for creating and updating Dispatcher accounts by Admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatcherRequest {
    private Long id;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    private String password;
}
