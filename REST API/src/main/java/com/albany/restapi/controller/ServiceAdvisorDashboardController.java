package com.albany.restapi.controller;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.InventoryService;
import com.albany.restapi.service.ServiceAdvisorDashboardService;
import com.albany.restapi.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/serviceAdvisor/dashboard")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardController {

    private final ServiceAdvisorDashboardService dashboardService;
    private final InventoryService inventoryService;
    private final ServiceRequestService serviceRequestService;

    /**
     * Get all vehicles assigned to the authenticated service advisor
     */
    @GetMapping("/assigned-vehicles")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<VehicleInServiceDTO>> getAssignedVehicles(Authentication authentication) {
        log.info("Fetching assigned vehicles for service advisor: {}", authentication.getName());
        List<VehicleInServiceDTO> assignedVehicles = dashboardService.getAssignedVehicles(authentication.getName());
        return ResponseEntity.ok(assignedVehicles);
    }

    /**
     * Get details for a specific service request
     */
    @GetMapping("/service-details/{requestId}")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceDetailResponseDTO> getServiceDetails(
            @PathVariable Integer requestId,
            Authentication authentication) {
        
        log.info("Fetching service details for request ID: {} by service advisor: {}", 
                requestId, authentication.getName());
                
        ServiceDetailResponseDTO details = dashboardService.getServiceDetails(requestId, authentication.getName());
        return ResponseEntity.ok(details);
    }

    /**
     * Get available inventory items
     */
    @GetMapping("/inventory-items")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getInventoryItems() {
        log.info("Fetching inventory items for service advisor dashboard");
        List<InventoryItemDTO> items = inventoryService.getAllInventoryItems();
        return ResponseEntity.ok(items);
    }

    /**
     * Add inventory items to a service request
     */
    @PostMapping("/service/{requestId}/inventory-items")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceMaterialsDTO> addInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody ServiceMaterialsDTO materialsRequest,
            Authentication authentication) {
        
        log.info("Adding inventory items to service request ID: {} by service advisor: {}", 
                requestId, authentication.getName());
                
        ServiceMaterialsDTO response = dashboardService.addMaterialsToServiceRequest(
                requestId, materialsRequest, authentication.getName());
                
        return ResponseEntity.ok(response);
    }

    /**
     * Add labor charges to a service request
     */
    @PostMapping("/service/{requestId}/labor-charges")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceBillSummaryDTO> addLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<LaborChargeDTO> laborCharges,
            Authentication authentication) {
        
        log.info("Adding labor charges to service request ID: {} by service advisor: {}", 
                requestId, authentication.getName());
                
        ServiceBillSummaryDTO response = dashboardService.addLaborCharges(
                requestId, laborCharges, authentication.getName());
                
        return ResponseEntity.ok(response);
    }

    /**
     * Update service request status
     */
    @PutMapping("/service/{requestId}/status")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<Map<String, Object>> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate,
            Authentication authentication) {
        
        String status = statusUpdate.get("status");
        String notes = statusUpdate.get("notes");
        Boolean notifyCustomer = Boolean.parseBoolean(statusUpdate.getOrDefault("notifyCustomer", "false"));
        
        log.info("Updating status for service request ID: {} to {} by service advisor: {}", 
                requestId, status, authentication.getName());
                
        ServiceRequest.Status newStatus;
        try {
            newStatus = ServiceRequest.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid status value: " + status,
                "validStatuses", ServiceRequest.Status.values()
            ));
        }
        
        Map<String, Object> result = dashboardService.updateServiceStatus(
                requestId, newStatus, notes, notifyCustomer, authentication.getName());
                
        return ResponseEntity.ok(result);
    }

    /**
     * Generate bill for a service
     */
    @PostMapping("/service/{requestId}/generate-bill")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<BillResponseDTO> generateBill(
            @PathVariable Integer requestId,
            @RequestBody BillRequestDTO billRequest,
            Authentication authentication) {
        
        log.info("Generating bill for service request ID: {} by service advisor: {}", 
                requestId, authentication.getName());
                
        BillResponseDTO response = dashboardService.generateServiceBill(
                requestId, billRequest, authentication.getName());
                
        return ResponseEntity.ok(response);
    }
}