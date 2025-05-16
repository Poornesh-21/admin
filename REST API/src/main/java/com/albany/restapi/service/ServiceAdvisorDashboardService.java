package com.albany.restapi.service;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final ServiceAdvisorProfileRepository serviceAdvisorProfileRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final EmailService emailService;

    /**
     * Get vehicles assigned to a specific service advisor, excluding completed services
     */
    public List<VehicleInServiceDTO> getAssignedVehicles(String serviceAdvisorEmail) {
        // Find the service advisor user
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));

        // Get service advisor profile
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

        // Find all service requests assigned to this service advisor
        List<ServiceRequest> assignedRequests = serviceRequestRepository
                .findByServiceAdvisor_AdvisorId(advisor.getAdvisorId());

        // Filter out completed services
        assignedRequests = assignedRequests.stream()
                .filter(request -> request.getStatus() != ServiceRequest.Status.Completed)
                .collect(Collectors.toList());

        log.info("Found {} active service requests for advisor: {}", assignedRequests.size(), advisor.getAdvisorId());

        // Convert to DTOs
        return assignedRequests.stream()
                .map(this::mapToVehicleInServiceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed information about a service request
     */
    public ServiceDetailResponseDTO getServiceDetails(Integer requestId, String serviceAdvisorEmail) {
        // Find the service advisor user
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));

        // Get service advisor profile
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

        // Get service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));

        // Check if this service request is assigned to this service advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new RuntimeException("This service request is not assigned to you");
        }

        // Create response DTO
        ServiceDetailResponseDTO response = new ServiceDetailResponseDTO();

        // Basic service request info
        response.setRequestId(request.getRequestId());
        response.setServiceType(request.getServiceType());
        response.setDeliveryDate(request.getDeliveryDate());
        response.setAdditionalDescription(request.getAdditionalDescription());
        response.setServiceDescription(request.getServiceDescription());
        response.setStatus(request.getStatus().name());
        response.setVehicleId(request.getVehicle().getVehicleId());
        response.setVehicleBrand(request.getVehicle().getBrand());
        response.setVehicleModel(request.getVehicle().getModel());
        response.setRegistrationNumber(request.getVehicle().getRegistrationNumber());
        response.setVehicleType(request.getVehicle().getCategory() != null ?
                request.getVehicle().getCategory().name() : null);
        response.setVehicleYear(request.getVehicle().getYear());

        // Customer information
        CustomerProfile customer = request.getVehicle().getCustomer();
        User customerUser = customer.getUser();
        response.setCustomerId(customer.getCustomerId());
        response.setCustomerName(customerUser.getFirstName() + " " + customerUser.getLastName());
        response.setCustomerEmail(customerUser.getEmail());
        response.setCustomerPhone(customerUser.getPhoneNumber());
        response.setMembershipStatus(customer.getMembershipStatus());

        // Service advisor information
        response.setServiceAdvisorId(advisor.getAdvisorId());
        response.setServiceAdvisorName(user.getFirstName() + " " + user.getLastName());

        // Request dates
        response.setRequestDate(request.getCreatedAt().toLocalDate());

        // Get service history - for display purposes, we'll still show all history entries
        List<ServiceTracking> trackingHistory = serviceTrackingRepository.findByRequestId(requestId);
        response.setServiceHistory(trackingHistory.stream()
                .map(this::mapToServiceHistoryDTO)
                .collect(Collectors.toList()));

        // Get current bill data
        ServiceBillSummaryDTO currentBill = getCurrentBillSummary(requestId);
        response.setCurrentBill(currentBill);

        return response;
    }

    /**
     * Helper method to get or create a single tracking record for a service request
     * with improved logging and error handling
     */
    private ServiceTracking getOrCreateServiceTracking(Integer requestId, ServiceAdvisorProfile advisor, ServiceRequest.Status status) {
        // Try to find existing tracking record for this request
        List<ServiceTracking> existingTrackings = serviceTrackingRepository.findByRequestId(requestId);

        log.debug("Found {} existing tracking records for request {}",
                existingTrackings.size(), requestId);

        if (!existingTrackings.isEmpty()) {
            // Return the first tracking record (we'll always update this one)
            ServiceTracking tracking = existingTrackings.get(0);
            log.debug("Using existing tracking record: ID={}, labor cost={}, minutes={}",
                    tracking.getTrackingId(), tracking.getLaborCost(), tracking.getLaborMinutes());
            return tracking;
        } else {
            // Create a new tracking entry if none exists
            log.info("Creating new tracking record for request {}", requestId);
            ServiceTracking tracking = new ServiceTracking();
            tracking.setRequestId(requestId);
            tracking.setStatus(status);
            tracking.setServiceAdvisor(advisor);

            // IMPORTANT: Initialize with zero values instead of null
            tracking.setLaborCost(BigDecimal.ZERO);
            tracking.setLaborMinutes(0);
            tracking.setTotalMaterialCost(BigDecimal.ZERO);
            tracking.setWorkDescription("Service tracking initialized");

            // Save and log the new tracking record
            ServiceTracking savedTracking = serviceTrackingRepository.save(tracking);
            log.info("Created new tracking record: ID={}", savedTracking.getTrackingId());
            return savedTracking;
        }
    }

    /**
     * Helper method to get service advisor profile
     */
    private ServiceAdvisorProfile getServiceAdvisorProfile(String serviceAdvisorEmail) {
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));

        return serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));
    }

    /**
     * Method to save materials to a service request
     * Fixed: Uses safe string handling for work description
     */
    @Transactional
    public ServiceMaterialsDTO addMaterialsToServiceRequest(
            Integer requestId,
            ServiceMaterialsDTO materialsRequest,
            String serviceAdvisorEmail) {

        // Validate service request and service advisor
        ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

        // Get the service advisor profile
        ServiceAdvisorProfile advisor = getServiceAdvisorProfile(serviceAdvisorEmail);

        // Get existing tracking record FIRST to preserve labor cost data
        ServiceTracking tracking = getOrCreateServiceTracking(requestId, advisor, request.getStatus());

        // Store current labor cost and minutes so we don't lose them
        BigDecimal existingLaborCost = tracking.getLaborCost();
        Integer existingLaborMinutes = tracking.getLaborMinutes();

        // Log the current data for debugging
        log.debug("Current tracking data - ID: {}, labor cost: {}, labor minutes: {}",
                tracking.getTrackingId(), existingLaborCost, existingLaborMinutes);

        // Delete existing material usages if requested
        if (materialsRequest.isReplaceExisting()) {
            List<MaterialUsage> existingUsages = materialUsageRepository.findByServiceRequest_RequestId(requestId);
            log.info("Deleting {} existing material usages for request {}", existingUsages.size(), requestId);
            materialUsageRepository.deleteAll(existingUsages);
        }

        // Process each material in the request
        List<MaterialItemDTO> processedMaterials = new ArrayList<>();
        BigDecimal totalMaterialsCost = BigDecimal.ZERO;

        for (MaterialItemDTO materialItem : materialsRequest.getItems()) {
            InventoryItem inventoryItem = inventoryItemRepository.findById(materialItem.getItemId())
                    .orElseThrow(() -> new RuntimeException("Inventory item not found: " + materialItem.getItemId()));

            // Check if there's enough stock
            if (inventoryItem.getCurrentStock().compareTo(materialItem.getQuantity()) < 0) {
                throw new RuntimeException(
                        "Not enough stock for item: " + inventoryItem.getName() +
                                ". Available: " + inventoryItem.getCurrentStock() +
                                ", Requested: " + materialItem.getQuantity());
            }

            // Create material usage record
            MaterialUsage usage = new MaterialUsage();
            usage.setServiceRequest(request);
            usage.setInventoryItem(inventoryItem);
            usage.setQuantity(materialItem.getQuantity());
            materialUsageRepository.save(usage);

            // Update inventory stock
            inventoryItem.setCurrentStock(inventoryItem.getCurrentStock().subtract(materialItem.getQuantity()));
            inventoryItemRepository.save(inventoryItem);

            // Calculate total for this item
            BigDecimal itemTotal = materialItem.getQuantity().multiply(inventoryItem.getUnitPrice());
            totalMaterialsCost = totalMaterialsCost.add(itemTotal);

            // Add to processed materials list
            MaterialItemDTO processedItem = new MaterialItemDTO();
            processedItem.setItemId(inventoryItem.getItemId());
            processedItem.setName(inventoryItem.getName());
            processedItem.setQuantity(materialItem.getQuantity());
            processedItem.setUnitPrice(inventoryItem.getUnitPrice());
            processedItem.setTotal(itemTotal);
            processedMaterials.add(processedItem);

            log.debug("Added material: {} x {} = {}",
                    inventoryItem.getName(), materialItem.getQuantity(), itemTotal);
        }

        // Update the tracking record with new material cost while preserving labor information
        if (!processedMaterials.isEmpty()) {
            // Update the material cost
            tracking.setTotalMaterialCost(totalMaterialsCost);

            // IMPORTANT: Make sure we don't lose the labor information
            tracking.setLaborCost(existingLaborCost);
            tracking.setLaborMinutes(existingLaborMinutes);

            // Update work description (safely with truncation handling)
            String materialsDesc = "Added " + processedMaterials.size() + " material items";

            // If there are only a few items, include their names
            if (processedMaterials.size() <= 3) {
                materialsDesc = "Added materials: " +
                        processedMaterials.stream()
                                .map(MaterialItemDTO::getName)
                                .collect(Collectors.joining(", "));
            }

            // Safely update work description
            String currentDesc = tracking.getWorkDescription();
            String newDesc = safelyUpdateWorkDescription(currentDesc, materialsDesc);
            tracking.setWorkDescription(newDesc);

            serviceTrackingRepository.save(tracking);

            log.info("Updated tracking record for materials: ID={}, total cost={}",
                    tracking.getTrackingId(), totalMaterialsCost);
        }

        // Prepare response
        ServiceMaterialsDTO response = new ServiceMaterialsDTO();
        response.setItems(processedMaterials);
        response.setTotalMaterialsCost(totalMaterialsCost);
        response.setCurrentBill(getCurrentBillSummary(requestId));

        return response;
    }

    /**
     * Fixed method to add labor charges to a service request
     */
    @Transactional
    public ServiceBillSummaryDTO addLaborCharges(
            Integer requestId,
            List<LaborChargeDTO> laborCharges,
            String serviceAdvisorEmail) {

        try {
            // Log incoming request for debugging
            log.info("Processing labor charges for request {}: {} charges",
                    requestId, laborCharges != null ? laborCharges.size() : 0);

            if (laborCharges == null || laborCharges.isEmpty()) {
                log.warn("No labor charges provided for request {}", requestId);
                return getCurrentBillSummary(requestId);
            }

            // Validate service request and service advisor
            ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

            // Get service advisor profile
            ServiceAdvisorProfile advisor = getServiceAdvisorProfile(serviceAdvisorEmail);

            // Calculate total labor cost and minutes
            BigDecimal totalLaborCost = BigDecimal.ZERO;
            int totalMinutes = 0;

            for (LaborChargeDTO chargeDTO : laborCharges) {
                // Detailed logging to troubleshoot
                log.debug("Processing labor charge: description={}, hours={}, rate={}",
                        chargeDTO.getDescription(), chargeDTO.getHours(), chargeDTO.getRatePerHour());

                // Validate the labor charge data with explicit null checks
                if (chargeDTO.getHours() == null || chargeDTO.getHours().compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Invalid hours value in labor charge: {}", chargeDTO.getHours());
                    continue; // Skip this charge
                }

                if (chargeDTO.getRatePerHour() == null || chargeDTO.getRatePerHour().compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Invalid rate value in labor charge: {}", chargeDTO.getRatePerHour());
                    continue; // Skip this charge
                }

                // Calculate total cost and minutes
                BigDecimal totalCost = chargeDTO.getHours().multiply(chargeDTO.getRatePerHour());
                totalLaborCost = totalLaborCost.add(totalCost);

                // Convert hours to minutes
                int minutes = chargeDTO.getHours().multiply(new BigDecimal("60")).intValue();
                totalMinutes += minutes;

                log.debug("Calculated cost for labor charge: {}h * {}₹/h = {}₹, {} minutes",
                        chargeDTO.getHours(), chargeDTO.getRatePerHour(), totalCost, minutes);
            }

            // Handle case where all charges were invalid
            if (totalLaborCost.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("No valid labor charges found for request {}", requestId);
                return getCurrentBillSummary(requestId);
            }

            // IMPORTANT: Get or create the tracking record
            ServiceTracking tracking = getOrCreateServiceTracking(requestId, advisor, request.getStatus());

            // Log before update for debugging
            log.info("Before update - tracking ID: {}, labor cost: {}, labor minutes: {}",
                    tracking.getTrackingId(), tracking.getLaborCost(), tracking.getLaborMinutes());

            // Update labor information with explicit setting of values
            tracking.setLaborCost(totalLaborCost);
            tracking.setLaborMinutes(totalMinutes);

            // Don't lose existing material cost data
            if (tracking.getTotalMaterialCost() == null) {
                tracking.setTotalMaterialCost(BigDecimal.ZERO);
            }

            // Set a simplified work description
            String laborDetails = "Added labor: " + totalMinutes + " minutes @ " +
                    totalLaborCost.divide(new BigDecimal(Math.max(1, totalMinutes)), 2, RoundingMode.HALF_UP) + "₹/min";

            // Safely update work description
            String currentDesc = tracking.getWorkDescription();
            String newDesc = safelyUpdateWorkDescription(currentDesc, laborDetails);
            tracking.setWorkDescription(newDesc);

            // Save the changes
            tracking = serviceTrackingRepository.save(tracking);

            // Log after save to confirm changes were persisted
            log.info("Successfully saved labor charges for request {}: tracking ID={}, minutes={}, cost={}",
                    requestId, tracking.getTrackingId(), tracking.getLaborMinutes(), tracking.getLaborCost());

            // Return updated bill summary
            return getCurrentBillSummary(requestId);

        } catch (Exception e) {
            // Log the full exception for debugging
            log.error("Error adding labor charges for request {}: {}", requestId, e.getMessage(), e);
            throw e; // Re-throw so it's handled by the controller
        }
    }

    /**
     * Helper method to safely update work description without exceeding database field size
     */
    private String safelyUpdateWorkDescription(String currentDesc, String newInfo) {
        final int MAX_LENGTH = 250; // Buffer from 255 limit

        if (newInfo == null || newInfo.isEmpty()) {
            return currentDesc;
        }

        String separator = (currentDesc == null || currentDesc.isEmpty()) ? "" : "; ";
        String combined = (currentDesc == null ? "" : currentDesc) + separator + newInfo;

        if (combined.length() <= MAX_LENGTH) {
            return combined;
        } else {
            // Strategy: Keep beginning of old + end of new
            int oldLength = Math.min(100, currentDesc == null ? 0 : currentDesc.length());
            String oldPart = currentDesc == null ? "" :
                    (oldLength < currentDesc.length() ?
                            currentDesc.substring(0, oldLength) + "..." :
                            currentDesc);

            int remainingSpace = MAX_LENGTH - oldPart.length() - separator.length();
            String newPart = newInfo.length() <= remainingSpace ?
                    newInfo :
                    "..." + newInfo.substring(newInfo.length() - remainingSpace + 3);

            return oldPart + separator + newPart;
        }
    }

    /**
     * Update service request status
     * Fixed: Uses safe string handling for work description
     */
    @Transactional
    public Map<String, Object> updateServiceStatus(
            Integer requestId,
            ServiceRequest.Status newStatus,
            String notes,
            boolean notifyCustomer,
            String serviceAdvisorEmail) {

        // Validate service request and service advisor
        ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

        // Get service advisor profile
        ServiceAdvisorProfile advisor = getServiceAdvisorProfile(serviceAdvisorEmail);

        // Update service request status
        ServiceRequest.Status oldStatus = request.getStatus();
        request.setStatus(newStatus);
        serviceRequestRepository.save(request);

        // Get or create the single tracking record and update it
        ServiceTracking tracking = getOrCreateServiceTracking(requestId, advisor, newStatus);

        // Update status in tracking
        tracking.setStatus(newStatus);

        // Create a short status update note
        String statusUpdateNote = "Status: " + oldStatus + " → " + newStatus;
        if (notes != null && !notes.isEmpty()) {
            // Truncate notes if they're too long (to be safe)
            if (notes.length() > 50) {
                notes = notes.substring(0, 47) + "...";
            }
            statusUpdateNote += ": " + notes;
        }

        // Safely update work description
        String currentDesc = tracking.getWorkDescription();
        String newDesc = safelyUpdateWorkDescription(currentDesc, statusUpdateNote);
        tracking.setWorkDescription(newDesc);

        serviceTrackingRepository.save(tracking);

        log.info("Updated service request {} status from {} to {}",
                requestId, oldStatus, newStatus);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("oldStatus", oldStatus.name());
        response.put("newStatus", newStatus.name());
        response.put("timestamp", LocalDateTime.now());
        response.put("updatedBy", advisor.getUser().getFirstName() + " " + advisor.getUser().getLastName());

        // Send notification to customer if requested
        if (notifyCustomer) {
            // Get customer email
            String customerEmail = request.getVehicle().getCustomer().getUser().getEmail();
            String customerName = request.getVehicle().getCustomer().getUser().getFirstName() + " "
                    + request.getVehicle().getCustomer().getUser().getLastName();

            // Send email notification
            try {
                sendStatusUpdateEmail(
                        customerEmail,
                        customerName,
                        request.getVehicle().getBrand() + " " + request.getVehicle().getModel(),
                        request.getVehicle().getRegistrationNumber(),
                        newStatus.name(),
                        notes
                );
                response.put("notificationSent", true);
            } catch (Exception e) {
                log.error("Failed to send status update notification: {}", e.getMessage());
                response.put("notificationSent", false);
                response.put("notificationError", e.getMessage());
            }
        } else {
            response.put("notificationSent", false);
        }

        return response;
    }

    /**
     * Get inventory item details
     */
    public InventoryItemDTO getInventoryItemDetails(Integer itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found: " + itemId));

        return InventoryItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .category(item.getCategory())
                .currentStock(item.getCurrentStock())
                .unitPrice(item.getUnitPrice())
                .reorderLevel(item.getReorderLevel())
                .stockStatus(getStockStatus(item.getCurrentStock(), item.getReorderLevel()))
                .totalValue(item.getCurrentStock().multiply(item.getUnitPrice()))
                .build();
    }

    /**
     * Helper method to determine stock status
     */
    private String getStockStatus(BigDecimal currentStock, BigDecimal reorderLevel) {
        if (currentStock.compareTo(reorderLevel) <= 0) {
            return "Low";
        } else if (currentStock.compareTo(reorderLevel.multiply(new BigDecimal("2"))) <= 0) {
            return "Medium";
        } else {
            return "Good";
        }
    }

    /**
     * Generate bill for a service
     * Fixed: Uses safe string handling for work description
     */
    @Transactional
    public BillResponseDTO generateServiceBill(
            Integer requestId,
            BillRequestDTO billRequest,
            String serviceAdvisorEmail) {

        // Validate service request and service advisor
        ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

        // Get service advisor profile
        ServiceAdvisorProfile advisor = getServiceAdvisorProfile(serviceAdvisorEmail);

        // Create bill response
        BillResponseDTO response = new BillResponseDTO();
        String billId = generateBillId(); // Generate unique bill ID as a String
        response.setBillId(billId);
        response.setRequestId(requestId);
        response.setVehicleName(request.getVehicle().getBrand() + " " + request.getVehicle().getModel());
        response.setRegistrationNumber(request.getVehicle().getRegistrationNumber());
        response.setCustomerName(request.getVehicle().getCustomer().getUser().getFirstName() + " "
                + request.getVehicle().getCustomer().getUser().getLastName());
        response.setCustomerEmail(request.getVehicle().getCustomer().getUser().getEmail());

        // Set financial details
        response.setMaterialsTotal(billRequest.getMaterialsTotal());
        response.setLaborTotal(billRequest.getLaborTotal());
        response.setSubtotal(billRequest.getSubtotal());
        response.setGst(billRequest.getGst());
        response.setGrandTotal(billRequest.getGrandTotal());
        response.setNotes(billRequest.getNotes());

        // Set generation timestamp
        response.setGeneratedAt(LocalDateTime.now());

        // Send email if requested
        if (billRequest.isSendEmail()) {
            try {
                sendBillEmail(
                        response.getCustomerEmail(),
                        response.getCustomerName(),
                        response.getVehicleName(),
                        response.getRegistrationNumber(),
                        billId, // Use the String billId
                        response.getGrandTotal(),
                        request.getServiceType()
                );
                response.setEmailSent(true);
            } catch (Exception e) {
                log.error("Failed to send bill email: {}", e.getMessage());
                response.setEmailSent(false);
            }
        } else {
            response.setEmailSent(false);
        }

        // Create a download URL
        response.setDownloadUrl("/api/bills/" + response.getBillId() + "/download");

        // Get or create the tracking record and update it
        ServiceTracking tracking = getOrCreateServiceTracking(requestId, advisor, request.getStatus());

        // Make sure tracking record has the correct financial information
        // This ensures that both materials and labor are properly represented
        tracking.setLaborCost(billRequest.getLaborTotal());
        tracking.setTotalMaterialCost(billRequest.getMaterialsTotal());

        // Calculate labor minutes if we have labor cost but no minutes
        if (billRequest.getLaborTotal().compareTo(BigDecimal.ZERO) > 0 &&
                (tracking.getLaborMinutes() == null || tracking.getLaborMinutes() == 0)) {
            // Estimate minutes based on standard rate of ₹65 per hour
            BigDecimal hourlyRate = new BigDecimal("65");
            BigDecimal hours = billRequest.getLaborTotal().divide(hourlyRate, 2, RoundingMode.HALF_UP);
            int minutes = hours.multiply(new BigDecimal("60")).intValue();
            tracking.setLaborMinutes(minutes);
        }

        // Update work description with bill generation info
        String billNote = "Generated service bill: " + response.getBillId();

        // Safely update work description
        String currentDesc = tracking.getWorkDescription();
        String newDesc = safelyUpdateWorkDescription(currentDesc, billNote);
        tracking.setWorkDescription(newDesc);

        serviceTrackingRepository.save(tracking);

        log.info("Generated bill {} for service request {}", billId, requestId);

        return response;
    }

    /**
     * Helper method to get the current bill summary for a service request
     * Fixed: Added more detailed logging to diagnose labor cost issues
     */
    private ServiceBillSummaryDTO getCurrentBillSummary(Integer requestId) {
        ServiceBillSummaryDTO bill = new ServiceBillSummaryDTO();
        bill.setRequestId(requestId);

        log.debug("Generating bill summary for request ID: {}", requestId);

        // Get all material usages for this request
        List<MaterialUsage> materialUsages = materialUsageRepository.findByServiceRequest_RequestId(requestId);
        log.debug("Found {} material usages", materialUsages.size());

        // Calculate parts subtotal and prepare items for response
        BigDecimal partsSubtotal = BigDecimal.ZERO;
        List<MaterialItemDTO> materials = new ArrayList<>();

        for (MaterialUsage usage : materialUsages) {
            InventoryItem item = usage.getInventoryItem();
            BigDecimal itemTotal = usage.getQuantity().multiply(item.getUnitPrice());
            partsSubtotal = partsSubtotal.add(itemTotal);

            MaterialItemDTO materialItem = new MaterialItemDTO();
            materialItem.setItemId(item.getItemId());
            materialItem.setName(item.getName());
            materialItem.setQuantity(usage.getQuantity());
            materialItem.setUnitPrice(item.getUnitPrice());
            materialItem.setTotal(itemTotal);

            materials.add(materialItem);

            log.debug("Material: {} x {} = {}", item.getName(), usage.getQuantity(), itemTotal);
        }

        // Get the single tracking entry (if exists)
        List<ServiceTracking> trackingEntries = serviceTrackingRepository.findByRequestId(requestId);
        log.debug("Found {} tracking entries", trackingEntries.size());

        // Set default values
        BigDecimal laborSubtotal = BigDecimal.ZERO;
        List<LaborChargeDTO> laborChargeDTOs = new ArrayList<>();

        // Get labor information from tracking if available
        if (!trackingEntries.isEmpty()) {
            ServiceTracking tracking = trackingEntries.get(0);

            if (tracking != null && tracking.getLaborCost() != null && tracking.getLaborCost().compareTo(BigDecimal.ZERO) > 0) {
                log.info("Found labor cost: {} for request: {}", tracking.getLaborCost(), requestId);

                BigDecimal hours = BigDecimal.ZERO;
                if (tracking.getLaborMinutes() != null && tracking.getLaborMinutes() > 0) {
                    hours = new BigDecimal(tracking.getLaborMinutes())
                            .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
                } else {
                    log.warn("Labor cost exists but minutes is null or zero for tracking ID: {}", tracking.getTrackingId());
                }

                BigDecimal ratePerHour = BigDecimal.ZERO;
                if (tracking.getLaborMinutes() != null && tracking.getLaborMinutes() > 0
                        && tracking.getLaborCost() != null) {
                    ratePerHour = tracking.getLaborCost()
                            .multiply(new BigDecimal("60"))
                            .divide(new BigDecimal(tracking.getLaborMinutes()), 2, RoundingMode.HALF_UP);
                } else {
                    // Default hourly rate if we can't calculate
                    ratePerHour = new BigDecimal("65");
                    log.warn("Using default hourly rate: {}", ratePerHour);
                }

                LaborChargeDTO laborChargeDTO = new LaborChargeDTO();
                laborChargeDTO.setDescription("Labor Charge");
                laborChargeDTO.setHours(hours);
                laborChargeDTO.setRatePerHour(ratePerHour);
                laborChargeDTO.setTotal(tracking.getLaborCost());

                laborChargeDTOs.add(laborChargeDTO);
                laborSubtotal = tracking.getLaborCost();

                log.debug("Added labor charge: {}h @ {}₹/h = {}₹",
                        hours, ratePerHour, tracking.getLaborCost());
            } else {
                log.info("No labor cost found for request: {}", requestId);
                if (tracking != null) {
                    log.debug("Tracking details - ID: {}, labor cost: {}, minutes: {}",
                            tracking.getTrackingId(),
                            tracking.getLaborCost(),
                            tracking.getLaborMinutes());
                }
            }

            // Get notes from tracking entry
            if (tracking != null) {
                bill.setNotes(tracking.getWorkDescription());
            }
        } else {
            log.info("No tracking records found for request: {}", requestId);
        }

        // Calculate subtotal, tax and total
        BigDecimal subtotal = partsSubtotal.add(laborSubtotal);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP); // 7% tax
        BigDecimal total = subtotal.add(tax);

        log.debug("Bill summary: materials={}, labor={}, subtotal={}, tax={}, total={}",
                partsSubtotal, laborSubtotal, subtotal, tax, total);

        // Populate bill summary
        bill.setMaterials(materials);
        bill.setLaborCharges(laborChargeDTOs);
        bill.setPartsSubtotal(partsSubtotal);
        bill.setLaborSubtotal(laborSubtotal);
        bill.setSubtotal(subtotal);
        bill.setTax(tax);
        bill.setTotal(total);

        return bill;
    }

    /**
     * Helper method to validate a service request access by a service advisor
     */
    private ServiceRequest validateServiceRequestAccess(Integer requestId, String serviceAdvisorEmail) {
        // Find the service advisor user
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));

        // Get service advisor profile
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

        // Get service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));

        // Check if this service request is assigned to this service advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new RuntimeException("This service request is not assigned to you");
        }

        return request;
    }

    /**
     * Helper method to map a service request to VehicleInServiceDTO
     */
    private VehicleInServiceDTO mapToVehicleInServiceDTO(ServiceRequest request) {
        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();

        // Get service advisor name if assigned
        String serviceAdvisorName = "Not Assigned";
        String serviceAdvisorId = "N/A";

        if (request.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = request.getServiceAdvisor();
            User advisorUser = advisor.getUser();
            serviceAdvisorName = advisorUser.getFirstName() + " " + advisorUser.getLastName();
            serviceAdvisorId = advisor.getFormattedId();
        }

        // Calculate estimated completion date if not set
        LocalDate estimatedCompletionDate = request.getDeliveryDate();
        if (estimatedCompletionDate == null) {
            // Simple estimation: 3 days from created date
            estimatedCompletionDate = request.getCreatedAt().toLocalDate().plusDays(3);
        }

        return VehicleInServiceDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .serviceAdvisorName(serviceAdvisorName)
                .serviceAdvisorId(serviceAdvisorId)
                .status(request.getStatus().name())
                .startDate(request.getCreatedAt().toLocalDate())
                .estimatedCompletionDate(estimatedCompletionDate)
                .category(vehicle.getCategory().name())
                .customerName(customerUser.getFirstName() + " " + customerUser.getLastName())
                .customerEmail(customerUser.getEmail())
                .membershipStatus(customer.getMembershipStatus())
                .serviceType(request.getServiceType())
                .additionalDescription(request.getAdditionalDescription())
                .build();
    }

    /**
     * Helper method to map ServiceTracking to ServiceHistoryDTO
     */
    private ServiceHistoryDTO mapToServiceHistoryDTO(ServiceTracking tracking) {
        ServiceHistoryDTO historyDTO = new ServiceHistoryDTO();
        historyDTO.setTrackingId(tracking.getTrackingId());
        historyDTO.setStatus(tracking.getStatus().name());
        historyDTO.setWorkDescription(tracking.getWorkDescription());
        historyDTO.setTimestamp(tracking.getUpdatedAt());

        // Set updated by if service advisor is present
        if (tracking.getServiceAdvisor() != null && tracking.getServiceAdvisor().getUser() != null) {
            User advisorUser = tracking.getServiceAdvisor().getUser();
            historyDTO.setUpdatedBy(advisorUser.getFirstName() + " " + advisorUser.getLastName());
        } else {
            historyDTO.setUpdatedBy("System");
        }

        return historyDTO;
    }

    /**
     * Helper method to generate a unique bill ID
     */
    private String generateBillId() {
        return "BILL-" + System.currentTimeMillis() % 1000000;
    }

    /**
     * Send status update email to customer
     */
    private void sendStatusUpdateEmail(
            String customerEmail,
            String customerName,
            String vehicleName,
            String registration,
            String newStatus,
            String notes) {

        String subject = "Vehicle Service Status Update - " + registration;

        StringBuilder message = new StringBuilder();
        message.append("Dear ").append(customerName).append(",\n\n");
        message.append("The status of your vehicle service has been updated:\n\n");
        message.append("Vehicle: ").append(vehicleName).append(" (").append(registration).append(")\n");
        message.append("New Status: ").append(newStatus).append("\n\n");

        if (notes != null && !notes.isEmpty()) {
            message.append("Service Notes: ").append(notes).append("\n\n");
        }

        message.append("For more details or any questions, please contact Albany Service.\n\n");
        message.append("Thank you for choosing Albany Service.\n\n");
        message.append("Best regards,\n");
        message.append("Albany Service Team");

        emailService.sendSimpleEmail(customerEmail, subject, message.toString());
    }

    /**
     * Send bill email to customer
     */
    private void sendBillEmail(
            String customerEmail,
            String customerName,
            String vehicleName,
            String registration,
            String billId,
            BigDecimal totalAmount,
            String serviceType) {

        String subject = "Service Bill for " + registration + " - " + billId;

        StringBuilder message = new StringBuilder();
        message.append("Dear ").append(customerName).append(",\n\n");
        message.append("Your service bill is now ready for your vehicle:\n\n");
        message.append("Vehicle: ").append(vehicleName).append(" (").append(registration).append(")\n");
        message.append("Service Type: ").append(serviceType).append("\n");
        message.append("Bill Reference: ").append(billId).append("\n");
        message.append("Total Amount: $").append(totalAmount.setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        message.append("Please find your detailed bill in the attachment or by logging into your account.\n\n");
        message.append("Thank you for choosing Albany Service.\n\n");
        message.append("Best regards,\n");
        message.append("Albany Service Team");

        emailService.sendSimpleEmail(customerEmail, subject, message.toString());
    }
}