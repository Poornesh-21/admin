package com.albany.mvc.controller.Admin;

import com.albany.mvc.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController extends AdminBaseController {

    private final CustomerService customerService;

    @GetMapping
    public String customersPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        try {
            List<Map<String, Object>> customers = customerService.getAllCustomers(validToken);
            model.addAttribute("customers", customers);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load customers: " + e.getMessage());
        }

        addCommonAttributes(model);
        return "admin/customers";
    }

    @GetMapping("/api")
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Map<String, Object>> customers = customerService.getAllCustomers(validToken);
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> customer = customerService.getCustomerById(id, validToken);
            if (customer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    @PutMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> updatedCustomer = customerService.updateCustomer(id, customerData, validToken);
            if (updatedCustomer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedCustomer);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean deleted = customerService.deleteCustomer(id, validToken);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Failed to delete customer"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createCustomer(
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!customerData.containsKey("password") ||
                customerData.get("password") == null ||
                customerData.get("password").toString().isEmpty()) {
            customerData.put("password", generateTempPassword());
        }

        try {
            Map<String, Object> createdCustomer = customerService.createCustomer(customerData, validToken);
            return ResponseEntity.ok(createdCustomer);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    private String generateTempPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String numbers = "123456789";

        StringBuilder password = new StringBuilder("CUS2025-");

        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * letters.length());
            password.append(letters.charAt(index));
        }

        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * numbers.length());
            password.append(numbers.charAt(index));
        }

        return password.toString();
    }
}