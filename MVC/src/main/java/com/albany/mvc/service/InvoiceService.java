package com.albany.mvc.service;

import com.albany.mvc.dto.CompletedServiceDTO;
import com.albany.mvc.dto.LaborChargeDTO;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling invoice generation and management for completed services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;
    
    /**
     * Get invoice information for a completed service
     */
    public Map<String, Object> getInvoiceInfo(Integer serviceId, String token) {
        try {
            log.info("Fetching invoice info for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/invoices/service-request/" + serviceId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> invoiceInfo = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                
                log.debug("Successfully fetched invoice info for service ID: {}", serviceId);
                return invoiceInfo;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error fetching invoice info: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Generate an invoice for a completed service
     */
    public Map<String, Object> generateInvoice(Integer serviceId, Map<String, Object> invoiceDetails, String token) {
        try {
            log.info("Generating invoice for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceDetails, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/invoices/service-request/" + serviceId + "/generate",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                
                log.debug("Successfully generated invoice for service ID: {}", serviceId);
                return result;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Failed to generate invoice: Unexpected response " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error generating invoice: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to generate invoice: " + e.getMessage());
        }
    }
    
    /**
     * Download invoice PDF
     */
    public byte[] downloadInvoice(Integer serviceId, String token) {
        try {
            log.info("Downloading invoice for service ID: {}", serviceId);
            HttpHeaders headers = createAuthHeaders(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    apiBaseUrl + "/invoices/service-request/" + serviceId + "/download",
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully downloaded invoice PDF for service ID: {}", serviceId);
                return response.getBody();
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return new byte[0];
            }
        } catch (Exception e) {
            log.error("Error downloading invoice: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
    
    /**
     * Process service data to extract invoice and financial information
     */
    public CompletedServiceDTO processServiceForInvoice(Map<String, Object> serviceData) {
        try {
            CompletedServiceDTO completedService = new CompletedServiceDTO();
            
            // Basic service info
            completedService.setServiceId(getIntegerValue(serviceData, "serviceId"));
            completedService.setVehicleName(getStringValue(serviceData, "vehicleName"));
            completedService.setRegistrationNumber(getStringValue(serviceData, "registrationNumber"));
            completedService.setCustomerName(getStringValue(serviceData, "customerName"));
            completedService.setCustomerEmail(getStringValue(serviceData, "customerEmail"));
            completedService.setMembershipStatus(getStringValue(serviceData, "membershipStatus"));
            completedService.setServiceType(getStringValue(serviceData, "serviceType"));
            completedService.setAdditionalDescription(getStringValue(serviceData, "additionalDescription"));
            completedService.setStatus(getStringValue(serviceData, "status"));
            completedService.setCategory(getStringValue(serviceData, "category"));
            completedService.setVehicleBrand(getStringValue(serviceData, "vehicleBrand"));
            completedService.setVehicleModel(getStringValue(serviceData, "vehicleModel"));
            
            // Dates
            completedService.setRequestDate(getLocalDateValue(serviceData, "requestDate"));
            completedService.setCompletedDate(getLocalDateValue(serviceData, "completedDate"));
            
            // Service advisor
            completedService.setServiceAdvisorName(getStringValue(serviceData, "serviceAdvisorName"));
            completedService.setServiceAdvisorId(getIntegerValue(serviceData, "serviceAdvisorId"));
            
            // Financial details
            completedService.setMaterialsTotal(getBigDecimalValue(serviceData, "materialsTotal"));
            completedService.setLaborTotal(getBigDecimalValue(serviceData, "laborTotal"));
            completedService.setDiscount(getBigDecimalValue(serviceData, "discount"));
            completedService.setSubtotal(getBigDecimalValue(serviceData, "subtotal"));
            completedService.setTax(getBigDecimalValue(serviceData, "tax"));
            completedService.setTotalCost(getBigDecimalValue(serviceData, "totalCost"));
            
            // Materials and labor
            completedService.setMaterials(getMaterialsList(serviceData));
            completedService.setLaborCharges(getLaborChargesList(serviceData));
            
            // Invoice and payment status
            completedService.setHasBill(getBooleanValue(serviceData, "hasBill"));
            completedService.setIsPaid(getBooleanValue(serviceData, "isPaid"));
            completedService.setHasInvoice(getBooleanValue(serviceData, "hasInvoice"));
            completedService.setIsDelivered(getBooleanValue(serviceData, "isDelivered"));
            
            // Invoice details
            completedService.setInvoiceId(getIntegerValue(serviceData, "invoiceId"));
            completedService.setInvoiceDate(getLocalDateValue(serviceData, "invoiceDate"));
            
            // Notes
            completedService.setNotes(getStringValue(serviceData, "notes"));
            
            return completedService;
        } catch (Exception e) {
            log.error("Error processing service data for invoice: {}", e.getMessage(), e);
            return new CompletedServiceDTO();
        }
    }
    
    /**
     * Extract materials list from service data
     */
    @SuppressWarnings("unchecked")
    private List<MaterialItemDTO> getMaterialsList(Map<String, Object> serviceData) {
        try {
            if (serviceData.containsKey("materials") && serviceData.get("materials") instanceof List) {
                List<Map<String, Object>> materialsList = (List<Map<String, Object>>) serviceData.get("materials");
                
                return materialsList.stream()
                        .map(materialData -> {
                            MaterialItemDTO material = new MaterialItemDTO();
                            material.setItemId(getIntegerValue(materialData, "itemId"));
                            material.setName(getStringValue(materialData, "name"));
                            material.setCategory(getStringValue(materialData, "category"));
                            material.setQuantity(getBigDecimalValue(materialData, "quantity"));
                            material.setUnitPrice(getBigDecimalValue(materialData, "unitPrice"));
                            material.setTotal(getBigDecimalValue(materialData, "total"));
                            material.setDescription(getStringValue(materialData, "description"));
                            return material;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error extracting materials list: {}", e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Extract labor charges list from service data
     */
    @SuppressWarnings("unchecked")
    private List<LaborChargeDTO> getLaborChargesList(Map<String, Object> serviceData) {
        try {
            if (serviceData.containsKey("laborCharges") && serviceData.get("laborCharges") instanceof List) {
                List<Map<String, Object>> laborList = (List<Map<String, Object>>) serviceData.get("laborCharges");
                
                return laborList.stream()
                        .map(laborData -> {
                            LaborChargeDTO labor = new LaborChargeDTO();
                            labor.setChargeId(getIntegerValue(laborData, "chargeId"));
                            labor.setDescription(getStringValue(laborData, "description"));
                            labor.setHours(getBigDecimalValue(laborData, "hours"));
                            labor.setRatePerHour(getBigDecimalValue(laborData, "ratePerHour"));
                            labor.setTotal(getBigDecimalValue(laborData, "total"));
                            return labor;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error extracting labor charges list: {}", e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Safe extraction of String value from a map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return null;
    }
    
    /**
     * Safe extraction of Integer value from a map
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
     * Safe extraction of BigDecimal value from a map
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
     * Safe extraction of LocalDate value from a map
     */
    private LocalDate getLocalDateValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof LocalDate) {
                return (LocalDate) map.get(key);
            } else {
                try {
                    return LocalDate.parse(map.get(key).toString());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    /**
     * Safe extraction of Boolean value from a map
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof Boolean) {
                return (Boolean) map.get(key);
            } else {
                return Boolean.parseBoolean(map.get(key).toString());
            }
        }
        return false;
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