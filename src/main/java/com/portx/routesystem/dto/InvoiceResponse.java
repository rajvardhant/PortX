package com.portx.routesystem.dto;

import com.portx.routesystem.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    private Long invoiceId;
    private Long deliveryId;
    private String customerName;
    private LocalDate invoiceDate;
    private Double amount;
    private InvoiceStatus status;
    private String remarks;
    private LocalDateTime createdAt;
}
