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
 * InvoiceService - Business logic layer for managing Rupee GST Invoices.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return mapToResponse(invoice);
    }

    public InvoiceResponse getInvoiceByDeliveryId(Long deliveryId) {
        Invoice invoice = invoiceRepository.findByDelivery_DeliveryId(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for Delivery ID", deliveryId));
        return mapToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoiceStatus(Long id, String statusStr) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        InvoiceStatus status = InvoiceStatus.valueOf(statusStr.toUpperCase());
        invoice.setStatus(status);

        return mapToResponse(invoiceRepository.save(invoice));
    }

    private InvoiceResponse mapToResponse(Invoice inv) {
        InvoiceResponse.InvoiceResponseBuilder builder = InvoiceResponse.builder()
                .invoiceId(inv.getInvoiceId())
                .customerName(inv.getCustomerName())
                .invoiceDate(inv.getInvoiceDate())
                .amount(inv.getAmount())
                .status(inv.getStatus())
                .remarks(inv.getRemarks());

        if (inv.getDelivery() != null) {
            builder.deliveryId(inv.getDelivery().getDeliveryId());
        }

        return builder.build();
    }
}
