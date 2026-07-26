package com.portx.routesystem.service;

import com.portx.routesystem.dto.VehicleRequest;
import com.portx.routesystem.dto.VehicleResponse;
import com.portx.routesystem.entity.Vehicle;
import com.portx.routesystem.entity.VehicleStatus;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VehicleService — Business logic layer for managing commercial vehicles (TRUCK and VAN).
 *
 * PURPOSE:
 * 1. Handles vehicle registration and updates.
 * 2. Enforces UPPERCASE formatting for registration numbers (e.g. MH01AB1234).
 * 3. Prevents duplicate vehicle registration numbers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    // Database access repository
    private final VehicleRepository vehicleRepository;

    /**
     * Retrieves all registered vehicles in the system.
     */
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves vehicles currently marked AVAILABLE for dispatch assignment.
     */
    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a vehicle by ID or throws ResourceNotFoundException.
     */
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        return mapToResponse(vehicle);
    }

    /**
     * Creates a new vehicle record after converting registration number to UPPERCASE.
     */
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest req) {
        // Step 1: Trim and convert registration number to UPPERCASE
        cleanRequest(req);

        // Step 2: Validate registration number uniqueness
        if (vehicleRepository.existsByRegistrationNumber(req.getRegistrationNumber())) {
            throw new IllegalArgumentException("Registration number already exists: " + req.getRegistrationNumber());
        }

        // Step 3: Build Vehicle entity and save to database
        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(req.getRegistrationNumber())
                .vehicleType(req.getVehicleType())
                .capacity(req.getCapacity())
                .status(req.getStatus() != null ? req.getStatus() : VehicleStatus.AVAILABLE)
                .build();

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    /**
     * Updates existing vehicle details.
     */
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest req) {
        cleanRequest(req);

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        vehicle.setRegistrationNumber(req.getRegistrationNumber());
        vehicle.setVehicleType(req.getVehicleType());
        vehicle.setCapacity(req.getCapacity());
        if (req.getStatus() != null) {
            vehicle.setStatus(req.getStatus());
        }

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    /**
     * Deletes a vehicle by ID.
     */
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        vehicleRepository.delete(vehicle);
    }

    /**
     * Helper method to sanitize registration numbers to UPPERCASE.
     */
    private void cleanRequest(VehicleRequest req) {
        if (req.getRegistrationNumber() != null) {
            req.setRegistrationNumber(req.getRegistrationNumber().toUpperCase().trim());
        }
    }

    /**
     * Maps Vehicle JPA entity to VehicleResponse DTO.
     */
    private VehicleResponse mapToResponse(Vehicle v) {
        return VehicleResponse.builder()
                .vehicleId(v.getVehicleId())
                .registrationNumber(v.getRegistrationNumber())
                .vehicleType(v.getVehicleType())
                .capacity(v.getCapacity())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
