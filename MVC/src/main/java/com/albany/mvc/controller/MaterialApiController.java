package com.albany.mvc.controller;

import com.albany.mvc.dto.MaterialItemDTO;
import com.albany.mvc.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for materials in completed services
 */
@RestController
@RequestMapping("/admin/api/materials")
@RequiredArgsConstructor
@Slf4j
public class MaterialApiController {

    private final MaterialService materialService;

    /**
     * Get materials for a service
     */
    @GetMapping("/service-request/{id}")
    public ResponseEntity<List<MaterialItemDTO>> getMaterialsForService(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            // Use service to get materials
            List<MaterialItemDTO> materials = materialService.getMaterialsForService(id, validToken);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            log.error("Error fetching materials for service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Update materials for a service
     */
    @PutMapping("/service-request/{id}")
    public ResponseEntity<Map<String, Object>> updateMaterialsForService(
            @PathVariable Integer id,
            @RequestBody List<MaterialItemDTO> materials,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            // Use service to update materials
            boolean success = materialService.updateMaterialsForService(id, materials, validToken);
            
            if (success) {
                return ResponseEntity.ok(Collections.singletonMap("message", "Materials updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Failed to update materials"));
            }
        } catch (Exception e) {
            log.error("Error updating materials for service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to update materials: " + e.getMessage()));
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