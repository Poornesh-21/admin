package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for handling completed services functionality
 */
@Service
@RequiredArgsConstructor
public class CompletedServicesService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all completed services
     */
    public List<Map<String, Object>> getCompletedServices(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> completedServices = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Process and enhance each service
                completedServices.forEach(this::enhanceServiceData);
                return completedServices;
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
    }

    /**
     * Enhance service data with properly formatted fields
     */
    private void enhanceServiceData(Map<String, Object> service) {
        // Ensure consistent ID fields
        ensureConsistentIdFields(service);

        // Ensure vehicle information is complete
        enhanceVehicleInfo(service);

        // Ensure customer information is complete
        enhanceCustomerInfo(service);

        // Ensure financial information is present
        ensureFinancialData(service);

        // Ensure service status flags are present
        ensureServiceStatusFlags(service);
    }

    /**
     * Ensure consistent ID fields (serviceId/requestId)
     */
    private void ensureConsistentIdFields(Map<String, Object> service) {
        if (service.containsKey("serviceId") && !service.containsKey("requestId")) {
            service.put("requestId", service.get("serviceId"));
        } else if (service.containsKey("requestId") && !service.containsKey("serviceId")) {
            service.put("serviceId", service.get("requestId"));
        }
    }

    /**
     * Ensure vehicle information is complete and consistent
     */
    private void enhanceVehicleInfo(Map<String, Object> service) {
        // Ensure vehicle name exists
        if (!service.containsKey("vehicleName") || service.get("vehicleName") == null) {
            String brand = getStringValue(service, "vehicleBrand");
            String model = getStringValue(service, "vehicleModel");
            if (brand != null && model != null) {
                service.put("vehicleName", brand + " " + model);
            } else {
                service.put("vehicleName", "Unknown Vehicle");
            }
        }

        // Ensure registration number is present
        if (!service.containsKey("registrationNumber") || service.get("registrationNumber") == null) {
            service.put("registrationNumber", "Unknown");
        }

        // Ensure vehicle category is present
        if (!service.containsKey("category")) {
            service.put("category", "Vehicle");
        }
    }

    /**
     * Enhance customer information in service details
     */
    private void enhanceCustomerInfo(Map<String, Object> service) {
        // Set default customer name if missing
        if (!service.containsKey("customerName") || service.get("customerName") == null) {
            service.put("customerName", "Unknown Customer");
        }

        // Set default customer phone if missing
        if (!service.containsKey("customerPhone")) {
            service.put("customerPhone", "Not available");
        }

        // Set default customer email if missing
        if (!service.containsKey("customerEmail")) {
            service.put("customerEmail", "Not available");
        }

        // Set default membership status if missing
        if (!service.containsKey("membershipStatus")) {
            service.put("membershipStatus", "Standard");
        }
    }

    /**
     * Ensure financial data is present and consistent
     */
    private void ensureFinancialData(Map<String, Object> service) {
        // Ensure all financial fields have at least default values
        service.putIfAbsent("materialsTotal", 0.0);
        service.putIfAbsent("laborTotal", 0.0);

        // Get values with safety checks
        double materialsTotal = service.get("materialsTotal") instanceof Number ?
                ((Number) service.get("materialsTotal")).doubleValue() : 0.0;
        double laborTotal = service.get("laborTotal") instanceof Number ?
                ((Number) service.get("laborTotal")).doubleValue() : 0.0;

        // Calculate discount if premium
        double discount = 0.0;
        String membershipStatus = getStringValue(service, "membershipStatus");
        if (membershipStatus != null && membershipStatus.toLowerCase().contains("premium")) {
            // 20% discount on labor
            discount = laborTotal * 0.2;
            service.put("discount", discount);
        }

        // Calculate subtotal
        double subtotal = materialsTotal + laborTotal - discount;
        service.putIfAbsent("subtotal", subtotal);

        // Calculate tax
        double tax = subtotal * 0.18; // 18% GST
        service.putIfAbsent("tax", tax);
        service.putIfAbsent("gst", tax); // Alternative field name

        // Calculate total cost
        double totalCost = subtotal + tax;
        service.putIfAbsent("totalCost", totalCost);
    }

    /**
     * Ensure service status flags are present
     */
    private void ensureServiceStatusFlags(Map<String, Object> service) {
        // Ensure service status flags have default values
        service.putIfAbsent("hasBill", true);
        service.putIfAbsent("isPaid", true);
        service.putIfAbsent("hasInvoice", true);
        service.putIfAbsent("isDelivered", false);

        // Ensure alternative field name consistency
        if (service.containsKey("isPaid") && !service.containsKey("paid")) {
            service.put("paid", service.get("isPaid"));
        } else if (service.containsKey("paid") && !service.containsKey("isPaid")) {
            service.put("isPaid", service.get("paid"));
        }

        if (service.containsKey("isDelivered") && !service.containsKey("delivered")) {
            service.put("delivered", service.get("isDelivered"));
        } else if (service.containsKey("delivered") && !service.containsKey("isDelivered")) {
            service.put("isDelivered", service.get("delivered"));
        }
    }

    /**
     * Get details for a specific service request
     */
    public Map<String, Object> getServiceDetails(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + serviceId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> serviceDetails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Apply the standard data enhancement
                enhanceServiceData(serviceDetails);

                // Then apply additional enhancements specific to details view
                enrichServiceDetails(serviceDetails);

                return serviceDetails;
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyMap();
    }

    /**
     * Enrich service details with materials and labor information
     */
    private void enrichServiceDetails(Map<String, Object> serviceDetails) {
        // Make sure service advisor name is present
        if (!serviceDetails.containsKey("serviceAdvisorName") || serviceDetails.get("serviceAdvisorName") == null) {
            serviceDetails.put("serviceAdvisorName", "Not assigned");
        }

        // Make sure service type is present
        if (!serviceDetails.containsKey("serviceType") || serviceDetails.get("serviceType") == null) {
            serviceDetails.put("serviceType", "General Service");
        }
    }

    /**
     * Get a string value from a map safely
     */
    private String getStringValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return null;
    }

    /**
     * Filter completed services based on criteria
     */
    public List<Map<String, Object>> filterCompletedServices(Map<String, Object> filterCriteria, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(filterCriteria, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services/filter",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> filteredServices = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Process and enhance each filtered service
                filteredServices.forEach(this::enhanceServiceData);
                return filteredServices;
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
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
     * Record payment for a service
     */
    public Map<String, Object> recordPayment(Integer serviceId, Map<String, Object> paymentDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentDetails, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/payments/service-request/" + serviceId,
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
            return Collections.singletonMap("error", "Failed to record payment: " + e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Handle vehicle delivery/dispatch
     */
    public Map<String, Object> dispatchVehicle(Integer serviceId, Map<String, Object> dispatchDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(dispatchDetails, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + serviceId + "/dispatch",
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
            return Collections.singletonMap("error", "Failed to dispatch vehicle: " + e.getMessage());
        }

        return Collections.emptyMap();
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