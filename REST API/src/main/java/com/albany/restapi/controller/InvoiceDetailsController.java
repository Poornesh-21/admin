package com.albany.restapi.controller;

import com.albany.restapi.dto.InvoiceDetailsDTO;
import com.albany.restapi.service.InvoiceDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoice-details")
@RequiredArgsConstructor
@Slf4j
public class InvoiceDetailsController {

    private final InvoiceDetailsService invoiceDetailsService;

    /**
     * Get detailed invoice information for a service request
     */
    @GetMapping("/service-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<InvoiceDetailsDTO> getInvoiceDetails(@PathVariable Integer requestId) {
        log.info("Request to get invoice details for service request ID: {}", requestId);
        
        try {
            InvoiceDetailsDTO invoiceDetails = invoiceDetailsService.getInvoiceDetails(requestId);
            return ResponseEntity.ok(invoiceDetails);
        } catch (Exception e) {
            log.error("Error fetching invoice details for service request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}