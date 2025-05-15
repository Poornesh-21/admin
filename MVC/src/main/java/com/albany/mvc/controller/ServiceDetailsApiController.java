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

import java.util.*;

/**
 * Controller for retrieving consolidated service details including materials and labor charges
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ServiceDetailsApiController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Original endpoint for getting comprehensive service details
     */
    @GetMapping("/admin/api/service-details/{serviceId}")
    public ResponseEntity<Map<String, Object>> getConsolidatedServiceDetails(
            @PathVariable Integer serviceId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Request to /admin/api/service-details/{} received", serviceId);
        return getServiceDetailsInternal(serviceId, token, authHeader, request);
    }

    /**
     * Alternative endpoint that uses a different URL pattern
     */
    @GetMapping("/admin/api/services/{serviceId}/details")
    public ResponseEntity<Map<String, Object>> getServiceDetailsAlternate(
            @PathVariable Integer serviceId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Request to /admin/api/services/{}/details received", serviceId);
        return getServiceDetailsInternal(serviceId, token, authHeader, request);
    }

    /**
     * Debug endpoint that returns test data
     */
    @GetMapping("/admin/api/debug/service-details/{serviceId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugServiceDetails(
            @PathVariable Integer serviceId,
            @RequestParam(required = false) String token) {

        log.info("Debug request for service details ID: {}", serviceId);

        // Create dummy data for testing
        Map<String, Object> testData = new HashMap<>();
        testData.put("requestId", serviceId);
        testData.put("vehicleName", "Test Vehicle " + serviceId);
        testData.put("registrationNumber", "TEST-" + serviceId);
        testData.put("customerName", "Test Customer");
        testData.put("membershipStatus", "Standard");
        testData.put("completedDate", "2025-05-15");

        // Add materials
        List<Map<String, Object>> materials = new ArrayList<>();
        Map<String, Object> material1 = new HashMap<>();
        material1.put("name", "Engine Oil");
        material1.put("quantity", 2);
        material1.put("unitPrice", 150.0);
        material1.put("total", 300.0);
        materials.add(material1);

        Map<String, Object> material2 = new HashMap<>();
        material2.put("name", "Oil Filter");
        material2.put("quantity", 1);
        material2.put("unitPrice", 100.0);
        material2.put("total", 100.0);
        materials.add(material2);

        testData.put("materials", materials);

        // Add labor charges
        List<Map<String, Object>> laborCharges = new ArrayList<>();
        Map<String, Object> labor = new HashMap<>();
        labor.put("description", "Oil Change Service");
        labor.put("hours", 1.5);
        labor.put("ratePerHour", 200.0);
        labor.put("total", 300.0);
        laborCharges.add(labor);
        testData.put("laborCharges", laborCharges);

        // Add calculated values
        testData.put("calculatedMaterialsTotal", 400.0);
        testData.put("calculatedLaborTotal", 300.0);
        testData.put("calculatedSubtotal", 700.0);
        testData.put("calculatedTax", 126.0);
        testData.put("calculatedTotal", 826.0);

        // Mock invoice and payment status
        testData.put("hasInvoice", false);
        testData.put("isPaid", false);
        testData.put("isDelivered", false);

        log.info("Returning debug test data for service ID: {}", serviceId);
        return ResponseEntity.ok(testData);
    }

    /**
     * Internal method that implements the actual service details retrieval logic
     */
    private ResponseEntity<Map<String, Object>> getServiceDetailsInternal(
            Integer serviceId,
            String token,
            String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for service details request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            log.info("Fetching service details for ID: {}", serviceId);

            // Step 1: Get basic service details
            Map<String, Object> serviceDetails = getServiceRequestDetails(serviceId, validToken);

            if (serviceDetails.isEmpty()) {
                log.warn("No service details found for ID: {}", serviceId);
                return ResponseEntity.notFound().build();
            }

            log.info("Found service details, enhancing with additional data");

            // Step 2: Get materials used data
            List<Map<String, Object>> materials = getMaterialsForService(serviceId, validToken);
            if (!materials.isEmpty()) {
                serviceDetails.put("materials", materials);
                log.info("Added {} materials to service details", materials.size());
            } else {
                log.info("No materials found for service ID: {}", serviceId);
            }

            // Step 3: Get service tracking data (for labor charges)
            List<Map<String, Object>> serviceTracking = getServiceTrackingForService(serviceId, validToken);
            if (!serviceTracking.isEmpty()) {
                serviceDetails.put("serviceTracking", serviceTracking);
                log.info("Added {} tracking entries to service details", serviceTracking.size());
            } else {
                log.info("No service tracking entries found for service ID: {}", serviceId);
            }

            // Step 4: Get invoice data if it exists
            Map<String, Object> invoice = getInvoiceForService(serviceId, validToken);
            if (!invoice.isEmpty()) {
                serviceDetails.put("invoice", invoice);
                serviceDetails.put("hasInvoice", true);
                log.info("Added invoice data to service details");
            } else {
                log.info("No invoice found for service ID: {}", serviceId);
            }

            // Step 5: Get payment data if it exists
            Map<String, Object> payment = getPaymentForService(serviceId, validToken);
            if (!payment.isEmpty()) {
                serviceDetails.put("payment", payment);
                serviceDetails.put("isPaid", "Completed".equals(payment.get("status")));
                log.info("Added payment data to service details");
            } else {
                log.info("No payment found for service ID: {}", serviceId);
            }

            log.info("Successfully consolidated service details for ID: {}", serviceId);
            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            log.error("Error retrieving consolidated service details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to retrieve service details: " + e.getMessage()));
        }
    }

    /**
     * Get basic service request details
     */
    private Map<String, Object> getServiceRequestDetails(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = apiBaseUrl + "/vehicle-tracking/service-request/" + serviceId;
            log.debug("Fetching service details from: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> serviceDetails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                log.debug("Retrieved basic service details for ID {}", serviceId);
                return serviceDetails;
            } else {
                log.warn("Unexpected response status from service details API: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error fetching service details: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get materials used for a service
     */
    private List<Map<String, Object>> getMaterialsForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Try the direct materials API endpoint first
            String url = apiBaseUrl + "/materials/service-request/" + serviceId;
            log.debug("Fetching materials from primary endpoint: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Map<String, Object>> materials = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    if (!materials.isEmpty()) {
                        log.debug("Retrieved {} materials for service ID {} from primary endpoint",
                                materials.size(), serviceId);
                        return materials;
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching materials from primary endpoint: {}", e.getMessage());
                // Continue to fallback method
            }

            // Fallback: Try the alternative materials endpoint
            url = apiBaseUrl + "/service-details/" + serviceId + "/materials";
            log.debug("Fetching materials from alternate endpoint: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Map<String, Object>> materials = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    log.debug("Retrieved {} materials for service ID {} from alternate endpoint",
                            materials.size(), serviceId);
                    return materials;
                }
            } catch (Exception e) {
                log.warn("Error fetching materials from alternate endpoint: {}", e.getMessage());
                // Continue to the next fallback
            }

            // Final fallback: Check for materials in the MaterialUsage table directly
            url = apiBaseUrl + "/material-usage/service-request/" + serviceId;
            log.debug("Fetching material usage data: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Map<String, Object>> materialUsages = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    log.debug("Retrieved {} material usage entries for service ID {}",
                            materialUsages.size(), serviceId);
                    return materialUsages;
                }
            } catch (Exception e) {
                log.warn("Error fetching material usage data: {}", e.getMessage());
            }

            // If all attempts fail, return empty list
            log.warn("Unable to retrieve materials data for service ID {}", serviceId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error in getMaterialsForService: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get service tracking entries (for labor charges)
     */
    private List<Map<String, Object>> getServiceTrackingForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Try to get service tracking entries
            String url = apiBaseUrl + "/service-tracking/" + serviceId;
            log.debug("Fetching service tracking entries: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Map<String, Object>> trackingEntries = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    log.debug("Retrieved {} service tracking entries for service ID {}",
                            trackingEntries.size(), serviceId);
                    return trackingEntries;
                }
            } catch (Exception e) {
                log.warn("Error fetching service tracking entries: {}", e.getMessage());
                // Try alternative endpoint
            }

            // Alternative endpoint for labor charges
            url = apiBaseUrl + "/labor-charges/service-request/" + serviceId;
            log.debug("Fetching labor charges: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Map<String, Object>> laborCharges = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    // Convert labor charges to tracking format
                    List<Map<String, Object>> convertedEntries = new ArrayList<>();
                    for (Map<String, Object> charge : laborCharges) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("trackingId", charge.get("chargeId"));
                        entry.put("workDescription", charge.get("description"));

                        // Convert hours to minutes
                        if (charge.containsKey("hours")) {
                            double hours = parseDouble(charge.get("hours"));
                            entry.put("laborMinutes", (int)(hours * 60));
                        }

                        entry.put("laborCost", charge.get("total"));
                        entry.put("status", "Completed");

                        convertedEntries.add(entry);
                    }

                    log.debug("Converted {} labor charges to tracking entries for service ID {}",
                            convertedEntries.size(), serviceId);
                    return convertedEntries;
                }
            } catch (Exception e) {
                log.warn("Error fetching labor charges: {}", e.getMessage());
            }

            // If all attempts fail, return empty list
            log.warn("Unable to retrieve labor/tracking data for service ID {}", serviceId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error in getServiceTrackingForService: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get invoice for a service
     */
    private Map<String, Object> getInvoiceForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = apiBaseUrl + "/invoices/service-request/" + serviceId;
            log.debug("Fetching invoice data: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> invoice = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<Map<String, Object>>() {}
                    );

                    log.debug("Retrieved invoice data for service ID {}", serviceId);
                    return invoice;
                }
            } catch (Exception e) {
                log.warn("Error fetching invoice data: {}", e.getMessage());
            }

            // If failed, return empty map
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error in getInvoiceForService: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get payment for a service
     */
    private Map<String, Object> getPaymentForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = apiBaseUrl + "/payments/service-request/" + serviceId;
            log.debug("Fetching payment data: {}", url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> payment = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<Map<String, Object>>() {}
                    );

                    log.debug("Retrieved payment data for service ID {}", serviceId);
                    return payment;
                }
            } catch (Exception e) {
                log.warn("Error fetching payment data: {}", e.getMessage());
            }

            // If failed, return empty map
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error in getPaymentForService: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
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

    /**
     * Helper method to parse double safely
     */
    private double parseDouble(Object value) {
        if (value == null) return 0.0;

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Helper method to get token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                return sessionToken;
            }
        }

        return null;
    }
}