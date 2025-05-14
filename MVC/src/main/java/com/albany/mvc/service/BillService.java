package com.albany.mvc.service;

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
 * Service for handling bill generation for completed services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;
    
    /**
     * Generate a bill for a completed service
     */
    public Map<String, Object> generateBill(Integer serviceId, Map<String, Object> billDetails, String token) {
        try {
            log.info("Generating bill for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(billDetails, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/bills/service-request/" + serviceId + "/generate",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                
                log.debug("Successfully generated bill for service ID: {}", serviceId);
                return result;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Failed to generate bill: Unexpected response " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to generate bill: " + e.getMessage());
        }
    }
    
    /**
     * Get bill for a service
     */
    public Map<String, Object> getBill(Integer serviceId, String token) {
        try {
            log.info("Fetching bill for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/bills/service-request/" + serviceId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> bill = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                
                log.debug("Successfully fetched bill for service ID: {}", serviceId);
                return bill;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error fetching bill: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Download bill PDF
     */
    public byte[] downloadBill(Integer serviceId, String token) {
        try {
            log.info("Downloading bill for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    apiBaseUrl + "/bills/service-request/" + serviceId + "/download",
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully downloaded bill PDF for service ID: {}", serviceId);
                return response.getBody();
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return new byte[0];
            }
        } catch (Exception e) {
            log.error("Error downloading bill: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
    
    /**
     * Calculate bill totals based on service details
     */
    public Map<String, Object> calculateBillTotals(Map<String, Object> serviceDetails) {
        Map<String, Object> totals = new HashMap<>();
        
        try {
            // Get materials and labor from service details
            List<Map<String, Object>> materials = getListValue(serviceDetails, "materials");
            List<Map<String, Object>> laborCharges = getListValue(serviceDetails, "laborCharges");
            
            // Calculate materials total
            BigDecimal materialsTotal = BigDecimal.ZERO;
            if (materials != null) {
                for (Map<String, Object> material : materials) {
                    BigDecimal quantity = getBigDecimalValue(material, "quantity", BigDecimal.ONE);
                    BigDecimal unitPrice = getBigDecimalValue(material, "unitPrice", BigDecimal.ZERO);
                    materialsTotal = materialsTotal.add(quantity.multiply(unitPrice));
                }
            }
            
            // Calculate labor total
            BigDecimal laborTotal = BigDecimal.ZERO;
            if (laborCharges != null) {
                for (Map<String, Object> labor : laborCharges) {
                    BigDecimal total = getBigDecimalValue(labor, "total", BigDecimal.ZERO);
                    laborTotal = laborTotal.add(total);
                }
            }
            
            // Check if premium membership discount applies
            BigDecimal discount = BigDecimal.ZERO;
            String membershipStatus = getStringValue(serviceDetails, "membershipStatus", "Standard");
            if ("Premium".equalsIgnoreCase(membershipStatus)) {
                // Apply 20% discount on labor
                discount = laborTotal.multiply(new BigDecimal("0.20"));
                totals.put("discount", discount);
                totals.put("isPremium", true);
            } else {
                totals.put("isPremium", false);
            }
            
            // Calculate subtotal
            BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
            
            // Calculate GST (18%)
            BigDecimal gst = subtotal.multiply(new BigDecimal("0.18"));
            
            // Calculate grand total
            BigDecimal grandTotal = subtotal.add(gst);
            
            // Set the totals
            totals.put("materialsTotal", materialsTotal);
            totals.put("laborTotal", laborTotal);
            totals.put("subtotal", subtotal);
            totals.put("gst", gst);
            totals.put("grandTotal", grandTotal);
            
        } catch (Exception e) {
            log.error("Error calculating bill totals: {}", e.getMessage(), e);
        }
        
        return totals;
    }
    
    /**
     * Helper method to get list value from a map
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof List) {
            return (List<Map<String, Object>>) map.get(key);
        }
        return new ArrayList<>();
    }
    
    /**
     * Helper method to get string value from a map with default
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return defaultValue;
    }
    
    /**
     * Helper method to get BigDecimal value from a map with default
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key, BigDecimal defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof BigDecimal) {
                return (BigDecimal) map.get(key);
            } else if (map.get(key) instanceof Number) {
                return new BigDecimal(((Number) map.get(key)).toString());
            } else {
                try {
                    return new BigDecimal(map.get(key).toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
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