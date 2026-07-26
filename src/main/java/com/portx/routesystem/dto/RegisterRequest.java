package com.portx.routesystem.dto;

import com.portx.routesystem.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Size(min=6)
    private String password;

    @Email
    private String email;

    private String fullName;
    private UserRole role;
}
