package com.portx.routesystem.service;

import com.portx.routesystem.dto.DriverRequest;
import com.portx.routesystem.dto.DriverResponse;
import com.portx.routesystem.entity.Driver;
import com.portx.routesystem.entity.DriverStatus;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.DriverRepository;
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
public class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void testGetAllDrivers() {
        Driver driver = new Driver();
        driver.setDriverId(1L);
        driver.setStatus(DriverStatus.AVAILABLE);

        when(driverRepository.findAll()).thenReturn(Collections.singletonList(driver));

        List<DriverResponse> responses = driverService.getAllDrivers();

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void testGetDriverById_Success() {
        Driver driver = new Driver();
        driver.setDriverId(1L);
        driver.setStatus(DriverStatus.AVAILABLE);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));

        DriverResponse response = driverService.getDriverById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getDriverId());
    }

    @Test
    void testGetDriverById_NotFound() {
        when(driverRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> driverService.getDriverById(1L));
    }

    @Test
    void testCreateDriver_Success() {
        DriverRequest request = new DriverRequest();
        request.setName("John Doe");
        request.setLicenseNumber("DL123");

        when(driverRepository.existsByLicenseNumber("DL123")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> {
            Driver d = (Driver) i.getArguments()[0];
            d.setDriverId(1L);
            return d;
        });

        DriverResponse response = driverService.createDriver(request);

        assertNotNull(response);
        assertEquals(1L, response.getDriverId());
        assertEquals("John Doe", response.getName());
    }

    @Test
    void testDeleteDriver_Success() {
        Driver driver = new Driver();
        driver.setDriverId(1L);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        doNothing().when(driverRepository).delete(driver);

        driverService.deleteDriver(1L);

        verify(driverRepository, times(1)).delete(driver);
    }
}
