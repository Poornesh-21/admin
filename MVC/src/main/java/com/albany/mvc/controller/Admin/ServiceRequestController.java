package com.albany.mvc.controller.Admin;

import com.albany.mvc.dto.ServiceRequestDto;
import com.albany.mvc.service.ServiceRequestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/service-requests")
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestController extends AdminBaseController {

    private final ServiceRequestService serviceRequestService;

    @GetMapping
    public String serviceRequestsPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        addCommonAttributes(model);
        return "admin/serviceRequests";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<ServiceRequestDto>> getAllServiceRequests(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<ServiceRequestDto> serviceRequests = serviceRequestService.getAllServiceRequests(validToken);
            return ResponseEntity.ok(serviceRequests);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createServiceRequest(
            @RequestBody ServiceRequestDto serviceRequestDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceRequestDto createdRequest = serviceRequestService.createServiceRequest(serviceRequestDto, validToken);
            return ResponseEntity.ok(createdRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create service request: " + e.getMessage()));
        }
    }

    @PutMapping("/api/{id}/assign")
    @ResponseBody
    public ResponseEntity<?> assignServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> requestBody,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Integer advisorId = requestBody.get("advisorId");
            if (advisorId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Advisor ID is required"));
            }

            ServiceRequestDto updatedRequest = serviceRequestService.assignServiceAdvisor(id, advisorId, validToken);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to assign service advisor: " + e.getMessage()));
        }
    }

    @PutMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateServiceStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> requestBody,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String status = requestBody.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }

            ServiceRequestDto updatedRequest = serviceRequestService.updateServiceRequestStatus(id, status, validToken);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update service status: " + e.getMessage()));
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getServiceRequestById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceRequestDto serviceRequest = serviceRequestService.getServiceRequestById(id, validToken);
            if (serviceRequest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(serviceRequest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch service request details: " + e.getMessage()));
        }
    }
}