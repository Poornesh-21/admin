package com.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {
    private Long id;
    private String vehicleType;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleRegistration;
    private String serviceDescription;
    private List<String> requestedServices;
    private LocalDateTime preferredDate;
    private String status;
    private LocalDateTime createdAt;
}