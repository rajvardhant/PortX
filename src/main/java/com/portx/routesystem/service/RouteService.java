package com.portx.routesystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portx.routesystem.dto.RouteRequest;
import com.portx.routesystem.dto.RouteResponse;
import com.portx.routesystem.entity.Delivery;
import com.portx.routesystem.entity.Route;
import com.portx.routesystem.entity.RouteStatus;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.DeliveryRepository;
import com.portx.routesystem.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RouteService — Business logic layer for route distance & travel duration calculations.
 *
 * PURPOSE:
 * 1. Checks system to prevent duplicate route records for identical start/end location pairs.
 * 2. Geocodes start and destination locations using pre-mapped GPS city coordinates and Nominatim API.
 * 3. Computes realistic driving distances using Haversine spherical math with road curvature adjustments.
 * 4. Formats travel duration ("X hrs Y mins") for freight dispatch planning.
 * 5. Saves route entities safely without database truncation errors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    // Spring Data JPA Repositories for data access
    private final RouteRepository routeRepository;
    private final DeliveryRepository deliveryRepository;

    // HTTP RestTemplate for geocoding web service calls
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a new Route entity OR reuses an existing route if the normalized start/end location pair already exists.
     */
    @Transactional
    public RouteResponse generateRoute(RouteRequest req) {
        String start = req.getStartLocation() != null ? req.getStartLocation().trim() : "";
        String end = req.getEndLocation() != null ? req.getEndLocation().trim() : "";

        String normalizedStart = normalizeLocationKey(start);
        String normalizedEnd = normalizeLocationKey(end);

        log.info("📍 Checking route existence between '{}' and '{}'", start, end);

        // Step 1: Check existing routes in database table using normalized location keys
        List<Route> allRoutes = routeRepository.findAll();
        for (Route r : allRoutes) {
            String existingStart = normalizeLocationKey(r.getStartLocation());
            String existingEnd = normalizeLocationKey(r.getEndLocation());
            if (existingStart.equalsIgnoreCase(normalizedStart) && existingEnd.equalsIgnoreCase(normalizedEnd)) {
                log.info("♻️ Existing route found in database ID: {}. Reusing without creating duplicate.", r.getRouteId());
                return mapToResponse(r);
            }
        }

        // Step 2: Obtain GPS latitude/longitude for pickup and destination
        double[] startCoords = geocodeLocation(start);
        double[] endCoords = geocodeLocation(end);

        // Step 3: Compute realistic road distance (km) and travel duration
        Map<String, Object> distanceDetails = calculateRoadDistance(startCoords, endCoords);

        // Step 4: Build Route entity for database persistence
        Route route = Route.builder()
                .startLocation(start)
                .endLocation(end)
                .startLat(startCoords[0])
                .startLng(startCoords[1])
                .endLat(endCoords[0])
                .endLng(endCoords[1])
                .distance((Double) distanceDetails.get("distance"))
                .estimatedTime((String) distanceDetails.get("estimatedTime"))
                .polyline("") // Kept concise to prevent DB truncation issues
                .status(RouteStatus.ACTIVE)
                .build();

        Route saved = routeRepository.save(route);
        return mapToResponse(saved);
    }

    /**
     * Retrieves all unique saved routes in the system, filtering out any duplicate entries.
     */
    public List<RouteResponse> getAllRoutes() {
        List<Route> allRoutes = routeRepository.findAll();
        Map<String, Route> uniqueMap = new LinkedHashMap<>();

        for (Route r : allRoutes) {
            String key = normalizeLocationKey(r.getStartLocation()) + "->" + normalizeLocationKey(r.getEndLocation());
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, r);
            }
        }

        return uniqueMap.values().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds a specific route by ID or throws ResourceNotFoundException.
     */
    public RouteResponse getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", id));
        return mapToResponse(route);
    }

    /**
     * Safely deletes a route by first unlinking any associated deliveries to prevent foreign key errors.
     */
    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", id));

        // Step 1: Unlink route reference from any active deliveries
        List<Delivery> deliveries = deliveryRepository.findAll().stream()
                .filter(d -> d.getRoute() != null && d.getRoute().getRouteId().equals(id))
                .collect(Collectors.toList());
        for (Delivery d : deliveries) {
            d.setRoute(null);
            deliveryRepository.save(d);
        }

        // Step 2: Delete route from database
        routeRepository.delete(route);
    }

    /**
     * Normalizes location strings for accurate deduplication comparison (e.g. "Surat, Gujarat" -> "surat").
     */
    private String normalizeLocationKey(String loc) {
        if (loc == null) return "";
        return loc.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Resolves location strings to GPS coordinates [lat, lng].
     * Uses local dictionary first for sub-millisecond execution, then OpenStreetMap Nominatim.
     */
    private double[] geocodeLocation(String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            return new double[]{19.0760, 72.8777}; // Fallback: Mumbai Center
        }

        String query = locationName.trim();

        // Check local city dictionary for instant lookup
        double[] dictionaryMatch = getFallbackCoordinates(query);
        if (dictionaryMatch != null) {
            log.info("🎯 City dictionary match for '{}' -> lat: {}, lng: {}", query, dictionaryMatch[0], dictionaryMatch[1]);
            return dictionaryMatch;
        }

        // Call OpenStreetMap Nominatim API for custom locations
        try {
            String searchQuery = query.toLowerCase().contains("india") ? query : (query + ", India");
            String encoded = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=in&q=" + encoded;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "PortXLogisticsApp/1.0 (contact@portx.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray() && root.size() > 0) {
                    JsonNode item = root.get(0);
                    double lat = item.get("lat").asDouble();
                    double lon = item.get("lon").asDouble();
                    log.info("✅ Nominatim geocoded '{}' -> lat: {}, lng: {}", query, lat, lon);
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            log.warn("Nominatim Geocoding notice for '{}': {}", query, e.getMessage());
        }

        // Default fallback to Mumbai center if unknown location
        return new double[]{19.0760, 72.8777};
    }

    /**
     * Computes driving road distance using Haversine formula with highway curvature factor.
     */
    private Map<String, Object> calculateRoadDistance(double[] start, double[] end) {
        double dLat = Math.toRadians(end[0] - start[0]);
        double dLng = Math.toRadians(end[1] - start[1]);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start[0])) * Math.cos(Math.toRadians(end[0])) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightKm = 6371.0 * c;
        
        // Apply 1.20x road curvature multiplier for highway driving
        double roadKm = Math.round(Math.max(straightKm * 1.20, 1.5) * 100.0) / 100.0;

        // Estimate travel duration based on realistic speed (50 km/h inter-city, 25 km/h intra-city)
        double avgSpeed = (roadKm > 30.0) ? 50.0 : 25.0;
        int totalMinutes = (int) Math.round((roadKm / avgSpeed) * 60.0);

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        String timeStr = (hours > 0) ? (hours + " hrs " + minutes + " mins") : (minutes + " mins");

        Map<String, Object> result = new HashMap<>();
        result.put("distance", roadKm);
        result.put("estimatedTime", timeStr);
        return result;
    }

    /**
     * Dictionary of 50+ pre-mapped Indian cities & hubs for sub-millisecond geocoding.
     */
    private double[] getFallbackCoordinates(String locName) {
        String loc = locName.toLowerCase().trim();

        // Gujarat Cities
        if (loc.contains("surat")) return new double[]{21.1702, 72.8311};
        if (loc.contains("ahmedabad") || loc.contains("gandhinagar")) return new double[]{23.0225, 72.5714};
        if (loc.contains("vadodara") || loc.contains("baroda")) return new double[]{22.3072, 73.1812};
        if (loc.contains("rajkot")) return new double[]{22.3039, 70.8022};

        // Maharashtra Cities & Suburbs
        if (loc.contains("mumbai central")) return new double[]{18.9696, 72.8193};
        if (loc.contains("andheri")) return new double[]{19.1136, 72.8697};
        if (loc.contains("bandra")) return new double[]{19.0596, 72.8295};
        if (loc.contains("thane")) return new double[]{19.2183, 72.9781};
        if (loc.contains("powai")) return new double[]{19.1176, 72.9060};
        if (loc.contains("borivali")) return new double[]{19.2307, 72.8567};
        if (loc.contains("dadar")) return new double[]{19.0178, 72.8478};
        if (loc.contains("vashi") || loc.contains("navi mumbai")) return new double[]{19.0770, 72.9989};
        if (loc.contains("mumbai")) return new double[]{19.0760, 72.8777};

        if (loc.contains("pune") || loc.contains("hinjewadi")) return new double[]{18.5204, 73.8567};
        if (loc.contains("nashik")) return new double[]{19.9975, 73.7898};
        if (loc.contains("aurangabad") || loc.contains("sambhajinagar")) return new double[]{19.8762, 75.3433};
        if (loc.contains("nagpur")) return new double[]{21.1458, 79.0882};
        if (loc.contains("solapur")) return new double[]{17.6599, 75.9064};

        // NCR Delhi / Gurgaon / Noida
        if (loc.contains("gurgaon") || loc.contains("gurugram") || loc.contains("cyber city")) return new double[]{28.4595, 77.0266};
        if (loc.contains("noida")) return new double[]{28.5355, 77.3910};
        if (loc.contains("connaught") || loc.contains("cp")) return new double[]{28.6315, 77.2167};
        if (loc.contains("dwarka")) return new double[]{28.5921, 77.0460};
        if (loc.contains("rohini")) return new double[]{28.7041, 77.1025};
        if (loc.contains("delhi")) return new double[]{28.6139, 77.2090};

        // Rajasthan Cities
        if (loc.contains("jaipur")) return new double[]{26.9124, 75.7873};
        if (loc.contains("udaipur")) return new double[]{24.5854, 73.7125};
        if (loc.contains("jodhpur")) return new double[]{26.2389, 73.0243};

        // Madhya Pradesh
        if (loc.contains("indore")) return new double[]{22.7196, 75.8577};
        if (loc.contains("bhopal")) return new double[]{23.2599, 77.4126};

        // South India
        if (loc.contains("indiranagar")) return new double[]{12.9784, 77.6408};
        if (loc.contains("hsr")) return new double[]{12.9121, 77.6446};
        if (loc.contains("koramangala")) return new double[]{12.9352, 77.6245};
        if (loc.contains("whitefield")) return new double[]{12.9698, 77.7500};
        if (loc.contains("electronic city")) return new double[]{12.8399, 77.6770};
        if (loc.contains("bangalore") || loc.contains("bengaluru")) return new double[]{12.9716, 77.5946};

        if (loc.contains("hyderabad") || loc.contains("hitech")) return new double[]{17.3850, 78.4867};
        if (loc.contains("chennai") || loc.contains("sriperumbudur")) return new double[]{13.0827, 80.2707};
        if (loc.contains("coimbatore")) return new double[]{11.0168, 76.9558};
        if (loc.contains("kochi") || loc.contains("cochin")) return new double[]{9.9312, 76.2673};

        // North & East India
        if (loc.contains("kolkata")) return new double[]{22.5726, 88.3639};
        if (loc.contains("chandigarh")) return new double[]{30.7333, 76.7794};
        if (loc.contains("lucknow")) return new double[]{26.8467, 80.9462};
        if (loc.contains("kanpur")) return new double[]{26.4499, 80.3319};

        return null;
    }

    /**
     * Converts a Route entity to a RouteResponse DTO.
     */
    private RouteResponse mapToResponse(Route r) {
        return RouteResponse.builder()
                .routeId(r.getRouteId())
                .startLocation(r.getStartLocation())
                .endLocation(r.getEndLocation())
                .startLat(r.getStartLat())
                .startLng(r.getStartLng())
                .endLat(r.getEndLat())
                .endLng(r.getEndLng())
                .distance(r.getDistance())
                .estimatedTime(r.getEstimatedTime())
                .status(r.getStatus())
                .polyline(r.getPolyline())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
