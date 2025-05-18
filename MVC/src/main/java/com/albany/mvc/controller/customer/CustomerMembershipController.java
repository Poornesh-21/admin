package com.albany.mvc.controller.customer;

import com.albany.mvc.dto.customer.UserResponseDTO;
import com.albany.mvc.util.customer.CustomerJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerMembershipController {

    private final CustomerJwtUtil jwtUtil;
    
    @Value("${razorpay.key.id:rzp_test_sYi5vRvjFXugtR}")
    private String razorpayKeyId;

    @GetMapping("/membership")
    public String showMembershipPage(Model model, HttpSession session, HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromCookies(request);
        if (token == null) {
            return "redirect:/customer/auth/login";
        }
        
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        if (user == null) {
            return "redirect:/customer/auth/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        
        return "customer/membership";
    }
}