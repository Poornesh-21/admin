package com.albany.restapi.controller;

import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service-materials")
@RequiredArgsConstructor
@Slf4j
public class ServiceMaterialController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;

    /**
     * Get materials used for a service request
     */
    @GetMapping("/service-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<MaterialItemDTO>> getMaterialsForService(@PathVariable Integer requestId) {
        log.info("Getting materials for service request ID: {}", requestId);
        
        try {
            // Get materials used from repository
            List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(requestId);
            
            log.debug("Found {} materials for service request ID: {}", materials.size(), requestId);
            
            if (materials.isEmpty()) {
                log.debug("No materials found in MaterialUsage, checking ServiceTracking");
                
                // If no materials in materials_used table, try to get from service_tracking
                List<ServiceTracking> trackingWithMaterials = serviceTrackingRepository.findByRequestIdWithMaterialCosts(requestId);
                
                if (!trackingWithMaterials.isEmpty()) {
                    log.debug("Found {} tracking entries with material costs", trackingWithMaterials.size());
                    
                    // Create material items from tracking entries
                    List<MaterialItemDTO> materialsFromTracking = new ArrayList<>();
                    
                    for (ServiceTracking tracking : trackingWithMaterials) {
                        // Only use entries with material costs
                        if (tracking.getTotalMaterialCost() != null && tracking.getTotalMaterialCost().compareTo(BigDecimal.ZERO) > 0) {
                            MaterialItemDTO materialItem = MaterialItemDTO.builder()
                                .name("Service Materials")
                                .description(tracking.getWorkDescription() != null ? 
                                            tracking.getWorkDescription() : "Materials used for service")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(tracking.getTotalMaterialCost())
                                .total(tracking.getTotalMaterialCost())
                                .build();
                                
                            materialsFromTracking.add(materialItem);
                        }
                    }
                    
                    return ResponseEntity.ok(materialsFromTracking);
                }
                
                // If still no materials, return empty list
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            // Convert to DTOs
            List<MaterialItemDTO> materialItems = materials.stream()
                .filter(m -> m.getInventoryItem() != null)
                .map(this::convertToMaterialItemDTO)
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(materialItems);
            
        } catch (Exception e) {
            log.error("Error getting materials for service request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get labor charges for a service request
     */
    @GetMapping("/labor/service-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<LaborChargeDTO>> getLaborChargesForService(@PathVariable Integer requestId) {
        log.info("Getting labor charges for service request ID: {}", requestId);
        
        try {
            // Get service tracking entries for labor charges
            List<ServiceTracking> trackingEntries = serviceTrackingRepository.findByRequestIdAndLaborCostNotNull(requestId);
            
            log.debug("Found {} tracking entries with labor costs for service request ID: {}", 
                    trackingEntries.size(), requestId);
            
            if (trackingEntries.isEmpty()) {
                // Try to get total labor cost for the service request
                BigDecimal totalLaborCost = serviceTrackingRepository.findTotalLaborCostByRequestId(requestId)
                        .orElse(BigDecimal.ZERO);
                
                if (totalLaborCost.compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("No labor charges in tracking entries, but found total labor cost: {}", totalLaborCost);
                    
                    // Get service request for additional info
                    ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId).orElse(null);
                    
                    String laborDescription = "Service Labor";
                    if (serviceRequest != null && serviceRequest.getServiceType() != null) {
                        laborDescription = serviceRequest.getServiceType() + " Labor";
                    }
                    
                    // Create a default labor charge
                    LaborChargeDTO defaultLabor = LaborChargeDTO.builder()
                        .description(laborDescription)
                        .hours(new BigDecimal("2.0")) // Assume 2 hours as default
                        .ratePerHour(totalLaborCost.divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP))
                        .total(totalLaborCost)
                        .build();
                        
                    return ResponseEntity.ok(List.of(defaultLabor));
                }
                
                // If still no labor charges, return empty list
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            // Convert tracking entries to labor charges
            List<LaborChargeDTO> laborCharges = trackingEntries.stream()
                .map(this::convertToLaborChargeDTO)
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(laborCharges);
            
        } catch (Exception e) {
            log.error("Error getting labor charges for service request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get combined materials and labor total for a service
     */
    @GetMapping("/totals/service-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<Map<String, Object>> getServiceTotals(@PathVariable Integer requestId) {
        log.info("Getting service totals for service request ID: {}", requestId);
        
        try {
            // Get service request for membership status
            ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Service request not found"));
            
            // Get materials total
            BigDecimal materialsTotal = BigDecimal.ZERO;
            List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(requestId);
            
            if (!materials.isEmpty()) {
                for (MaterialUsage material : materials) {
                    if (material.getInventoryItem() != null) {
                        BigDecimal quantity = material.getQuantity() != null ? material.getQuantity() : BigDecimal.ONE;
                        BigDecimal unitPrice = material.getInventoryItem().getUnitPrice() != null ? 
                                            material.getInventoryItem().getUnitPrice() : BigDecimal.ZERO;
                        
                        materialsTotal = materialsTotal.add(quantity.multiply(unitPrice));
                    }
                }
            } else {
                // Check service tracking for material costs
                List<ServiceTracking> trackingWithMaterials = serviceTrackingRepository.findByRequestIdWithMaterialCosts(requestId);
                
                for (ServiceTracking tracking : trackingWithMaterials) {
                    if (tracking.getTotalMaterialCost() != null) {
                        materialsTotal = materialsTotal.add(tracking.getTotalMaterialCost());
                    }
                }
            }
            
            // Get labor total
            BigDecimal laborTotal = serviceTrackingRepository.findTotalLaborCostByRequestId(requestId)
                    .orElse(BigDecimal.ZERO);
            
            // Calculate discount if premium customer
            BigDecimal discount = BigDecimal.ZERO;
            boolean isPremium = false;
            
            if (serviceRequest.getVehicle() != null && 
                serviceRequest.getVehicle().getCustomer() != null) {
                
                String membershipStatus = serviceRequest.getVehicle().getCustomer().getMembershipStatus();
                if ("Premium".equalsIgnoreCase(membershipStatus)) {
                    // 20% discount on labor for premium members
                    discount = laborTotal.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
                    isPremium = true;
                }
            }
            
            // Calculate subtotal and tax
            BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
            BigDecimal tax = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal grandTotal = subtotal.add(tax);
            
            // Create response map
            Map<String, Object> totals = new HashMap<>();
            totals.put("materialsTotal", materialsTotal);
            totals.put("laborTotal", laborTotal);
            totals.put("discount", discount);
            totals.put("isPremium", isPremium);
            totals.put("subtotal", subtotal);
            totals.put("tax", tax);
            totals.put("grandTotal", grandTotal);
            
            return ResponseEntity.ok(totals);
            
        } catch (Exception e) {
            log.error("Error getting service totals for request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convert material usage to DTO
     */
    private MaterialItemDTO convertToMaterialItemDTO(MaterialUsage materialUsage) {
        InventoryItem item = materialUsage.getInventoryItem();
        BigDecimal quantity = materialUsage.getQuantity() != null ? materialUsage.getQuantity() : BigDecimal.ONE;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal total = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        return MaterialItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .description(item.getCategory()) // Use category as description
                .quantity(quantity)
                .unitPrice(unitPrice)
                .total(total)
                .build();
    }

    /**
     * Convert service tracking to labor charge DTO
     */
    private LaborChargeDTO convertToLaborChargeDTO(ServiceTracking tracking) {
        // Calculate hours from minutes
        BigDecimal hours = BigDecimal.ZERO;
        BigDecimal ratePerHour = BigDecimal.ZERO;

        if (tracking.getLaborMinutes() != null && tracking.getLaborMinutes() > 0) {
            hours = new BigDecimal(tracking.getLaborMinutes())
                    .divide(new BigDecimal(60), 1, RoundingMode.HALF_UP);

            // Calculate hourly rate
            if (tracking.getLaborCost() != null) {
                ratePerHour = tracking.getLaborCost()
                        .multiply(new BigDecimal(60))
                        .divide(new BigDecimal(tracking.getLaborMinutes()), 2, RoundingMode.HALF_UP);
            }
        }

        // Extract description
        String description = tracking.getWorkDescription();
        if (description != null && description.startsWith("Labor:")) {
            description = description.substring(6).trim();
        } else if (description == null || description.trim().isEmpty()) {
            description = "Service Labor";
        }

        return LaborChargeDTO.builder()
                .description(description)
                .hours(hours)
                .ratePerHour(ratePerHour)
                .total(tracking.getLaborCost() != null ? tracking.getLaborCost() : BigDecimal.ZERO)
                .build();
    }
}