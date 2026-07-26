package com.portx.routesystem.service;

import com.portx.routesystem.dto.InvoiceResponse;
import com.portx.routesystem.entity.Invoice;
import com.portx.routesystem.entity.InvoiceStatus;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * InvoiceService — Business logic layer for managing Rupee (₹) GST Invoices.
 *
 * PURPOSE:
 * 1. Handles listing, detail retrieval, and payment status updates (PENDING / PAID) for invoices.
 * 2. Formats financial amounts for clean screen display and PDF downloads.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    // Database access repository for Invoice entities
    private final InvoiceRepository invoiceRepository;

    /**
     * Retrieves all invoices in the system.
     */
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds an invoice by its primary key ID or throws ResourceNotFoundException.
     */
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return mapToResponse(invoice);
    }

    /**
     * Finds an invoice linked to a specific delivery ID.
     */
    public InvoiceResponse getInvoiceByDeliveryId(Long deliveryId) {
        Invoice invoice = invoiceRepository.findByDelivery_DeliveryId(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for Delivery", deliveryId));
        return mapToResponse(invoice);
    }

    /**
     * Updates invoice payment status (e.g. to PAID).
     */
    @Transactional
    public InvoiceResponse updateInvoiceStatus(Long id, String status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
                
        invoice.setStatus(InvoiceStatus.valueOf(status.toUpperCase()));
        return mapToResponse(invoiceRepository.save(invoice));
    }

    /**
     * Maps Invoice JPA entity to InvoiceResponse DTO.
     */
    private InvoiceResponse mapToResponse(Invoice i) {
        return InvoiceResponse.builder()
                .invoiceId(i.getInvoiceId())
                .deliveryId(i.getDelivery() != null ? i.getDelivery().getDeliveryId() : null)
                .customerName(i.getCustomerName())
                .invoiceDate(i.getInvoiceDate())
                .amount(i.getAmount())
                .status(i.getStatus())
                .remarks(i.getRemarks())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
