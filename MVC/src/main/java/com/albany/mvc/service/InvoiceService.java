package com.albany.mvc.service;

import com.albany.mvc.dto.CompletedServiceDTO;
import com.albany.mvc.util.ModelMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for handling invoice generation and management for completed services
 */
@Service
@RequiredArgsConstructor
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
            // Simplified error handling
        }

        return Collections.emptyMap();
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
            return new CompletedServiceDTO();
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
}