package com.albany.mvc.controller.customer;

import com.albany.mvc.dto.customer.JwtResponseDTO;
import com.albany.mvc.dto.customer.RegisterRequestDTO;
import com.albany.mvc.service.customer.CustomerAuthService;
import com.albany.mvc.util.customer.CustomerJwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/customer/auth")
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthController {

    private final CustomerAuthService authService;
    private final CustomerJwtUtil jwtUtil;

    @GetMapping("/login")
    public String loginPage() {
        return "customer/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, HttpSession session) {
        jwtUtil.removeJwtToken(response);
        jwtUtil.clearUserFromSession(session);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @PostMapping("/login/send-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendLoginOtp(@RequestParam String email) {
        try {
            Map<String, Object> response = authService.sendLoginOtp(email);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to your email"
            ));
        } catch (Exception e) {
            log.error("Error sending login OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login/verify-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyLoginOtp(
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
            session.setAttribute("customerAuthenticated", true);
            
            log.info("Customer login successful for: {}", email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful",
                "redirectUrl", "/customer/bookService"
            ));
        } catch (Exception e) {
            log.error("Error verifying login OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/register/send-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendRegistrationOtp(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            Map<String, Object> response = authService.sendRegistrationOtp(registerRequest);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to your email"
            ));
        } catch (Exception e) {
            log.error("Error sending registration OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/register/verify-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyRegistrationOtp(
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
            session.setAttribute("customerAuthenticated", true);
            
            log.info("Customer registration successful for: {}", registerRequest.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Registration successful",
                "redirectUrl", "/customer/bookService"
            ));
        } catch (Exception e) {
            log.error("Error verifying registration OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}