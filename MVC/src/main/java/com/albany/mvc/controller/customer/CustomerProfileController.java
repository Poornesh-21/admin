package com.albany.mvc.controller.customer;

import com.albany.mvc.dto.customer.UserResponseDTO;
import com.albany.mvc.dto.customer.UserUpdateDTO;
import com.albany.mvc.util.customer.CustomerJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileController {

    private final CustomerJwtUtil jwtUtil;

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model, HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            return "redirect:/customer/auth/login";
        }
        
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            return "redirect:/customer/auth/login";
        }
        
        model.addAttribute("user", user);
        
        return "customer/profile";
    }
    
    // This would be implemented further to interact with the backend API
    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody UserUpdateDTO updateRequest,
            HttpSession session,
            HttpServletRequest request) {
        
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        // This would call the backend API to update the profile
        // For now, we'll just update the session data
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setStreet(updateRequest.getStreet());
        user.setCity(updateRequest.getCity());
        user.setState(updateRequest.getState());
        user.setPostalCode(updateRequest.getPostalCode());
        
        jwtUtil.storeUserInSession(session, user);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile updated successfully"
        ));
    }
}