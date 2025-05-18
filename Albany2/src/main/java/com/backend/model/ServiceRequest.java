package com.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "service_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long serviceRequestId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String vehicleType;
    
    @Column(nullable = false)
    private String vehicleBrand;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(nullable = false)
    private String vehicleRegistration;

    @Column(columnDefinition = "TEXT")
    private String serviceDescription;

    @ElementCollection(targetClass = ServiceType.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "requested_service_types", joinColumns = @JoinColumn(name = "service_request_id"))
    @Column(name = "service_type")
    private List<ServiceType> requestedServices;

    @Column(name = "preferred_date", nullable = false)
    private LocalDateTime preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus status = ServiceRequestStatus.RECEIVED;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "admin_id")
    private Long adminId;
    
    @Column(name = "service_advisor_id")
    private Long serviceAdvisorId;

    public enum ServiceRequestStatus {
        RECEIVED,
        DIAGNOSIS,
        REPAIR,
        COMPLETED
    }
}
