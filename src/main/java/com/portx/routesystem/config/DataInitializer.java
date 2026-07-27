package com.portx.routesystem.config;

import com.portx.routesystem.entity.*;
import com.portx.routesystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DataInitializer — Runs on startup.
 * Automatically cleans up duplicate routes, migrates legacy database rows (BIKE -> VAN),
 * and initializes default system users with valid encoded passwords.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {

        // ── 0. Database Migration: Fix legacy BIKE rows in MySQL ─────────
        try {
            int updated = jdbcTemplate.update("UPDATE vehicles SET vehicle_type = 'VAN' WHERE vehicle_type = 'BIKE'");
            if (updated > 0) {
                log.info("✅ Converted {} legacy BIKE vehicle record(s) to VAN", updated);
            }
        } catch (Exception e) {
            log.warn("Database cleanup notice: {}", e.getMessage());
        }

        // ── 0.1 Database Cleanup: Remove duplicate route records ─────────
        try {
            List<Route> allRoutes = routeRepository.findAll();
            Map<String, Route> uniqueMap = new HashMap<>();
            List<Route> duplicatesToDelete = new ArrayList<>();

            for (Route r : allRoutes) {
                if (r.getStartLocation() == null || r.getEndLocation() == null) continue;
                String startKey = r.getStartLocation().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                String endKey = r.getEndLocation().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                String pairKey = startKey + "->" + endKey;

                if (uniqueMap.containsKey(pairKey)) {
                    // Unlink deliveries pointing to duplicate and point to primary route
                    Route primaryRoute = uniqueMap.get(pairKey);
                    List<Delivery> linkedDeliveries = deliveryRepository.findAll().stream()
                            .filter(d -> d.getRoute() != null && d.getRoute().getRouteId().equals(r.getRouteId()))
                            .collect(Collectors.toList());
                    for (Delivery d : linkedDeliveries) {
                        d.setRoute(primaryRoute);
                        deliveryRepository.save(d);
                    }
                    duplicatesToDelete.add(r);
                } else {
                    uniqueMap.put(pairKey, r);
                }
            }

            if (!duplicatesToDelete.isEmpty()) {
                routeRepository.deleteAll(duplicatesToDelete);
                log.info("🧹 Cleaned up {} duplicate route record(s) from database table", duplicatesToDelete.size());
            }
        } catch (Exception e) {
            log.warn("Route deduplication startup notice: {}", e.getMessage());
        }

        // ── 1. Default System Users ──────────────────────────────────────
        User adminUser      = createOrUpdateUser("admin",      "admin123", "admin@portx.com",      "System Administrator", UserRole.ADMIN);
        User dispatcherUser = createOrUpdateUser("dispatcher", "admin123", "dispatcher@portx.com", "John Dispatcher",      UserRole.DISPATCHER);
        User driverUser     = createOrUpdateUser("driver1",    "admin123", "driver1@portx.com",    "Mike Driver",          UserRole.DRIVER);

        // ── 2. Sample 10 Vehicles ─────────────────────────────────────────
        List<Vehicle> vehicles = new ArrayList<>();
        if (vehicleRepository.count() < 10) {
            String[][] sampleVehicles = {
                {"MH01AB1234", "TRUCK", "5000.0"},
                {"MH01CD5678", "VAN",   "1000.0"},
                {"DL01EF9012", "TRUCK", "7500.0"},
                {"KA03GH3456", "VAN",   "1200.0"},
                {"GJ01IJ7890", "TRUCK", "6000.0"},
                {"TN01KL1234", "VAN",   "1500.0"},
                {"KL07MN5678", "TRUCK", "8000.0"},
                {"HR26OP9012", "VAN",   "1100.0"},
                {"UP16QR3456", "TRUCK", "10000.0"},
                {"MH12ST7890", "VAN",   "1400.0"}
            };

            for (String[] vData : sampleVehicles) {
                if (!vehicleRepository.existsByRegistrationNumber(vData[0])) {
                    Vehicle v = vehicleRepository.save(Vehicle.builder()
                            .registrationNumber(vData[0])
                            .vehicleType(VehicleType.valueOf(vData[1]))
                            .capacity(Double.parseDouble(vData[2]))
                            .status(VehicleStatus.AVAILABLE)
                            .build());
                    vehicles.add(v);
                }
            }
            log.info("✅ 10 Sample Vehicles initialized");
        }
        vehicles = vehicleRepository.findAll();

        // ── 3. Sample 10 Drivers ──────────────────────────────────────────
        List<Driver> drivers = new ArrayList<>();
        if (driverRepository.count() < 10) {
            String[][] sampleDrivers = {
                {"Mike Driver",      "9876543210", "MH1234567"},
                {"Rahul Sharma",     "9820011223", "MH01202311"},
                {"Vikram Singh",     "9811223344", "DL04202155"},
                {"Amit Patel",       "9723456789", "GJ01202288"},
                {"Suresh Verma",     "9612345678", "KA02202044"},
                {"Rajesh Kumar",     "9501234567", "TN03201933"},
                {"Anil Deshmukh",    "9412345678", "MH12202199"},
                {"Priya Nair",       "9345678901", "KL07202211"},
                {"Sunil Chawla",     "9234567890", "HR26202077"},
                {"Deepak Gupta",     "9123456789", "UP16202166"}
            };

            int vIdx = 0;
            for (int i = 0; i < sampleDrivers.length; i++) {
                String[] dData = sampleDrivers[i];
                if (!driverRepository.existsByLicenseNumber(dData[2])) {
                    Vehicle assignedVehicle = (vIdx < vehicles.size()) ? vehicles.get(vIdx++) : null;
                    User linkedUser = (i == 0) ? driverUser : null; // Link driver1 user account to Mike Driver

                    Driver d = driverRepository.save(Driver.builder()
                            .name(dData[0])
                            .phone(dData[1])
                            .licenseNumber(dData[2])
                            .user(linkedUser)
                            .vehicle(assignedVehicle)
                            .status(DriverStatus.AVAILABLE)
                            .build());
                    drivers.add(d);
                }
            }
            log.info("✅ 10 Sample Drivers initialized");
        } else {
            // Make sure driver1 user is linked to first driver if not linked
            List<Driver> existingDrivers = driverRepository.findAll();
            if (!existingDrivers.isEmpty() && existingDrivers.get(0).getUser() == null) {
                Driver d1 = existingDrivers.get(0);
                d1.setUser(driverUser);
                driverRepository.save(d1);
            }
        }
        drivers = driverRepository.findAll();

        // ── 4. Sample 10 Routes ───────────────────────────────────────────
        List<Route> routes = new ArrayList<>();
        if (routeRepository.count() < 10) {
            String[][] sampleRoutes = {
                {"Mumbai Central", "Andheri East, Mumbai", "18.5", "45 mins"},
                {"Bandra West, Mumbai", "Thane West", "28.2", "55 mins"},
                {"Connaught Place, Delhi", "Cyber City, Gurgaon", "32.0", "50 mins"},
                {"MG Road, Bangalore", "Electronic City, Bangalore", "19.8", "40 mins"},
                {"SG Highway, Ahmedabad", "Sector 11, Gandhinagar", "25.4", "35 mins"},
                {"T. Nagar, Chennai", "Sriperumbudur Industrial Hub", "42.1", "65 mins"},
                {"MG Road, Kochi", "Infopark Kakkanad, Kochi", "14.6", "30 mins"},
                {"Sector 17, Chandigarh", "Mohali IT Park", "12.3", "25 mins"},
                {"Hazratganj, Lucknow", "Gomti Nagar, Lucknow", "11.0", "20 mins"},
                {"Pune Railway Station", "Hinjewadi Phase 1, Pune", "21.7", "45 mins"}
            };

            for (String[] rData : sampleRoutes) {
                Route r = routeRepository.save(Route.builder()
                        .startLocation(rData[0])
                        .endLocation(rData[1])
                        .distance(Double.parseDouble(rData[2]))
                        .estimatedTime(rData[3])
                        .status(RouteStatus.ACTIVE)
                        .build());
                routes.add(r);
            }
            log.info("✅ 10 Sample Routes initialized");
        }
        routes = routeRepository.findAll();

        // ── 5. Sample 10 Deliveries & Invoices ───────────────────────────
        if (deliveryRepository.count() < 10) {
            String[][] sampleDeliveries = {
                {"Sunny", "123 Anywhere St., Mumbai", "15.5", "PENDING"},
                {"Rajesh Transport", "456 Commerce Rd., Thane", "45.0", "ASSIGNED"},
                {"TechCorp India", "789 Cyber Hub, Gurgaon", "20.0", "OUT_FOR_DELIVERY"},
                {"Infotech Systems", "101 IT Park, Bangalore", "12.5", "DELIVERED"},
                {"Swastik Logistics", "202 GIDC, Gandhinagar", "65.0", "PENDING"},
                {"Global Auto Parts", "303 Industrial Estate, Sriperumbudur", "110.0", "DELIVERED"},
                {"Cyberpark Solutions", "404 Infopark, Kochi", "8.0", "ASSIGNED"},
                {"Northern Retail", "505 Mall Rd., Mohali", "35.0", "DELIVERED"},
                {"Avadh Enterprises", "606 Express Zone, Lucknow", "25.0", "PENDING"},
                {"Deccan Traders", "707 Phase 1, Hinjewadi", "50.0", "DELIVERED"}
            };

            for (int i = 0; i < sampleDeliveries.length; i++) {
                String[] dData = sampleDeliveries[i];
                Driver assignedDriver = (i < drivers.size()) ? drivers.get(i) : null;
                Vehicle assignedVehicle = (i < vehicles.size()) ? vehicles.get(i) : null;
                Route assignedRoute = (i < routes.size()) ? routes.get(i) : null;

                DeliveryStatus delStatus = DeliveryStatus.valueOf(dData[3]);

                Delivery delivery = deliveryRepository.save(Delivery.builder()
                        .customerName(dData[0])
                        .customerAddress(dData[1])
                        .packageWeight(Double.parseDouble(dData[2]))
                        .status(delStatus)
                        .driver(assignedDriver)
                        .vehicle(assignedVehicle)
                        .route(assignedRoute)
                        .notes("Standard priority delivery for " + dData[0])
                        .build());

                // Auto Create Invoice
                double dist = (assignedRoute != null && assignedRoute.getDistance() != null) ? assignedRoute.getDistance() : 10.0;
                double amount = 100.0 + (dist * 10.0);

                InvoiceStatus invStatus = (delStatus == DeliveryStatus.DELIVERED) ? InvoiceStatus.PAID : InvoiceStatus.PENDING;

                if (invoiceRepository.findByDelivery_DeliveryId(delivery.getDeliveryId()).isEmpty()) {
                    invoiceRepository.save(Invoice.builder()
                            .delivery(delivery)
                            .customerName(delivery.getCustomerName())
                            .invoiceDate(LocalDate.now())
                            .amount(amount)
                            .status(invStatus)
                            .remarks("Auto-generated invoice for Delivery #" + delivery.getDeliveryId())
                            .build());
                }
            }
            log.info("✅ 10 Sample Deliveries and Invoices initialized");
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 PortX Logistics ready → http://localhost:8080");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private User createOrUpdateUser(String username, String rawPassword,
                                    String email, String fullName, UserRole role) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            user = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .role(role)
                    .build();
            log.info("✅ Creating user: {}", username);
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }
}
