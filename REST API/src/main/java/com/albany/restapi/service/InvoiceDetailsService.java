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
import java.util.Optional;
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

        // Get service request details first
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + serviceRequestId));

        // Get invoice for the service request if exists, otherwise create a temporary one for display
        Invoice invoice = invoiceRepository.findByRequestId(serviceRequestId)
                .orElse(Invoice.builder()
                        .requestId(serviceRequestId)
                        .build());

        // Create DTO
        InvoiceDetailsDTO dto = new InvoiceDetailsDTO();

        // Set basic invoice info
        dto.setInvoiceId(invoice.getInvoiceId());
        dto.setInvoiceNumber(invoice.getInvoiceId() != null ? "INV-" + invoice.getInvoiceId() : "DRAFT");
        dto.setRequestId(serviceRequestId);
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

        // Set materials used and labor charges
        setMaterialsDetails(dto, serviceRequest);
        setLaborCharges(dto, serviceRequest);

        // Calculate financials
        calculateFinancials(dto);

        // If invoice exists, ensure invoice values are used
        if (invoice.getInvoiceId() != null && invoice.getNetAmount() != null) {
            applyInvoiceValues(dto, invoice);
        }

        return dto;
    }

    /**
     * Set materials details in the DTO with enhanced error handling and logging
     */
    private void setMaterialsDetails(InvoiceDetailsDTO dto, ServiceRequest serviceRequest) {
        log.debug("Retrieving materials for service request ID: {}", serviceRequest.getRequestId());

        try {
            // Get materials used with explicit error handling
            List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(serviceRequest.getRequestId());

            log.debug("Found {} materials for service request ID: {}", materials.size(), serviceRequest.getRequestId());

            if (materials.isEmpty()) {
                // If we don't have materials, create a list and set empty values
                dto.setMaterials(new ArrayList<>());
                dto.setMaterialsTotal(BigDecimal.ZERO);
                return;
            }

            // Convert to DTOs with additional validation
            List<MaterialItemDTO> materialItems = materials.stream()
                    .filter(m -> m.getInventoryItem() != null)
                    .map(this::convertToMaterialItemDTO)
                    .collect(Collectors.toList());

            dto.setMaterials(materialItems);

            // Calculate materials total
            BigDecimal materialsTotal = BigDecimal.ZERO;
            for (MaterialUsage material : materials) {
                if (material.getInventoryItem() != null) {
                    BigDecimal quantity = material.getQuantity() != null ? material.getQuantity() : BigDecimal.ONE;
                    BigDecimal unitPrice = material.getInventoryItem().getUnitPrice() != null ?
                            material.getInventoryItem().getUnitPrice() : BigDecimal.ZERO;

                    materialsTotal = materialsTotal.add(quantity.multiply(unitPrice));
                }
            }

            dto.setMaterialsTotal(materialsTotal.setScale(2, RoundingMode.HALF_UP));

            log.debug("Calculated materials total: {}", materialsTotal);

        } catch (Exception e) {
            log.error("Error retrieving materials for service request ID {}: {}", serviceRequest.getRequestId(), e.getMessage(), e);

            // Ensure we always have at least empty lists
            dto.setMaterials(new ArrayList<>());
            dto.setMaterialsTotal(BigDecimal.ZERO);
        }
    }

    /**
     * Set labor charges in the DTO with enhanced error handling and fallbacks
     */
    private void setLaborCharges(InvoiceDetailsDTO dto, ServiceRequest serviceRequest) {
        log.debug("Retrieving labor charges for service request ID: {}", serviceRequest.getRequestId());

        try {
            // Get service tracking entries for labor charges
            List<ServiceTracking> trackingEntries = serviceTrackingRepository.findByRequestId(serviceRequest.getRequestId());

            log.debug("Found {} tracking entries for service request ID: {}", trackingEntries.size(), serviceRequest.getRequestId());

            if (trackingEntries.isEmpty()) {
                // If we don't have labor charges, create a list and set empty values
                dto.setLaborCharges(new ArrayList<>());
                dto.setLaborTotal(BigDecimal.ZERO);
                return;
            }

            // Filter and convert to DTOs (only entries with labor cost)
            List<LaborChargeDTO> laborCharges = trackingEntries.stream()
                    .filter(entry -> entry.getLaborCost() != null &&
                            entry.getLaborCost().compareTo(BigDecimal.ZERO) > 0)
                    .map(this::convertToLaborChargeDTO)
                    .collect(Collectors.toList());

            dto.setLaborCharges(laborCharges);

            // Calculate labor total
            BigDecimal laborTotal = serviceTrackingRepository.findTotalLaborCostByRequestId(serviceRequest.getRequestId())
                    .orElse(BigDecimal.ZERO);

            // If repository sum returns zero but we have entries, calculate manually as fallback
            if (laborTotal.compareTo(BigDecimal.ZERO) == 0 && !laborCharges.isEmpty()) {
                laborTotal = laborCharges.stream()
                        .map(LaborChargeDTO::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            dto.setLaborTotal(laborTotal.setScale(2, RoundingMode.HALF_UP));

            log.debug("Calculated labor total: {}", laborTotal);

        } catch (Exception e) {
            log.error("Error retrieving labor charges for service request ID {}: {}", serviceRequest.getRequestId(), e.getMessage(), e);

            // Ensure we always have at least empty lists
            dto.setLaborCharges(new ArrayList<>());
            dto.setLaborTotal(BigDecimal.ZERO);
        }
    }

    /**
     * Calculate financial details with better handling of null values
     */
    private void calculateFinancials(InvoiceDetailsDTO dto) {
        // Safe getters to handle null values
        BigDecimal laborTotal = dto.getLaborTotal() != null ? dto.getLaborTotal() : BigDecimal.ZERO;
        BigDecimal materialsTotal = dto.getMaterialsTotal() != null ? dto.getMaterialsTotal() : BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        // Apply premium discount if applicable
        if (dto.isPremiumMember()) {
            // 20% discount on labor for premium members
            discount = laborTotal.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            dto.setDiscount(discount);
            log.debug("Applied premium discount: {}", discount);
        } else {
            dto.setDiscount(BigDecimal.ZERO);
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

        log.debug("Financial calculation completed: materials={}, labor={}, discount={}, subtotal={}, tax={}, total={}",
                materialsTotal, laborTotal, discount, subtotal, tax, grandTotal);
    }

    /**
     * Apply official invoice values if they exist
     */
    private void applyInvoiceValues(InvoiceDetailsDTO dto, Invoice invoice) {
        log.debug("Applying official invoice values from invoice ID: {}", invoice.getInvoiceId());

        // Only override if the invoice has actual values
        if (invoice.getNetAmount() != null && invoice.getNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netAmount = invoice.getNetAmount();
            BigDecimal taxAmount = invoice.getTaxes() != null ? invoice.getTaxes() : BigDecimal.ZERO;
            BigDecimal subtotal = netAmount.subtract(taxAmount);

            // Only override our calculations if the invoice has different values
            if (Math.abs(netAmount.subtract(dto.getGrandTotal()).doubleValue()) > 0.01) {
                log.debug("Using invoice values instead of calculated values");
                dto.setGrandTotal(netAmount);
                dto.setTax(taxAmount);
                dto.setSubtotal(subtotal);

                // Attempt to distribute the subtotal between materials and labor
                // if our calculated values don't add up
                BigDecimal calculatedSubtotal = dto.getMaterialsTotal().add(dto.getLaborTotal()).subtract(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO);
                if (Math.abs(calculatedSubtotal.subtract(subtotal).doubleValue()) > 0.01) {
                    // Default split: 60% materials, 40% labor
                    BigDecimal materialsTotal = subtotal.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal laborTotal = subtotal.multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP);

                    dto.setMaterialsTotal(materialsTotal);
                    dto.setLaborTotal(laborTotal);

                    // Adjust materials and labor items if needed
                    adjustItemsToMatchTotals(dto);
                }
            }
        }
    }

    /**
     * Adjust individual line items to match totals
     */
    private void adjustItemsToMatchTotals(InvoiceDetailsDTO dto) {
        // If we have no materials but have a total, create a default material
        if ((dto.getMaterials() == null || dto.getMaterials().isEmpty()) &&
                dto.getMaterialsTotal().compareTo(BigDecimal.ZERO) > 0) {

            List<MaterialItemDTO> materials = new ArrayList<>();
            MaterialItemDTO defaultMaterial = MaterialItemDTO.builder()
                    .name("Service Materials")
                    .description("Standard materials for service")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(dto.getMaterialsTotal())
                    .total(dto.getMaterialsTotal())
                    .build();
            materials.add(defaultMaterial);
            dto.setMaterials(materials);
        }

        // If we have no labor but have a total, create a default labor charge
        if ((dto.getLaborCharges() == null || dto.getLaborCharges().isEmpty()) &&
                dto.getLaborTotal().compareTo(BigDecimal.ZERO) > 0) {

            List<LaborChargeDTO> laborCharges = new ArrayList<>();
            LaborChargeDTO defaultLabor = LaborChargeDTO.builder()
                    .description("Service labor")
                    .hours(new BigDecimal("2.0"))
                    .ratePerHour(dto.getLaborTotal().divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP))
                    .total(dto.getLaborTotal())
                    .build();
            laborCharges.add(defaultLabor);
            dto.setLaborCharges(laborCharges);
        }
    }

    /**
     * Convert material usage to DTO with additional validation
     */
    private MaterialItemDTO convertToMaterialItemDTO(MaterialUsage materialUsage) {
        if (materialUsage == null || materialUsage.getInventoryItem() == null) {
            log.warn("Attempted to convert null or invalid MaterialUsage to DTO");
            return MaterialItemDTO.builder()
                    .name("Unknown Item")
                    .description("Unknown")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(BigDecimal.ZERO)
                    .total(BigDecimal.ZERO)
                    .build();
        }

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
     * Convert service tracking to labor charge DTO with additional validation
     */
    private LaborChargeDTO convertToLaborChargeDTO(ServiceTracking tracking) {
        if (tracking == null) {
            log.warn("Attempted to convert null ServiceTracking to DTO");
            return LaborChargeDTO.builder()
                    .description("Unknown Labor")
                    .hours(BigDecimal.ZERO)
                    .ratePerHour(BigDecimal.ZERO)
                    .total(BigDecimal.ZERO)
                    .build();
        }

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

    /**
     * Format customer address
     */
    private String formatCustomerAddress(CustomerProfile customer) {
        if (customer == null) {
            return "";
        }

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