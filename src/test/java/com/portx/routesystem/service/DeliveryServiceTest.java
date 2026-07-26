package com.portx.routesystem.service;

import com.portx.routesystem.dto.DeliveryRequest;
import com.portx.routesystem.dto.DeliveryResponse;
import com.portx.routesystem.entity.Delivery;
import com.portx.routesystem.entity.DeliveryStatus;
import com.portx.routesystem.entity.Invoice;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    void testCreateDelivery_Success() {
        DeliveryRequest request = new DeliveryRequest();
        request.setCustomerName("Customer A");

        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> {
            Delivery d = (Delivery) i.getArguments()[0];
            d.setDeliveryId(1L);
            return d;
        });
        
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(new Invoice());

        DeliveryResponse response = deliveryService.createDelivery(request);

        assertNotNull(response);
        assertEquals(1L, response.getDeliveryId());
        assertEquals("Customer A", response.getCustomerName());
    }

    @Test
    void testUpdateDeliveryStatus_Success() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryId(1L);
        delivery.setStatus(DeliveryStatus.PENDING);

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);

        DeliveryResponse response = deliveryService.updateDeliveryStatus(1L, "OUT_FOR_DELIVERY");

        assertNotNull(response);
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, response.getStatus());
    }

    @Test
    void testGetDeliveryById_NotFound() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deliveryService.getDeliveryById(1L));
    }
}
