package com.portx.routesystem.service;

import com.portx.routesystem.dto.VehicleRequest;
import com.portx.routesystem.dto.VehicleResponse;
import com.portx.routesystem.entity.Vehicle;
import com.portx.routesystem.entity.VehicleStatus;
import com.portx.routesystem.entity.VehicleType;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VehicleService - Business logic layer for managing commercial vehicles (TRUCK and VAN).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        return mapToResponse(vehicle);
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest req) {
        String regNumber = req.getRegistrationNumber().toUpperCase().trim();
        if (vehicleRepository.existsByRegistrationNumber(regNumber)) {
            throw new IllegalArgumentException("Registration number already registered: " + regNumber);
        }

        VehicleType type = req.getVehicleType() != null ? req.getVehicleType() : VehicleType.VAN;

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(regNumber)
                .vehicleType(type)
                .capacity(req.getCapacity())
                .status(req.getStatus() != null ? req.getStatus() : VehicleStatus.AVAILABLE)
                .build();

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest req) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        String regNumber = req.getRegistrationNumber().toUpperCase().trim();
        vehicle.setRegistrationNumber(regNumber);

        if (req.getVehicleType() != null) {
            vehicle.setVehicleType(req.getVehicleType());
        }

        vehicle.setCapacity(req.getCapacity());

        if (req.getStatus() != null) {
            vehicle.setStatus(req.getStatus());
        }

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        vehicleRepository.delete(vehicle);
    }

    private VehicleResponse mapToResponse(Vehicle v) {
        return VehicleResponse.builder()
                .vehicleId(v.getVehicleId())
                .registrationNumber(v.getRegistrationNumber())
                .vehicleType(v.getVehicleType())
                .capacity(v.getCapacity())
                .status(v.getStatus())
                .build();
    }
}
