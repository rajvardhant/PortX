package com.portx.routesystem.repository;

import com.portx.routesystem.entity.Vehicle;
import com.portx.routesystem.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(VehicleStatus status);
    boolean existsByRegistrationNumber(String registrationNumber);
}
