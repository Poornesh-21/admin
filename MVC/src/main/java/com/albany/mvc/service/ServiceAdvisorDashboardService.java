package com.albany.mvc.service;

import com.albany.mvc.dto.MaterialItemDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Consolidated service for Service Advisor Dashboard functionality
 * Includes materials management (migrated from MaterialService)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all vehicles assigned to the authenticated service advisor
     */
    public List<Map<String, Object>> getAssignedVehicles(String token, String advisorEmail) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/dashboard/assigned-vehicles",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> assignedVehicles = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return assignedVehicles;
            }
        } catch (Exception e) {
            log.error("Error getting assigned vehicles for service advisor {}: {}", advisorEmail, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * Get details for a specific service request
     */
    public Map<String, Object> getServiceDetails(Integer requestId, String token, String advisorEmail) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/dashboard/service-details/" + requestId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> serviceDetails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return serviceDetails;
            }
        } catch (Exception e) {
            log.error("Error getting service details for request {}: {}", requestId, e.getMessage(), e);
        }

        return Collections.emptyMap();
    }

    /**
     * Add inventory items to a service request
     */
    public Map<String, Object> addMaterialsToServiceRequest(
            Integer requestId, Map<String, Object> materialsRequest, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(materialsRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/inventory-items",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return result;
            }
        } catch (Exception e) {
            log.error("Error adding materials to service request {}: {}", requestId, e.getMessage(), e);
        }

        return Collections.emptyMap();
    }

    /**
     * Add labor charges to a service request
     */
    public Map<String, Object> addLaborCharges(
            Integer requestId, List<Map<String, Object>> laborCharges, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(laborCharges, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/labor-charges",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return result;
            }
        } catch (Exception e) {
            log.error("Error adding labor charges to service request {}: {}", requestId, e.getMessage(), e);
        }

        return Collections.emptyMap();
    }

    /**
     * Update service status
     */
    public Map<String, Object> updateServiceStatus(
            Integer requestId, String status, String notes, Boolean notifyCustomer, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("status", status);
            statusUpdate.put("notes", notes);
            statusUpdate.put("notifyCustomer", notifyCustomer);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(statusUpdate, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return result;
            }
        } catch (Exception e) {
            log.error("Error updating status for service request {}: {}", requestId, e.getMessage(), e);
        }

        return Collections.emptyMap();
    }

    /**
     * Generate bill for a service
     */
    public Map<String, Object> generateServiceBill(
            Integer requestId, Map<String, Object> billRequest, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(billRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/generate-bill",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return result;
            }
        } catch (Exception e) {
            log.error("Error generating bill for service request {}: {}", requestId, e.getMessage(), e);
        }

        return Collections.emptyMap();
    }

    /**
     * Get materials used in a service (migrated from MaterialService)
     */
    public List<MaterialItemDTO> getMaterialsForService(Integer serviceId, String token) {
        try {
            log.info("Fetching materials for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/materials/service-request/" + serviceId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<MaterialItemDTO> materials = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<MaterialItemDTO>>() {}
                );
                
                log.debug("Fetched {} materials for service ID: {}", materials.size(), serviceId);
                return materials;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching materials for service: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Update materials for a service (migrated from MaterialService)
     */
    public boolean updateMaterialsForService(Integer serviceId, List<MaterialItemDTO> materials, String token) {
        try {
            log.info("Updating materials for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Convert DTOs to map format
            List<Map<String, Object>> materialsData = new ArrayList<>();
            for (MaterialItemDTO material : materials) {
                Map<String, Object> materialMap = new HashMap<>();
                
                if (material.getItemId() != null) {
                    materialMap.put("itemId", material.getItemId());
                }
                
                if (material.getName() != null) {
                    materialMap.put("name", material.getName());
                }
                
                if (material.getCategory() != null) {
                    materialMap.put("category", material.getCategory());
                }
                
                if (material.getQuantity() != null) {
                    materialMap.put("quantity", material.getQuantity());
                }
                
                if (material.getUnitPrice() != null) {
                    materialMap.put("unitPrice", material.getUnitPrice());
                }
                
                if (material.getTotal() != null) {
                    materialMap.put("total", material.getTotal());
                } else if (material.getQuantity() != null && material.getUnitPrice() != null) {
                    materialMap.put("total", material.getQuantity().multiply(material.getUnitPrice()));
                }
                
                if (material.getDescription() != null) {
                    materialMap.put("description", material.getDescription());
                }
                
                materialsData.add(materialMap);
            }

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(materialsData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/materials/service-request/" + serviceId,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                log.debug("Successfully updated materials for service ID: {}", serviceId);
            } else {
                log.warn("Failed to update materials. Response status: {}", response.getStatusCode());
            }
            
            return success;
        } catch (Exception e) {
            log.error("Error updating materials for service: {}", e.getMessage(), e);
            return false;
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
}