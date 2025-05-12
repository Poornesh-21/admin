package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialItemDTO {
    private Integer itemId;  // Added this field
    private String name;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
}