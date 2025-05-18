package com.albany.mvc.controller.Admin;

import com.albany.mvc.dto.ServiceAdvisorDto;
import com.albany.mvc.service.ServiceAdvisorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/service-advisors")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorController extends AdminBaseController {

    private final ServiceAdvisorService serviceAdvisorService;

    @GetMapping
    public String serviceAdvisorsPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        addCommonAttributes(model);
        return "admin/serviceAdvisor";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDto>> getAllServiceAdvisors(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<ServiceAdvisorDto> serviceAdvisors = serviceAdvisorService.getAllServiceAdvisors(validToken);
            return ResponseEntity.ok(serviceAdvisors != null ? serviceAdvisors : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @GetMapping("/api/advisors")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDto>> getServiceAdvisorsJson(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        return getAllServiceAdvisors(token, authHeader, request);
    }

    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDto>> getAllServiceAdvisorsAlternate(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        return getAllServiceAdvisors(token, authHeader, request);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ServiceAdvisorDto> getServiceAdvisor(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceAdvisorDto advisor = serviceAdvisorService.getServiceAdvisorById(id, validToken);
            if (advisor == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(advisor);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> createServiceAdvisor(
            @RequestBody ServiceAdvisorDto advisorDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            if (advisorDto.getPassword() == null || advisorDto.getPassword().isEmpty()) {
                advisorDto.setPassword(generateRandomPassword());
            }

            ServiceAdvisorDto createdAdvisor = serviceAdvisorService.createServiceAdvisor(advisorDto, validToken);
            if (createdAdvisor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to create service advisor"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("advisor", createdAdvisor);
            response.put("tempPassword", advisorDto.getPassword());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ServiceAdvisorDto> updateServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody ServiceAdvisorDto advisorDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceAdvisorDto updatedAdvisor = serviceAdvisorService.updateServiceAdvisor(id, advisorDto, validToken);
            if (updatedAdvisor == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(updatedAdvisor);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteServiceAdvisor(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            boolean deleted = serviceAdvisorService.deleteServiceAdvisor(id, validToken);
            if (!deleted) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private String generateRandomPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String numbers = "123456789";

        StringBuilder password = new StringBuilder("SA2025-");

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