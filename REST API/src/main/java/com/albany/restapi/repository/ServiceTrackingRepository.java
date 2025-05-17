package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceTrackingRepository extends JpaRepository<ServiceTracking, Integer> {

    /**
     * Find all service tracking entries for a request ID
     */
    List<ServiceTracking> findByRequestId(Integer requestId);

    /**
     * Find service tracking entries by status
     */
    List<ServiceTracking> findByStatus(com.albany.restapi.model.ServiceRequest.Status status);

    /**
     * Calculate total labor cost for a service request
     */
    @Query("SELECT COALESCE(SUM(st.laborCost), 0) FROM ServiceTracking st WHERE st.requestId = :requestId")
    Optional<BigDecimal> findTotalLaborCostByRequestId(@Param("requestId") Integer requestId);

    /**
     * Calculate total labor minutes for a service request
     */
    @Query("SELECT COALESCE(SUM(st.laborMinutes), 0) FROM ServiceTracking st WHERE st.requestId = :requestId")
    Integer sumLaborMinutesByRequestId(@Param("requestId") Integer requestId);

    /**
     * Find the date of the last update with a particular status
     */
    @Query("SELECT CAST(MAX(st.updatedAt) AS LocalDate) FROM ServiceTracking st WHERE st.requestId = :requestId AND st.status = :status")
    Optional<LocalDate> findLastUpdateDateByRequestIdAndStatus(
            @Param("requestId") Integer requestId,
            @Param("status") com.albany.restapi.model.ServiceRequest.Status status);

    /**
     * Find service tracking entries that have labor costs (for labor charges)
     */
    List<ServiceTracking> findByRequestIdAndLaborCostNotNull(Integer requestId);

    /**
     * Find service tracking entries for a request with material costs
     */
    @Query("SELECT st FROM ServiceTracking st WHERE st.requestId = :requestId AND st.totalMaterialCost IS NOT NULL AND st.totalMaterialCost > 0")
    List<ServiceTracking> findByRequestIdWithMaterialCosts(@Param("requestId") Integer requestId);

    /**
     * Count services by status
     */
    long countByStatus(com.albany.restapi.model.ServiceRequest.Status status);
}