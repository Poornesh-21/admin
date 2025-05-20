package com.albany.mvc.service;

import com.albany.mvc.dto.CompletedServiceDTO;
import com.albany.mvc.util.ModelMapper;
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
 * Service for handling invoice and bill generation for completed services
 * Consolidated service that combines InvoiceService and BillService functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get invoice information for a completed service
     */
    public Map<String, Object> getInvoiceInfo(Integer serviceId, String token) {
        try {
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

                // Add customer contact information if missing
                enhanceInvoiceInfo(invoiceInfo);
                return invoiceInfo;
            }
        } catch (Exception e) {
            log.error("Error fetching invoice info: {}", e.getMessage(), e);
        }

        return Collections.emptyMap();
    }

    /**
     * Generate an invoice for a completed service
     */
    public Map<String, Object> generateInvoice(Integer serviceId, Map<String, Object> invoiceDetails, String token) {
        try {
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
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            }
        } catch (Exception e) {
            log.error("Error generating invoice: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to generate invoice: " + e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Process service data for invoice generation
     */
    public CompletedServiceDTO processServiceForInvoice(Map<String, Object> serviceData) {
        try {
            // Use the model mapper to convert Map to DTO
            CompletedServiceDTO dto = modelMapper.mapToCompletedServiceDTO(serviceData);

            // Calculate financial details if missing
            if (dto.getSubtotal() == null || dto.getTax() == null || dto.getTotalCost() == null) {
                calculateFinancialDetails(dto);
            }

            return dto;
        } catch (Exception e) {
            log.error("Error processing service for invoice: {}", e.getMessage(), e);
            return new CompletedServiceDTO();
        }
    }

    /**
     * Generate a bill for a service (migrated from BillService)
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
     * Get bill for a service (migrated from BillService)
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
     * Download bill PDF (migrated from BillService)
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
     * Calculate bill totals based on service details (migrated from BillService)
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
     * Enhance invoice information with additional data
     */
    private void enhanceInvoiceInfo(Map<String, Object> invoiceInfo) {
        // Add customer phone if missing
        if (!invoiceInfo.containsKey("customerPhone")) {
            if (invoiceInfo.containsKey("customer") && invoiceInfo.get("customer") instanceof Map) {
                Map<String, Object> customer = (Map<String, Object>) invoiceInfo.get("customer");
                if (customer.containsKey("phoneNumber")) {
                    invoiceInfo.put("customerPhone", customer.get("phoneNumber"));
                } else if (customer.containsKey("user") && customer.get("user") instanceof Map) {
                    Map<String, Object> user = (Map<String, Object>) customer.get("user");
                    if (user.containsKey("phoneNumber")) {
                        invoiceInfo.put("customerPhone", user.get("phoneNumber"));
                    }
                }
            }
        }

        // Normalize membership status
        if (invoiceInfo.containsKey("membershipStatus")) {
            String status = (String) invoiceInfo.get("membershipStatus");
            if (status != null) {
                status = status.trim();
                invoiceInfo.put("membershipStatus", status.substring(0, 1).toUpperCase() +
                        status.substring(1).toLowerCase());
            }
        }
    }

    /**
     * Calculate financial details for invoice
     */
    private void calculateFinancialDetails(CompletedServiceDTO dto) {
        // Materials total calculation
        BigDecimal materialsTotal = BigDecimal.ZERO;
        if (dto.getMaterials() != null && !dto.getMaterials().isEmpty()) {
            materialsTotal = dto.getMaterials().stream()
                    .map(material -> {
                        BigDecimal quantity = material.getQuantity() != null ? material.getQuantity() : BigDecimal.ONE;
                        BigDecimal unitPrice = material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO;
                        return quantity.multiply(unitPrice);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Labor total calculation
        BigDecimal laborTotal = BigDecimal.ZERO;
        if (dto.getLaborCharges() != null && !dto.getLaborCharges().isEmpty()) {
            laborTotal = dto.getLaborCharges().stream()
                    .map(labor -> labor.getTotal() != null ? labor.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Set calculated totals if not present
        if (dto.getMaterialsTotal() == null) {
            dto.setMaterialsTotal(materialsTotal);
        }

        if (dto.getLaborTotal() == null) {
            dto.setLaborTotal(laborTotal);
        }

        // Calculate premium discount
        BigDecimal discount = BigDecimal.ZERO;
        if ("Premium".equalsIgnoreCase(dto.getMembershipStatus())) {
            // 20% discount on labor
            discount = dto.getLaborTotal().multiply(new BigDecimal("0.20"));
            dto.setDiscount(discount);
        }

        // Calculate subtotal
        BigDecimal subtotal = dto.getMaterialsTotal().add(dto.getLaborTotal()).subtract(discount);
        dto.setSubtotal(subtotal);

        // Calculate tax (GST 18%)
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        dto.setTax(tax);

        // Calculate total cost
        BigDecimal totalCost = subtotal.add(tax);
        dto.setTotalCost(totalCost);
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
}