package com.frontend.controller;

import com.frontend.dto.UserResponseDTO;
import com.frontend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/customer")
public class MembershipController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/membership")
    public String showMembershipPage(Model model, HttpSession session) {
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("razorpayKeyId", "rzp_test_sYi5vRvjFXugtR"); // Replace with actual key
        }
        return "customer/membership";
    }
}