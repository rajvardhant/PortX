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
import java.util.List;

/**
 * DataInitializer - Runs on application startup.
 * Truncates database tables to reset AUTO_INCREMENT primary key counters back to 1
 * and populates EXACTLY 5 clean sample records in each category (IDs 1, 2, 3, 4, 5).
 * Enables distinct role demo login accounts (admin, dispatcher, driver1).
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

        // 0. TRUNCATE TABLES & RESET AUTO-INCREMENT COUNTERS TO 1
        try {
            log.info("Truncating database tables and resetting Auto-Increment counters to 1...");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("TRUNCATE TABLE invoices");
            jdbcTemplate.execute("TRUNCATE TABLE deliveries");
            jdbcTemplate.execute("TRUNCATE TABLE routes");
            jdbcTemplate.execute("TRUNCATE TABLE drivers");
            jdbcTemplate.execute("TRUNCATE TABLE vehicles");
            jdbcTemplate.execute("TRUNCATE TABLE users");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            log.info("Database tables truncated and Auto-Increment counters reset to 1 successfully.");
        } catch (Exception e) {
            log.warn("Database reset notice: {}", e.getMessage());
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (Exception ignored) {}
        }

        // 1. DISTINCT ROLE DEMO SYSTEM USERS (ADMIN, DISPATCHER, DRIVER)
        User adminUser      = createOrUpdateUser("admin",      "admin123", "admin@portx.com",      "System Administrator", UserRole.ADMIN);
        User dispatcherUser = createOrUpdateUser("dispatcher", "admin123", "dispatcher@portx.com", "John Dispatcher",      UserRole.DISPATCHER);
        User driverUser     = createOrUpdateUser("driver1",    "admin123", "driver1@portx.com",    "Mike Driver",          UserRole.DRIVER);

        // 2. EXACTLY 5 SAMPLE VEHICLES (IDs 1 to 5)
        List<Vehicle> vehicles = new ArrayList<>();
        String[][] sampleVehicles = {
            {"MH01AB1234", "TRUCK", "5000.0"},
            {"MH01CD5678", "VAN",   "1000.0"},
            {"DL01EF9012", "TRUCK", "7500.0"},
            {"KA03GH3456", "VAN",   "1200.0"},
            {"GJ01IJ7890", "TRUCK", "6000.0"}
        };

        for (String[] vData : sampleVehicles) {
            Vehicle v = vehicleRepository.save(Vehicle.builder()
                    .registrationNumber(vData[0])
                    .vehicleType(VehicleType.valueOf(vData[1]))
                    .capacity(Double.parseDouble(vData[2]))
                    .status(VehicleStatus.AVAILABLE)
                    .build());
            vehicles.add(v);
        }
        log.info("5 Sample Vehicles initialized (IDs 1-5)");

        // 3. EXACTLY 5 SAMPLE DRIVERS (IDs 1 to 5)
        List<Driver> drivers = new ArrayList<>();
        String[][] sampleDrivers = {
            {"Mike Driver",      "9876543210", "MH1234567"},
            {"Rahul Sharma",     "9820011223", "MH01202311"},
            {"Vikram Singh",     "9811223344", "DL04202155"},
            {"Amit Patel",       "9723456789", "GJ01202288"},
            {"Suresh Verma",     "9612345678", "KA02202044"}
        };

        for (int i = 0; i < sampleDrivers.length; i++) {
            String[] dData = sampleDrivers[i];
            Vehicle assignedVehicle = vehicles.get(i);
            User linkedUser = (i == 0) ? driverUser : null;

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
        log.info("5 Sample Drivers initialized (IDs 1-5)");

        // 4. EXACTLY 5 SAMPLE ROUTES (IDs 1 to 5)
        List<Route> routes = new ArrayList<>();
        String[][] sampleRoutes = {
            {"Mumbai Central", "Andheri East, Mumbai", "18.5", "45 mins", "18.9696", "72.8193", "19.1136", "72.8697"},
            {"Bandra West, Mumbai", "Thane West", "28.2", "55 mins", "19.0596", "72.8295", "19.2183", "72.9781"},
            {"Connaught Place, Delhi", "Cyber City, Gurgaon", "32.0", "50 mins", "28.6315", "77.2167", "28.4595", "77.0266"},
            {"MG Road, Bangalore", "Electronic City, Bangalore", "19.8", "40 mins", "12.9716", "77.5946", "12.8399", "77.6770"},
            {"SG Highway, Ahmedabad", "Sector 11, Gandhinagar", "25.4", "35 mins", "23.0225", "72.5714", "23.2156", "72.6369"}
        };

        for (String[] rData : sampleRoutes) {
            Route r = routeRepository.save(Route.builder()
                    .startLocation(rData[0])
                    .endLocation(rData[1])
                    .distance(Double.parseDouble(rData[2]))
                    .estimatedTime(rData[3])
                    .startLat(Double.parseDouble(rData[4]))
                    .startLng(Double.parseDouble(rData[5]))
                    .endLat(Double.parseDouble(rData[6]))
                    .endLng(Double.parseDouble(rData[7]))
                    .polyline("")
                    .status(RouteStatus.ACTIVE)
                    .build());
            routes.add(r);
        }
        log.info("5 Sample Routes initialized (IDs 1-5)");

        // 5. EXACTLY 5 SAMPLE DELIVERIES & INVOICES (IDs 1 to 5)
        String[][] sampleDeliveries = {
            {"Sunny", "123 Anywhere St., Mumbai", "15.5", "PENDING"},
            {"Rajesh Transport", "456 Commerce Rd., Thane", "45.0", "ASSIGNED"},
            {"TechCorp India", "789 Cyber Hub, Gurgaon", "20.0", "OUT_FOR_DELIVERY"},
            {"Infotech Systems", "101 IT Park, Bangalore", "12.5", "DELIVERED"},
            {"Swastik Logistics", "202 GIDC, Gandhinagar", "65.0", "PENDING"}
        };

        for (int i = 0; i < sampleDeliveries.length; i++) {
            String[] dData = sampleDeliveries[i];
            Driver assignedDriver = drivers.get(i);
            Vehicle assignedVehicle = vehicles.get(i);
            Route assignedRoute = routes.get(i);
            DeliveryStatus delStatus = DeliveryStatus.valueOf(dData[3]);

            if (delStatus == DeliveryStatus.ASSIGNED || delStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
                assignedVehicle.setStatus(VehicleStatus.IN_TRANSIT);
                vehicleRepository.save(assignedVehicle);

                assignedDriver.setStatus(DriverStatus.ON_DUTY);
                driverRepository.save(assignedDriver);
            }

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

            double dist = (assignedRoute != null && assignedRoute.getDistance() != null) ? assignedRoute.getDistance() : 10.0;
            double amount = 100.0 + (dist * 10.0);
            InvoiceStatus invStatus = (delStatus == DeliveryStatus.DELIVERED) ? InvoiceStatus.PAID : InvoiceStatus.PENDING;

            invoiceRepository.save(Invoice.builder()
                    .delivery(delivery)
                    .customerName(delivery.getCustomerName())
                    .invoiceDate(LocalDate.now())
                    .amount(amount)
                    .status(invStatus)
                    .remarks("Auto-generated invoice for Delivery #" + delivery.getDeliveryId())
                    .build());
        }
        log.info("5 Sample Deliveries initialized with synced Vehicle IN_TRANSIT and Driver ON_DUTY status");

        log.info("PortX Logistics ready - http://localhost:8080");
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
            log.info("Creating demo user [{}] with role [{}]", username, role);
        } else {
            user.setRole(role);
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }
}
