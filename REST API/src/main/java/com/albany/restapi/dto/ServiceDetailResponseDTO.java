package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceDetailResponseDTO {
    // Service Request Basic Info
    private Integer requestId;
    private String serviceType;
    private LocalDate deliveryDate;
    private String additionalDescription;
    private String serviceDescription;
    private String status;
    
    // Vehicle Info
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String vehicleType;
    private Integer vehicleYear;
    
    // Customer Info
    private Integer customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String membershipStatus;
    
    // Service Advisor Info
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    
    // Dates
    private LocalDate requestDate;
    
    // Service History
    private List<ServiceHistoryDTO> serviceHistory;
    
    // Current Bill
    private ServiceBillSummaryDTO currentBill;
}