package com.albany.mvc.controller;

import com.albany.mvc.service.BillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for bill management in completed services
 */
@RestController
@RequestMapping("/admin/api/bills")
@RequiredArgsConstructor
@Slf4j
public class BillApiController {

    private final BillService billService;

    /**
     * Generate a bill for a service
     */
    @PostMapping("/service-request/{id}/generate")
    public ResponseEntity<Map<String, Object>> generateBill(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> billDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to generate bill
            Map<String, Object> result = billService.generateBill(id, billDetails, validToken);
            
            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to generate bill: " + e.getMessage()));
        }
    }

    /**
     * Get bill for a service
     */
    @GetMapping("/service-request/{id}")
    public ResponseEntity<Map<String, Object>> getBill(
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
            // Use service to get bill
            Map<String, Object> bill = billService.getBill(id, validToken);
            
            if (bill.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            log.error("Error fetching bill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    /**
     * Calculate bill totals
     */
    @PostMapping("/calculate-totals")
    public ResponseEntity<Map<String, Object>> calculateBillTotals(
            @RequestBody Map<String, Object> serviceDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to calculate bill totals
            Map<String, Object> totals = billService.calculateBillTotals(serviceDetails);
            return ResponseEntity.ok(totals);
        } catch (Exception e) {
            log.error("Error calculating bill totals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to calculate bill totals: " + e.getMessage()));
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