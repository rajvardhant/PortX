package com.portx.routesystem.repository;

import com.portx.routesystem.entity.Driver;
import com.portx.routesystem.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByStatus(DriverStatus status);
    boolean existsByLicenseNumber(String licenseNumber);
}
