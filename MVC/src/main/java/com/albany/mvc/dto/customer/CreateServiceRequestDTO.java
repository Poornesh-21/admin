package com.albany.mvc.dto.customer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRequestDTO {
    @NotEmpty(message = "Vehicle type is required")
    private String vehicleType;

    @NotEmpty(message = "Vehicle brand is required")
    private String vehicleBrand;

    @NotEmpty(message = "Vehicle model is required")
    private String vehicleModel;

    @NotNull(message = "Vehicle year is required")
    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2025, message = "Year cannot be in the future")
    private Integer vehicleYear;

    @NotEmpty(message = "Vehicle registration is required")
    private String vehicleRegistration;

    @NotEmpty(message = "Service description is required")
    private String serviceDescription;

    @NotEmpty(message = "At least one service type is required")
    private List<String> requestedServices;

    @NotNull(message = "Preferred date is required")
    @Future(message = "Preferred date must be in the future")
    private LocalDateTime preferredDate;
}