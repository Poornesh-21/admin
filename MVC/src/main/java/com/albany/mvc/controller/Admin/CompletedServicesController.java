package com.albany.mvc.controller.Admin;

import com.albany.mvc.service.CompletedServicesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class CompletedServicesController extends AdminBaseController {

    private final CompletedServicesService completedServicesService;

    @GetMapping("/completed-services")
    public String completedServicesPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        
        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        addCommonAttributes(model);
        model.addAttribute("token", validToken);
        return "admin/completed_services";
    }

    @GetMapping("/api/completed-services")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllCompletedServices(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            List<Map<String, Object>> completedServices = completedServicesService.getCompletedServices(validToken);
            return ResponseEntity.ok(completedServices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/api/completed-services/{id}/invoice-details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServiceInvoiceDetails(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            Map<String, Object> serviceDetails = completedServicesService.getServiceDetails(id, validToken);

            if (!serviceDetails.isEmpty()) {
                // Add default materials if missing
                if (!serviceDetails.containsKey("materials") || serviceDetails.get("materials") == null) {
                    serviceDetails.put("materials", new ArrayList<>());
                    serviceDetails.put("materialsTotal", BigDecimal.ZERO);
                }

                // Add default labor charges if missing
                if (!serviceDetails.containsKey("laborCharges") || serviceDetails.get("laborCharges") == null) {
                    serviceDetails.put("laborCharges", new ArrayList<>());
                    serviceDetails.put("laborTotal", BigDecimal.ZERO);
                }

                // Calculate financial totals even if they're missing
                ensureFinancialTotals(serviceDetails);
            }

            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            Map<String, Object> fallbackData = createFallbackInvoiceData(id);
            return ResponseEntity.ok(fallbackData);
        }
    }

    @PostMapping("/api/completed-services/filter")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> filterCompletedServices(
            @RequestBody Map<String, Object> filterCriteria,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            List<Map<String, Object>> filteredServices = completedServicesService.filterCompletedServices(
                    filterCriteria, validToken);
            return ResponseEntity.ok(filteredServices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/api/completed-services/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCompletedServiceDetails(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            Map<String, Object> serviceDetails = completedServicesService.getServiceDetails(id, validToken);

            if (serviceDetails.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    @PostMapping("/api/completed-services/{id}/invoice")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> invoiceDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            Map<String, Object> result = completedServicesService.generateInvoice(id, invoiceDetails, validToken);
            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to generate invoice: " + e.getMessage()));
        }
    }

    @PostMapping("/api/completed-services/{id}/payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> paymentDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            Map<String, Object> result = completedServicesService.recordPayment(id, paymentDetails, validToken);
            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to record payment: " + e.getMessage()));
        }
    }

    @PostMapping("/api/completed-services/{id}/dispatch")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> dispatchVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> dispatchDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            Map<String, Object> result = completedServicesService.dispatchVehicle(id, dispatchDetails, validToken);
            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to dispatch vehicle: " + e.getMessage()));
        }
    }

    private Map<String, Object> createFallbackInvoiceData(Integer serviceId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("requestId", serviceId);
        fallback.put("serviceId", serviceId);
        fallback.put("vehicleName", "Unknown Vehicle");
        fallback.put("registrationNumber", "Unknown");
        fallback.put("customerName", "Unknown Customer");
        fallback.put("membershipStatus", "Standard");
        fallback.put("completedDate", LocalDate.now());
        fallback.put("materials", new ArrayList<>());
        fallback.put("laborCharges", new ArrayList<>());
        fallback.put("materialsTotal", BigDecimal.ZERO);
        fallback.put("laborTotal", BigDecimal.ZERO);
        fallback.put("subtotal", BigDecimal.ZERO);
        fallback.put("tax", BigDecimal.ZERO);
        fallback.put("grandTotal", BigDecimal.ZERO);
        return fallback;
    }

    private void ensureFinancialTotals(Map<String, Object> data) {
        // Get existing values or default to zero
        BigDecimal materialsTotal = getBigDecimalValue(data, "materialsTotal", BigDecimal.ZERO);
        BigDecimal laborTotal = getBigDecimalValue(data, "laborTotal", BigDecimal.ZERO);

        // Calculate discount if premium
        BigDecimal discount = BigDecimal.ZERO;
        String membershipStatus = getStringValue(data, "membershipStatus", "Standard");
        if ("Premium".equalsIgnoreCase(membershipStatus)) {
            discount = laborTotal.multiply(new BigDecimal("0.20"));
            data.put("discount", discount);
        }

        // Calculate subtotal
        BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
        data.put("subtotal", subtotal);

        // Calculate tax
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        data.put("tax", tax);

        // Calculate grand total
        BigDecimal grandTotal = subtotal.add(tax);
        data.put("grandTotal", grandTotal);

        // Also set standard fields that might be expected
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

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key, BigDecimal defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else {
                try {
                    return new BigDecimal(value.toString());
                } catch (Exception e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return defaultValue;
    }
}