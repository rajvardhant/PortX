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
 * WebController - Main MVC Controller handling page navigation and HTML form submissions.
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final DeliveryService deliveryService;
    private final InvoiceService invoiceService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    // PUBLIC & AUTHENTICATION PAGES

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/services")
    public String services() {
        return "redirect:/#services";
    }

    @GetMapping("/about")
    public String about() {
        return "redirect:/#about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "redirect:/#contact";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

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

    // DASHBOARDS

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("recentDeliveries", dashboardService.getRecentDeliveries());
        model.addAttribute("deliveryStatusCounts", dashboardService.getDeliveryStatusCounts());
        model.addAttribute("invoiceStatusCounts", dashboardService.getInvoiceStatusCounts());
        return "admin/dashboard";
    }

    @GetMapping("/dispatcher/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String dispatcherDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("recentDeliveries", dashboardService.getRecentDeliveries());
        model.addAttribute("deliveryStatusCounts", dashboardService.getDeliveryStatusCounts());
        model.addAttribute("invoiceStatusCounts", dashboardService.getInvoiceStatusCounts());
        return "admin/dashboard";
    }

    // DRIVER PORTAL & HISTORY

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

    @RequestMapping(value = "/driver/deliveries/{id}/status", method = {RequestMethod.GET, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'DISPATCHER')")
    public String updateDriverDeliveryStatus(@PathVariable Long id, @RequestParam String status) {
        deliveryService.updateDeliveryStatus(id, status);
        return "redirect:/driver/dashboard";
    }

    // DRIVER MANAGEMENT

    @GetMapping("/admin/drivers")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listDrivers(Model model) {
        model.addAttribute("drivers", driverService.getAllDrivers());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/drivers";
    }

    @GetMapping("/admin/drivers/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newDriverForm(Model model) {
        DriverRequest request = new DriverRequest();
        model.addAttribute("driverRequest", request);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/driver-form";
    }

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

    @PostMapping("/admin/drivers/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return "redirect:/admin/drivers";
    }

    // VEHICLE MANAGEMENT

    @GetMapping("/admin/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/vehicles";
    }

    @GetMapping("/admin/vehicles/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newVehicleForm(Model model) {
        VehicleRequest request = new VehicleRequest();
        model.addAttribute("vehicleRequest", request);
        return "admin/vehicle-form";
    }

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

    @PostMapping("/admin/vehicles/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return "redirect:/admin/vehicles";
    }

    // ROUTE MANAGEMENT

    @GetMapping("/admin/routes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listRoutes(Model model) {
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/routes";
    }

    @GetMapping("/admin/routes/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String newRouteForm() {
        return "admin/route-form";
    }

    @PostMapping("/admin/routes/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return "redirect:/admin/routes";
    }

    // DELIVERY MANAGEMENT

    @GetMapping({"/admin/deliveries", "/dispatcher/deliveries"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listDeliveries(Model model) {
        model.addAttribute("deliveries", deliveryService.getAllDeliveries());
        model.addAttribute("drivers", driverService.getAllDrivers());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/deliveries";
    }

    @GetMapping({"/admin/deliveries/new", "/dispatcher/deliveries/new"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String newDeliveryForm(Model model) {
        model.addAttribute("deliveryRequest", new DeliveryRequest());
        model.addAttribute("drivers", driverService.getAvailableDrivers());
        model.addAttribute("vehicles", vehicleService.getAvailableVehicles());
        model.addAttribute("routes", routeService.getAllRoutes());
        return "admin/delivery-form";
    }

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

    @PostMapping({"/admin/deliveries/{id}/delete", "/dispatcher/deliveries/{id}/delete"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return "redirect:/admin/deliveries";
    }

    // INVOICE MANAGEMENT

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice/list";
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String invoiceDetail(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoiceById(id));
        return "invoice/detail";
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public String payInvoice(@PathVariable Long id) {
        invoiceService.updateInvoiceStatus(id, "PAID");
        return "redirect:/invoices";
    }
}
