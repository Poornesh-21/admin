package com.albany.restapi.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "labor_charges")
public class LaborCharge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chargeId;
    
    private Integer requestId;
    
    private String description;
    
    private BigDecimal hours;
    
    private BigDecimal ratePerHour;
    
    private BigDecimal total;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}