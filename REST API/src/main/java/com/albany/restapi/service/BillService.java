package com.albany.restapi.service;

import com.albany.restapi.dto.BillRequestDTO;
import com.albany.restapi.dto.BillResponseDTO;
import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final EmailService emailService;

    /**
     * Generate a bill for a service request
     */
    @Transactional
    public BillResponseDTO generateBill(Integer requestId, BillRequestDTO billRequest) {
        // Get service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));

        // Create bill response
        BillResponseDTO response = new BillResponseDTO();

        // Convert requestId to String for the billId
        String billId = "BILL-" + String.valueOf(requestId); // Convert Integer to String

        response.setBillId(billId);
        response.setRequestId(requestId);
        response.setVehicleName(request.getVehicle().getBrand() + " " + request.getVehicle().getModel());
        response.setRegistrationNumber(request.getVehicle().getRegistrationNumber());
        response.setCustomerName(request.getVehicle().getCustomer().getUser().getFirstName() + " "
                + request.getVehicle().getCustomer().getUser().getLastName());
        response.setCustomerEmail(request.getVehicle().getCustomer().getUser().getEmail());

        // Add financial details
        calculateBillTotals(billRequest, response);

        // Set additional details
        response.setNotes(billRequest.getNotes());
        response.setGeneratedAt(LocalDateTime.now());

        // Handle email sending
        if (billRequest.isSendEmail()) {
            try {
                sendBillEmail(response);
                response.setEmailSent(true);
            } catch (Exception e) {
                log.error("Failed to send bill email: {}", e.getMessage());
                response.setEmailSent(false);
            }
        }

        // Create download URL
        response.setDownloadUrl("/api/bills/" + billId + "/download"); // Use String billId

        return response;
    }

    /**
     * Get a bill for a service request
     */
    public BillResponseDTO getBillByServiceRequest(Integer requestId) {
        // For demonstration, generate a bill on-the-fly
        // In a real system, you would fetch from a database

        BillRequestDTO dummyRequest = createDummyBillRequest(requestId);
        return generateBill(requestId, dummyRequest);
    }

    /**
     * Generate PDF content for a bill
     */
    public byte[] generateBillPdf(Integer requestId) {
        // Get or generate the bill
        BillResponseDTO bill = getBillByServiceRequest(requestId);

        // Generate PDF (this is a placeholder - in a real system you'd use a PDF library)
        // For example, you might use iText, Flying Saucer, or Apache PDFBox

        // Convert requestId to String for the sample PDF content
        String requestIdStr = String.valueOf(requestId); // Convert int to String

        // Return dummy PDF content
        String pdfContent = "PDF Bill Content for Request: " + requestIdStr + "\n" +
                "Customer: " + bill.getCustomerName() + "\n" +
                "Vehicle: " + bill.getVehicleName() + " (" + bill.getRegistrationNumber() + ")\n" +
                "Total Amount: $" + bill.getGrandTotal();

        return pdfContent.getBytes();
    }

    /**
     * Helper method to calculate bill totals
     */
    private void calculateBillTotals(BillRequestDTO request, BillResponseDTO response) {
        // Set provided totals
        response.setMaterialsTotal(request.getMaterialsTotal());
        response.setLaborTotal(request.getLaborTotal());
        response.setSubtotal(request.getSubtotal());
        response.setGst(request.getGst());
        response.setGrandTotal(request.getGrandTotal());
    }

    /**
     * Helper method to send bill email
     */
    private void sendBillEmail(BillResponseDTO bill) {
        String subject = "Service Bill for " + bill.getRegistrationNumber() + " - " + bill.getBillId();

        StringBuilder message = new StringBuilder();
        message.append("Dear ").append(bill.getCustomerName()).append(",\n\n");
        message.append("Your service bill is now ready for your vehicle:\n\n");
        message.append("Vehicle: ").append(bill.getVehicleName()).append(" (").append(bill.getRegistrationNumber()).append(")\n");
        message.append("Bill Reference: ").append(bill.getBillId()).append("\n");
        message.append("Total Amount: $").append(bill.getGrandTotal().setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        message.append("Please find your detailed bill in the attachment or by logging into your account.\n\n");
        message.append("Thank you for choosing Albany Service.\n\n");
        message.append("Best regards,\n");
        message.append("Albany Service Team");

        emailService.sendSimpleEmail(bill.getCustomerEmail(), subject, message.toString());
    }

    /**
     * Helper method to create a dummy bill request for demonstration
     */
    private BillRequestDTO createDummyBillRequest(Integer requestId) {
        // Get service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));

        // Get material usages
        List<MaterialUsage> materialUsages = materialUsageRepository.findByServiceRequest_RequestId(requestId);
        List<MaterialItemDTO> materials = new ArrayList<>();
        BigDecimal materialsTotal = BigDecimal.ZERO;

        for (MaterialUsage usage : materialUsages) {
            InventoryItem item = usage.getInventoryItem();
            BigDecimal itemTotal = usage.getQuantity().multiply(item.getUnitPrice());
            materialsTotal = materialsTotal.add(itemTotal);

            MaterialItemDTO materialItem = new MaterialItemDTO();
            materialItem.setItemId(item.getItemId());
            materialItem.setName(item.getName());
            materialItem.setQuantity(usage.getQuantity());
            materialItem.setUnitPrice(item.getUnitPrice());
            materialItem.setTotal(itemTotal);

            materials.add(materialItem);
        }

        // Get labor charges
        List<ServiceTracking> laborEntries = serviceTrackingRepository.findByRequestIdAndLaborCostNotNull(requestId);
        List<LaborChargeDTO> laborCharges = new ArrayList<>();
        BigDecimal laborTotal = BigDecimal.ZERO;

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
                laborChargeDTO.setDescription(tracking.getWorkDescription().substring(7).trim());
                laborChargeDTO.setHours(hours);
                laborChargeDTO.setRatePerHour(ratePerHour);
                laborChargeDTO.setTotal(tracking.getLaborCost());

                laborCharges.add(laborChargeDTO);
                laborTotal = laborTotal.add(tracking.getLaborCost());
            }
        }

        // Calculate totals
        BigDecimal subtotal = materialsTotal.add(laborTotal);
        BigDecimal gst = subtotal.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(gst);

        // Get notes if any
        Optional<ServiceTracking> latestTracking = serviceTrackingRepository.findByRequestId(requestId).stream()
                .max((a, b) -> a.getUpdatedAt().compareTo(b.getUpdatedAt()));
        String notes = latestTracking.map(ServiceTracking::getWorkDescription).orElse("");

        // Create bill request
        BillRequestDTO billRequest = new BillRequestDTO();
        billRequest.setMaterials(materials);
        billRequest.setLaborCharges(laborCharges);
        billRequest.setMaterialsTotal(materialsTotal);
        billRequest.setLaborTotal(laborTotal);
        billRequest.setSubtotal(subtotal);
        billRequest.setGst(gst);
        billRequest.setGrandTotal(grandTotal);
        billRequest.setNotes(notes);
        billRequest.setSendEmail(false);

        return billRequest;
    }
}