//package com.backend.controller;
//
//import com.backend.dto.CreateServiceRequestDTO;
//import com.backend.dto.ServiceRequestDTO;
//import com.backend.model.ServiceType;
//import com.backend.model.VehicleType;
//import com.backend.service.ServiceRequestService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/service-requests")
//public class ServiceRequestController {
//
//    @Autowired
//    private ServiceRequestService serviceRequestService;
//
//    @PostMapping
//    public ResponseEntity<?> createServiceRequest(@RequestBody CreateServiceRequestDTO requestDTO) {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String email = authentication.getName();
//            ServiceRequestDTO response = serviceRequestService.createServiceRequest(email, requestDTO);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @GetMapping
//    public ResponseEntity<?> getUserServiceRequests() {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String email = authentication.getName();
//            List<ServiceRequestDTO> requests = serviceRequestService.getUserServiceRequests(email);
//            return ResponseEntity.ok(requests);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<?> getServiceRequestById(@PathVariable Long id) {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String email = authentication.getName();
//            ServiceRequestDTO request = serviceRequestService.getServiceRequestById(id, email);
//            return ResponseEntity.ok(request);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @GetMapping("/service-types")
//    public ResponseEntity<?> getServiceTypes() {
//        try {
//            List<String> serviceTypes = Arrays.stream(ServiceType.values())
//                    .map(Enum::name)
//                    .collect(Collectors.toList());
//            return ResponseEntity.ok(serviceTypes);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @GetMapping("/vehicle-types")
//    public ResponseEntity<?> getVehicleTypes() {
//        try {
//            List<String> vehicleTypes = Arrays.stream(VehicleType.values())
//                    .map(Enum::name)
//                    .collect(Collectors.toList());
//            return ResponseEntity.ok(vehicleTypes);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//}

package com.backend.controller;

import com.backend.dto.CreateServiceRequestDTO;
import com.backend.dto.ServiceRequestDTO;
import com.backend.model.ServiceType;
import com.backend.model.VehicleType;
import com.backend.service.ServiceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @PostMapping
    public ResponseEntity<?> createServiceRequest(@RequestBody CreateServiceRequestDTO requestDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            String email = authentication.getName();
            ServiceRequestDTO response = serviceRequestService.createServiceRequest(email, requestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserServiceRequests() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            String email = authentication.getName();
            List<ServiceRequestDTO> requests = serviceRequestService.getUserServiceRequests(email);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceRequestById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            String email = authentication.getName();
            ServiceRequestDTO request = serviceRequestService.getServiceRequestById(id, email);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/service-types")
    public ResponseEntity<?> getServiceTypes() {
        try {
            List<String> serviceTypes = Arrays.stream(ServiceType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(serviceTypes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/vehicle-types")
    public ResponseEntity<?> getVehicleTypes() {
        try {
            List<String> vehicleTypes = Arrays.stream(VehicleType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(vehicleTypes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}