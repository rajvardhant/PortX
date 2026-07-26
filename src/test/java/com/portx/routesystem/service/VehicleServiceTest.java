package com.portx.routesystem.service;

import com.portx.routesystem.dto.VehicleRequest;
import com.portx.routesystem.dto.VehicleResponse;
import com.portx.routesystem.entity.Vehicle;
import com.portx.routesystem.entity.VehicleStatus;
import com.portx.routesystem.entity.VehicleType;
import com.portx.routesystem.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void testGetAllVehicles() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(1L);
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        when(vehicleRepository.findAll()).thenReturn(Collections.singletonList(vehicle));

        List<VehicleResponse> responses = vehicleService.getAllVehicles();
        
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void testCreateVehicle_Success() {
        VehicleRequest request = new VehicleRequest();
        request.setRegistrationNumber("REG123");
        request.setVehicleType(VehicleType.TRUCK);
        request.setCapacity(500.0);

        when(vehicleRepository.existsByRegistrationNumber("REG123")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> {
            Vehicle v = (Vehicle) i.getArguments()[0];
            v.setVehicleId(1L);
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle(request);

        assertNotNull(response);
        assertEquals(1L, response.getVehicleId());
        assertEquals("REG123", response.getRegistrationNumber());
    }

    @Test
    void testUpdateVehicle_Success() {
        VehicleRequest request = new VehicleRequest();
        request.setRegistrationNumber("REG123");
        request.setVehicleType(VehicleType.VAN);
        request.setCapacity(1000.0);

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(1L);
        vehicle.setRegistrationNumber("REG123");

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        VehicleResponse response = vehicleService.updateVehicle(1L, request);

        assertNotNull(response);
        verify(vehicleRepository, times(1)).save(vehicle);
    }

    @Test
    void testDeleteVehicle_Success() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(1L);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        doNothing().when(vehicleRepository).delete(vehicle);

        vehicleService.deleteVehicle(1L);

        verify(vehicleRepository, times(1)).delete(vehicle);
    }
}
