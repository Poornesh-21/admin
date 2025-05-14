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
import java.util.stream.Collectors;

/**
 * Service for handling materials used in completed services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;
    
    /**
     * Get materials used in a service
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
                List<Map<String, Object>> materialsData = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                
                List<MaterialItemDTO> materials = materialsData.stream()
                        .map(this::convertToMaterialItemDTO)
                        .collect(Collectors.toList());
                
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
     * Update materials for a service
     */
    public boolean updateMaterialsForService(Integer serviceId, List<MaterialItemDTO> materials, String token) {
        try {
            log.info("Updating materials for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Convert DTOs to map format
            List<Map<String, Object>> materialsData = materials.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

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
     * Convert map to MaterialItemDTO
     */
    private MaterialItemDTO convertToMaterialItemDTO(Map<String, Object> map) {
        MaterialItemDTO dto = new MaterialItemDTO();
        
        if (map.containsKey("itemId")) {
            dto.setItemId(getIntegerValue(map, "itemId"));
        }
        
        if (map.containsKey("name")) {
            dto.setName(getStringValue(map, "name"));
        }
        
        if (map.containsKey("category")) {
            dto.setCategory(getStringValue(map, "category"));
        }
        
        if (map.containsKey("quantity")) {
            dto.setQuantity(getBigDecimalValue(map, "quantity"));
        }
        
        if (map.containsKey("unitPrice")) {
            dto.setUnitPrice(getBigDecimalValue(map, "unitPrice"));
        }
        
        if (map.containsKey("total")) {
            dto.setTotal(getBigDecimalValue(map, "total"));
        } else if (dto.getQuantity() != null && dto.getUnitPrice() != null) {
            // Calculate total if not provided
            dto.setTotal(dto.getQuantity().multiply(dto.getUnitPrice()));
        }
        
        if (map.containsKey("description")) {
            dto.setDescription(getStringValue(map, "description"));
        }
        
        return dto;
    }
    
    /**
     * Convert MaterialItemDTO to map
     */
    private Map<String, Object> convertToMap(MaterialItemDTO dto) {
        Map<String, Object> map = new HashMap<>();
        
        if (dto.getItemId() != null) {
            map.put("itemId", dto.getItemId());
        }
        
        if (dto.getName() != null) {
            map.put("name", dto.getName());
        }
        
        if (dto.getCategory() != null) {
            map.put("category", dto.getCategory());
        }
        
        if (dto.getQuantity() != null) {
            map.put("quantity", dto.getQuantity());
        }
        
        if (dto.getUnitPrice() != null) {
            map.put("unitPrice", dto.getUnitPrice());
        }
        
        if (dto.getTotal() != null) {
            map.put("total", dto.getTotal());
        } else if (dto.getQuantity() != null && dto.getUnitPrice() != null) {
            // Calculate total if not provided
            map.put("total", dto.getQuantity().multiply(dto.getUnitPrice()));
        }
        
        if (dto.getDescription() != null) {
            map.put("description", dto.getDescription());
        }
        
        return map;
    }
    
    /**
     * Helper method to get string value from a map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return null;
    }
    
    /**
     * Helper method to get integer value from a map
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof Integer) {
                return (Integer) map.get(key);
            } else if (map.get(key) instanceof Number) {
                return ((Number) map.get(key)).intValue();
            } else {
                try {
                    return Integer.parseInt(map.get(key).toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to get BigDecimal value from a map
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof BigDecimal) {
                return (BigDecimal) map.get(key);
            } else if (map.get(key) instanceof Number) {
                return BigDecimal.valueOf(((Number) map.get(key)).doubleValue());
            } else {
                try {
                    return new BigDecimal(map.get(key).toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
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