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

                // Process and enhance each service to ensure all required fields are present
                completedServices.forEach(this::enhanceServiceData);

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

        // Extract customer phone if available
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
        }

        // Format dates if needed
        formatDateField(service, "completedDate");
        formatDateField(service, "requestDate");
        formatDateField(service, "deliveryDate");
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

                // Enhance the service details with additional information
                enrichServiceDetails(serviceDetails);

                log.debug("Fetched details for service ID: {}", serviceId);
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
            Integer requestId = (Integer) serviceDetails.get("requestId");
            if (requestId == null) return;

            // Enhance customer contact information
            enhanceCustomerInfo(serviceDetails);

            // Add materials info if not present
            if (!serviceDetails.containsKey("materials")) {
                List<Map<String, Object>> materials = getMaterialsForService(requestId);
                serviceDetails.put("materials", materials);

                // Calculate materials total
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

            // Add labor charges if not present
            if (!serviceDetails.containsKey("laborCharges")) {
                List<Map<String, Object>> laborCharges = getLaborChargesForService(requestId);
                serviceDetails.put("laborCharges", laborCharges);

                // Calculate labor total
                double laborTotal = laborCharges.stream()
                        .mapToDouble(l -> l.containsKey("total") ?
                                parseDoubleOrZero(l.get("total")) : 0)
                        .sum();

                serviceDetails.put("laborTotal", laborTotal);
            }

            // Calculate totals if not present
            if (!serviceDetails.containsKey("subtotal")) {
                double materialsTotal = serviceDetails.containsKey("materialsTotal") ?
                        parseDoubleOrZero(serviceDetails.get("materialsTotal")) : 0;
                double laborTotal = serviceDetails.containsKey("laborTotal") ?
                        parseDoubleOrZero(serviceDetails.get("laborTotal")) : 0;

                // Apply premium discount if applicable
                String membershipStatus = getStringValue(serviceDetails, "membershipStatus");
                if (membershipStatus != null && membershipStatus.toLowerCase().contains("premium")) {
                    // 20% discount on labor
                    double discount = laborTotal * 0.2;
                    serviceDetails.put("discount", discount);
                    serviceDetails.put("isPremium", true);
                    double discountedLabor = laborTotal - discount;
                    serviceDetails.put("discountedLaborTotal", discountedLabor);
                    double subtotal = materialsTotal + discountedLabor;
                    serviceDetails.put("subtotal", subtotal);
                } else {
                    serviceDetails.put("isPremium", false);
                    double subtotal = materialsTotal + laborTotal;
                    serviceDetails.put("subtotal", subtotal);
                }

                // Get subtotal
                double subtotal = serviceDetails.containsKey("subtotal") ?
                        parseDoubleOrZero(serviceDetails.get("subtotal")) :
                        materialsTotal + laborTotal;

                // Calculate GST (18%)
                double gst = subtotal * 0.18;
                serviceDetails.put("gst", gst);

                // Calculate total cost
                double totalCost = subtotal + gst;
                serviceDetails.put("totalCost", totalCost);
            }

            // Set workflow status flags if not present
            serviceDetails.putIfAbsent("hasBill", isServiceBillGenerated(requestId));
            serviceDetails.putIfAbsent("isPaid", isServicePaid(requestId));
            serviceDetails.putIfAbsent("hasInvoice", isInvoiceGenerated(requestId));
            serviceDetails.putIfAbsent("isDelivered", isVehicleDelivered(requestId));

            // Enhance and normalize vehicle info
            enhanceVehicleInfo(serviceDetails);

            // Format dates
            enhanceServiceDates(serviceDetails);

        } catch (Exception e) {
            log.error("Error enriching service details: {}", e.getMessage(), e);
        }
    }

    /**
     * Enhance customer information in service details
     */
    private void enhanceCustomerInfo(Map<String, Object> serviceDetails) {
        try {
            // Extract customer phone if available
            if (!serviceDetails.containsKey("customerPhone") || serviceDetails.get("customerPhone") == null) {
                // Try to extract from nested customer data
                if (serviceDetails.containsKey("customer") && serviceDetails.get("customer") instanceof Map) {
                    Map<String, Object> customer = (Map<String, Object>) serviceDetails.get("customer");
                    if (customer.containsKey("phoneNumber")) {
                        serviceDetails.put("customerPhone", customer.get("phoneNumber"));
                    } else if (customer.containsKey("user") && customer.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) customer.get("user");
                        if (user.containsKey("phoneNumber")) {
                            serviceDetails.put("customerPhone", user.get("phoneNumber"));
                        }
                    }
                }
            }

            // If unable to find phone, create a placeholder
            if (!serviceDetails.containsKey("customerPhone") || serviceDetails.get("customerPhone") == null) {
                serviceDetails.put("customerPhone", "Not available");
            }

            // Format customer name if available
            if (!serviceDetails.containsKey("customerName") || serviceDetails.get("customerName") == null) {
                if (serviceDetails.containsKey("customer") && serviceDetails.get("customer") instanceof Map) {
                    Map<String, Object> customer = (Map<String, Object>) serviceDetails.get("customer");

                    if (customer.containsKey("firstName") && customer.containsKey("lastName")) {
                        String firstName = getStringValue(customer, "firstName");
                        String lastName = getStringValue(customer, "lastName");
                        if (firstName != null && lastName != null) {
                            serviceDetails.put("customerName", firstName + " " + lastName);
                        }
                    } else if (customer.containsKey("user") && customer.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) customer.get("user");
                        if (user.containsKey("firstName") && user.containsKey("lastName")) {
                            String firstName = getStringValue(user, "firstName");
                            String lastName = getStringValue(user, "lastName");
                            if (firstName != null && lastName != null) {
                                serviceDetails.put("customerName", firstName + " " + lastName);
                            }
                        }
                    }
                }
            }

            // Normalize membership status
            if (serviceDetails.containsKey("membershipStatus")) {
                String status = getStringValue(serviceDetails, "membershipStatus");
                if (status != null) {
                    status = status.trim();
                    serviceDetails.put("membershipStatus", status.substring(0, 1).toUpperCase() +
                            status.substring(1).toLowerCase());
                } else {
                    serviceDetails.put("membershipStatus", "Standard");
                }
            } else {
                serviceDetails.put("membershipStatus", "Standard");
            }
        } catch (Exception e) {
            log.error("Error enhancing customer info: {}", e.getMessage(), e);
        }
    }

    /**
     * Enhance vehicle information in service details
     */
    private void enhanceVehicleInfo(Map<String, Object> serviceDetails) {
        try {
            // Ensure vehicle name exists
            if (!serviceDetails.containsKey("vehicleName") || serviceDetails.get("vehicleName") == null) {
                String brand = getStringValue(serviceDetails, "vehicleBrand");
                String model = getStringValue(serviceDetails, "vehicleModel");
                if (brand != null && model != null) {
                    serviceDetails.put("vehicleName", brand + " " + model);
                } else {
                    serviceDetails.put("vehicleName", "Unknown Vehicle");
                }
            }

            // Ensure vehicle category is normalized
            if (serviceDetails.containsKey("category")) {
                String category = getStringValue(serviceDetails, "category");
                if (category != null) {
                    // Normalize category to capitalize first letter
                    category = category.trim();
                    serviceDetails.put("category", category.substring(0, 1).toUpperCase() +
                            category.substring(1).toLowerCase());
                }
            }
        } catch (Exception e) {
            log.error("Error enhancing vehicle info: {}", e.getMessage(), e);
        }
    }

    /**
     * Enhance service dates in service details
     */
    private void enhanceServiceDates(Map<String, Object> serviceDetails) {
        try {
            // Format request date
            if (serviceDetails.containsKey("requestDate") && serviceDetails.get("requestDate") != null) {
                Object dateObj = serviceDetails.get("requestDate");
                if (dateObj instanceof String) {
                    try {
                        LocalDate date = LocalDate.parse((String) dateObj);
                        serviceDetails.put("formattedRequestDate",
                                date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                    } catch (Exception e) {
                        log.warn("Error parsing request date: {}", e.getMessage());
                    }
                }
            }

            // Format completed date
            if (serviceDetails.containsKey("completedDate") && serviceDetails.get("completedDate") != null) {
                Object dateObj = serviceDetails.get("completedDate");
                if (dateObj instanceof String) {
                    try {
                        LocalDate date = LocalDate.parse((String) dateObj);
                        serviceDetails.put("formattedCompletedDate",
                                date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                    } catch (Exception e) {
                        log.warn("Error parsing completed date: {}", e.getMessage());
                    }
                }
            }

            // Format delivery date
            if (serviceDetails.containsKey("deliveryDate") && serviceDetails.get("deliveryDate") != null) {
                Object dateObj = serviceDetails.get("deliveryDate");
                if (dateObj instanceof String) {
                    try {
                        LocalDate date = LocalDate.parse((String) dateObj);
                        serviceDetails.put("formattedDeliveryDate",
                                date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                    } catch (Exception e) {
                        log.warn("Error parsing delivery date: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error enhancing service dates: {}", e.getMessage(), e);
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
     * Get materials used for a service
     */
    private List<Map<String, Object>> getMaterialsForService(Integer requestId) {
        try {
            // In a production implementation, this would call the API to get real data

            // Make an API call to get materials
            String token = "YOUR_TOKEN"; // You would need to have a proper token here
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // This URL would need to be adjusted to match your API
            String url = apiBaseUrl + "/materials/service/" + requestId;

            try {
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

            // Placeholder data for illustration purposes
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
            // In a production implementation, this would call the API to get real data

            // Make an API call to get labor charges
            String token = "YOUR_TOKEN"; // You would need to have a proper token here
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // This URL would need to be adjusted to match your API
            String url = apiBaseUrl + "/labor/service/" + requestId;

            try {
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

            // Placeholder data for illustration purposes
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting labor charges for service: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Additional methods for service status checks
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