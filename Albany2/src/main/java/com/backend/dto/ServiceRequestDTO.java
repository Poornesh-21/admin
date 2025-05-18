package com.backend.dto;

import com.backend.model.ServiceRequest.ServiceRequestStatus;
import com.backend.model.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {
    private Long id; // Aligned with frontend
    private Long userId;
    private String vehicleType;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleRegistration;
    private String serviceDescription;
    private List<ServiceType> requestedServices;
    private LocalDateTime preferredDate;
    private String status; // Aligned with frontend
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long adminId;
    private Long serviceAdvisorId;
}