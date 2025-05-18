package com.albany.mvc.controller;

import com.albany.mvc.dto.AuthResponse;
import com.albany.mvc.dto.PasswordChangeDto;
import com.albany.mvc.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PasswordController extends BaseController {

    private final PasswordService passwordService;

    @PostMapping("/serviceAdvisor/api/change-password")
    public ResponseEntity<?> changeServiceAdvisorPassword(
            @RequestBody PasswordChangeDto passwordChangeDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authorization required"));
        }

        try {
            AuthResponse response = passwordService.changePassword(passwordChangeDto, validToken);
            
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", response.getToken());
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Password changed successfully");
            successResponse.put("token", response.getToken());
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/admin/api/change-password")
    public ResponseEntity<?> changeAdminPassword(
            @RequestBody PasswordChangeDto passwordChangeDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authorization required"));
        }

        try {
            AuthResponse response = passwordService.changePassword(passwordChangeDto, validToken);
            
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", response.getToken());
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Password changed successfully");
            successResponse.put("token", response.getToken());
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}