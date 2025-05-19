package com.albany.mvc.controller.serviceadvisor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/serviceAdvisor")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardController extends ServiceAdvisorBaseController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        addCommonAttributes(model, request);
        return "serviceAdvisor/dashboard";
    }

    @GetMapping("/api/assigned-vehicles")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAssignedVehicles(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/assigned-vehicles";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> vehicles = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return ResponseEntity.ok(vehicles);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/api/service-details/{requestId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServiceDetails(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service-details/" + requestId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> serviceDetails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(serviceDetails);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    @GetMapping("/api/inventory-items")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getInventoryItems(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/inventory-items";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> inventoryItems = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return ResponseEntity.ok(inventoryItems);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @PutMapping("/api/service/{requestId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusUpdate, headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/status";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    @PostMapping("/api/service/{requestId}/inventory-items")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> materialsRequest,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(materialsRequest, headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/inventory-items";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    @PostMapping("/api/service/{requestId}/labor-charges")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<Map<String, Object>> laborCharges,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(laborCharges, headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/labor-charges";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    @PostMapping("/api/service/{requestId}/generate-invoice")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> invoiceRequest,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (validToken.startsWith("Bearer ")) {
                headers.set("Authorization", validToken);
            } else {
                headers.setBearerAuth(validToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceRequest, headers);
            String url = apiBaseUrl + "/serviceAdvisor/dashboard/service/" + requestId + "/generate-invoice";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }
}