package com.portx.routesystem.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardStats {
    private long totalDrivers;
    private long totalVehicles;
    private long totalDeliveries;
    private long pendingDeliveries;
    private long ongoingDeliveries;
    private long completedDeliveries;
    private long pendingInvoices;
    private long completedInvoices;
}
