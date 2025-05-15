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

import java.math.BigDecimal;
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

            // Step 2: Get materials used data - try multiple possible endpoints
            List<Map<String, Object>> materials = getMaterialsForService(serviceId, validToken);
            if (!materials.isEmpty()) {
                serviceDetails.put("materials", materials);
                log.info("Added {} materials to service details", materials.size());
            } else {
                // Set empty materials array instead of not setting it at all
                serviceDetails.put("materials", new ArrayList<>());
                log.info("No materials found for service ID: {}, setting empty array", serviceId);
            }

            // Step 3: Get labor charges specifically - try multiple possible endpoints
            List<Map<String, Object>> laborCharges = getLaborChargesForService(serviceId, validToken);
            if (!laborCharges.isEmpty()) {
                serviceDetails.put("laborCharges", laborCharges);
                log.info("Added {} labor charges to service details", laborCharges.size());
            } else {
                // Set empty labor charges array instead of not setting it at all
                serviceDetails.put("laborCharges", new ArrayList<>());
                log.info("No labor charges found for service ID: {}, setting empty array", serviceId);
            }

            // Also get service tracking data (as it might contain labor info)
            List<Map<String, Object>> serviceTracking = getServiceTrackingForService(serviceId, validToken);
            if (!serviceTracking.isEmpty()) {
                serviceDetails.put("serviceTracking", serviceTracking);
                log.info("Added {} tracking entries to service details", serviceTracking.size());

                // Extract labor charges from service tracking if dedicated labor charges weren't found
                if (laborCharges.isEmpty()) {
                    List<Map<String, Object>> derivedLaborCharges = extractLaborChargesFromTracking(serviceTracking);
                    if (!derivedLaborCharges.isEmpty()) {
                        serviceDetails.put("laborCharges", derivedLaborCharges);
                        log.info("Extracted {} labor charges from tracking data", derivedLaborCharges.size());
                    }
                }
            }

            // Step 4: Get invoice data if it exists
            Map<String, Object> invoice = getInvoiceForService(serviceId, validToken);
            if (!invoice.isEmpty()) {
                serviceDetails.put("invoice", invoice);
                serviceDetails.put("hasInvoice", true);
                serviceDetails.put("invoiceId", invoice.get("invoiceId"));
                log.info("Added invoice data to service details");
            } else {
                // Explicitly set hasInvoice flag to false
                serviceDetails.put("hasInvoice", false);
                log.info("No invoice found for service ID: {}, setting hasInvoice=false", serviceId);
            }

            // Step 5: Get payment data if it exists
            Map<String, Object> payment = getPaymentForService(serviceId, validToken);
            if (!payment.isEmpty()) {
                serviceDetails.put("payment", payment);
                serviceDetails.put("isPaid", "Completed".equals(payment.get("status")));
                serviceDetails.put("paid", "Completed".equals(payment.get("status"))); // Add alternative field
                log.info("Added payment data to service details");
            } else {
                // Explicitly set isPaid flag to false
                serviceDetails.put("isPaid", false);
                serviceDetails.put("paid", false); // Add alternative field
                log.info("No payment found for service ID: {}, setting isPaid=false", serviceId);
            }

            // Step 6: Calculate and add financial summary
            calculateFinancialSummary(serviceDetails);

            log.info("Successfully consolidated service details for ID: {}", serviceId);
            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            log.error("Error retrieving consolidated service details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to retrieve service details: " + e.getMessage()));
        }
    }

    /**
     * Calculate financial summary based on materials and labor charges
     */
    private void calculateFinancialSummary(Map<String, Object> serviceDetails) {
        try {
            // Calculate materials total
            BigDecimal materialsTotal = new java.math.BigDecimal("0.00");

            // Get materials list
            List<Map<String, Object>> materials = new ArrayList<>();
            if (serviceDetails.containsKey("materials") && serviceDetails.get("materials") instanceof List) {
                materials = (List<Map<String, Object>>) serviceDetails.get("materials");
            }

            // Calculate total from materials
            for (Map<String, Object> material : materials) {
                BigDecimal quantity = getBigDecimal(material.get("quantity"), new java.math.BigDecimal("0.00"));
                BigDecimal unitPrice = getBigDecimal(material.get("unitPrice"), new java.math.BigDecimal("0.00"));

                // If we don't have unitPrice directly, try to get it from inventoryItem
                if (unitPrice.equals(new java.math.BigDecimal("0.00")) && material.containsKey("inventoryItem") &&
                        material.get("inventoryItem") instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) material.get("inventoryItem");
                    unitPrice = getBigDecimal(item.get("unitPrice"), new java.math.BigDecimal("0.00"));
                }

                // Try to use direct total if available
                if (material.containsKey("total") && material.get("total") != null) {
                    materialsTotal = materialsTotal.add(getBigDecimal(material.get("total"), new java.math.BigDecimal("0.00")));
                } else {
                    materialsTotal = materialsTotal.add(quantity.multiply(unitPrice));
                }
            }

            // Calculate labor total
            BigDecimal laborTotal = new java.math.BigDecimal("0.00");

            // Get labor charges list
            List<Map<String, Object>> laborCharges = new ArrayList<>();
            if (serviceDetails.containsKey("laborCharges") && serviceDetails.get("laborCharges") instanceof List) {
                laborCharges = (List<Map<String, Object>>) serviceDetails.get("laborCharges");
            }

            // Calculate total from labor charges
            for (Map<String, Object> labor : laborCharges) {
                // Try to use direct total if available
                if (labor.containsKey("total") && labor.get("total") != null) {
                    laborTotal = laborTotal.add(getBigDecimal(labor.get("total"), new java.math.BigDecimal("0.00")));
                } else {
                    BigDecimal hours = getBigDecimal(labor.get("hours"), new java.math.BigDecimal("0.00"));
                    BigDecimal rate = getBigDecimal(labor.get("ratePerHour"), new java.math.BigDecimal("0.00"));
                    laborTotal = laborTotal.add(hours.multiply(rate));
                }
            }

            // Calculate discount if premium membership
            BigDecimal discount = new java.math.BigDecimal("0.00");
            String membershipStatus = getStringValue(serviceDetails, "membershipStatus", "Standard");
            if (membershipStatus.equalsIgnoreCase("Premium")) {
                // 20% discount on labor for premium members
                discount = laborTotal.multiply(new java.math.BigDecimal("0.20"));
            }

            // Calculate subtotal
            BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);

            // Calculate tax (18% GST)
            BigDecimal tax = subtotal.multiply(new java.math.BigDecimal("0.18"));

            // Calculate grand total
            BigDecimal grandTotal = subtotal.add(tax);

            // Store calculated values in service details
            serviceDetails.put("calculatedMaterialsTotal", materialsTotal);
            serviceDetails.put("calculatedLaborTotal", laborTotal);
            serviceDetails.put("calculatedDiscount", discount);
            serviceDetails.put("calculatedSubtotal", subtotal);
            serviceDetails.put("calculatedTax", tax);
            serviceDetails.put("calculatedTotal", grandTotal);

            // Also set standard fields that might be expected
            serviceDetails.put("materialsTotal", materialsTotal);
            serviceDetails.put("laborTotal", laborTotal);
            serviceDetails.put("discount", discount);
            serviceDetails.put("subtotal", subtotal);
            serviceDetails.put("tax", tax);
            serviceDetails.put("gst", tax);  // Alternative field name
            serviceDetails.put("total", grandTotal);
            serviceDetails.put("totalCost", grandTotal);  // Alternative field name
            serviceDetails.put("totalAmount", grandTotal);  // Alternative field name

            log.debug("Calculated financial summary: materialsTotal={}, laborTotal={}, discount={}, subtotal={}, tax={}, total={}",
                    materialsTotal, laborTotal, discount, subtotal, tax, grandTotal);

        } catch (Exception e) {
            log.error("Error calculating financial summary: {}", e.getMessage(), e);
            // Don't throw exception, just log it
        }
    }

    /**
     * Extract labor charges from service tracking entries
     */
    private List<Map<String, Object>> extractLaborChargesFromTracking(List<Map<String, Object>> trackingEntries) {
        List<Map<String, Object>> laborCharges = new ArrayList<>();

        for (Map<String, Object> entry : trackingEntries) {
            // Filter entries that have labor cost
            if (entry.containsKey("laborCost") && entry.get("laborCost") != null &&
                    !getBigDecimal(entry.get("laborCost"), new java.math.BigDecimal("0.00")).equals(new java.math.BigDecimal("0.00"))) {

                Map<String, Object> laborCharge = new HashMap<>();

                // Set description
                String workDescription = getStringValue(entry, "workDescription", "Labor Service");
                // If description starts with "Labor:", remove the prefix
                if (workDescription.startsWith("Labor:")) {
                    workDescription = workDescription.substring(6).trim();
                } else if (workDescription.startsWith("Labor charges:")) {
                    workDescription = workDescription.substring(14).trim();
                }
                laborCharge.put("description", workDescription);

                // Convert minutes to hours if available
                Integer laborMinutes = getIntValue(entry, "laborMinutes", 0);
                BigDecimal hours = new java.math.BigDecimal("0.00");
                if (laborMinutes > 0) {
                    hours = new java.math.BigDecimal(laborMinutes).divide(new java.math.BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);
                }
                laborCharge.put("hours", hours);

                // Calculate rate per hour if possible
                BigDecimal laborCost = getBigDecimal(entry.get("laborCost"), new java.math.BigDecimal("0.00"));
                BigDecimal ratePerHour = new java.math.BigDecimal("0.00");
                if (laborMinutes > 0) {
                    ratePerHour = laborCost.multiply(new java.math.BigDecimal("60"))
                            .divide(new java.math.BigDecimal(laborMinutes), 2, java.math.RoundingMode.HALF_UP);
                }
                laborCharge.put("ratePerHour", ratePerHour);

                // Set total cost
                laborCharge.put("total", laborCost);

                laborCharges.add(laborCharge);
            }
        }

        return laborCharges;
    }

    /**
     * Get basic service request details
     */
    private Map<String, Object> getServiceRequestDetails(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // First try the vehicle-tracking endpoint
            String url = apiBaseUrl + "/vehicle-tracking/service-request/" + serviceId;
            log.debug("Fetching service details from: {}", url);

            try {
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
                }
            } catch (Exception e) {
                log.warn("Error fetching from primary endpoint: {}", e.getMessage());
                // Continue to try alternative endpoint
            }

            // Try alternative endpoint
            url = apiBaseUrl + "/service-requests/" + serviceId;
            log.debug("Fetching service details from alternative endpoint: {}", url);

            try {
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

                    log.debug("Retrieved basic service details from alternative endpoint for ID {}", serviceId);
                    return serviceDetails;
                }
            } catch (Exception e) {
                log.warn("Error fetching from alternative endpoint: {}", e.getMessage());
                // Continue to third attempt
            }

            // Try service-advisor dashboard endpoint as a last resort
            url = apiBaseUrl + "/serviceAdvisor/dashboard/service-details/" + serviceId;
            log.debug("Fetching service details from service-advisor endpoint: {}", url);

            try {
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

                    log.debug("Retrieved basic service details from service-advisor endpoint for ID {}", serviceId);
                    return serviceDetails;
                }
            } catch (Exception e) {
                log.warn("Error fetching from service-advisor endpoint: {}", e.getMessage());
            }

            log.warn("All attempts to fetch basic service details failed");
            return Collections.emptyMap();
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

            // Try multiple endpoints to get materials data
            String[] materialEndpoints = {
                    "/admin/api/materials/service-request/" + serviceId,
                    "/materials/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/materials",
                    "/material-usage/service-request/" + serviceId,
                    "/serviceAdvisor/dashboard/service/" + serviceId + "/inventory-items"
            };

            for (String endpoint : materialEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                log.debug("Trying to fetch materials from: {}", url);

                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        try {
                            // First try parsing as a list of materials
                            List<Map<String, Object>> materials = objectMapper.readValue(
                                    response.getBody(),
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );

                            if (!materials.isEmpty()) {
                                log.debug("Successfully retrieved {} materials from {}", materials.size(), url);
                                return materials;
                            }
                        } catch (Exception e) {
                            // If not a direct list, it might be wrapped in an object
                            try {
                                Map<String, Object> wrapper = objectMapper.readValue(
                                        response.getBody(),
                                        new TypeReference<Map<String, Object>>() {}
                                );

                                // Check for various possible field names
                                for (String field : new String[]{"items", "materials", "materialItems", "materialUsages"}) {
                                    if (wrapper.containsKey(field) && wrapper.get(field) instanceof List) {
                                        List<Map<String, Object>> materials = (List<Map<String, Object>>) wrapper.get(field);
                                        if (!materials.isEmpty()) {
                                            log.debug("Retrieved {} materials from wrapped field '{}' in {}",
                                                    materials.size(), field, url);
                                            return materials;
                                        }
                                    }
                                }
                            } catch (Exception nestedEx) {
                                log.debug("Error parsing wrapped materials: {}", nestedEx.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error fetching materials from {}: {}", url, e.getMessage());
                    // Continue to next endpoint
                }
            }

            // If all attempts fail, check if there's materialsUsed in service details
            log.warn("All attempts to fetch materials data failed");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error in getMaterialsForService: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get labor charges specifically for a service
     */
    private List<Map<String, Object>> getLaborChargesForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Try multiple endpoints to get labor charges data
            String[] laborEndpoints = {
                    "/admin/api/labor/service-request/" + serviceId,
                    "/labor-charges/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/labor-charges",
                    "/serviceAdvisor/dashboard/service/" + serviceId + "/labor-charges"
            };

            for (String endpoint : laborEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                log.debug("Trying to fetch labor charges from: {}", url);

                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        try {
                            // First try parsing as a list of labor charges
                            List<Map<String, Object>> laborCharges = objectMapper.readValue(
                                    response.getBody(),
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );

                            if (!laborCharges.isEmpty()) {
                                log.debug("Successfully retrieved {} labor charges from {}", laborCharges.size(), url);
                                return laborCharges;
                            }
                        } catch (Exception e) {
                            // If not a direct list, it might be wrapped in an object
                            try {
                                Map<String, Object> wrapper = objectMapper.readValue(
                                        response.getBody(),
                                        new TypeReference<Map<String, Object>>() {}
                                );

                                // Check for various possible field names
                                for (String field : new String[]{"laborCharges", "charges", "labor"}) {
                                    if (wrapper.containsKey(field) && wrapper.get(field) instanceof List) {
                                        List<Map<String, Object>> charges = (List<Map<String, Object>>) wrapper.get(field);
                                        if (!charges.isEmpty()) {
                                            log.debug("Retrieved {} labor charges from wrapped field '{}' in {}",
                                                    charges.size(), field, url);
                                            return charges;
                                        }
                                    }
                                }
                            } catch (Exception nestedEx) {
                                log.debug("Error parsing wrapped labor charges: {}", nestedEx.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error fetching labor charges from {}: {}", url, e.getMessage());
                    // Continue to next endpoint
                }
            }

            // If all attempts fail, log warning
            log.warn("All attempts to fetch labor charges data failed");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error in getLaborChargesForService: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get service tracking entries for labor charges
     */
    private List<Map<String, Object>> getServiceTrackingForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Try multiple endpoints to get service tracking data
            String[] trackingEndpoints = {
                    "/admin/api/service-tracking/" + serviceId,
                    "/service-tracking/" + serviceId,
                    "/service-details/" + serviceId + "/tracking"
            };

            for (String endpoint : trackingEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                log.debug("Trying to fetch service tracking from: {}", url);

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

                        if (!trackingEntries.isEmpty()) {
                            log.debug("Successfully retrieved {} tracking entries from {}", trackingEntries.size(), url);
                            return trackingEntries;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error fetching service tracking from {}: {}", url, e.getMessage());
                    // Continue to next endpoint
                }
            }

            // If all attempts fail, check service history in the details
            log.warn("All attempts to fetch service tracking data failed");
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

            // Try multiple endpoints to get invoice data
            String[] invoiceEndpoints = {
                    "/admin/api/invoices/service-request/" + serviceId,
                    "/invoices/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/invoice"
            };

            for (String endpoint : invoiceEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                log.debug("Trying to fetch invoice from: {}", url);

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

                        if (!invoice.isEmpty()) {
                            log.debug("Successfully retrieved invoice from {}", url);
                            return invoice;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error fetching invoice from {}: {}", url, e.getMessage());
                    // Continue to next endpoint
                }
            }

            // If all attempts fail, return empty map
            log.warn("All attempts to fetch invoice data failed");
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

            // Try multiple endpoints to get payment data
            String[] paymentEndpoints = {
                    "/admin/api/payments/service-request/" + serviceId,
                    "/payments/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/payment"
            };

            for (String endpoint : paymentEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                log.debug("Trying to fetch payment from: {}", url);

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

                        if (!payment.isEmpty()) {
                            log.debug("Successfully retrieved payment from {}", url);
                            return payment;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error fetching payment from {}: {}", url, e.getMessage());
                    // Continue to next endpoint
                }
            }

            // If all attempts fail, return empty map
            log.warn("All attempts to fetch payment data failed");
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

    /**
     * Helper method to get string value safely
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return defaultValue;
    }

    /**
     * Helper method to get integer value safely
     */
    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            try {
                if (map.get(key) instanceof Integer) {
                    return (Integer) map.get(key);
                } else if (map.get(key) instanceof Number) {
                    return ((Number) map.get(key)).intValue();
                } else {
                    return Integer.parseInt(String.valueOf(map.get(key)));
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Helper method to get BigDecimal value safely
     */
    private BigDecimal getBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return new BigDecimal(((Number) value).toString());
            } else {
                return new BigDecimal(String.valueOf(value));
            }
        } catch (NumberFormatException | ArithmeticException e) {
            return defaultValue;
        }
    }
}