package com.portx.routesystem.controller;

import com.portx.routesystem.dto.*;
import com.portx.routesystem.entity.DeliveryStatus;
import com.portx.routesystem.entity.Driver;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.repository.DriverRepository;
import com.portx.routesystem.repository.UserRepository;
import com.portx.routesystem.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebController — Main MVC Controller handling page navigation and HTML form submissions.
 *
 * PURPOSE:
 * 1. Serves public landing page, services, login, registration, and logout handlers.
 * 2. Provides distinct role-based dashboard views and permission enforcement.
 * 3. Handles form POST/GET actions for Drivers, Vehicles, Routes, Deliveries, and Invoices.
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    // Inject service layer components for business logic and model binding
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final DeliveryService deliveryService;
    private final InvoiceService invoiceService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC & AUTHENTICATION PAGES
    // ────────────────────────────────────────────────────────────────────────

    /** Serves public landing page */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Serves public services overview page */
    @GetMapping("/services")
    public String services() {
        return "public/services";
    }

    /** Serves public about page */
    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    /** Serves public contact support page */
    @GetMapping("/contact")
    public String contact() {
        return "public/contact";
    }

    /** Serves login portal page */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /** Serves register account page */
    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    /** Handles user logout via GET request */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    /** Serves 403 access denied page */
    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

    /** Dynamically redirects logged-in user to their role-specific dashboard */
    @GetMapping("/dashboard")
    public String dashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"))) {
                return "redirect:/driver/dashboard";
            } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DISPATCHER"))) {
                return "redirect:/dispatcher/dashboard";
            }
        }
        return "redirect:/admin/dashboard";
    }

    // ────────────────────────────────────────────────────────────────────────
    // ADMIN DASHBOARD
    // ────────────────────────────────────────────────────────────────────────

    /** Loads Admin overview dashboard with system statistics and status charts */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("recentDeliveries", dashboardService.getRecentDeliveries());
        model.addAttribute("deliveryStatusCounts", dashboardService.getDeliveryStatusCounts());
        model.addAttribute("invoiceStatusCounts", dashboardService.getInvoiceStatusCounts());
        return "admin/dashboard";
    }

    // ────────────────────────────────────────────────────────────────────────
    // DISPATCHER DASHBOARD (Uses same visual dashboard design as Admin)
    // ────────────────────────────────────────────────────────────────────────

    /** Serves Dispatcher Operations Dashboard using the full Admin dashboard UI */
    @GetMapping("/dispatcher/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String dispatcherDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("recentDeliveries", dashboardService.getRecentDeliveries());
        model.addAttribute("deliveryStatusCounts", dashboardService.getDeliveryStatusCounts());
        model.addAttribute("invoiceStatusCounts", dashboardService.getInvoiceStatusCounts());
        return "admin/dashboard";
    }

    // ────────────────────────────────────────────────────────────────────────
    // DRIVER PORTAL & HISTORY
    // ────────────────────────────────────────────────────────────────────────

    /** Serves Driver Portal showing active assigned deliveries for logged-in driver */
    @GetMapping("/driver/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public String driverDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user != null) {
            Driver driver = driverRepository.findAll().stream()
                    .filter(d -> d.getUser() != null && d.getUser().getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);
                    
            if (driver != null) {
                List<DeliveryResponse> activeDeliveries = deliveryService.getDeliveriesByDriver(driver.getDriverId()).stream()
                        .filter(d -> d.getStatus() != DeliveryStatus.DELIVERED)
                        .collect(Collectors.toList());
                model.addAttribute("deliveries", activeDeliveries);
                model.addAttribute("driver", driver);
            } else {
                List<DeliveryResponse> activeDeliveries = deliveryService.getAllDeliveries().stream()
                        .filter(d -> d.getStatus() != DeliveryStatus.DELIVERED)
                        .collect(Collectors.toList());
                model.addAttribute("deliveries", activeDeliveries);
            }
        } else {
            List<DeliveryResponse> activeDeliveries = deliveryService.getAllDeliveries().stream()
                    .filter(d -> d.getStatus() != DeliveryStatus.DELIVERED)
                    .collect(Collectors.toList());
            model.addAttribute("deliveries", activeDeliveries);
        }
        
        return "driver/dashboard";
    }

    /** Serves Driver Delivery History showing completed shipments */
    @GetMapping("/driver/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public String driverHistory(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        
        List<DeliveryResponse> completedDeliveries;
        if (user != null) {
            Driver driver = driverRepository.findAll().stream()
                    .filter(d -> d.getUser() != null && d.getUser().getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);
                    
            if (driver != null) {
                completedDeliveries = deliveryService.getDeliveriesByDriver(driver.getDriverId()).stream()
                        .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                        .collect(Collectors.toList());
                model.addAttribute("driver", driver);
            } else {
                completedDeliveries = deliveryService.getAllDeliveries().stream()
                        .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                        .collect(Collectors.toList());
            }
        } else {
            completedDeliveries = deliveryService.getAllDeliveries().stream()
                    .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                    .collect(Collectors.toList());
        }
        
        model.addAttribute("completedDeliveries", completedDeliveries);
        return "driver/history";
    }

    /** Updates delivery status from Driver Portal */
    @RequestMapping(value = "/driver/deliveries/{id}/status", method = {RequestMethod.GET, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'DISPATCHER')")
    public String updateDriverDeliveryStatus(@PathVariable Long id, @RequestParam String status) {
        deliveryService.updateDeliveryStatus(id, status);
        return "redirect:/driver/dashboard";
    }

    // ────────────────────────────────────────────────────────────────────────
    // DRIVER MANAGEMENT (Admin full CRUD, Dispatcher Read-Only)
    // ────────────────────────────────────────────────────────────────────────

    /** Displays driver directory table */
    @GetMapping("/admin/drivers")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listDrivers(Model model) {
        model.addAttribute("drivers", driverService.getAllDrivers());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/drivers";
    }

    /** Opens Add Driver form (Admin Only) */
    @GetMapping("/admin/drivers/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newDriverForm(Model model) {
        DriverRequest request = new DriverRequest();
        model.addAttribute("driverRequest", request);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/driver-form";
    }

    /** Opens Edit Driver form pre-filled with driver details (Admin Only) */
    @GetMapping("/admin/drivers/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editDriverForm(@PathVariable Long id, Model model) {
        DriverResponse resp = driverService.getDriverById(id);
        DriverRequest request = new DriverRequest();
        request.setId(resp.getDriverId());
        request.setName(resp.getName());
        request.setPhone(resp.getPhone());
        request.setLicenseNumber(resp.getLicenseNumber());
        request.setStatus(resp.getStatus());
        request.setVehicleId(resp.getVehicleId());
        request.setUsername(resp.getUserUsername());

        model.addAttribute("driverRequest", request);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/driver-form";
    }

    /** Saves new or updated Driver record (Admin Only) */
    @PostMapping("/admin/drivers/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveDriver(@ModelAttribute("driverRequest") DriverRequest request) {
        if (request.getId() != null) {
            driverService.updateDriver(request.getId(), request);
        } else {
            driverService.createDriver(request);
        }
        return "redirect:/admin/drivers";
    }

    /** Deletes driver record (Admin Only) */
    @PostMapping("/admin/drivers/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return "redirect:/admin/drivers";
    }

    // ────────────────────────────────────────────────────────────────────────
    // VEHICLE MANAGEMENT (Admin full CRUD, Dispatcher Read-Only)
    // ────────────────────────────────────────────────────────────────────────

    /** Displays vehicle directory table */
    @GetMapping("/admin/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/vehicles";
    }

    /** Opens Add Vehicle form (Admin Only) */
    @GetMapping("/admin/vehicles/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newVehicleForm(Model model) {
        VehicleRequest request = new VehicleRequest();
        model.addAttribute("vehicleRequest", request);
        return "admin/vehicle-form";
    }

    /** Opens Edit Vehicle form (Admin Only) */
    @GetMapping("/admin/vehicles/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editVehicleForm(@PathVariable Long id, Model model) {
        VehicleResponse resp = vehicleService.getVehicleById(id);
        VehicleRequest request = new VehicleRequest();
        request.setId(resp.getVehicleId());
        request.setRegistrationNumber(resp.getRegistrationNumber());
        request.setVehicleType(resp.getVehicleType());
        request.setCapacity(resp.getCapacity());
        request.setStatus(resp.getStatus());

        model.addAttribute("vehicleRequest", request);
        return "admin/vehicle-form";
    }

    /** Saves new or updated Vehicle record (Admin Only) */
    @PostMapping("/admin/vehicles/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveVehicle(@ModelAttribute("vehicleRequest") VehicleRequest request) {
        if (request.getId() != null) {
            vehicleService.updateVehicle(request.getId(), request);
        } else {
            vehicleService.createVehicle(request);
        }
        return "redirect:/admin/vehicles";
    }

    /** Deletes vehicle record (Admin Only) */
    @PostMapping("/admin/vehicles/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return "redirect:/admin/vehicles";
    }

    // ────────────────────────────────────────────────────────────────────────
    // ROUTE MANAGEMENT
    // ────────────────────────────────────────────────────────────────────────

    /** Displays generated routes table */
    @GetMapping("/admin/routes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listRoutes(Model model) {
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/routes";
    }

    /** Opens Calculate Route Distance page (Admin & Dispatcher) */
    @GetMapping("/admin/routes/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String newRouteForm() {
        return "admin/route-form";
    }

    /** Deletes route record (Admin Only) */
    @PostMapping("/admin/routes/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return "redirect:/admin/routes";
    }

    // ────────────────────────────────────────────────────────────────────────
    // DELIVERY MANAGEMENT (CRUD)
    // ────────────────────────────────────────────────────────────────────────

    /** Displays deliveries directory table */
    @GetMapping({"/admin/deliveries", "/dispatcher/deliveries"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listDeliveries(Model model) {
        model.addAttribute("deliveries", deliveryService.getAllDeliveries());
        model.addAttribute("drivers", driverService.getAllDrivers());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/deliveries";
    }

    /** Opens Create Delivery form */
    @GetMapping({"/admin/deliveries/new", "/dispatcher/deliveries/new"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String newDeliveryForm(Model model) {
        model.addAttribute("deliveryRequest", new DeliveryRequest());
        model.addAttribute("drivers", driverService.getAvailableDrivers());
        model.addAttribute("vehicles", vehicleService.getAvailableVehicles());
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/delivery-form";
    }

    /** Opens Edit Delivery form */
    @GetMapping({"/admin/deliveries/{id}/edit", "/dispatcher/deliveries/{id}/edit"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String editDeliveryForm(@PathVariable Long id, Model model) {
        DeliveryResponse resp = deliveryService.getDeliveryById(id);
        DeliveryRequest request = new DeliveryRequest();
        request.setId(resp.getDeliveryId());
        request.setCustomerName(resp.getCustomerName());
        request.setCustomerAddress(resp.getCustomerAddress());
        request.setPackageWeight(resp.getPackageWeight());
        request.setNotes(resp.getNotes());
        request.setStatus(resp.getStatus());
        request.setDriverId(resp.getDriverId());
        request.setVehicleId(resp.getVehicleId());
        request.setRouteId(resp.getRouteId());

        model.addAttribute("deliveryRequest", request);
        model.addAttribute("drivers", driverService.getAllDrivers());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/delivery-form";
    }

    /** Saves new or updated Delivery record */
    @PostMapping({"/admin/deliveries/save", "/dispatcher/deliveries/save"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String saveDelivery(@ModelAttribute("deliveryRequest") DeliveryRequest request) {
        if (request.getId() != null) {
            deliveryService.updateDelivery(request.getId(), request);
            if (request.getStatus() != null) {
                deliveryService.updateDeliveryStatus(request.getId(), request.getStatus().name());
            }
        } else {
            deliveryService.createDelivery(request);
        }
        return "redirect:/admin/deliveries";
    }

    /** Deletes delivery record */
    @PostMapping({"/admin/deliveries/{id}/delete", "/dispatcher/deliveries/{id}/delete"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return "redirect:/admin/deliveries";
    }

    // ────────────────────────────────────────────────────────────────────────
    // INVOICE MANAGEMENT
    // ────────────────────────────────────────────────────────────────────────

    /** Displays all invoices table */
    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice/list";
    }

    /** Displays individual invoice detail view card */
    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String invoiceDetail(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoiceById(id));
        return "invoice/detail";
    }

    /** Marks invoice as PAID */
    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String payInvoice(@PathVariable Long id) {
        invoiceService.updateInvoiceStatus(id, "PAID");
        return "redirect:/invoices";
    }
}
