package com.albany.mvc.controller.Admin;

import com.albany.mvc.dto.MaterialItemDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class ServiceDetailsController extends AdminBaseController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    @GetMapping("/service-details/{serviceId}")
    public ResponseEntity<Map<String, Object>> getServiceDetails(
            @PathVariable Integer serviceId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            Map<String, Object> serviceDetails = getServiceRequestDetails(serviceId, validToken);
            if (serviceDetails.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Get materials
            List<Map<String, Object>> materials = getMaterialsForService(serviceId, validToken);
            serviceDetails.put("materials", !materials.isEmpty() ? materials : new ArrayList<>());

            // Get labor charges
            List<Map<String, Object>> laborCharges = getLaborChargesForService(serviceId, validToken);
            serviceDetails.put("laborCharges", !laborCharges.isEmpty() ? laborCharges : new ArrayList<>());

            // Get service tracking
            List<Map<String, Object>> serviceTracking = getServiceTrackingForService(serviceId, validToken);
            if (!serviceTracking.isEmpty()) {
                serviceDetails.put("serviceTracking", serviceTracking);
                
                // Extract labor charges from tracking if needed
                if (laborCharges.isEmpty()) {
                    List<Map<String, Object>> derivedLaborCharges = extractLaborChargesFromTracking(serviceTracking);
                    if (!derivedLaborCharges.isEmpty()) {
                        serviceDetails.put("laborCharges", derivedLaborCharges);
                    }
                }
            }

            // Get invoice data
            Map<String, Object> invoice = getInvoiceForService(serviceId, validToken);
            if (!invoice.isEmpty()) {
                serviceDetails.put("invoice", invoice);
                serviceDetails.put("hasInvoice", true);
                serviceDetails.put("invoiceId", invoice.get("invoiceId"));
            } else {
                serviceDetails.put("hasInvoice", false);
            }

            // Get payment data
            Map<String, Object> payment = getPaymentForService(serviceId, validToken);
            if (!payment.isEmpty()) {
                serviceDetails.put("payment", payment);
                serviceDetails.put("isPaid", "Completed".equals(payment.get("status")));
                serviceDetails.put("paid", "Completed".equals(payment.get("status")));
            } else {
                serviceDetails.put("isPaid", false);
                serviceDetails.put("paid", false);
            }

            // Calculate financial summary
            calculateFinancialSummary(serviceDetails);

            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to retrieve service details: " + e.getMessage()));
        }
    }

    @GetMapping("/materials/service-request/{id}")
    public ResponseEntity<List<MaterialItemDTO>> getMaterialsForServiceRequest(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = apiBaseUrl + "/materials/service-request/" + id;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<MaterialItemDTO> materials = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<MaterialItemDTO>>() {}
                );
                return ResponseEntity.ok(materials);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyList());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PutMapping("/materials/service-request/{id}")
    public ResponseEntity<Map<String, Object>> updateMaterialsForService(
            @PathVariable Integer id,
            @RequestBody List<MaterialItemDTO> materials,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<MaterialItemDTO>> entity = new HttpEntity<>(materials, headers);

            String url = apiBaseUrl + "/materials/service-request/" + id;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "Materials updated successfully"));
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body(Collections.singletonMap("error", "Failed to update materials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to update materials: " + e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> getServiceRequestDetails(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = apiBaseUrl + "/vehicle-tracking/service-request/" + serviceId;
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
                            new TypeReference<Map<String, Object>>() {}
                    );
                }
            } catch (Exception e) {
                // Try alternative endpoint if the first one fails
            }

            // Alternative endpoint
            url = apiBaseUrl + "/service-requests/" + serviceId;
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
                            new TypeReference<Map<String, Object>>() {}
                    );
                }
            } catch (Exception e) {
                // Continue to third attempt
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> getMaterialsForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String[] materialEndpoints = {
                    "/admin/api/materials/service-request/" + serviceId,
                    "/materials/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/materials"
            };

            for (String endpoint : materialEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        try {
                            return objectMapper.readValue(
                                    response.getBody(),
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );
                        } catch (Exception e) {
                            // If parsing as list fails, try as a wrapped object
                            try {
                                Map<String, Object> wrapper = objectMapper.readValue(
                                        response.getBody(),
                                        new TypeReference<Map<String, Object>>() {}
                                );

                                for (String field : new String[]{"items", "materials", "materialItems"}) {
                                    if (wrapper.containsKey(field) && wrapper.get(field) instanceof List) {
                                        return (List<Map<String, Object>>) wrapper.get(field);
                                    }
                                }
                            } catch (Exception nestedEx) {
                                // Continue to next endpoint
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next endpoint
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getLaborChargesForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String[] laborEndpoints = {
                    "/admin/api/labor/service-request/" + serviceId,
                    "/labor-charges/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/labor-charges"
            };

            for (String endpoint : laborEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        try {
                            return objectMapper.readValue(
                                    response.getBody(),
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );
                        } catch (Exception e) {
                            // Try parsing as wrapped object
                            try {
                                Map<String, Object> wrapper = objectMapper.readValue(
                                        response.getBody(),
                                        new TypeReference<Map<String, Object>>() {}
                                );

                                for (String field : new String[]{"laborCharges", "charges", "labor"}) {
                                    if (wrapper.containsKey(field) && wrapper.get(field) instanceof List) {
                                        return (List<Map<String, Object>>) wrapper.get(field);
                                    }
                                }
                            } catch (Exception nestedEx) {
                                // Continue to next endpoint
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next endpoint
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getServiceTrackingForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String[] trackingEndpoints = {
                    "/admin/api/service-tracking/" + serviceId,
                    "/service-tracking/" + serviceId,
                    "/service-details/" + serviceId + "/tracking"
            };

            for (String endpoint : trackingEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
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
                    // Continue to next endpoint
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> getInvoiceForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String[] invoiceEndpoints = {
                    "/admin/api/invoices/service-request/" + serviceId,
                    "/invoices/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/invoice"
            };

            for (String endpoint : invoiceEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
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
                                new TypeReference<Map<String, Object>>() {}
                        );
                    }
                } catch (Exception e) {
                    // Continue to next endpoint
                }
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> getPaymentForService(Integer serviceId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String[] paymentEndpoints = {
                    "/admin/api/payments/service-request/" + serviceId,
                    "/payments/service-request/" + serviceId,
                    "/service-details/" + serviceId + "/payment"
            };

            for (String endpoint : paymentEndpoints) {
                String url = endpoint.startsWith("/admin") ? endpoint : apiBaseUrl + endpoint;
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
                                new TypeReference<Map<String, Object>>() {}
                        );
                    }
                } catch (Exception e) {
                    // Continue to next endpoint
                }
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> extractLaborChargesFromTracking(List<Map<String, Object>> trackingEntries) {
        List<Map<String, Object>> laborCharges = new ArrayList<>();

        for (Map<String, Object> entry : trackingEntries) {
            if (entry.containsKey("laborCost") && entry.get("laborCost") != null &&
                    !getBigDecimal(entry.get("laborCost"), BigDecimal.ZERO).equals(BigDecimal.ZERO)) {

                Map<String, Object> laborCharge = new HashMap<>();
                String workDescription = getStringValue(entry, "workDescription", "Labor Service");
                
                if (workDescription.startsWith("Labor:")) {
                    workDescription = workDescription.substring(6).trim();
                } else if (workDescription.startsWith("Labor charges:")) {
                    workDescription = workDescription.substring(14).trim();
                }
                laborCharge.put("description", workDescription);

                Integer laborMinutes = getIntValue(entry, "laborMinutes", 0);
                BigDecimal hours = BigDecimal.ZERO;
                if (laborMinutes > 0) {
                    hours = new BigDecimal(laborMinutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
                }
                laborCharge.put("hours", hours);

                BigDecimal laborCost = getBigDecimal(entry.get("laborCost"), BigDecimal.ZERO);
                BigDecimal ratePerHour = BigDecimal.ZERO;
                if (laborMinutes > 0) {
                    ratePerHour = laborCost.multiply(new BigDecimal("60"))
                            .divide(new BigDecimal(laborMinutes), 2, RoundingMode.HALF_UP);
                }
                laborCharge.put("ratePerHour", ratePerHour);
                laborCharge.put("total", laborCost);

                laborCharges.add(laborCharge);
            }
        }

        return laborCharges;
    }

    private void calculateFinancialSummary(Map<String, Object> data) {
        BigDecimal materialsTotal = BigDecimal.ZERO;
        List<Map<String, Object>> materials = new ArrayList<>();
        if (data.containsKey("materials") && data.get("materials") instanceof List) {
            materials = (List<Map<String, Object>>) data.get("materials");
        }

        for (Map<String, Object> material : materials) {
            BigDecimal quantity = getBigDecimal(material.get("quantity"), BigDecimal.ZERO);
            BigDecimal unitPrice = getBigDecimal(material.get("unitPrice"), BigDecimal.ZERO);

            if (unitPrice.equals(BigDecimal.ZERO) && material.containsKey("inventoryItem") &&
                    material.get("inventoryItem") instanceof Map) {
                Map<String, Object> item = (Map<String, Object>) material.get("inventoryItem");
                unitPrice = getBigDecimal(item.get("unitPrice"), BigDecimal.ZERO);
            }

            if (material.containsKey("total") && material.get("total") != null) {
                materialsTotal = materialsTotal.add(getBigDecimal(material.get("total"), BigDecimal.ZERO));
            } else {
                materialsTotal = materialsTotal.add(quantity.multiply(unitPrice));
            }
        }

        BigDecimal laborTotal = BigDecimal.ZERO;
        List<Map<String, Object>> laborCharges = new ArrayList<>();
        if (data.containsKey("laborCharges") && data.get("laborCharges") instanceof List) {
            laborCharges = (List<Map<String, Object>>) data.get("laborCharges");
        }

        for (Map<String, Object> labor : laborCharges) {
            if (labor.containsKey("total") && labor.get("total") != null) {
                laborTotal = laborTotal.add(getBigDecimal(labor.get("total"), BigDecimal.ZERO));
            } else {
                BigDecimal hours = getBigDecimal(labor.get("hours"), BigDecimal.ZERO);
                BigDecimal rate = getBigDecimal(labor.get("ratePerHour"), BigDecimal.ZERO);
                laborTotal = laborTotal.add(hours.multiply(rate));
            }
        }

        // Calculate discount for premium membership
        BigDecimal discount = BigDecimal.ZERO;
        String membershipStatus = getStringValue(data, "membershipStatus", "Standard");
        if (membershipStatus.equalsIgnoreCase("Premium")) {
            discount = laborTotal.multiply(new BigDecimal("0.20"));
        }

        // Calculate subtotal, tax, and grand total
        BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        BigDecimal grandTotal = subtotal.add(tax);

        // Store calculated values
        data.put("materialsTotal", materialsTotal);
        data.put("laborTotal", laborTotal);
        data.put("discount", discount);
        data.put("subtotal", subtotal);
        data.put("tax", tax);
        data.put("gst", tax);
        data.put("total", grandTotal);
        data.put("totalCost", grandTotal);
        data.put("totalAmount", grandTotal);
    }

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

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return defaultValue;
    }

    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            try {
                if (map.get(key) instanceof Integer) {
                    return (Integer) map.get(key);
                } else if (map.get(key) instanceof Number) {
                    return ((Number) map.get(key)).intValue();
                } else {
                    return Integer.parseInt(String.valueOf(map.get(key)));
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private BigDecimal getBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return new BigDecimal(((Number) value).toString());
            } else {
                return new BigDecimal(String.valueOf(value));
            }
        } catch (NumberFormatException | ArithmeticException e) {
            return defaultValue;
        }
    }
}