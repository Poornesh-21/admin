package com.albany.mvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for completed service information
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompletedServiceDTO {
    private Integer serviceId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private String customerEmail;
    private String membershipStatus;
    private String serviceType;
    private String additionalDescription;
    private String status;
    private String category;
    private String vehicleBrand;
    private String vehicleModel;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate completedDate;
    
    private String serviceAdvisorName;
    private Integer serviceAdvisorId;
    
    // Financial details
    private BigDecimal materialsTotal;
    private BigDecimal laborTotal;
    private BigDecimal discount;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal totalCost;
    
    // Materials and labor
    private List<MaterialItemDTO> materials;
    private List<LaborChargeDTO> laborCharges;
    
    // Invoice and payment status
    private boolean hasBill;
    private boolean isPaid;
    private boolean hasInvoice;
    private boolean isDelivered;
    
    // Invoice details
    private Integer invoiceId;
    private LocalDate invoiceDate;
    
    // Additional info
    private String notes;
}