package com.albany.restapi.service;

import com.albany.restapi.dto.InvoiceDetailsDTO;
import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceDetailsService {

    private final InvoiceRepository invoiceRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Get detailed invoice information for a service request
     */
    public InvoiceDetailsDTO getInvoiceDetails(Integer serviceRequestId) {
        log.info("Retrieving invoice details for service request ID: {}", serviceRequestId);

        // Get invoice for the service request
        Invoice invoice = invoiceRepository.findByRequestId(serviceRequestId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for service request ID: " + serviceRequestId));

        // Get service request details
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + serviceRequestId));

        // Create DTO
        InvoiceDetailsDTO dto = new InvoiceDetailsDTO();

        // Set basic invoice info
        dto.setInvoiceId(invoice.getInvoiceId());
        dto.setInvoiceNumber("INV-" + invoice.getInvoiceId());
        dto.setRequestId(invoice.getRequestId());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setTaxes(invoice.getTaxes());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setInvoiceDate(invoice.getInvoiceDate());

        // Set service request info
        dto.setServiceType(serviceRequest.getServiceType());
        dto.setDeliveryDate(serviceRequest.getDeliveryDate());
        dto.setStatus(serviceRequest.getStatus().toString());
        dto.setAdditionalDescription(serviceRequest.getAdditionalDescription());
        dto.setCreatedAt(serviceRequest.getCreatedAt());
        dto.setUpdatedAt(serviceRequest.getUpdatedAt());

        // Set vehicle info
        if (serviceRequest.getVehicle() != null) {
            Vehicle vehicle = serviceRequest.getVehicle();
            dto.setVehicleId(vehicle.getVehicleId());
            dto.setVehicleBrand(vehicle.getBrand());
            dto.setVehicleModel(vehicle.getModel());
            dto.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());
            dto.setRegistrationNumber(vehicle.getRegistrationNumber());
            dto.setVehicleCategory(vehicle.getCategory().toString());
            dto.setVehicleYear(vehicle.getYear());

            // Set customer info
            if (vehicle.getCustomer() != null) {
                CustomerProfile customer = vehicle.getCustomer();
                dto.setCustomerId(customer.getCustomerId());

                if (customer.getUser() != null) {
                    User user = customer.getUser();
                    dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                    dto.setCustomerEmail(user.getEmail());
                    dto.setCustomerPhone(user.getPhoneNumber());
                    dto.setCustomerAddress(formatCustomerAddress(customer));
                }

                dto.setMembershipStatus(customer.getMembershipStatus());
                dto.setPremiumMember("Premium".equalsIgnoreCase(customer.getMembershipStatus()));
            }
        }

        // Set service advisor info
        if (serviceRequest.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = serviceRequest.getServiceAdvisor();
            dto.setServiceAdvisorId(advisor.getAdvisorId());
            if (advisor.getUser() != null) {
                dto.setServiceAdvisorName(advisor.getUser().getFirstName() + " " + advisor.getUser().getLastName());
            }
        }

        // Set materials used
        List<MaterialItemDTO> materialItems = setMaterialsDetails(dto, serviceRequest);

        // If there are no materials but we have a total amount, generate default entries
        if (materialItems.isEmpty() && invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            materialItems = generateDefaultMaterials(serviceRequest, invoice.getTotalAmount());
            dto.setMaterials(materialItems);

            // Recalculate materials total
            BigDecimal materialsTotal = materialItems.stream()
                    .map(MaterialItemDTO::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setMaterialsTotal(materialsTotal);
        }

        // Set labor charges
        List<LaborChargeDTO> laborCharges = setLaborCharges(dto, serviceRequest);

        // If there are no labor charges but we have a total amount, generate default entries
        if (laborCharges.isEmpty() && invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            laborCharges = generateDefaultLaborCharges(serviceRequest, invoice.getTotalAmount());
            dto.setLaborCharges(laborCharges);

            // Recalculate labor total
            BigDecimal laborTotal = laborCharges.stream()
                    .map(LaborChargeDTO::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setLaborTotal(laborTotal);
        }

        // Calculate financials
        calculateFinancials(dto);

        // Use invoice values if our calculations are zero
        if (dto.getGrandTotal().compareTo(BigDecimal.ZERO) == 0 && invoice.getNetAmount() != null) {
            // Split the invoice amount between materials and labor
            BigDecimal netAmount = invoice.getNetAmount();
            BigDecimal taxAmount = invoice.getTaxes() != null ? invoice.getTaxes() : BigDecimal.ZERO;
            BigDecimal subtotal = netAmount.subtract(taxAmount);

            // Allocate 60% to materials, 40% to labor as a default split
            BigDecimal materialsTotal = subtotal.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal laborTotal = subtotal.multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP);

            dto.setMaterialsTotal(materialsTotal);
            dto.setLaborTotal(laborTotal);
            dto.setSubtotal(subtotal);
            dto.setTax(taxAmount);
            dto.setGrandTotal(netAmount);

            // Update material items if using default
            if (materialItems.isEmpty()) {
                MaterialItemDTO defaultMaterial = MaterialItemDTO.builder()
                        .name("Service Materials")
                        .description("Standard materials for " + serviceRequest.getServiceType() + " service")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(materialsTotal)
                        .total(materialsTotal)
                        .build();
                dto.setMaterials(List.of(defaultMaterial));
            }

            // Update labor items if using default
            if (laborCharges.isEmpty()) {
                LaborChargeDTO defaultLabor = LaborChargeDTO.builder()
                        .description(serviceRequest.getServiceType() + " service labor")
                        .hours(new BigDecimal("2.0"))
                        .ratePerHour(laborTotal.divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP))
                        .total(laborTotal)
                        .build();
                dto.setLaborCharges(List.of(defaultLabor));
            }
        }

        return dto;
    }

    /**
     * Generate default materials for invoice display
     */
    private List<MaterialItemDTO> generateDefaultMaterials(ServiceRequest serviceRequest, BigDecimal totalAmount) {
        List<MaterialItemDTO> defaultMaterials = new ArrayList<>();

        // Estimate materials as 60% of total if no specific data
        BigDecimal materialsEstimate = totalAmount.multiply(new BigDecimal("0.6"))
                .divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP); // Remove GST component

        // Create a generic service materials entry
        MaterialItemDTO serviceItem = MaterialItemDTO.builder()
                .itemId(1)
                .name("Service Materials")
                .description("Standard materials for " + serviceRequest.getServiceType() + " service")
                .quantity(BigDecimal.ONE)
                .unitPrice(materialsEstimate)
                .total(materialsEstimate)
                .build();

        defaultMaterials.add(serviceItem);

        return defaultMaterials;
    }

    /**
     * Generate default labor charges for invoice display
     */
    private List<LaborChargeDTO> generateDefaultLaborCharges(ServiceRequest serviceRequest, BigDecimal totalAmount) {
        List<LaborChargeDTO> defaultCharges = new ArrayList<>();

        // Estimate labor as 40% of total if no specific data
        BigDecimal laborEstimate = totalAmount.multiply(new BigDecimal("0.4"))
                .divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP); // Remove GST component

        // Estimate 2 hours of work by default
        BigDecimal hours = new BigDecimal("2.0");
        BigDecimal ratePerHour = laborEstimate.divide(hours, 2, RoundingMode.HALF_UP);

        LaborChargeDTO laborCharge = LaborChargeDTO.builder()
                .description(serviceRequest.getServiceType() + " service labor")
                .hours(hours)
                .ratePerHour(ratePerHour)
                .total(laborEstimate)
                .build();

        defaultCharges.add(laborCharge);

        return defaultCharges;
    }

    /**
     * Set materials details in the DTO
     */
    private List<MaterialItemDTO> setMaterialsDetails(InvoiceDetailsDTO dto, ServiceRequest serviceRequest) {
        // Get materials used
        List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(serviceRequest.getRequestId());

        // Log materials count for debugging
        log.debug("Found {} materials for service request ID: {}", materials.size(), serviceRequest.getRequestId());

        // Convert to DTOs
        List<MaterialItemDTO> materialItems = materials.stream()
                .filter(m -> m.getInventoryItem() != null)
                .map(this::convertToMaterialItemDTO)
                .collect(Collectors.toList());

        dto.setMaterials(materialItems);

        // Calculate materials total - reimplement the logic instead of using private method
        BigDecimal materialsTotal = materials.stream()
                .filter(m -> m.getInventoryItem() != null)
                .map(m -> m.getInventoryItem().getUnitPrice().multiply(m.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setMaterialsTotal(materialsTotal);

        return materialItems;
    }

    /**
     * Set labor charges in the DTO
     */
    private List<LaborChargeDTO> setLaborCharges(InvoiceDetailsDTO dto, ServiceRequest serviceRequest) {
        // Get service tracking entries for labor charges
        List<ServiceTracking> trackingEntries = serviceTrackingRepository.findByRequestId(serviceRequest.getRequestId());

        // Log tracking entries count for debugging
        log.debug("Found {} tracking entries for service request ID: {}", trackingEntries.size(), serviceRequest.getRequestId());

        // Convert to DTOs (only entries with labor cost)
        List<LaborChargeDTO> laborCharges = trackingEntries.stream()
                .filter(entry -> entry.getLaborCost() != null && entry.getLaborCost().compareTo(BigDecimal.ZERO) > 0)
                .map(this::convertToLaborChargeDTO)
                .collect(Collectors.toList());

        dto.setLaborCharges(laborCharges);

        // Calculate labor total - reimplement the logic instead of using private method
        BigDecimal laborTotal = serviceTrackingRepository.findTotalLaborCostByRequestId(serviceRequest.getRequestId())
                .orElse(BigDecimal.ZERO);

        dto.setLaborTotal(laborTotal);

        return laborCharges;
    }

    /**
     * Calculate financial details
     */
    private void calculateFinancials(InvoiceDetailsDTO dto) {
        // Check for premium discount
        BigDecimal laborTotal = dto.getLaborTotal() != null ? dto.getLaborTotal() : BigDecimal.ZERO;
        BigDecimal materialsTotal = dto.getMaterialsTotal() != null ? dto.getMaterialsTotal() : BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        if (dto.isPremiumMember()) {
            // 20% discount on labor for premium members
            discount = laborTotal.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            dto.setDiscount(discount);
        }

        // Calculate subtotal
        BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
        dto.setSubtotal(subtotal);

        // Calculate tax (18% GST)
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        dto.setTax(tax);

        // Calculate grand total
        BigDecimal grandTotal = subtotal.add(tax);
        dto.setGrandTotal(grandTotal);
    }

    /**
     * Convert material usage to DTO
     */
    private MaterialItemDTO convertToMaterialItemDTO(MaterialUsage materialUsage) {
        InventoryItem item = materialUsage.getInventoryItem();
        BigDecimal quantity = materialUsage.getQuantity();
        BigDecimal unitPrice = item.getUnitPrice();
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

        // Extract description - if it starts with "Labor:", remove prefix
        String description = tracking.getWorkDescription();
        if (description != null && description.startsWith("Labor:")) {
            description = description.substring(6).trim();
        }

        return LaborChargeDTO.builder()
                .description(description)
                .hours(hours)
                .ratePerHour(ratePerHour)
                .total(tracking.getLaborCost())
                .build();
    }

    /**
     * Format customer address
     */
    private String formatCustomerAddress(CustomerProfile customer) {
        StringBuilder address = new StringBuilder();

        if (customer.getStreet() != null && !customer.getStreet().isEmpty()) {
            address.append(customer.getStreet());
        }

        if (customer.getCity() != null && !customer.getCity().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(customer.getCity());
        }

        if (customer.getState() != null && !customer.getState().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(customer.getState());
        }

        if (customer.getPostalCode() != null && !customer.getPostalCode().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(customer.getPostalCode());
        }

        return address.toString();
    }
}