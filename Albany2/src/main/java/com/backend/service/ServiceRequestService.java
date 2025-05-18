package com.backend.service;

import com.backend.dto.CreateServiceRequestDTO;
import com.backend.dto.ServiceRequestDTO;
import com.backend.model.ServiceRequest;
import com.backend.model.ServiceType;
import com.backend.model.User;
import com.backend.repository.ServiceRequestRepository;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public ServiceRequestDTO createServiceRequest(String userEmail, CreateServiceRequestDTO requestDTO) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setUser(user);
        serviceRequest.setVehicleType(requestDTO.getVehicleType());
        serviceRequest.setVehicleBrand(requestDTO.getVehicleBrand());
        serviceRequest.setVehicleModel(requestDTO.getVehicleModel());
        serviceRequest.setVehicleYear(requestDTO.getVehicleYear());
        serviceRequest.setVehicleRegistration(requestDTO.getVehicleRegistration());
        serviceRequest.setServiceDescription(requestDTO.getServiceDescription());
        List<ServiceType> serviceTypes = requestDTO.getRequestedServices().stream()
                .map(ServiceType::valueOf)
                .collect(Collectors.toList());
        serviceRequest.setRequestedServices(serviceTypes);
        serviceRequest.setPreferredDate(requestDTO.getPreferredDate());
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.RECEIVED);
        serviceRequest.setCreatedAt(LocalDateTime.now());
        serviceRequest.setUpdatedAt(LocalDateTime.now());

     // In the createServiceRequest method:
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);

        // Send confirmation email with more robust error handling
        String serviceDate = requestDTO.getPreferredDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String serviceTypesStr = serviceTypes.stream().map(Enum::name).collect(Collectors.joining(", "));
        try {
            emailService.sendBookingConfirmationEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName(),
                    savedRequest.getServiceRequestId().toString(),
                    serviceDate,
                    serviceTypesStr
            );
        } catch (Exception e) {
            // Log the error but don't fail the request creation
            System.err.println("Failed to send confirmation email: " + e.getMessage());
            e.printStackTrace(); // to get full stack trace in logs
        }

        return convertToDTO(savedRequest);
    }

    public List<ServiceRequestDTO> getUserServiceRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        List<ServiceRequest> requests = serviceRequestRepository.findByUserOrderByCreatedAtDesc(user);
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServiceRequestDTO getServiceRequestById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found with id: " + id));

        if (!request.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: Service request does not belong to requesting user");
        }

        return convertToDTO(request);
    }

    private ServiceRequestDTO convertToDTO(ServiceRequest serviceRequest) {
        return new ServiceRequestDTO(
                serviceRequest.getServiceRequestId(),
                serviceRequest.getUser().getId(),
                serviceRequest.getVehicleType(),
                serviceRequest.getVehicleBrand(),
                serviceRequest.getVehicleModel(),
                serviceRequest.getVehicleYear(),
                serviceRequest.getVehicleRegistration(),
                serviceRequest.getServiceDescription(),
                serviceRequest.getRequestedServices(),
                serviceRequest.getPreferredDate(),
                serviceRequest.getStatus().name(),
                serviceRequest.getCreatedAt(),
                serviceRequest.getUpdatedAt(),
                serviceRequest.getAdminId(),
                serviceRequest.getServiceAdvisorId()
        );
    }
}