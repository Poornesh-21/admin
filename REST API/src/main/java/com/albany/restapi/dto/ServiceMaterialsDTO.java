package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceMaterialsDTO {
    private List<MaterialItemDTO> items;
    private BigDecimal totalMaterialsCost;
    private boolean replaceExisting = true;
    private ServiceBillSummaryDTO currentBill;
}