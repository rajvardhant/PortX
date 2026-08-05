package com.portx.routesystem.service;

import com.portx.routesystem.dto.DriverRequest;
import com.portx.routesystem.dto.DriverResponse;
import com.portx.routesystem.entity.Driver;
import com.portx.routesystem.entity.DriverStatus;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.entity.UserRole;
import com.portx.routesystem.entity.Vehicle;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.DriverRepository;
import com.portx.routesystem.repository.UserRepository;
import com.portx.routesystem.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DriverService - Business logic layer for managing commercial logistics drivers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DriverResponse> getAvailableDrivers() {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DriverResponse getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
        return mapToResponse(driver);
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest req) {
        cleanAndValidateRequest(req);

        if (driverRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            throw new IllegalArgumentException("License number already registered: " + req.getLicenseNumber());
        }

        Vehicle vehicle = null;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.getVehicleId()));
        }

        User user = null;
        if (req.getUsername() != null && !req.getUsername().trim().isEmpty()) {
            String username = req.getUsername().trim();
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
            String rawPassword = (req.getPassword() != null && !req.getPassword().trim().isEmpty()) ? req.getPassword().trim() : "driver123";
            user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .email(username + "@portx.com")
                    .fullName(req.getName().trim())
                    .role(UserRole.DRIVER)
                    .build();
            user = userRepository.save(user);
        }

        Driver driver = Driver.builder()
                .name(req.getName().trim())
                .phone(req.getPhone())
                .licenseNumber(req.getLicenseNumber())
                .status(req.getStatus() != null ? req.getStatus() : DriverStatus.AVAILABLE)
                .vehicle(vehicle)
                .user(user)
                .build();

        return mapToResponse(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest req) {
        cleanAndValidateRequest(req);

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));

        Vehicle vehicle = null;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.getVehicleId()));
        }

        driver.setName(req.getName().trim());
        driver.setPhone(req.getPhone());
        driver.setLicenseNumber(req.getLicenseNumber());
        driver.setVehicle(vehicle);
        if (req.getStatus() != null) {
            driver.setStatus(req.getStatus());
        }

        if (req.getUsername() != null && !req.getUsername().trim().isEmpty()) {
            String newUsername = req.getUsername().trim();
            User existingUser = driver.getUser();

            if (existingUser == null) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException("Username already exists: " + newUsername);
                }
                String rawPassword = (req.getPassword() != null && !req.getPassword().trim().isEmpty()) ? req.getPassword().trim() : "driver123";
                User newUser = User.builder()
                        .username(newUsername)
                        .password(passwordEncoder.encode(rawPassword))
                        .email(newUsername + "@portx.com")
                        .fullName(req.getName().trim())
                        .role(UserRole.DRIVER)
                        .build();
                newUser = userRepository.save(newUser);
                driver.setUser(newUser);
            } else {
                existingUser.setUsername(newUsername);
                existingUser.setFullName(req.getName().trim());
                if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(req.getPassword().trim()));
                }
                userRepository.save(existingUser);
            }
        }

        return mapToResponse(driverRepository.save(driver));
    }

    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
        
        User user = driver.getUser();
        driver.setUser(null);
        driverRepository.save(driver);
        driverRepository.delete(driver);

        if (user != null) {
            userRepository.delete(user);
        }
    }

    private void cleanAndValidateRequest(DriverRequest req) {
        if (req.getLicenseNumber() != null) {
            req.setLicenseNumber(req.getLicenseNumber().toUpperCase().trim());
        }
        if (req.getPhone() != null) {
            String digitsOnly = req.getPhone().replaceAll("[^0-9]", "");
            if (!digitsOnly.matches("^[0-9]{10}$")) {
                throw new IllegalArgumentException("Phone number must be exactly 10 numeric digits.");
            }
            req.setPhone(digitsOnly);
        }
    }

    private DriverResponse mapToResponse(Driver d) {
        DriverResponse.DriverResponseBuilder builder = DriverResponse.builder()
                .driverId(d.getDriverId())
                .name(d.getName())
                .phone(d.getPhone())
                .licenseNumber(d.getLicenseNumber())
                .status(d.getStatus());

        if (d.getVehicle() != null) {
            builder.vehicleRegistrationNumber(d.getVehicle().getRegistrationNumber())
                   .vehicleId(d.getVehicle().getVehicleId());
        }

        if (d.getUser() != null) {
            builder.userUsername(d.getUser().getUsername());
        }

        return builder.build();
    }
}
