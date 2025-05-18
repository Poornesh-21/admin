package com.frontend.controller;

import com.frontend.dto.JwtResponseDTO;
import com.frontend.dto.RegisterRequestDTO;
import com.frontend.service.AuthService;
import com.frontend.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/authentication")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/login")
    public String loginPage() {
        return "authentication/login"; // Display login page
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, HttpSession session) {
        jwtUtil.removeJwtToken(response);
        jwtUtil.clearUserFromSession(session);
        SecurityContextHolder.clearContext(); // Clear security context
        return "redirect:/"; // Redirect to index.html after logout
    }

    @PostMapping("/login/send-otp")
    @ResponseBody
    public Map<String, Object> sendLoginOtp(@RequestParam String email) {
        try {
            Map<String, Object> response = authService.sendLoginOtp(email);
            return Map.of(
                "success", true,
                "message", "OTP sent successfully to your email"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @PostMapping("/login/verify-otp")
    @ResponseBody
    public Map<String, Object> verifyLoginOtp(
            @RequestParam String email,
            @RequestParam String otp,
            HttpServletResponse response,
            HttpSession session) {
        
        try {
            JwtResponseDTO jwtResponse = authService.verifyLoginOtp(email, otp);
            
            // Store JWT token in cookie
            jwtUtil.storeJwtToken(response, jwtResponse.getToken());
            
            // Store user in session
            jwtUtil.storeUserInSession(session, jwtResponse.getUser());
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    email, 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + jwtResponse.getUser().getRole()))
                )
            );
            
            // Force session saving
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            session.setAttribute("redirectAfterLogin", true); // Add a flag to prevent session loss
            
            System.out.println("Login successful for: " + email);
            System.out.println("User stored in session: " + jwtResponse.getUser());
            System.out.println("Token stored in cookie: " + (jwtResponse.getToken() != null ? "Yes" : "No"));
            System.out.println("Authentication set in SecurityContext: " + 
                (SecurityContextHolder.getContext().getAuthentication() != null ? "Yes" : "No"));
            
            return Map.of(
                "success", true,
                "message", "Login successful",
                "redirectUrl", "/customer/bookService"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }
    
    @PostMapping("/register/send-otp")
    @ResponseBody
    public Map<String, Object> sendRegistrationOtp(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            Map<String, Object> response = authService.sendRegistrationOtp(registerRequest);
            return Map.of(
                "success", true,
                "message", "OTP sent successfully to your email"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }

    @PostMapping("/register/verify-otp")
    @ResponseBody
    public Map<String, Object> verifyRegistrationOtp(
            @RequestBody RegisterRequestDTO registerRequest,
            @RequestParam String otp,
            HttpServletResponse response,
            HttpSession session) {
        
        try {
            JwtResponseDTO jwtResponse = authService.verifyRegistrationOtp(registerRequest, otp);
            
            // Store JWT token in cookie
            jwtUtil.storeJwtToken(response, jwtResponse.getToken());
            
            // Store user in session
            jwtUtil.storeUserInSession(session, jwtResponse.getUser());
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    registerRequest.getEmail(), 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + jwtResponse.getUser().getRole()))
                )
            );
            
            // Force session saving
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            session.setAttribute("redirectAfterLogin", true); // Add a flag to prevent session loss
            
            System.out.println("Registration successful for: " + registerRequest.getEmail());
            System.out.println("User stored in session: " + jwtResponse.getUser());
            
            return Map.of(
                "success", true,
                "message", "Registration successful",
                "redirectUrl", "/customer/bookService"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }
}