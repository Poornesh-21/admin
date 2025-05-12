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

        // Get service history
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
     * Add materials to a service request
     */
    @Transactional
    public ServiceMaterialsDTO addMaterialsToServiceRequest(
            Integer requestId,
            ServiceMaterialsDTO materialsRequest,
            String serviceAdvisorEmail) {

        // Validate service request and service advisor
        ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

        // Get service advisor profile
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

        // Delete existing material usages if requested
        if (materialsRequest.isReplaceExisting()) {
            List<MaterialUsage> existingUsages = materialUsageRepository.findByServiceRequest_RequestId(requestId);
            materialUsageRepository.deleteAll(existingUsages);
        }

        // Process each new material
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
        }

        // Create service tracking entry for materials added
        ServiceTracking tracking = new ServiceTracking();
        tracking.setRequestId(requestId);
        tracking.setWorkDescription("Added parts and materials to service");
        tracking.setStatus(request.getStatus());
        tracking.setTotalMaterialCost(totalMaterialsCost);
        tracking.setServiceAdvisor(advisor);
        serviceTrackingRepository.save(tracking);

        // Prepare response
        ServiceMaterialsDTO response = new ServiceMaterialsDTO();
        response.setItems(processedMaterials);
        response.setTotalMaterialsCost(totalMaterialsCost);
        response.setCurrentBill(getCurrentBillSummary(requestId));

        return response;
    }

    /**
     * Add labor charges to a service request - using ServiceTracking
     */
    @Transactional
    public ServiceBillSummaryDTO addLaborCharges(
            Integer requestId,
            List<LaborChargeDTO> laborCharges,
            String serviceAdvisorEmail) {

        // Validate service request and service advisor
        ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

        // Get service advisor profile
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

        // Delete existing labor charges (service tracking entries with laborCost > 0)
        List<ServiceTracking> existingLaborEntries = serviceTrackingRepository.findByRequestIdAndLaborCostNotNull(requestId);
        serviceTrackingRepository.deleteAll(existingLaborEntries);

        // Process each labor charge
        BigDecimal totalLaborCost = BigDecimal.ZERO;

        for (LaborChargeDTO chargeDTO : laborCharges) {
            // Calculate total cost and minutes
            BigDecimal totalCost = chargeDTO.getHours().multiply(chargeDTO.getRatePerHour());
            totalLaborCost = totalLaborCost.add(totalCost);

            // Convert hours to minutes (1 hour = 60 minutes)
            int minutes = chargeDTO.getHours().multiply(new BigDecimal("60")).intValue();

            // Create a service tracking entry for this labor charge
            ServiceTracking laborTracking = new ServiceTracking();
            laborTracking.setRequestId(requestId);
            laborTracking.setWorkDescription("Labor: " + chargeDTO.getDescription());
            laborTracking.setStatus(request.getStatus());
            laborTracking.setLaborCost(totalCost);
            laborTracking.setLaborMinutes(minutes);
            laborTracking.setServiceAdvisor(advisor);

            serviceTrackingRepository.save(laborTracking);
        }

        // Create a summary tracking entry
        if (!laborCharges.isEmpty()) {
            ServiceTracking summaryTracking = new ServiceTracking();
            summaryTracking.setRequestId(requestId);
            summaryTracking.setWorkDescription("Added labor charges to service");
            summaryTracking.setStatus(request.getStatus());
            summaryTracking.setLaborCost(totalLaborCost);
            summaryTracking.setServiceAdvisor(advisor);
            serviceTrackingRepository.save(summaryTracking);
        }

        // Return updated bill summary
        return getCurrentBillSummary(requestId);
    }

    /**
     * Update service request status
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
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

        // Update service request status
        ServiceRequest.Status oldStatus = request.getStatus();
        request.setStatus(newStatus);
        serviceRequestRepository.save(request);

        // Create service tracking entry
        ServiceTracking tracking = new ServiceTracking();
        tracking.setRequestId(requestId);
        tracking.setWorkDescription(notes != null && !notes.isEmpty() ?
                notes : "Status updated from " + oldStatus + " to " + newStatus);
        tracking.setStatus(newStatus);
        tracking.setServiceAdvisor(advisor);
        serviceTrackingRepository.save(tracking);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("oldStatus", oldStatus.name());
        response.put("newStatus", newStatus.name());
        response.put("timestamp", LocalDateTime.now());
        response.put("updatedBy", user.getFirstName() + " " + user.getLastName());

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
     * Generate bill for a service
     */
    @Transactional
    public BillResponseDTO generateServiceBill(
            Integer requestId,
            BillRequestDTO billRequest,
            String serviceAdvisorEmail) {

        // Validate service request and service advisor
        ServiceRequest request = validateServiceRequestAccess(requestId, serviceAdvisorEmail);

        // Get service advisor profile
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " + serviceAdvisorEmail));
        ServiceAdvisorProfile advisor = serviceAdvisorProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + user.getUserId()));

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

        // Create service tracking entry for bill generation
        ServiceTracking tracking = new ServiceTracking();
        tracking.setRequestId(requestId);
        tracking.setWorkDescription("Generated service bill: " + response.getBillId());
        tracking.setStatus(request.getStatus());
        tracking.setLaborCost(billRequest.getLaborTotal());
        tracking.setTotalMaterialCost(billRequest.getMaterialsTotal());
        tracking.setServiceAdvisor(advisor);
        serviceTrackingRepository.save(tracking);

        return response;
    }

    /**
     * Helper method to get the current bill summary for a service request
     */
    private ServiceBillSummaryDTO getCurrentBillSummary(Integer requestId) {
        ServiceBillSummaryDTO bill = new ServiceBillSummaryDTO();
        bill.setRequestId(requestId);

        // Get all material usages for this request
        List<MaterialUsage> materialUsages = materialUsageRepository.findByServiceRequest_RequestId(requestId);

        // Calculate parts subtotal and prepare items for response
        BigDecimal partsSubtotal = BigDecimal.ZERO;
        List<MaterialItemDTO> materials = new ArrayList<>();

        for (MaterialUsage usage : materialUsages) {
            InventoryItem item = usage.getInventoryItem();
            BigDecimal itemTotal = usage.getQuantity().multiply(item.getUnitPrice());
            partsSubtotal = partsSubtotal.add(itemTotal);

            MaterialItemDTO materialItem = new MaterialItemDTO();
            materialItem.setItemId(item.getItemId()); // Fixed: Using Integer itemId
            materialItem.setName(item.getName());
            materialItem.setQuantity(usage.getQuantity());
            materialItem.setUnitPrice(item.getUnitPrice());
            materialItem.setTotal(itemTotal);

            materials.add(materialItem);
        }

        // Get all labor charges from service tracking
        List<ServiceTracking> laborEntries = serviceTrackingRepository.findByRequestIdAndLaborCostNotNull(requestId);

        // Calculate labor subtotal and prepare charges for response
        BigDecimal laborSubtotal = BigDecimal.ZERO;
        List<LaborChargeDTO> laborChargeDTOs = new ArrayList<>();

        // Filter entries - only include those that have a description starting with "Labor:"
        for (ServiceTracking tracking : laborEntries) {
            if (tracking.getWorkDescription() != null &&
                    tracking.getWorkDescription().startsWith("Labor:") &&
                    tracking.getLaborCost() != null) {

                BigDecimal hours = BigDecimal.ZERO;
                if (tracking.getLaborMinutes() != null) {
                    hours = new BigDecimal(tracking.getLaborMinutes()).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
                }

                BigDecimal ratePerHour = BigDecimal.ZERO;
                if (tracking.getLaborMinutes() != null && tracking.getLaborMinutes() > 0 && tracking.getLaborCost() != null) {
                    ratePerHour = tracking.getLaborCost()
                            .multiply(new BigDecimal("60"))
                            .divide(new BigDecimal(tracking.getLaborMinutes()), 2, RoundingMode.HALF_UP);
                }

                LaborChargeDTO laborChargeDTO = new LaborChargeDTO();
                laborChargeDTO.setDescription(tracking.getWorkDescription().substring(7).trim()); // Remove "Labor: " prefix
                laborChargeDTO.setHours(hours);
                laborChargeDTO.setRatePerHour(ratePerHour);
                laborChargeDTO.setTotal(tracking.getLaborCost());

                laborChargeDTOs.add(laborChargeDTO);
                laborSubtotal = laborSubtotal.add(tracking.getLaborCost());
            }
        }

        // Calculate subtotal, tax and total
        BigDecimal subtotal = partsSubtotal.add(laborSubtotal);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP); // 7% tax
        BigDecimal total = subtotal.add(tax);

        // Populate bill summary
        bill.setMaterials(materials);
        bill.setLaborCharges(laborChargeDTOs);
        bill.setPartsSubtotal(partsSubtotal);
        bill.setLaborSubtotal(laborSubtotal);
        bill.setSubtotal(subtotal);
        bill.setTax(tax);
        bill.setTotal(total);

        // Get notes if any from tracking entries but not labor entries
        List<ServiceTracking> trackingEntries = serviceTrackingRepository.findByRequestId(requestId);
        if (!trackingEntries.isEmpty()) {
            Optional<ServiceTracking> latestNonLaborEntry = trackingEntries.stream()
                    .filter(t -> t.getWorkDescription() == null || !t.getWorkDescription().startsWith("Labor:"))
                    .max(Comparator.comparing(ServiceTracking::getUpdatedAt));

            if (latestNonLaborEntry.isPresent()) {
                bill.setNotes(latestNonLaborEntry.get().getWorkDescription());
            }
        }

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
        // Changed from returning Integer to String
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
            String billId, // Changed from Integer to String
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