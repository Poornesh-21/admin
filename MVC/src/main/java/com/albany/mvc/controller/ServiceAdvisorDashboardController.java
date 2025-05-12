package com.albany.mvc.controller;

import com.albany.mvc.dto.ServiceRequestDto;
import com.albany.mvc.service.ServiceAssignmentService;
import com.albany.mvc.service.ServiceRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/serviceAdvisor")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardController {

    private final ServiceAssignmentService assignmentService;
    private final ServiceRequestService serviceRequestService;

    /**
     * Dashboard page rendering
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        log.info("Accessing service advisor dashboard");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/serviceAdvisor/login?error=session_expired";
        }

        // Add user information to the model
        HttpSession session = request.getSession(false);
        if (session != null) {
            String firstName = (String) session.getAttribute("firstName");
            String lastName = (String) session.getAttribute("lastName");
            if (firstName != null && lastName != null) {
                model.addAttribute("userName", firstName + " " + lastName);
            } else {
                model.addAttribute("userName", "Service Advisor");
            }
        } else {
            model.addAttribute("userName", "Service Advisor");
        }

        return "serviceAdvisor/dashboard";
    }

    /**
     * Gets a valid token from various sources (without Auth header)
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }

    /**
     * Gets a valid token from various sources with Auth header
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            // Store token in session
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", tokenParam);
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Using token from Authorization header");
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                log.debug("Using token from session");
                return sessionToken;
            }
        }

        log.warn("No valid token found from any source");
        return null;
    }
}