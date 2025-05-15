package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service to handle completed services functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
            log.info("Fetching completed services from API");
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

                // Debug logging for first item
                if (!completedServices.isEmpty()) {
                    log.debug("First service item before enhancement: {}", completedServices.get(0));
                }

                // Process and enhance each service to ensure all required fields are present
                completedServices.forEach(this::enhanceServiceData);

                // Debug logging for after enhancement
                if (!completedServices.isEmpty()) {
                    log.debug("First service item after enhancement: {}", completedServices.get(0));
                }

                log.debug("Fetched {} completed services", completedServices.size());
                return completedServices;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching completed services: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Enhance service data with properly formatted fields
     */
    private void enhanceServiceData(Map<String, Object> service) {
        // Debug log
        log.debug("Enhancing service data for ID: {}", service.get("serviceId") != null ?
                service.get("serviceId") : service.get("requestId"));

        // Ensure consistent ID fields
        ensureConsistentIdFields(service);

        // Ensure vehicle information is complete
        enhanceVehicleInfo(service);

        // Ensure customer information is complete
        enhanceCustomerInfo(service);

        // Format dates for display
        enhanceServiceDates(service);

        // Ensure financial information is present
        ensureFinancialData(service);

        // Ensure service status flags are present
        ensureServiceStatusFlags(service);
    }

    /**
     * Ensure consistent ID fields (serviceId/requestId)
     */
    private void ensureConsistentIdFields(Map<String, Object> service) {
        // Make sure we have both serviceId and requestId consistently
        if (service.containsKey("serviceId") && !service.containsKey("requestId")) {
            service.put("requestId", service.get("serviceId"));
        } else if (service.containsKey("requestId") && !service.containsKey("serviceId")) {
            service.put("serviceId", service.get("requestId"));
        } else if (!service.containsKey("serviceId") && !service.containsKey("requestId")) {
            // This shouldn't happen, but just in case
            log.warn("Service missing both serviceId and requestId");
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
            // Try different possible field names
            for (String field : Arrays.asList("vehicleRegistration", "regNumber", "registration")) {
                if (service.containsKey(field) && service.get(field) != null) {
                    service.put("registrationNumber", service.get(field));
                    break;
                }
            }

            // If still not found, use placeholder
            if (!service.containsKey("registrationNumber") || service.get("registrationNumber") == null) {
                service.put("registrationNumber", "Unknown");
            }
        }

        // Ensure vehicle category is normalized
        if (service.containsKey("category")) {
            String category = getStringValue(service, "category");
            if (category != null) {
                // Normalize category to capitalize first letter
                category = category.trim();
                service.put("category", category.substring(0, 1).toUpperCase() +
                        category.substring(1).toLowerCase());
            }
        } else if (service.containsKey("vehicleType")) {
            // If category missing but vehicleType present, use that
            service.put("category", service.get("vehicleType"));
        } else {
            // Default category
            service.put("category", "Vehicle");
        }
    }

    /**
     * Enhance customer information in service details
     */
    private void enhanceCustomerInfo(Map<String, Object> service) {
        try {
            // Extract customer name if missing
            if (!service.containsKey("customerName") || service.get("customerName") == null) {
                if (service.containsKey("customer") && service.get("customer") instanceof Map) {
                    Map<String, Object> customer = (Map<String, Object>) service.get("customer");

                    if (customer.containsKey("firstName") && customer.containsKey("lastName")) {
                        String firstName = getStringValue(customer, "firstName");
                        String lastName = getStringValue(customer, "lastName");
                        if (firstName != null && lastName != null) {
                            service.put("customerName", firstName + " " + lastName);
                        }
                    } else if (customer.containsKey("user") && customer.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) customer.get("user");
                        if (user.containsKey("firstName") && user.containsKey("lastName")) {
                            String firstName = getStringValue(user, "firstName");
                            String lastName = getStringValue(user, "lastName");
                            if (firstName != null && lastName != null) {
                                service.put("customerName", firstName + " " + lastName);
                            }
                        }
                    }
                }

                // If still no name found, set placeholder
                if (!service.containsKey("customerName") || service.get("customerName") == null) {
                    service.put("customerName", "Unknown Customer");
                }
            }

            // Extract customer phone if missing
            if (!service.containsKey("customerPhone") || service.get("customerPhone") == null) {
                // Try to extract from nested customer data
                if (service.containsKey("customer") && service.get("customer") instanceof Map) {
                    Map<String, Object> customer = (Map<String, Object>) service.get("customer");
                    if (customer.containsKey("phoneNumber")) {
                        service.put("customerPhone", customer.get("phoneNumber"));
                    } else if (customer.containsKey("user") && customer.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) customer.get("user");
                        if (user.containsKey("phoneNumber")) {
                            service.put("customerPhone", user.get("phoneNumber"));
                        }
                    }
                }

                // If still no phone found, set placeholder
                if (!service.containsKey("customerPhone") || service.get("customerPhone") == null) {
                    service.put("customerPhone", "Not available");
                }
            }

            // Extract customer email if missing
            if (!service.containsKey("customerEmail") || service.get("customerEmail") == null) {
                // Try to extract from nested customer data
                if (service.containsKey("customer") && service.get("customer") instanceof Map) {
                    Map<String, Object> customer = (Map<String, Object>) service.get("customer");
                    if (customer.containsKey("email")) {
                        service.put("customerEmail", customer.get("email"));
                    } else if (customer.containsKey("user") && customer.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) customer.get("user");
                        if (user.containsKey("email")) {
                            service.put("customerEmail", user.get("email"));
                        }
                    }
                }

                // If still no email found, set placeholder
                if (!service.containsKey("customerEmail") || service.get("customerEmail") == null) {
                    service.put("customerEmail", "Not available");
                }
            }

            // Normalize membership status
            if (service.containsKey("membershipStatus")) {
                String status = getStringValue(service, "membershipStatus");
                if (status != null) {
                    status = status.trim();
                    service.put("membershipStatus", status.substring(0, 1).toUpperCase() +
                            status.substring(1).toLowerCase());
                } else {
                    service.put("membershipStatus", "Standard");
                }
            } else {
                service.put("membershipStatus", "Standard");
            }
        } catch (Exception e) {
            log.error("Error enhancing customer info: {}", e.getMessage(), e);
            // Set default values in case of error
            service.putIfAbsent("customerName", "Unknown Customer");
            service.putIfAbsent("customerPhone", "Not available");
            service.putIfAbsent("customerEmail", "Not available");
            service.putIfAbsent("membershipStatus", "Standard");
        }
    }

    /**
     * Format and enhance date fields in the service map
     */
    private void enhanceServiceDates(Map<String, Object> service) {
        try {
            // List of date fields to format
            List<String> dateFields = Arrays.asList("completedDate", "requestDate", "deliveryDate", "updatedAt");

            // Process each date field
            for (String fieldName : dateFields) {
                formatDateField(service, fieldName);
            }

            // Ensure completedDate exists
            if (!service.containsKey("completedDate") || service.get("completedDate") == null) {
                // Try using updatedAt as fallback
                if (service.containsKey("updatedAt") && service.get("updatedAt") != null) {
                    service.put("completedDate", service.get("updatedAt"));
                    formatDateField(service, "completedDate");
                } else {
                    // Last resort - use current date
                    service.put("completedDate", LocalDate.now().toString());
                    formatDateField(service, "completedDate");
                }
            }
        } catch (Exception e) {
            log.error("Error enhancing service dates: {}", e.getMessage(), e);
        }
    }

    /**
     * Format a date field in the service map
     */
    private void formatDateField(Map<String, Object> service, String fieldName) {
        if (service.containsKey(fieldName) && service.get(fieldName) != null) {
            try {
                Object dateObj = service.get(fieldName);
                if (dateObj instanceof String) {
                    // Try to parse and format the date
                    LocalDate date = LocalDate.parse((String) dateObj);
                    // Store both the original and a formatted version
                    service.put("formatted" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1),
                            date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                }
            } catch (Exception e) {
                log.warn("Error formatting date field {}: {}", fieldName, e.getMessage());
            }
        }
    }

    /**
     * Ensure financial data is present and consistent
     */
    private void ensureFinancialData(Map<String, Object> service) {
        try {
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

            // Ensure consistency between different field names
            if (service.containsKey("totalAmount") && !service.containsKey("totalCost")) {
                service.put("totalCost", service.get("totalAmount"));
            } else if (service.containsKey("totalCost") && !service.containsKey("totalAmount")) {
                service.put("totalAmount", service.get("totalCost"));
            }
        } catch (Exception e) {
            log.error("Error ensuring financial data: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensure service status flags are present
     */
    private void ensureServiceStatusFlags(Map<String, Object> service) {
        try {
            // Ensure service status flags have default values
            service.putIfAbsent("hasBill", isServiceBillGenerated(getIntegerValue(service, "requestId")));
            service.putIfAbsent("isPaid", isServicePaid(getIntegerValue(service, "requestId")));
            service.putIfAbsent("hasInvoice", isInvoiceGenerated(getIntegerValue(service, "requestId")));
            service.putIfAbsent("isDelivered", isVehicleDelivered(getIntegerValue(service, "requestId")));

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
        } catch (Exception e) {
            log.error("Error ensuring service status flags: {}", e.getMessage(), e);
        }
    }

    /**
     * Get details for a specific service request
     */
    public Map<String, Object> getServiceDetails(Integer serviceId, String token) {
        try {
            log.info("Fetching service details for ID: {}", serviceId);
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

                log.debug("Raw service details before enhancement: {}", serviceDetails);

                // Apply the standard data enhancement first for consistency
                enhanceServiceData(serviceDetails);

                // Then apply additional enhancements specific to details view
                enrichServiceDetails(serviceDetails);

                log.debug("Enhanced service details: {}", serviceDetails);
                return serviceDetails;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error fetching service details: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Enrich service details with materials and labor information
     */
    private void enrichServiceDetails(Map<String, Object> serviceDetails) {
        try {
            // Get service request ID
            Integer requestId = getIntegerValue(serviceDetails, "requestId");
            if (requestId == null) return;

            // Add materials info if not present
            if (!serviceDetails.containsKey("materials")) {
                List<Map<String, Object>> materials = getMaterialsForService(requestId);
                serviceDetails.put("materials", materials);

                // Calculate materials total if not present
                if (!serviceDetails.containsKey("materialsTotal")) {
                    double materialsTotal = materials.stream()
                            .mapToDouble(m -> {
                                double quantity = m.containsKey("quantity") ?
                                        parseDoubleOrZero(m.get("quantity")) : 0;
                                double unitPrice = m.containsKey("unitPrice") ?
                                        parseDoubleOrZero(m.get("unitPrice")) : 0;
                                return quantity * unitPrice;
                            })
                            .sum();
                    serviceDetails.put("materialsTotal", materialsTotal);
                }
            }

            // Add labor charges if not present
            if (!serviceDetails.containsKey("laborCharges")) {
                List<Map<String, Object>> laborCharges = getLaborChargesForService(requestId);
                serviceDetails.put("laborCharges", laborCharges);

                // Calculate labor total if not present
                if (!serviceDetails.containsKey("laborTotal")) {
                    double laborTotal = laborCharges.stream()
                            .mapToDouble(l -> l.containsKey("total") ?
                                    parseDoubleOrZero(l.get("total")) : 0)
                            .sum();
                    serviceDetails.put("laborTotal", laborTotal);
                }
            }

            // Make sure service advisor name is present
            if (!serviceDetails.containsKey("serviceAdvisorName") || serviceDetails.get("serviceAdvisorName") == null) {
                serviceDetails.put("serviceAdvisorName", "Not assigned");
            }

            // Make sure service type is present
            if (!serviceDetails.containsKey("serviceType") || serviceDetails.get("serviceType") == null) {
                serviceDetails.put("serviceType", "General Service");
            }

        } catch (Exception e) {
            log.error("Error enriching service details: {}", e.getMessage(), e);
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
     * Get an integer value from a map safely
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof Integer) {
                return (Integer) map.get(key);
            } else if (map.get(key) instanceof Number) {
                return ((Number) map.get(key)).intValue();
            } else if (map.get(key) instanceof String) {
                try {
                    return Integer.parseInt((String) map.get(key));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get materials used for a service
     */
    private List<Map<String, Object>> getMaterialsForService(Integer requestId) {
        try {
            log.debug("Fetching materials for service ID: {}", requestId);

            // API call to get materials (actual implementation)
            String url = apiBaseUrl + "/materials/service-request/" + requestId;

            try {
                HttpHeaders headers = createAuthHeaders(null); // Use placeholder token
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                }
            } catch (Exception e) {
                log.warn("Error fetching materials from API: {}", e.getMessage());
                // Fall back to placeholder data
            }

            // Return placeholder data if API call fails
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting materials for service: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get labor charges for a service
     */
    private List<Map<String, Object>> getLaborChargesForService(Integer requestId) {
        try {
            log.debug("Fetching labor charges for service ID: {}", requestId);

            // API call to get labor charges (actual implementation)
            String url = apiBaseUrl + "/labor/service-request/" + requestId;

            try {
                HttpHeaders headers = createAuthHeaders(null); // Use placeholder token
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                }
            } catch (Exception e) {
                log.warn("Error fetching labor charges from API: {}", e.getMessage());
                // Fall back to placeholder data
            }

            // Return placeholder data if API call fails
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting labor charges for service: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Additional methods for service status checks with default implementations
    private boolean isServiceBillGenerated(Integer requestId) {
        // In a real implementation, this would check the actual status
        // For now, return true as a placeholder
        return true;
    }

    private boolean isServicePaid(Integer requestId) {
        // In a real implementation, this would check if payment is recorded
        // For now, return true as a placeholder
        return true;
    }

    private boolean isInvoiceGenerated(Integer requestId) {
        // In a real implementation, this would check if an invoice exists
        // For now, return true as a placeholder
        return true;
    }

    private boolean isVehicleDelivered(Integer requestId) {
        // In a real implementation, this would check delivery status
        // For now, return false as a placeholder
        return false;
    }

    /**
     * Helper method to parse double or return 0
     */
    private double parseDoubleOrZero(Object value) {
        if (value == null) return 0;

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Filter completed services based on criteria
     */
    public List<Map<String, Object>> filterCompletedServices(Map<String, Object> filterCriteria, String token) {
        try {
            log.info("Filtering completed services with criteria: {}", filterCriteria);
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

                log.debug("Found {} services matching filter criteria", filteredServices.size());
                return filteredServices;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error filtering completed services: {}", e.getMessage(), e);
            return Collections.emptyList();
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
     * Record payment for a service
     */
    public Map<String, Object> recordPayment(Integer serviceId, Map<String, Object> paymentDetails, String token) {
        try {
            log.info("Recording payment for service ID: {}", serviceId);
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
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                log.debug("Successfully recorded payment for service ID: {}", serviceId);
                return result;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Failed to record payment: Unexpected response " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to record payment: " + e.getMessage());
        }
    }

    /**
     * Handle vehicle delivery/dispatch
     */
    public Map<String, Object> dispatchVehicle(Integer serviceId, Map<String, Object> dispatchDetails, String token) {
        try {
            log.info("Dispatching vehicle for service ID: {}", serviceId);
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
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                log.debug("Successfully dispatched vehicle for service ID: {}", serviceId);
                return result;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Failed to dispatch vehicle: Unexpected response " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error dispatching vehicle: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to dispatch vehicle: " + e.getMessage());
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