package com.albany.mvc.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for service advisor operations
 */
@RestController
@RequestMapping("/serviceAdvisor/api")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorApiController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get assigned vehicles for the authenticated service advisor
     */
    @GetMapping("/assigned-vehicles")
    public ResponseEntity<List<Map<String, Object>>> getAssignedVehicles(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Fetching assigned vehicles from API");
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/assigned-vehicles";
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> vehicles = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return ResponseEntity.ok(vehicles);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error fetching assigned vehicles from API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Get service details for a specific request
     */
    @GetMapping("/service-details/{requestId}")
    public ResponseEntity<Map<String, Object>> getServiceDetails(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Fetching service details for request ID: {}", requestId);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service-details/" + requestId;
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> serviceDetails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(serviceDetails);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error fetching service details from API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    /**
     * Get inventory items for dropdown
     */
    @GetMapping("/inventory-items")
    public ResponseEntity<List<Map<String, Object>>> getInventoryItems(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Fetching inventory items from API");
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/inventory-items";
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> inventoryItems = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return ResponseEntity.ok(inventoryItems);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error fetching inventory items from API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Update service status
     */
    @PutMapping("/service/{requestId}/status")
    public ResponseEntity<Map<String, Object>> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Updating status for service request ID: {}", requestId);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusUpdate, headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/status";
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error updating service status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    /**
     * Add inventory items to service request
     */
    @PostMapping("/service/{requestId}/inventory-items")
    public ResponseEntity<Map<String, Object>> addInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> materialsRequest,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Adding inventory items to service request ID: {}", requestId);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(materialsRequest, headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/inventory-items";
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error adding inventory items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    /**
     * Add labor charges to service request
     */
    @PostMapping("/service/{requestId}/labor-charges")
    public ResponseEntity<Map<String, Object>> addLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<Map<String, Object>> laborCharges,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Adding labor charges to service request ID: {}", requestId);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(laborCharges, headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/labor-charges";
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error adding labor charges: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    /**
     * Generate bill for service request
     */
    @PostMapping("/service/{requestId}/generate-bill")
    public ResponseEntity<Map<String, Object>> generateBill(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> billRequest,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Generating bill for service request ID: {}", requestId);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(billRequest, headers);

            // FIXED URL - removed duplicate "/api" prefix
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/generate-bill";
            log.debug("Making request to API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    /**
     * Helper method to get a valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            // Store token in session for future requests
            HttpSession session = request.getSession(true);
            session.setAttribute("jwt-token", tokenParam);
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Using token from Authorization header");
            return authHeader;
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                log.debug("Using token from session");
                return sessionToken;
            }
        }

        log.warn("No valid token found from any source");
        return null;
    }
}