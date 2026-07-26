package com.portx.routesystem.repository;

import com.portx.routesystem.entity.Delivery;
import com.portx.routesystem.entity.DeliveryStatus;
import com.portx.routesystem.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByStatus(DeliveryStatus status);
    List<Delivery> findByDriver(Driver driver);
    long countByStatus(DeliveryStatus status);
    List<Delivery> findTop5ByOrderByCreatedAtDesc();
}
