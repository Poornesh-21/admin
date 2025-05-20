package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolidated service that handles service requests and service assignments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all service requests
     */
    public List<ServiceRequestDto> getAllServiceRequests(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ServiceRequestDto> requests = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ServiceRequestDto>>() {}
                );

                if (requests != null) {
                    for (ServiceRequestDto req : requests) {
                        validateAndEnhanceRequest(req);
                    }
                }

                return requests;
            }
        } catch (Exception e) {
            log.error("Failed to fetch service requests: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch service requests: " + e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Get new service requests that need assignment (migrated from ServiceAssignmentService)
     */
    public List<Map<String, Object>> getNewServiceRequests(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-assignments/new",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching new service requests: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get assigned service requests (migrated from ServiceAssignmentService)
     */
    public List<Map<String, Object>> getAssignedRequests(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-assignments/assigned",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching assigned requests: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Assign a service to an advisor (migrated from ServiceAssignmentService)
     */
    public Map<String, Object> assignService(Integer serviceRequestId, Map<String, Object> assignmentData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(assignmentData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-assignments/assign",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error assigning service: {}", e.getMessage(), e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Validate and enhance a request with default values if needed
     */
    private void validateAndEnhanceRequest(ServiceRequestDto req) {
        if (req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            req.setStatus("Unknown");
        }

        if (req.getMembershipStatus() == null) {
            req.setMembershipStatus("Standard");
        }

        if ((req.getVehicleName() == null || req.getVehicleName().trim().isEmpty()) &&
                req.getVehicleBrand() != null && req.getVehicleModel() != null) {
            req.setVehicleName(req.getVehicleBrand() + " " + req.getVehicleModel());
        }
    }

    /**
     * Get service request by ID
     */
    public ServiceRequestDto getServiceRequestById(Integer requestId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto request = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                validateAndEnhanceRequest(request);
                return request;
            } else {
                throw new RuntimeException("Failed to fetch service request with ID: " + requestId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Service request not found with ID: " + requestId);
        } catch (Exception e) {
            log.error("Failed to fetch service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch service request: " + e.getMessage());
        }
    }

    /**
     * Create a new service request
     */
    public ServiceRequestDto createServiceRequest(ServiceRequestDto requestDto, String token) {
        try {
            validateAndEnhanceRequest(requestDto);

            // Create a new payload specifically for creating service requests
            Map<String, Object> requestMap = new HashMap<>();

            // CRITICAL: Set vehicleId properly - this was the root of the issue
            // API explicitly expects vehicleId (camelCase) and NOT vehicle_id
            if (requestDto.getVehicleId() != null) {
                requestMap.put("vehicleId", requestDto.getVehicleId());
            } else {
                throw new RuntimeException("Vehicle ID is required");
            }

            // Include both snake_case and camelCase versions of all critical fields
            if (requestDto.getVehicleBrand() != null) {
                requestMap.put("vehicleBrand", requestDto.getVehicleBrand());
                requestMap.put("vehicle_brand", requestDto.getVehicleBrand());
            }

            if (requestDto.getVehicleModel() != null) {
                requestMap.put("vehicleModel", requestDto.getVehicleModel());
                requestMap.put("vehicle_model", requestDto.getVehicleModel());
            }

            if (requestDto.getRegistrationNumber() != null) {
                requestMap.put("registrationNumber", requestDto.getRegistrationNumber());
                requestMap.put("vehicle_registration", requestDto.getRegistrationNumber());
            }

            if (requestDto.getVehicleCategory() != null) {
                requestMap.put("vehicleCategory", requestDto.getVehicleCategory());
                requestMap.put("vehicle_type", requestDto.getVehicleCategory());
            }

            // Set service details
            requestMap.put("serviceType", requestDto.getServiceType());
            requestMap.put("service_type", requestDto.getServiceType());
            requestMap.put("service_description", requestDto.getServiceType());

            // Delivery date handling
            if (requestDto.getDeliveryDate() != null) {
                requestMap.put("deliveryDate", requestDto.getDeliveryDate().toString());
                requestMap.put("delivery_date", requestDto.getDeliveryDate().toString());
            }

            // Description and status
            requestMap.put("additionalDescription", requestDto.getAdditionalDescription() != null ?
                    requestDto.getAdditionalDescription() : "");
            requestMap.put("additional_description", requestDto.getAdditionalDescription() != null ?
                    requestDto.getAdditionalDescription() : "");
            requestMap.put("status", requestDto.getStatus());

            // Add timestamps
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            requestMap.put("created_at", now);
            requestMap.put("updated_at", now);

            // Add user ID
            requestMap.put("user_id", 1);

            // Vehicle year
            requestMap.put("vehicle_year", LocalDate.now().getYear());

            log.info("Sending service request with payload: {}", requestMap);

            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto createdRequest = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                validateAndEnhanceRequest(createdRequest);
                return createdRequest;
            } else {
                throw new RuntimeException("Unexpected response from server: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("API error creating service request: Status {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            try {
                Map<String, Object> errorResponse = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        new TypeReference<Map<String, Object>>() {}
                );

                String errorMessage = errorResponse.containsKey("message")
                        ? errorResponse.get("message").toString()
                        : e.getMessage();

                throw new RuntimeException("API error: " + errorMessage);
            } catch (Exception ex) {
                throw new RuntimeException("API error: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to create service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create service request: " + e.getMessage());
        }
    }

    /**
     * Assign a service advisor
     */
    public ServiceRequestDto assignServiceAdvisor(Integer requestId, Integer advisorId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Integer> requestBody = Collections.singletonMap("advisorId", advisorId);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId + "/assign",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto updatedRequest = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                validateAndEnhanceRequest(updatedRequest);
                return updatedRequest;
            } else {
                throw new RuntimeException("Failed to assign service advisor");
            }
        } catch (Exception e) {
            log.error("Failed to assign service advisor: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign service advisor: " + e.getMessage());
        }
    }

    /**
     * Update service request status
     */
    public ServiceRequestDto updateServiceRequestStatus(Integer requestId, String status, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Collections.singletonMap("status", status);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto updatedRequest = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                validateAndEnhanceRequest(updatedRequest);
                return updatedRequest;
            } else {
                throw new RuntimeException("Failed to update service request status");
            }
        } catch (Exception e) {
            log.error("Failed to update service request status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update service request status: " + e.getMessage());
        }
    }

    /**
     * Create authentication headers with token
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