package com.portx.routesystem.repository;

import com.portx.routesystem.entity.Delivery;
import com.portx.routesystem.entity.Invoice;
import com.portx.routesystem.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStatus(InvoiceStatus status);
    Optional<Invoice> findByDelivery(Delivery delivery);
    Optional<Invoice> findByDelivery_DeliveryId(Long deliveryId);
    long countByStatus(InvoiceStatus status);
}
