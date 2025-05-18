package com.albany.mvc.controller.Admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    // Vehicle Tracking Endpoints
    @GetMapping("/api/vehicle-tracking/under-service")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getVehiclesUnderService(
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
                    apiBaseUrl + "/vehicle-tracking/under-service",
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
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
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
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
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}