package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VehicleTrackingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all vehicles under service
     */
    public List<Map<String, Object>> getVehiclesUnderService(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/vehicles-under-service",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
    }

    /**
     * Get all completed services
     */
    public List<Map<String, Object>> getCompletedServices(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
    }

    /**
     * Get service request details by ID
     */
    public Map<String, Object> getServiceRequestById(Integer id, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + id,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyMap();
    }

    /**
     * Update service request status
     */
    public boolean updateServiceStatus(Integer requestId, String status, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Collections.singletonMap("status", status);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Simplified error handling
        }

        return false;
    }

    /**
     * Record payment for a service
     */
    public Map<String, Object> recordPayment(Integer requestId, Map<String, Object> paymentDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentDetails, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/payment",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Generate bill for a service
     */
    public Map<String, Object> generateBill(Integer requestId, Map<String, Object> billData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(billData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/bills/service-request/" + requestId + "/generate",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    return objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                } catch (Exception e) {
                    return Collections.singletonMap("error", "Error parsing response");
                }
            }
        } catch (Exception e) {
            return Collections.singletonMap("error", "Failed to generate bill");
        }

        return Collections.emptyMap();
    }

    /**
     * Generate invoice for a service
     */
    public Map<String, Object> generateInvoice(Integer requestId, Map<String, Object> invoiceDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceDetails, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/invoice",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Dispatch a vehicle
     */
    public Map<String, Object> dispatchVehicle(Integer requestId, Map<String, Object> dispatchDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(dispatchDetails, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/dispatch",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Filter vehicles under service
     */
    public List<Map<String, Object>> filterVehiclesUnderService(Map<String, Object> filterCriteria, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(filterCriteria, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/vehicles-under-service/filter",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
    }

    /**
     * Filter completed services
     */
    public List<Map<String, Object>> filterCompletedServices(Map<String, Object> filterCriteria, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(filterCriteria, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services/filter",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
    }

    /**
     * Search for vehicles and services
     */
    public Map<String, List<Map<String, Object>>> searchVehiclesAndServices(String query, String token) {
        try {
            Map<String, Object> searchCriteria = Collections.singletonMap("search", query);

            // Get vehicles under service matching the search
            List<Map<String, Object>> vehiclesUnderService = filterVehiclesUnderService(searchCriteria, token);

            // Get completed services matching the search
            List<Map<String, Object>> completedServices = filterCompletedServices(searchCriteria, token);

            // Combine results
            Map<String, List<Map<String, Object>>> results = new HashMap<>();
            results.put("vehiclesUnderService", vehiclesUnderService);
            results.put("completedServices", completedServices);

            return results;
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyMap();
    }

    /**
     * Helper method to create authentication headers
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (token != null && !token.isEmpty()) {
            if (token.startsWith("Bearer ")) {
                headers.set("Authorization", token);
            } else {
                headers.setBearerAuth(token);
            }
        }

        return headers;
    }
}