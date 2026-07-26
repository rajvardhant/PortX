package com.portx.routesystem.service;

import com.portx.routesystem.dto.InvoiceResponse;
import com.portx.routesystem.entity.Invoice;
import com.portx.routesystem.entity.InvoiceStatus;
import com.portx.routesystem.repository.InvoiceRepository;
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
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void testGetAllInvoices() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findAll()).thenReturn(Collections.singletonList(invoice));

        List<InvoiceResponse> responses = invoiceService.getAllInvoices();

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void testGetInvoiceById_Success() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getInvoiceId());
    }

    @Test
    void testUpdateInvoiceStatus_Success() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = invoiceService.updateInvoiceStatus(1L, "PAID");

        assertNotNull(response);
        assertEquals(InvoiceStatus.PAID, response.getStatus());
    }
}
