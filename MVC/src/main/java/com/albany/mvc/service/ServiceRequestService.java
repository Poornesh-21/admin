package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
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
            throw new RuntimeException("Failed to fetch service requests: " + e.getMessage());
        }

        return Collections.emptyList();
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
            throw new RuntimeException("Failed to fetch service request: " + e.getMessage());
        }
    }

    /**
     * Create a new service request
     */
    public ServiceRequestDto createServiceRequest(ServiceRequestDto requestDto, String token) {
        try {
            validateAndEnhanceRequest(requestDto);

            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ServiceRequestDto> entity = new HttpEntity<>(requestDto, headers);

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