package com.backend.controller;

import com.backend.dto.UserResponseDTO;
import com.backend.dto.UserUpdateDTO;
import com.backend.dto.UpdateEmailDTO;
import com.backend.dto.UpdatePhoneDTO;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserResponseDTO user = userService.findByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/update")
    public ResponseEntity<UserResponseDTO> updateUser(@RequestBody UserUpdateDTO updateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserResponseDTO updatedUser = userService.updateUser(email, updateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/send-email-otp")
    public ResponseEntity<Map<String, Object>> sendEmailOtp(@RequestBody UpdateEmailDTO updateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        try {
            userService.sendEmailOtp(currentEmail, updateDTO.getNewEmail());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to your new email"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<UserResponseDTO> verifyEmailOtp(@RequestBody UpdateEmailDTO updateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        try {
            UserResponseDTO updatedUser = userService.updateEmail(currentEmail, updateDTO.getNewEmail(), updateDTO.getOtp());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/send-phone-otp")
    public ResponseEntity<Map<String, Object>> sendPhoneOtp(@RequestBody UpdatePhoneDTO updateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        try {
            userService.sendPhoneOtp(currentEmail, updateDTO.getNewPhone());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to your new phone number"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-phone-otp")
    public ResponseEntity<UserResponseDTO> verifyPhoneOtp(@RequestBody UpdatePhoneDTO updateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        try {
            UserResponseDTO updatedUser = userService.updatePhone(currentEmail, updateDTO.getNewPhone(), updateDTO.getOtp());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}