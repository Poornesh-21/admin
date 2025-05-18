package com.albany.mvc.service.customer;

import com.albany.mvc.dto.customer.CreateServiceRequestDTO;
import com.albany.mvc.dto.customer.ServiceRequestDTO;
import com.albany.mvc.util.customer.CustomerJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceRequestService {

    private final RestTemplate restTemplate;
    private final CustomerJwtUtil jwtUtil;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Create a new service request
     */
    public ServiceRequestDTO createServiceRequest(CreateServiceRequestDTO requestDTO, HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);
        if (jwt == null) {
            throw new RuntimeException("Authentication required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<CreateServiceRequestDTO> entity = new HttpEntity<>(requestDTO, headers);

        try {
            log.info("Creating service request for vehicle: {}", requestDTO.getVehicleRegistration());
            ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/api/service-requests",
                    HttpMethod.POST,
                    entity,
                    ServiceRequestDTO.class
            );

            log.info("Service request created successfully with status: {}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create service request: " + e.getMessage());
        }
    }

    /**
     * Get all service requests for the logged-in user
     */
    public List<ServiceRequestDTO> getUserServiceRequests(HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);
        if (jwt == null) {
            throw new RuntimeException("Authentication required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            log.info("Fetching user service requests");
            ResponseEntity<List<ServiceRequestDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/api/service-requests",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ServiceRequestDTO>>() {}
            );

            log.info("Retrieved {} service requests", response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching service requests: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch service requests: " + e.getMessage());
        }
    }

    /**
     * Get service request by ID
     */
    public ServiceRequestDTO getServiceRequestById(Long id, HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);
        if (jwt == null) {
            throw new RuntimeException("Authentication required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            log.info("Fetching service request with ID: {}", id);
            ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/api/service-requests/" + id,
                    HttpMethod.GET,
                    entity,
                    ServiceRequestDTO.class
            );

            log.info("Service request retrieved successfully");
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching service request details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch service request details: " + e.getMessage());
        }
    }

    /**
     * Get available service types
     */
    public List<String> getServiceTypes(HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);
        if (jwt == null) {
            throw new RuntimeException("Authentication required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            log.info("Fetching service types");
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    apiBaseUrl + "/api/service-requests/service-types",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            log.info("Retrieved {} service types", response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching service types: {}", e.getMessage(), e);
            // Return default service types if API fails
            log.info("Returning default service types");
            return List.of("GENERAL_SERVICE", "ENGINE_CHECK", "FULL_BODY_CLEANING", "REPAIR", "CUSTOM");
        }
    }

    /**
     * Get available vehicle types
     */
    public List<String> getVehicleTypes(HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);
        if (jwt == null) {
            throw new RuntimeException("Authentication required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            log.info("Fetching vehicle types");
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    apiBaseUrl + "/api/service-requests/vehicle-types",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            log.info("Retrieved {} vehicle types", response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching vehicle types: {}", e.getMessage(), e);
            // Return default vehicle types if API fails
            log.info("Returning default vehicle types");
            return List.of("Car", "Bike", "Truck");
        }
    }
}