package com.albany.mvc.controller.Admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class VehicleController extends AdminBaseController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    // Page rendering
    @GetMapping("/vehicles")
    public String vehiclesPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        addCommonAttributes(model);
        return "admin/underservices";
    }

    // API Endpoints for vehicle management
    @GetMapping("/api/customers/{customerId}/vehicles")
    @ResponseBody
    public ResponseEntity<?> getVehiclesForCustomer(
            @PathVariable Integer customerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId + "/vehicles",
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicles: " + e.getMessage()));
        }
    }

    @PostMapping("/api/customers/{customerId}/vehicles")
    @ResponseBody
    public ResponseEntity<?> createVehicleForCustomer(
            @PathVariable Integer customerId,
            @RequestBody Map<String, Object> vehicleData,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", validToken.startsWith("Bearer ") ?
                    validToken : "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            vehicleData.put("customerId", customerId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vehicleData, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId + "/vehicles",
                    HttpMethod.POST,
                    entity,
                    Object.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create vehicle: " + e.getMessage()));
        }
    }

    @GetMapping("/api/vehicles/{id}")
    @ResponseBody
    public ResponseEntity<?> getVehicleById(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicles/" + id,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicle details: " + e.getMessage()));
        }
    }

    @PutMapping("/api/vehicles/{id}")
    @ResponseBody
    public ResponseEntity<?> updateVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> vehicleData,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vehicleData, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicles/" + id,
                    HttpMethod.PUT,
                    entity,
                    Object.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update vehicle: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/vehicles/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteVehicle(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicles/" + id,
                    HttpMethod.DELETE,
                    entity,
                    Object.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete vehicle: " + e.getMessage()));
        }
    }

    // UPDATED METHOD - Enhanced vehicle under service fetching with fallback mechanisms
    @GetMapping("/api/vehicle-tracking/under-service")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getVehiclesUnderService(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            log.warn("Authorization token missing for vehicles under service request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            log.info("Fetching vehicles under service");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Try the primary API endpoint first
            String primaryUrl = apiBaseUrl + "/vehicle-tracking/vehicles-under-service";
            log.debug("Making request to: {}", primaryUrl);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        primaryUrl,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Map<String, Object>> vehiclesUnderService = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    log.info("Successfully retrieved {} vehicles under service", vehiclesUnderService.size());
                    return ResponseEntity.ok(vehiclesUnderService);
                } else {
                    log.warn("Unexpected response from primary API: {}", response.getStatusCode());
                }
            } catch (Exception e) {
                log.warn("Primary API call failed: {}", e.getMessage());
                // Continue to fallback
            }

            // Fallback: Try to get service requests and filter them
            log.info("Trying fallback approach: get all service requests and filter");

            String fallbackUrl = apiBaseUrl + "/service-requests";
            ResponseEntity<String> fallbackResponse = restTemplate.exchange(
                    fallbackUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (fallbackResponse.getStatusCode() == HttpStatus.OK && fallbackResponse.getBody() != null) {
                List<Map<String, Object>> allServices = objectMapper.readValue(
                        fallbackResponse.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Filter to only include services that are under service (not completed)
                List<Map<String, Object>> underServiceRequests = allServices.stream()
                        .filter(service -> {
                            String status = service.containsKey("status") ?
                                    String.valueOf(service.get("status")) : "";
                            return status != null &&
                                    !status.equalsIgnoreCase("Completed") &&
                                    !status.isEmpty();
                        })
                        .toList();

                log.info("Fallback method found {} vehicles under service", underServiceRequests.size());
                return ResponseEntity.ok(underServiceRequests);
            }

            log.warn("Both primary and fallback attempts failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        } catch (Exception e) {
            log.error("Error fetching vehicles under service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Health check endpoint to test API connectivity
     */
    @GetMapping("/api/health-check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> healthCheck(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Collections.singletonMap("status", "unauthorized")
            );
        }

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "timestamp", System.currentTimeMillis(),
                "environment", "production"
        ));
    }

    @GetMapping("/api/vehicle-tracking/completed-services")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCompletedServices(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching completed services: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PutMapping("/api/vehicle-tracking/service-request/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateServiceStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> statusUpdate,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String status = statusUpdate.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Status is required"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusUpdate, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + id + "/status",
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}