package com.albany.mvc.controller;

import com.albany.mvc.dto.CompletedServiceDTO;
import com.albany.mvc.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for invoice management in completed services
 */
@RestController
@RequestMapping("/admin/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceApiController {

    private final InvoiceService invoiceService;

    /**
     * Get invoice information for a service
     */
    @GetMapping("/service-request/{id}")
    public ResponseEntity<Map<String, Object>> getInvoiceInfo(
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
            // Use service to get invoice info
            Map<String, Object> invoiceInfo = invoiceService.getInvoiceInfo(id, validToken);
            
            if (invoiceInfo.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(invoiceInfo);
        } catch (Exception e) {
            log.error("Error fetching invoice info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    /**
     * Generate invoice for a completed service
     */
    @PostMapping("/service-request/{id}/generate")
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
            Map<String, Object> result = invoiceService.generateInvoice(id, invoiceDetails, validToken);
            
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
     * Process service data for invoice
     */
    @PostMapping("/process-service-data")
    public ResponseEntity<CompletedServiceDTO> processServiceData(
            @RequestBody Map<String, Object> serviceData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            // Use service to process data
            CompletedServiceDTO processedData = invoiceService.processServiceForInvoice(serviceData);
            return ResponseEntity.ok(processedData);
        } catch (Exception e) {
            log.error("Error processing service data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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