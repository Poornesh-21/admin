package com.albany.mvc.controller.customer;

import com.albany.mvc.dto.customer.UserResponseDTO;
import com.albany.mvc.util.customer.CustomerJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CustomerHomeController {

    private final CustomerJwtUtil jwtUtil;

    @GetMapping("/")
    public String home(HttpSession session, Model model, HttpServletRequest request) {
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        
        // Always show index.html first, regardless of login status
        model.addAttribute("isLoggedIn", user != null);
        if (user != null) {
            model.addAttribute("user", user);
        }
        
        return "customer/index"; // Return landing page (index)
    }
    
    @GetMapping("/customer")
    public String customerHome(HttpSession session, Model model, HttpServletRequest request) {
        return "redirect:/";
    }
    
    @GetMapping("/customer/home")
    public String customerHomePage(HttpSession session, Model model, HttpServletRequest request) {
        return "redirect:/";
    }
    
    @GetMapping("/aboutUs")
    public String aboutUs(Model model, HttpSession session) {
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        model.addAttribute("isLoggedIn", user != null);
        if (user != null) {
            model.addAttribute("user", user);
        }
        model.addAttribute("currentPage", "aboutUs");
        return "customer/aboutUs"; 
    }
}