package com.frontend.controller;

import com.frontend.dto.CreateServiceRequestDTO;
import com.frontend.dto.ServiceRequestDTO;
import com.frontend.dto.UserResponseDTO;
import com.frontend.service.ServiceRequestService;
import com.frontend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class ServiceController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/bookService")
    public String bookServiceForm(HttpSession session, Model model, HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            System.out.println("No JWT token found in cookies");
            return "redirect:/authentication/login";
        }

        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            System.out.println("No user found in session");
            return "redirect:/authentication/login";
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
            return "redirect:/authentication/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the form errors.");
            redirectAttributes.addFlashAttribute("serviceForm", requestDTO);
            return "redirect:/customer/bookService";
        }
        
        try {
            // Store the returned service request
            ServiceRequestDTO createdRequest = serviceRequestService.createServiceRequest(requestDTO, request);
            // Pass booking details to the view via flash attributes
            redirectAttributes.addFlashAttribute("bookingDetails", createdRequest);
            redirectAttributes.addFlashAttribute("success", "Service request created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create service request: " + e.getMessage());
            redirectAttributes.addFlashAttribute("serviceForm", requestDTO);  // Add this to preserve form data
        }
        return "redirect:/customer/bookService";
    }
}