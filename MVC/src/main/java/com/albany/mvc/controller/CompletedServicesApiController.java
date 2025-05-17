package com.albany.mvc.controller;

import com.albany.mvc.service.CompletedServicesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * REST API controller for completed vehicle services
 */
@RestController
@RequestMapping("/admin/api/completed-services")
@RequiredArgsConstructor
@Slf4j
public class CompletedServicesApiController {

    private final CompletedServicesService completedServicesService;

    /**
     * Get all completed services
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllCompletedServices(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            // Use service to fetch completed services
            List<Map<String, Object>> completedServices = completedServicesService.getCompletedServices(validToken);

            // Log the number of services and data verification
            log.info("Returning {} completed services", completedServices.size());
            return ResponseEntity.ok(completedServices);
        } catch (Exception e) {
            log.error("Error fetching completed services: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * NEW ENDPOINT: Get invoice details for a service
     */
    @GetMapping("/{id}/invoice-details")
    public ResponseEntity<Map<String, Object>> getServiceInvoiceDetails(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Try to get invoice details from service - use getServiceDetails method
            Map<String, Object> serviceDetails = completedServicesService.getServiceDetails(id, validToken);

            // If service details exist but don't have materials/labor data, add default data
            if (!serviceDetails.isEmpty()) {
                // Add default materials if missing
                if (!serviceDetails.containsKey("materials") || serviceDetails.get("materials") == null) {
                    serviceDetails.put("materials", new ArrayList<>());
                    serviceDetails.put("materialsTotal", BigDecimal.ZERO);
                }

                // Add default labor charges if missing
                if (!serviceDetails.containsKey("laborCharges") || serviceDetails.get("laborCharges") == null) {
                    serviceDetails.put("laborCharges", new ArrayList<>());
                    serviceDetails.put("laborTotal", BigDecimal.ZERO);
                }

                // Calculate financial totals even if they're missing
                ensureFinancialTotals(serviceDetails);
            }

            log.debug("Service details for invoice: {}", serviceDetails);
            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            log.error("Error fetching invoice details for service {}: {}", id, e.getMessage(), e);

            // Return a minimal structure with default values rather than empty map
            Map<String, Object> fallbackData = createFallbackInvoiceData(id);
            return ResponseEntity.ok(fallbackData);
        }
    }

    /**
     * Filter completed services based on criteria
     */
    @PostMapping("/filter")
    public ResponseEntity<List<Map<String, Object>>> filterCompletedServices(
            @RequestBody Map<String, Object> filterCriteria,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            // Use service to filter completed services
            List<Map<String, Object>> filteredServices = completedServicesService.filterCompletedServices(
                    filterCriteria, validToken);
            return ResponseEntity.ok(filteredServices);
        } catch (Exception e) {
            log.error("Error filtering completed services: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Get service details for a specific completed service
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCompletedServiceDetails(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to get service details
            Map<String, Object> serviceDetails = completedServicesService.getServiceDetails(id, validToken);

            if (serviceDetails.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            log.error("Error fetching service details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    /**
     * Generate invoice for a completed service
     */
    @PostMapping("/{id}/invoice")
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> invoiceDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to generate invoice
            Map<String, Object> result = completedServicesService.generateInvoice(id, invoiceDetails, validToken);

            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error generating invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to generate invoice: " + e.getMessage()));
        }
    }

    /**
     * Record payment for a completed service
     */
    @PostMapping("/{id}/payment")
    public ResponseEntity<Map<String, Object>> recordPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> paymentDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to record payment
            Map<String, Object> result = completedServicesService.recordPayment(id, paymentDetails, validToken);

            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to record payment: " + e.getMessage()));
        }
    }

    /**
     * Handle vehicle dispatch for completed service
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> dispatchDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to dispatch vehicle
            Map<String, Object> result = completedServicesService.dispatchVehicle(id, dispatchDetails, validToken);

            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error dispatching vehicle: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to dispatch vehicle: " + e.getMessage()));
        }
    }

    /**
     * Create fallback invoice data when API fails
     */
    private Map<String, Object> createFallbackInvoiceData(Integer serviceId) {
        Map<String, Object> fallback = new HashMap<>();

        // Basic service info
        fallback.put("requestId", serviceId);
        fallback.put("serviceId", serviceId);
        fallback.put("vehicleName", "Unknown Vehicle");
        fallback.put("registrationNumber", "Unknown");
        fallback.put("customerName", "Unknown Customer");
        fallback.put("membershipStatus", "Standard");
        fallback.put("completedDate", LocalDate.now());

        // Empty financial data
        fallback.put("materials", new ArrayList<>());
        fallback.put("laborCharges", new ArrayList<>());
        fallback.put("materialsTotal", BigDecimal.ZERO);
        fallback.put("laborTotal", BigDecimal.ZERO);
        fallback.put("subtotal", BigDecimal.ZERO);
        fallback.put("tax", BigDecimal.ZERO);
        fallback.put("grandTotal", BigDecimal.ZERO);

        return fallback;
    }

    /**
     * Ensure all financial totals are calculated and present
     */
    private void ensureFinancialTotals(Map<String, Object> data) {
        // Get existing values or default to zero
        BigDecimal materialsTotal = getBigDecimalValue(data, "materialsTotal", BigDecimal.ZERO);
        BigDecimal laborTotal = getBigDecimalValue(data, "laborTotal", BigDecimal.ZERO);

        // Calculate discount if premium
        BigDecimal discount = BigDecimal.ZERO;
        String membershipStatus = getStringValue(data, "membershipStatus", "Standard");
        if ("Premium".equalsIgnoreCase(membershipStatus)) {
            discount = laborTotal.multiply(new BigDecimal("0.20"));
            data.put("discount", discount);
        }

        // Calculate subtotal
        BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
        data.put("subtotal", subtotal);

        // Calculate tax
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        data.put("tax", tax);

        // Calculate grand total
        BigDecimal grandTotal = subtotal.add(tax);
        data.put("grandTotal", grandTotal);
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
     * Helper method to get BigDecimal value from a map with default
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key, BigDecimal defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else {
                try {
                    return new BigDecimal(value.toString());
                } catch (Exception e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Helper method to get String value from a map with default
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return defaultValue;
    }
}