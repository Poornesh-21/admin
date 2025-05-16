package com.albany.restapi.model;

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
@Table(name = "ServiceTracking")
public class ServiceTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer trackingId;

    private Integer requestId;

    // This field is causing the truncation error
    @Column(length = 255) // Make explicit that it's limited to 255 chars
    private String workDescription;

    private Integer laborMinutes;

    @Column(precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalMaterialCost;

    @Enumerated(EnumType.STRING)
    private ServiceRequest.Status status;

    @Column(updatable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "service_advisor_id")
    private ServiceAdvisorProfile serviceAdvisor;

    // Constants
    private static final int MAX_DESCRIPTION_LENGTH = 250; // Buffer from 255 limit

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Safe setter for work description that ensures the string won't exceed database limits
     */
    public void setWorkDescriptionSafe(String description) {
        if (description == null) {
            this.workDescription = null;
            return;
        }

        // Truncate if longer than max length
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            this.workDescription = description.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
        } else {
            this.workDescription = description;
        }
    }

    /**
     * Safely append additional information to the existing description
     * without exceeding the database field size limit
     */
    public void appendToWorkDescription(String additionalInfo) {
        if (additionalInfo == null || additionalInfo.isEmpty()) {
            return;
        }

        String currentDesc = this.workDescription == null ? "" : this.workDescription;
        String separator = currentDesc.isEmpty() ? "" : "; ";
        String newDesc = currentDesc + separator + additionalInfo;

        // If the new combined description is too long, truncate it
        if (newDesc.length() > MAX_DESCRIPTION_LENGTH) {
            // Strategy: Keep beginning of old + end of new
            int oldPortion = MAX_DESCRIPTION_LENGTH / 3;  // Use 1/3 for old content
            int newPortion = MAX_DESCRIPTION_LENGTH - oldPortion - 6; // Space for "..." and separator

            String trimmedOld = currentDesc.length() <= oldPortion ?
                    currentDesc :
                    currentDesc.substring(0, oldPortion) + "...";

            String trimmedNew = additionalInfo.length() <= newPortion ?
                    additionalInfo :
                    "..." + additionalInfo.substring(additionalInfo.length() - newPortion);

            this.workDescription = trimmedOld + separator + trimmedNew;
        } else {
            this.workDescription = newDesc;
        }
    }
}