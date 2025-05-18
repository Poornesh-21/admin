package com.backend.controller;

import com.backend.dto.*;
import com.backend.service.AuthService;
import com.backend.service.OtpService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    // Send OTP for login
    @PostMapping("/login/send-otp")
    public ResponseEntity<?> sendLoginOtp(@RequestBody LoginRequestDTO loginRequest) {
        try {
            String response = authService.sendLoginOtp(loginRequest.getEmail());
            
            // For testing purposes only - remove in production
            String otp = otpService.getLatestOtpForEmail(loginRequest.getEmail());
            return ResponseEntity.ok(Map.of(
                "message", response,
                "otp", otp // For testing only - remove in production
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Verify OTP for login
    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> verifyLoginOtp(@RequestBody OtpVerificationDTO otpVerification) {
        try {
            JwtResponseDTO response = authService.verifyLoginOtp(otpVerification);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Send OTP for registration
    @PostMapping("/register/send-otp")
    public ResponseEntity<?> sendRegistrationOtp(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            String response = authService.sendRegistrationOtp(registerRequest);
            
            // For testing purposes only - remove in production
            String otp = otpService.getLatestOtpForEmail(registerRequest.getEmail());
            return ResponseEntity.ok(Map.of(
                "message", response,
                "otp", otp // For testing only - remove in production
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Verify OTP for registration
    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegistrationOtp(
            @RequestBody RegisterRequestDTO registerRequest,
            @RequestParam String otp) {
        try {
            JwtResponseDTO response = authService.verifyRegistrationOtp(registerRequest, otp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
