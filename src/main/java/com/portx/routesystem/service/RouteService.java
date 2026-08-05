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
import java.util.*;
import java.util.stream.Collectors;

/**
 * RouteService - Business logic layer for route distance & travel duration calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final DeliveryRepository deliveryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Generates a new Route or returns existing route if already saved.
     */
    @Transactional
    public RouteResponse generateRoute(RouteRequest req) {
        String start = req.getStartLocation() != null ? req.getStartLocation().trim() : "";
        String end = req.getEndLocation() != null ? req.getEndLocation().trim() : "";

        String normalizedStart = normalizeLocationKey(start);
        String normalizedEnd = normalizeLocationKey(end);

        log.info("Checking route existence between '{}' and '{}'", start, end);

        // Step 1: Check existing routes in database table using normalized location keys
        List<Route> allRoutes = routeRepository.findAll();
        for (Route r : allRoutes) {
            String existingStart = normalizeLocationKey(r.getStartLocation());
            String existingEnd = normalizeLocationKey(r.getEndLocation());
            if (existingStart.equalsIgnoreCase(normalizedStart) && existingEnd.equalsIgnoreCase(normalizedEnd)) {
                log.info("Existing route found in database ID: {}. Reusing without creating duplicate.", r.getRouteId());
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
                .polyline("")
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

    private String normalizeLocationKey(String loc) {
        if (loc == null) return "";
        return loc.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private double[] geocodeLocation(String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            return new double[]{19.0760, 72.8777}; // Fallback: Mumbai Center
        }

        String query = locationName.trim();

        double[] dictionaryMatch = getFallbackCoordinates(query);
        if (dictionaryMatch != null) {
            log.info("City dictionary match for '{}' -> lat: {}, lng: {}", query, dictionaryMatch[0], dictionaryMatch[1]);
            return dictionaryMatch;
        }

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
                    log.info("Nominatim geocoded '{}' -> lat: {}, lng: {}", query, lat, lon);
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for '{}': {}", query, e.getMessage());
        }

        return new double[]{19.0760, 72.8777};
    }

    private Map<String, Object> calculateRoadDistance(double[] startCoords, double[] endCoords) {
        Map<String, Object> result = new HashMap<>();

        try {
            String url = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                startCoords[1], startCoords[0], endCoords[1], endCoords[0]
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "PortXLogisticsApp/1.0 (contact@portx.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("routes") && root.get("routes").isArray() && root.get("routes").size() > 0) {
                    JsonNode routeNode = root.get("routes").get(0);
                    double distanceMeters = routeNode.get("distance").asDouble();
                    double durationSeconds = routeNode.get("duration").asDouble();

                    double distanceKm = Math.round((distanceMeters / 1000.0) * 100.0) / 100.0;
                    int minutes = (int) Math.round(durationSeconds / 60.0);
                    String timeFormatted = formatDuration(minutes);

                    result.put("distance", distanceKm);
                    result.put("estimatedTime", timeFormatted);
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("OSRM calculation failed, fallback to Haversine: {}", e.getMessage());
        }

        double distanceKm = calculateHaversineDistance(startCoords[0], startCoords[1], endCoords[0], endCoords[1]);
        distanceKm = Math.round(distanceKm * 1.25 * 100.0) / 100.0; // 25% road curvature factor
        int minutes = (int) Math.round((distanceKm / 40.0) * 60);

        result.put("distance", distanceKm);
        result.put("estimatedTime", formatDuration(minutes));
        return result;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " mins";
        }
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        return mins > 0 ? hours + " hrs " + mins + " mins" : hours + " hrs";
    }

    private double[] getFallbackCoordinates(String city) {
        String key = normalizeLocationKey(city);
        Map<String, double[]> dict = new HashMap<>();
        dict.put("mumbai", new double[]{19.0760, 72.8777});
        dict.put("mumbaicentral", new double[]{18.9696, 72.8193});
        dict.put("andheri", new double[]{19.1136, 72.8697});
        dict.put("bandra", new double[]{19.0596, 72.8295});
        dict.put("thane", new double[]{19.2183, 72.9781});
        dict.put("delhi", new double[]{28.6139, 77.2090});
        dict.put("gurgaon", new double[]{28.4595, 77.0266});
        dict.put("bangalore", new double[]{12.9716, 77.5946});
        dict.put("bengaluru", new double[]{12.9716, 77.5946});
        dict.put("ahmedabad", new double[]{23.0225, 72.5714});
        dict.put("gandhinagar", new double[]{23.2156, 72.6369});
        dict.put("pune", new double[]{18.5204, 73.8567});
        dict.put("surat", new double[]{21.1702, 72.8311});

        for (Map.Entry<String, double[]> entry : dict.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

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
                .polyline(r.getPolyline())
                .status(r.getStatus())
                .build();
    }
}
