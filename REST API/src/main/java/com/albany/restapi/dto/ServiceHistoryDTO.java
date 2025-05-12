package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceHistoryDTO {
    private Integer trackingId;
    private String status;
    private String workDescription;
    private LocalDateTime timestamp;
    private String updatedBy;
}