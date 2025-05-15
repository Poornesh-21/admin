package com.albany.mvc.controller;

import com.albany.mvc.service.CompletedServicesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            logDataVerification(completedServices);

            return ResponseEntity.ok(completedServices);
        } catch (Exception e) {
            log.error("Error fetching completed services: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Log data verification for debugging
     */
    private void logDataVerification(List<Map<String, Object>> services) {
        if (services == null || services.isEmpty()) {
            log.debug("No services to verify");
            return;
        }

        // Check a sample service to verify fields
        Map<String, Object> sampleService = services.get(0);
        log.debug("Data verification for sample service:");
        log.debug("- ServiceId: {}", sampleService.get("serviceId"));
        log.debug("- VehicleName: {}", sampleService.get("vehicleName"));
        log.debug("- CustomerName: {}", sampleService.get("customerName"));
        log.debug("- CustomerEmail: {}", sampleService.get("customerEmail"));
        log.debug("- CustomerPhone: {}", sampleService.get("customerPhone"));
        log.debug("- MembershipStatus: {}", sampleService.get("membershipStatus"));
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

            // Log data verification
            log.debug("Service details for ID {}: customerPhone={}, membershipStatus={}",
                    id, serviceDetails.get("customerPhone"), serviceDetails.get("membershipStatus"));

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