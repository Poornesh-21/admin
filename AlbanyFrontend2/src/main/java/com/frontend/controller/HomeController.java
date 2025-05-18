package com.frontend.controller;

import com.frontend.dto.UserResponseDTO;
import com.frontend.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        UserResponseDTO user = jwtUtil.getUserFromSession(session);
        
        // Always show index.html first, regardless of login status
        model.addAttribute("isLoggedIn", user != null);
        if (user != null) {
            model.addAttribute("user", user);
        }
        
        return "customer/index"; // Return landing page (index)
    }
    
    @GetMapping("/aboutUs")
    public String aboutUs(Model model) {
        // Add model attributes as needed
        model.addAttribute("currentPage", "aboutUs");
        return "customer/aboutUs"; 
    }
}