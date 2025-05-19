package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service to handle customer management functionality
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all customers from API
     */
    public List<Map<String, Object>> getAllCustomers(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> customers = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Process each customer to format dates properly
                customers.forEach(this::processCustomerDates);
                return customers;
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyList();
    }

    /**
     * Get customer by ID
     */
    public Map<String, Object> getCustomerById(Integer customerId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> customer = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Process dates for this customer
                processCustomerDates(customer);
                return customer;
            }
        } catch (Exception e) {
            // Simplified error handling
        }

        return Collections.emptyMap();
    }

    /**
     * Process customer dates to ensure they're properly formatted for display
     */
    private void processCustomerDates(Map<String, Object> customer) {
        // Default placeholder for no service date
        customer.putIfAbsent("formattedLastServiceDate", "No service yet");
    }

    /**
     * Helper to create auth headers with token
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
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
     * Create a new customer
     */
    public Map<String, Object> createCustomer(Map<String, Object> customerData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> customer = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Process dates for the newly created customer
                processCustomerDates(customer);
                return customer;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating customer: " + e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Update an existing customer
     */
    public Map<String, Object> updateCustomer(Integer customerId, Map<String, Object> customerData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> customer = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Process dates for the updated customer
                processCustomerDates(customer);
                return customer;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating customer: " + e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Delete a customer
     */
    public boolean deleteCustomer(Integer customerId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Simplified error handling
        }

        return false;
    }
}