package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDetailsDTO {
    // Invoice details
    private Integer invoiceId;
    private String invoiceNumber;
    private Integer requestId;
    private BigDecimal totalAmount;
    private BigDecimal taxes;
    private BigDecimal netAmount;
    private LocalDateTime invoiceDate;
    
    // Service request details
    private String serviceType;
    private LocalDate deliveryDate;
    private String status;
    private String additionalDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Vehicle details
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleName;
    private String registrationNumber;
    private String vehicleCategory;
    private Integer vehicleYear;
    
    // Customer details
    private Integer customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private String membershipStatus;
    private boolean isPremiumMember;
    
    // Service advisor details
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    
    // Materials and labor details
    private List<MaterialItemDTO> materials;
    private List<LaborChargeDTO> laborCharges;
    
    // Financial details
    private BigDecimal materialsTotal;
    private BigDecimal laborTotal;
    private BigDecimal discount;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal grandTotal;
}