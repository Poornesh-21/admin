package com.albany.mvc.controller.customer;

import com.albany.mvc.dto.customer.CreateServiceRequestDTO;
import com.albany.mvc.dto.customer.ServiceRequestDTO;
import com.albany.mvc.dto.customer.UserResponseDTO;
import com.albany.mvc.service.customer.CustomerServiceRequestService;
import com.albany.mvc.util.customer.CustomerJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceController {

    private final CustomerServiceRequestService serviceRequestService;
    private final CustomerJwtUtil jwtUtil;

    @GetMapping("/bookService")
    public String bookServiceForm(HttpSession session, Model model, HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            log.info("No JWT token found in cookies");
            return "redirect:/customer/auth/login";
        }

        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            log.info("No user found in session");
            return "redirect:/customer/auth/login";
        }

        if (!model.containsAttribute("serviceForm")) {
            CreateServiceRequestDTO serviceForm = new CreateServiceRequestDTO();
            model.addAttribute("serviceForm", serviceForm);
        }

        try {
            List<String> serviceTypes = serviceRequestService.getServiceTypes(request);
            List<String> vehicleTypes = serviceRequestService.getVehicleTypes(request);

            model.addAttribute("user", user);
            model.addAttribute("serviceTypes", serviceTypes);
            model.addAttribute("vehicleTypes", vehicleTypes);
            model.addAttribute("modalTitle", "Book a Service");
        } catch (Exception e) {
            log.error("Error loading service or vehicle types: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load service or vehicle types: " + e.getMessage());
        }

        return "customer/bookService";
    }

    @PostMapping("/bookService")
    public String bookService(
            @ModelAttribute("serviceForm") @Valid CreateServiceRequestDTO requestDTO,
            BindingResult result,
            HttpSession session,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            return "redirect:/customer/auth/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the form errors.");
            redirectAttributes.addFlashAttribute("serviceForm", requestDTO);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.serviceForm", result);
            return "redirect:/customer/bookService";
        }
        
        try {
            // Store the returned service request
            ServiceRequestDTO createdRequest = serviceRequestService.createServiceRequest(requestDTO, request);
            // Pass booking details to the view via flash attributes
            redirectAttributes.addFlashAttribute("bookingDetails", createdRequest);
            redirectAttributes.addFlashAttribute("success", "Service request created successfully!");
            log.info("Service request created successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to create service request: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create service request: " + e.getMessage());
            redirectAttributes.addFlashAttribute("serviceForm", requestDTO);
        }
        return "redirect:/customer/bookService";
    }
    
    @GetMapping("/services")
    public String viewServices(HttpSession session, Model model, HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            return "redirect:/customer/auth/login";
        }

        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            return "redirect:/customer/auth/login";
        }

        try {
            List<ServiceRequestDTO> services = serviceRequestService.getUserServiceRequests(request);
            model.addAttribute("services", services);
            model.addAttribute("user", user);
        } catch (Exception e) {
            log.error("Error fetching service requests: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load your service requests: " + e.getMessage());
        }

        return "customer/services";
    }
    
    @GetMapping("/service/{id}")
    public String viewServiceDetails(@PathVariable Long id, HttpSession session, Model model, HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            return "redirect:/customer/auth/login";
        }

        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            return "redirect:/customer/auth/login";
        }

        try {
            ServiceRequestDTO service = serviceRequestService.getServiceRequestById(id, request);
            model.addAttribute("service", service);
            model.addAttribute("user", user);
        } catch (Exception e) {
            log.error("Error fetching service request details: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load service request details: " + e.getMessage());
        }

        return "customer/serviceDetail";
    }
    
    // API endpoint to get service types - for AJAX requests
    @GetMapping("/api/service-types")
    @ResponseBody
    public ResponseEntity<List<String>> getServiceTypes(HttpServletRequest request) {
        try {
            List<String> serviceTypes = serviceRequestService.getServiceTypes(request);
            return ResponseEntity.ok(serviceTypes);
        } catch (Exception e) {
            log.error("Error fetching service types: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // API endpoint to get vehicle types - for AJAX requests
    @GetMapping("/api/vehicle-types")
    @ResponseBody
    public ResponseEntity<List<String>> getVehicleTypes(HttpServletRequest request) {
        try {
            List<String> vehicleTypes = serviceRequestService.getVehicleTypes(request);
            return ResponseEntity.ok(vehicleTypes);
        } catch (Exception e) {
            log.error("Error fetching vehicle types: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}