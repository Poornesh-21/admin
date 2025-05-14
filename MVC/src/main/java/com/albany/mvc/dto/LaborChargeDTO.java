package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for labor charges in services
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LaborChargeDTO {
    private Integer chargeId;
    private String description;
    private BigDecimal hours;
    private BigDecimal ratePerHour;
    private BigDecimal total;
}